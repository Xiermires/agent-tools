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

import java.io.IOException;
import java.io.Serializable;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class AgentTools
{
    static final String REMOTE = "remote";
    static final String LOCAL = "local";

    private static Instrumentor theInstrumentor = null;

    private AgentTools()
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
        checkStarted();
        theInstrumentor.addTransformer(transformer);
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
        checkStarted();
        theInstrumentor.removeTransformer(transformer);
    }

    /**
     * Removes any instrumentation from the given classes.
     * 
     * @param classNames
     *            a list of classes that shall remove any instrumented code
     */
    public static void reset(String... classNames) throws AgentLoadingException
    {
        checkStarted();
        theInstrumentor.reset(classNames);
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
        checkStarted();
        theInstrumentor.retransform(transformer, classNames);
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
        checkStarted();
        _redefine(theInstrumentor, classNames);
    }

    public static long getObjectSize(Object o)
    {
        checkStarted();
        return theInstrumentor.getObjectSize(o);
    }

    /**
     * Remote version of {@link #add(ClassFileTransformer)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void add(int pid, ClassFileTransformer transformer) throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.addTransformer(transformer);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    /**
     * Remote version of {@link #remove(ClassFileTransformer)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void remove(int pid, SCFT transformer) throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.removeTransformer(transformer);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    /**
     * Remote version of {@link #reset(String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void reset(int pid, String... classNames) throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.reset(classNames);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    /**
     * Remote version of {@link #retransform(ClassFileTransformer, String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void retransform(int pid, ClassFileTransformer transformer, String... classNames)
            throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.retransform(transformer, classNames);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    /**
     * Remote version of {@link #redefine(ClassFileTransformer, String...)}
     */
    public static <SCFT extends ClassFileTransformer & Serializable> void redefine(int pid, ClassFileTransformer transformer, String... classNames)
            throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        _redefine(proxyConn.proxy, classNames);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    /**
     * Loads a jar file in a remote VM.
     * <p>
     * This is required whenever the remote VM hasn't got the ClassFileTransformer or any classes used by this.
     * <p>
     * For instance, let's assume we have implemented a Tracer implementation of the ClassFileTransformer using a byte code library, and the remote VM hasn't either the
     * Tracer nor the byte code library.
     * <p>
     * If we try to {@link #add(int, ClassFileTransformer)} it will fail because the remote VM can't load the classes (they don't exist). Hence, they must be loaded
     * first.
     * <p>
     * To create jar files, the {@link ClassTools} class provides a couple of helper methods:
     * <ul>
     * <li>{@link ClassTools#createTemporaryJar(String, java.util.jar.Manifest, String...)} creates a jar file including the provided classes.
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
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.appendToSystemClassLoader(jarName, jarBytes);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    static void initialize(Instrumentor instrumentor)
    {
        theInstrumentor = instrumentor;
    }

    private static void _redefine(Instrumentation theInstrumentor, String... classNames) throws AgentLoadingException
    {
        try
        {
            theInstrumentor.redefineClasses(ClassTools.getClassDefinition(classNames).toArray(new ClassDefinition[0]));
        }
        catch (ClassNotFoundException | URISyntaxException | UnmodifiableClassException | IOException e)
        {
            throw new AgentLoadingException(String.format("All or some of the following classes couldn't be redefined { %s }.", Arrays.asList(classNames).toString()), e);
        }
    }
    
    private static void checkStarted()
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
    }

    private static void start() throws AgentLoadingException
    {
        VirtualMachine vm;
        try
        {
            vm = VirtualMachine.attach(getPid());
            vm.loadAgent(AgentBootstrap.createBootstrapJar().getAbsolutePath(), LOCAL);
        }
        catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException | ClassNotFoundException | URISyntaxException e)
        {
            throw new AgentLoadingException("Unable to start the agent.", e);
        }
    }

    private static void startRemote(int pid)
    {
        VirtualMachine vm;
        try
        {
            vm = VirtualMachine.attach(String.valueOf(pid));
            vm.loadAgent(AgentBootstrap.createRemoteBootstrapJar().getAbsolutePath(), REMOTE);
        }
        catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException | URISyntaxException | ClassNotFoundException e)
        {
            throw new AgentLoadingException(String.format("Unable to start the remote agent in pid '%s'.", pid), e);
        }
    }

    private static <T> ProxyConnection<T> getProxy(int pid, Class<T> clazz)
    {
        JMXConnector connector;
        try
        {
            connector = JMXUtils.connect(VirtualMachine.attach(String.valueOf(pid)));
            final MBeanServerConnection server = connector.getMBeanServerConnection();
            final ObjectName on = new ObjectName("agenttools:service=AgentLoaderMBean");

            if (!server.isRegistered(on))
            {
                startRemote(pid);
                assert server.isRegistered(on);
            }

            return new ProxyConnection<>(connector, JMX.newMBeanProxy(server, on, clazz));

        }
        catch (IOException | AgentLoadException | AgentInitializationException | AttachNotSupportedException | MalformedObjectNameException e)
        {
            throw new AgentLoadingException(String.format("Unable to add the transformer in pid '%s'.", pid), e);
        }
    }

    private static String getPid()
    {
        final String os = System.getProperty("os.name").toLowerCase();
        try
        {
            if (os.contains("win"))
            {
                return String.valueOf(Kernel32.INSTANCE.GetCurrentProcessId());
            }
            else
            {
                final CLibrary clib = (CLibrary) Native.loadLibrary("c", CLibrary.class);
                return String.valueOf(clib.getpid());
            }
        }
        catch (Exception e)
        {
            return fallbackPid();
        }
    }

    // The name of the JVM can be arbitrary, but it happens to hold the process id in most JVMs.
    private static String fallbackPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }

    private static interface CLibrary extends Library
    {
        int getpid();
    }

    static class ProxyConnection<T>
    {
        JMXConnector conn;
        T proxy;

        ProxyConnection(JMXConnector conn, T proxy)
        {
            this.conn = conn;
            this.proxy = proxy;
        }
    }
}
