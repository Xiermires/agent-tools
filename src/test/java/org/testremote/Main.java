package org.testremote;

import java.lang.management.ManagementFactory;

import org.agenttools.AgentToolsTest;

/**
 * This class is to be used as part of the remote tests in {@link AgentToolsTest}. 
 * <p>
 * See {@link AgentToolsTest#pid} for more details.
 */
public class Main
{
    public static void main(String... args)
    {
        new Loop(() -> new Cat().meow()).start();

        new Loop(() -> new Dog().bark()).start();

        new Loop(() ->
        {
            try
            {
                Class.forName("org.agenttools.TestLoadRemoteJarClass");
                System.out.println("TestLoadRemoteJarClass loaded.");
            }
            catch (Exception e)
            {
                // ignore
            }
        }).start();

        System.out.println(pid());
    }

    private static String pid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }
}
