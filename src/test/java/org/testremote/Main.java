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
