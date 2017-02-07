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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class AgentBootstrap
{
    public static void premain(String agentArguments, Instrumentation instrumentation) throws Exception
    {
        agentmain(agentArguments, instrumentation);
    }

    public static void agentmain(String agentArguments, Instrumentation instrumentation) throws Exception
    {
        final Instrumentor instr = new Instrumentor(instrumentation);
        AgentTools.initialize(instr);
        startMBean(instr);
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
            throw new AgentLoadingException("Unable to load local agent", e);
        }
    }

    static File createBootstrapJar() throws IOException
    {
        final File bootstrapJar = File.createTempFile("bootsrap", ".jar");
        bootstrapJar.deleteOnExit();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(bootstrapJar)))
        {
            final ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);
            jos.putNextEntry(ze);
            createManifest().write(jos);
            jos.closeEntry();

            return bootstrapJar;
        }
    }

    static File createRemoteBootstrapJar() throws IOException, URISyntaxException, ClassNotFoundException
    {
        final File bootstrapJar = File.createTempFile("bootsrap", ".jar");
        bootstrapJar.deleteOnExit();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(bootstrapJar)))
        {
            final ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);
            jos.putNextEntry(ze);
            createManifest().write(jos);
            jos.closeEntry();

            final String pckgName = AgentBootstrap.class.getPackage().getName();
            final URI classUri = ClassTools.getClassURI(AgentBootstrap.class.getName());
            final URI pckgUri = ClassTools.getPckgURIFromClassURI(classUri);
            for (ClassDefinition cd : ClassTools.getPckgClassBytes(pckgUri, pckgName))
            {
                final ZipEntry z = new ZipEntry(cd.getDefinitionClass().getName().replace('.', '/'));
                jos.putNextEntry(z);
                jos.write(cd.getDefinitionClassFile());
                jos.closeEntry();
            }

            return bootstrapJar;
        }
    }

    private static Manifest createManifest()
    {
        final Manifest m = new Manifest();

        m.getMainAttributes().putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().putIfAbsent(new Name("Agent-Class"), AgentBootstrap.class.getName());
        m.getMainAttributes().putIfAbsent(new Name("Can-Redefine-Classes"), Boolean.TRUE.toString());
        m.getMainAttributes().putIfAbsent(new Name("Can-Retransform-Classes"), Boolean.TRUE.toString());
        m.getMainAttributes().putIfAbsent(new Name("Can-Set-Native-Method-Prefix"), Boolean.TRUE.toString());

        return m;
    }
}
