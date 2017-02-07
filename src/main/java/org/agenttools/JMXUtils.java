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
