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

import static org.agenttools.AgentBootstrap.getInstrumentor;
import static org.agenttools.AgentBootstrap.getInstrumentorProxy;

import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;

import org.agenttools.AgentBootstrap.ProxyConnection;

public class Agents
{
    private Agents()
    {
    }

    /**
     * Loads a transformer that will instrument all loaded classes from now on.
     * <p>
     * Previously loaded classes remain unaffected.
     * 
     * @param transformer
     *            a class file transformer
     */
    public static void add(ClassFileTransformer transformer) throws AgentLoadingException
    {
        getInstrumentor().addTransformer(transformer);
    }

    /**
     * Removes a transformer that was instrumenting loaded classes until on.
     * <p>
     * Previously loaded classes remain instrumented.
     * 
     * @param transformer
     *            a class file transformer
     */
    public static void remove(ClassFileTransformer transformer) throws AgentLoadingException
    {
        getInstrumentor().removeTransformer(transformer);
    }

    /**
     * Removes any instrumentation from the given classes.
     * 
     * @param classNames
     *            a list of classes that shall remove any instrumented code
     */
    public static void reset(String... classNames) throws AgentLoadingException
    {
        getInstrumentor().reset(classNames);
    }

    /**
     * Re-transforms classes using the given transformer.
     * <p>
     * This action is independent whenever classes have been loaded or not.
     * 
     * @param transformer
     *            a class file transformer
     * @param classNames
     *            a list of classes to instrument
     */
    public static void retransform(ClassFileTransformer transformer, String... classNames) throws AgentLoadingException
    {
        getInstrumentor().retransform(transformer, classNames);
    }

    /**
     * Re-defines already loaded classes using the currently loaded transformers.
     * 
     * @param transformer
     *            a class file transformer
     * @param classNames
     *            a list of classes to instrument
     */
    public static void redefine(String... classNames) throws AgentLoadingException
    {
        getInstrumentor().redefine(classNames);
    }

    public static long getObjectSize(Object o)
    {
        return getInstrumentor().getObjectSize(o);
    }

    /**
     * Remote version of {@link #add(ClassFileTransformer)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void add(int pid, ClassFileTransformer transformer)
            throws AgentLoadingException
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.addTransformer(transformer);
        }
    }

    /**
     * Remote version of {@link #remove(ClassFileTransformer)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void remove(int pid, SCFT transformer)
            throws AgentLoadingException
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.removeTransformer(transformer);
        }
    }

    /**
     * Remote version of {@link #reset(String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void reset(int pid, String... classNames)
            throws AgentLoadingException
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.reset(classNames);
        }
    }

    /**
     * Remote version of {@link #retransform(ClassFileTransformer, String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void retransform(int pid, ClassFileTransformer transformer,
            String... classNames) throws AgentLoadingException
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.retransform(transformer, classNames);
        }
    }

    /**
     * Remote version of {@link #redefine(ClassFileTransformer, String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void redefine(int pid, ClassFileTransformer transformer,
            String... classNames) throws AgentLoadingException
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.redefine(classNames);
        }
    }

    /**
     * Loads a jar file in a remote VM.
     * <p>
     * This is required whenever the remote VM hasn't got the ClassFileTransformer or any classes
     * used by this.
     * <p>
     * For instance, let's assume we have implemented a Tracer implementation of the
     * ClassFileTransformer using a byte code library, and the remote VM hasn't either the Tracer
     * nor the byte code library.
     * <p>
     * If we try to {@link #add(int, ClassFileTransformer)} it will fail because the remote VM can't
     * load the classes (they don't exist). Hence, they must be loaded first.
     * <p>
     * To create jar files, the {@link ClassTools} class provides a couple of helper methods:
     * <ul>
     * <li>{@link ClassTools#createTemporaryJar(String, java.util.jar.Manifest, String...)} creates
     * a jar file including the provided classes.
     * <li>{@link ClassTools#findJarOf(Class)} finds the jar in which a library class is included.
     * </ul>
     * 
     * @param pid
     *            the remote process id
     * @param jarName
     *            an identification name for the generated / loaded jar
     * @param jarBytes
     *            the jar file as a byte array
     */
    public static void loadJar(int pid, String jarName, byte[] jarBytes)
    {
        try (ProxyConnection<InstrumentorMBean> proxyConn = getInstrumentorProxy(pid))
        {
            proxyConn.proxy.appendToSystemClassLoader(jarName, jarBytes);
        }
    }

}
