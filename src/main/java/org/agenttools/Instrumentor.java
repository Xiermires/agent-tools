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
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Instrumentor implements InstrumentorMBean
{
    private final Instrumentation instrumentation;

    Instrumentor(Instrumentation instrumentation)
    {
        this.instrumentation = instrumentation;
    }

    @Override
    public void load(ClassFileTransformer transformer)
    {
        instrumentation.addTransformer(transformer);
    }

    @Override
    public void remove(ClassFileTransformer transformer)
    {
        instrumentation.removeTransformer(transformer);
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
    public void redefine(String... classNames)
    {
        try
        {
            instrumentation.redefineClasses(ClassTools.getClassDefinition(classNames).toArray(new ClassDefinition[0]));
        }
        catch (Exception e)
        {
            throw new AgentLoadingException(String.format("All or some of the following classes couldn't be redefined { %s }.", Arrays.asList(classNames).toString()), e);
        }
    }

    @Override
    public void loadJar(String jarName, byte[] jarBytes)
    {
        if (jarName != null && jarBytes != null)
        {
            File tmp = null;
            try
            {
                tmp = File.createTempFile(jarName, ".jar");
                Files.write(Paths.get(tmp.toURI()), jarBytes);

                // hack the SystemClassLoader into an URLClassLoader we can load stuff with.
                final URL url = tmp.toURI().toURL();
                final URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, url);
                method.setAccessible(false);
            }
            catch (Exception e)
            {
                throw new AgentLoadingException(String.format("Unable to load the provided jar { %s }.", jarName.concat(".jar")), e);
            }
            finally
            {
                tmp.deleteOnExit();
            }
        }
    }

    private Predicate<String> in(String[] classNames)
    {
        final Set<String> set = Arrays.asList(classNames).stream().map(x -> x.replace('.', '/')).collect(Collectors.toSet());
        return cn -> set.contains(cn);
    }
}
