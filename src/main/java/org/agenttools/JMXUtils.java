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

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;

class JMXUtils
{
    private static final String JAVA_HOME = "java.home";
    private static final String JMX_REMOTE = "com.sun.management.jmxremote";
    private static final String JMX_LOCAL_ADDRESS = JMX_REMOTE + ".localConnectorAddress";
    private static final String MANAGEMENT_AGENT_JAR = "%s" + File.separator + "lib" + File.separator + "management-agent.jar";

    static JMXConnector connect(VirtualMachine vm) throws IOException, AgentLoadException, AgentInitializationException
    {
        enableJmx(vm);

        final String lca = vm.getAgentProperties().getProperty(JMX_LOCAL_ADDRESS);
        return JMXConnectorFactory.connect(new JMXServiceURL(lca));
    }
    
    private static boolean enableJmx(VirtualMachine vm) throws IOException, AgentLoadException, AgentInitializationException
    {
        final boolean installed = Boolean.TRUE.equals(vm.getAgentProperties().getProperty(JMX_REMOTE));
        if (!installed)
        {
            final String javaHome = vm.getSystemProperties().getProperty(JAVA_HOME);
            final String managementAgentPath = new File(String.format(MANAGEMENT_AGENT_JAR, javaHome)).getAbsolutePath();
            vm.loadAgent(managementAgentPath);
        }
        return installed;
    }
}
