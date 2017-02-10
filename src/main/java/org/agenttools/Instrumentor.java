/*******************************************************************************
 * Copyright (c) 2017, Xavier Miret Andres <xavier.mires@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any 
 * purpose with or without fee is hereby granted, provided that the above 
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES 
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALLIMPLIED WARRANTIES OF 
 * MERCHANTABILITY  AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR 
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES 
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN 
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF 
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class Instrumentor implements InstrumentorMBean
{
    private final Instrumentation instrumentation;

    Instrumentor(Instrumentation instrumentation)
    {
        this.instrumentation = instrumentation;
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer)
    {
        instrumentation.addTransformer(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer)
    {
        return instrumentation.removeTransformer(transformer);
    }

    @Override
    public void reset(String... classNames)
    {
        retransform(new ResetChangesTransformer(), classNames);
    }

    @Override
    public void retransform(ClassFileTransformer transformer, String... classNames)
    {
        instrumentation.addTransformer(new FilteredClassFileTransformer(transformer, in(classNames)), true);
        try
        {
            instrumentation.retransformClasses(ClassTools.getClass(classNames).toArray(new Class[0]));
        }
        catch (Exception e)
        {
            throw new AgentLoadingException(String.format("All or some of the following classes couldn't be transformed { %s }.", Arrays.asList(classNames).toString()), e);
        }
        finally
        {
            instrumentation.removeTransformer(transformer);
        }
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException
    {
        instrumentation.redefineClasses(definitions);
    }
    
    private Predicate<String> in(String[] classNames)
    {
        final Set<String> set = Arrays.asList(classNames).stream().map(x -> x.replace('.', '/')).collect(Collectors.toSet());
        return cn -> set.contains(cn);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform)
    {
        instrumentation.addTransformer(transformer, canRetransform);
    }

    @Override
    public boolean isRetransformClassesSupported()
    {
        return instrumentation.isRetransformClassesSupported();
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException
    {
        instrumentation.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported()
    {
        return instrumentation.isRedefineClassesSupported();
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass)
    {
        return instrumentation.isModifiableClass(theClass);
    }

    @Override
    public Class<?>[] getAllLoadedClasses()
    {
        return instrumentation.getAllLoadedClasses();
    }

    @Override
    public Class<?>[] getInitiatedClasses(ClassLoader loader)
    {
        return instrumentation.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize)
    {
        return instrumentation.getObjectSize(objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile)
    {
        instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile)
    {
        instrumentation.appendToSystemClassLoaderSearch(jarfile);
    }

    @Override
    public boolean isNativeMethodPrefixSupported()
    {
        return instrumentation.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix)
    {
        instrumentation.setNativeMethodPrefix(transformer, prefix);
    }

    @Override
    public void appendToSystemClassLoader(String jarName, byte[] jarBytes)
    {
        instrumentation.appendToSystemClassLoaderSearch(getJarFile(jarName, jarBytes));
    }

    @Override
    public void appendToBootstrapClassLoader(String jarName, byte[] jarBytes)
    {
        instrumentation.appendToBootstrapClassLoaderSearch(getJarFile(jarName, jarBytes));
    }
    
    private static JarFile getJarFile(String jarName, byte[] jarBytes)
    {
        if (jarName != null && jarBytes != null)
        {
            File tmp = null;
            try
            {
                tmp = File.createTempFile(jarName, ".jar");
                Files.write(Paths.get(tmp.toURI()), jarBytes);
                return new JarFile(tmp);
            }
            catch (IOException e)
            {
                throw new AgentLoadingException(String.format("Unable to load the provided jar { %s }.", jarName.concat(".jar")), e);
            }
            finally
            {
                tmp.deleteOnExit();
            }
        }
        return null;
    }
}
