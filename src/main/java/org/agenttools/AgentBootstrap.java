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
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class AgentBootstrap
{
    static final String REMOTE = "remote";
    static final String LOCAL = "local";
    
    public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception
    {
        agentmain(agentArguments, instrumentation);
    }

    public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception
    {
        instrumentor = new Instrumentor(instrumentation);
        if (REMOTE.equals(agentArguments))
        {
            startMBean(instrumentor);
        }
    }
    
    private static void startMBean(Instrumentor instr)
    {
        try
        {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("agenttools:service=AgentLoaderMBean");
            server.registerMBean(instr, name);
        }
        catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e)
        {
            throw new AgentLoadingException("Unable to register local agent as an mbean.", e);
        }
    }

    private static Instrumentor instrumentor = null;
    
    static InstrumentorMBean getInstrumentor() {
        if (instrumentor == null) {
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
        return instrumentor;
    }
    
    static ProxyConnection<InstrumentorMBean> getInstrumentorProxy(int pid)
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

            return new ProxyConnection<>(connector, JMX.newMBeanProxy(server, on, InstrumentorMBean.class));

        }
        catch (IOException | AgentLoadException | AgentInitializationException | AttachNotSupportedException | MalformedObjectNameException e)
        {
            throw new AgentLoadingException(String.format("Unable to add the transformer in pid '%s'.", pid), e);
        }
    }

    private static File createBootstrapJar() throws IOException, ClassNotFoundException, URISyntaxException
    {
        final File bootstrapJar = ClassTools.createTemporaryJar("bootstrap", createManifest());
        bootstrapJar.deleteOnExit();
        return bootstrapJar;
    }

    // visible for testing
    static File createRemoteBootstrapJar() throws IOException, URISyntaxException, ClassNotFoundException
    {
        final String[] jarClasses = new String[] { //
                AgentBootstrap.class.getName(), //
                AgentLoadingException.class.getName(), //
                Agents.class.getName(), //
                ClassTools.class.getName(), //
                FilteredClassFileTransformer.class.getName(), //
                Instrumentor.class.getName(), //
                InstrumentorMBean.class.getName(),
                JMXUtils.class.getName(),
                ResetChangesTransformer.class.getName()
        };
        
        final File bootstrapJar = ClassTools.createTemporaryJar("bootstrap", createManifest(), jarClasses);
        bootstrapJar.deleteOnExit();
        return bootstrapJar;
    }
    
    private static Manifest createManifest()
    {
        final Manifest m = new Manifest();

        m.getMainAttributes().putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putIfAbsent(new Name("Premain-Class"), AgentBootstrap.class.getName()); // test only, not needed but doesn't hurt
        m.getMainAttributes().putIfAbsent(new Name("Agent-Class"), AgentBootstrap.class.getName());
        m.getMainAttributes().putIfAbsent(new Name("Can-Redefine-Classes"), Boolean.TRUE.toString());
        m.getMainAttributes().putIfAbsent(new Name("Can-Retransform-Classes"), Boolean.TRUE.toString());
        m.getMainAttributes().putIfAbsent(new Name("Can-Set-Native-Method-Prefix"), Boolean.TRUE.toString());

        return m;
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

    private static interface CLibrary extends Library
    {
        int getpid();
    }
    
    static class ProxyConnection<T> implements AutoCloseable
    {
        JMXConnector conn;
        T proxy;

        ProxyConnection(JMXConnector conn, T proxy)
        {
            this.conn = conn;
            this.proxy = proxy;
        }

        @Override
        public void close() 
        {
            try
            {
                if (conn != null)
                    conn.close();
            }
            catch (IOException e)
            {
                throw new AgentLoadingException("Unable to close the JMXConnection", e);
            }
        }
    }
}
