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
import java.lang.instrument.ClassFileTransformer;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
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
    private static Instrumentor theInstrumentor = null;

    private AgentTools()
    {
    }

    public static void load(ClassFileTransformer transformer) throws AgentLoadingException
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
        theInstrumentor.load(transformer);
    }

    public static void remove(ClassFileTransformer transformer) throws AgentLoadingException
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
        theInstrumentor.remove(transformer);
    }

    public static void reset(String... classNames) throws AgentLoadingException
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
        theInstrumentor.reset(classNames);
    }

    public static void retransform(ClassFileTransformer transformer, String... classNames) throws AgentLoadingException
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
        theInstrumentor.retransform(transformer, classNames);
    }

    public static void redefine(String... classNames) throws AgentLoadingException
    {
        if (Objects.isNull(theInstrumentor))
        {
            start();
        }
        theInstrumentor.redefine(classNames);
    }

    public static <SCFT extends ClassFileTransformer & Serializable> void load(SCFT transformer, int pid) throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.load(transformer);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

    public static <SCFT extends ClassFileTransformer & Serializable> void remove(SCFT transformer, int pid) throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.remove(transformer);
        try
        {
            proxyConn.conn.close();
        }
        catch (IOException e)
        {
            throw new AgentLoadingException("Unable to close the JMXConnection", e);
        }
    }

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

    public static <SCFT extends ClassFileTransformer & Serializable> void redefine(int pid, ClassFileTransformer transformer, String... classNames)
            throws AgentLoadingException
    {
        final ProxyConnection<InstrumentorMBean> proxyConn = getProxy(pid, InstrumentorMBean.class);
        proxyConn.proxy.redefine(classNames);
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

    private static void start() throws AgentLoadingException
    {
        VirtualMachine vm;
        try
        {
            vm = VirtualMachine.attach(getPid());
            vm.loadAgent(AgentBootstrap.createBootstrapJar().getAbsolutePath());
        }
        catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException e)
        {
            throw new AgentLoadingException("Unable to start the agent.", e);
        }
    }

    private static void startRemote(int pid)
    {
        if (String.valueOf(pid).equals(getPid())) // local
        {
            start();
        }

        VirtualMachine vm;
        try
        {
            vm = VirtualMachine.attach(String.valueOf(pid));
            vm.loadAgent(AgentBootstrap.createRemoteBootstrapJar().getAbsolutePath());
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
