/*******************************************************************************
 * Copyright (c) 2017, Xavier Miret Andres <xavier.mires@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package org.agenttools;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Instrumentor implements InstrumentorMBean {

    private static final Logger log = Logger.getLogger(Agents.class.getName());

    private final Instrumentation instrumentation;

    Instrumentor(Instrumentation instrumentation) {
	this.instrumentation = instrumentation;
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
	instrumentation.addTransformer(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
	return instrumentation.removeTransformer(transformer);
    }

    @Override
    public void reset(String... classNames) {
	retransform(reset, classNames);
    }

    @Override
    public void retransform(ClassFileTransformer transformer, String... classNames) throws AgentLoadingException {
	instrumentation.addTransformer(new FilteredClassFileTransformer(transformer, in(classNames)), true);
	try {
	    // Load classes which haven't been loaded. That applies the transformation.
	    final Set<String> loaded = loadIfUnloaded(classNames);

	    // Re-transform the remaining
	    final Set<String> remaining = new LinkedHashSet<>(Arrays.asList(classNames)).stream()
		    .filter(f -> !loaded.contains(f)).collect(Collectors.toSet());

	    if (!remaining.isEmpty()) {
		instrumentation.retransformClasses(ClassTools.getClass(remaining.toArray(new String[remaining.size()]))
			.toArray(new Class[0]));
	    }
	} catch (Exception e) {
	    throw new AgentLoadingException(String.format(
		    "All or some of the following classes couldn't be transformed { %s }.", Arrays.asList(classNames)
			    .toString()), e);
	} finally {
	    instrumentation.removeTransformer(transformer);
	}
    }

    private Set<String> loadIfUnloaded(String[] classNames) throws ClassNotFoundException {
	final Set<String> allLoadedClasses = Arrays.asList(instrumentation.getAllLoadedClasses()).stream()
		.map(c -> c.getName()).collect(Collectors.toSet());

	final Set<String> notLoaded = new LinkedHashSet<>(Arrays.asList(classNames)).stream()
		.filter(f -> !allLoadedClasses.contains(f)).collect(Collectors.toSet());

	// load them
	ClassTools.getClass(notLoaded.toArray(new String[notLoaded.size()]));
	return notLoaded;
    }

    @Override
    public void redefine(String className, byte[] bytes) throws AgentLoadingException {
	try {
	    redefineClasses(new ClassDefinition(Class.forName(className), bytes));
	} catch (Exception e) {
	    throw new AgentLoadingException(String.format("The following class couldn't be redefined { %s }.",
		    className), e);
	}
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
	    UnmodifiableClassException {
	for (ClassDefinition definition : definitions) {
	    retransform(redefine(definition.getDefinitionClassFile()), definition.getDefinitionClass().getName());
	}
    }

    private Predicate<String> in(String[] classNames) {
	final Set<String> set = Arrays.asList(classNames).stream().map(x -> x.replace('.', '/'))
		.collect(Collectors.toSet());
	return cn -> set.contains(cn);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
	instrumentation.addTransformer(transformer, canRetransform);
    }

    @Override
    public boolean isRetransformClassesSupported() {
	return instrumentation.isRetransformClassesSupported();
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
	instrumentation.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
	return instrumentation.isRedefineClassesSupported();
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
	return instrumentation.isModifiableClass(theClass);
    }

    @Override
    public Class<?>[] getAllLoadedClasses() {
	return instrumentation.getAllLoadedClasses();
    }

    @Override
    public Class<?>[] getInitiatedClasses(ClassLoader loader) {
	return instrumentation.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
	return instrumentation.getObjectSize(objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
	instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
	instrumentation.appendToSystemClassLoaderSearch(jarfile);
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
	return instrumentation.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
	instrumentation.setNativeMethodPrefix(transformer, prefix);
    }

    @Override
    public void appendToSystemClassLoader(String jarName, byte[] jarBytes) {
	instrumentation.appendToSystemClassLoaderSearch(getJarFile(jarName, jarBytes));
    }

    @Override
    public void appendToBootstrapClassLoader(String jarName, byte[] jarBytes) {
	instrumentation.appendToBootstrapClassLoaderSearch(getJarFile(jarName, jarBytes));
    }

    private static JarFile getJarFile(String jarName, byte[] jarBytes) {
	if (jarName != null && jarBytes != null) {
	    File tmp = null;
	    try {
		tmp = File.createTempFile(jarName, ".jar");
		Files.write(Paths.get(tmp.toURI()), jarBytes);
		return new JarFile(tmp);
	    } catch (IOException e) {
		throw new AgentLoadingException(String.format("Unable to load the provided jar { %s }.",
			jarName.concat(".jar")), e);
	    } finally {
		tmp.deleteOnExit();
	    }
	}
	return null;
    }

    private final static ClassFileTransformer reset = (cl, n, c, p, bytes) -> {
	try {
	    return ClassTools.getClassBytes(n);
	} catch (Throwable e) {
	    log.log(Level.FINE, String.format("Unable to revert instrumented changes in the class '%s'.", n), e);
	}
	return bytes;
    };

    private final static ClassFileTransformer redefine(final byte[] redefinition) {
	return (cl, n, c, p, bytes) -> redefinition;
    }
}
