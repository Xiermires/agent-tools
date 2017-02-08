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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javassist.CtClass;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

// The tests work perfectly individually.
// If run together, keep in mind once cat / dog are loaded they won't be instrumented again unless re-transformed / redefined.
public class AgentToolsTest
{
    @Test
    public void testAddRemove() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat", "org/agenttools/Dog");
        AgentTools.load(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        AgentTools.remove(transformer);
        final Dog dog = new Dog();
        dog.bark(); // silence
    }

    @Test
    public void testReset() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat", "org/agenttools/Dog");
        AgentTools.load(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        AgentTools.remove(transformer);
        AgentTools.reset(Cat.class.getName());
        cat.meow(); // silence
    }

    @Test
    public void testRetransform() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("woof-woof", "org/agenttools/Cat", "org/agenttools/Dog");
        AgentTools.load(transformer);

        final Dog dog = new Dog();
        dog.bark(); // woof-woof

        AgentTools.remove(transformer);
        AgentTools.reset(Dog.class.getName());

        dog.bark(); // silence

        AgentTools.retransform(new Onomatopoeia("bup bup", "org/agenttools/Cat", "org/agenttools/Dog"), Dog.class.getName());
        dog.bark(); // bup bup (Catalan)
    }

    @Test
    public void testRedefine() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat", "org/agenttools/Dog");
        AgentTools.load(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        AgentTools.remove(transformer);
        cat.meow(); // meow

        AgentTools.load(new Onomatopoeia("marrameu", "org/agenttools/Cat", "org/agenttools/Dog"));
        AgentTools.redefine(Cat.class.getName());
        cat.meow(); // marrameu (Catalan)
    }

    @Test
    @Ignore
    // white test only.
    public void createRemoteJar() throws IOException, URISyntaxException, ClassNotFoundException
    {
        AgentBootstrap.createRemoteBootstrapJar();
    }

    /* ***** REMOTE ***** */

    /**
     * Manual remote testing. Too much bother to handle a ProcessBuilder. Steps to test.
     * <ol>
     * <li>Open a console and run org.testremote.Main 
     *     (doesn't need any dependencies) (run it from an IDE only w/o dependencies, the IDE usually attaches them automatically)
     * <li>The console will print the running process pid. 
     * <li>Initialize the pid field with the running process.
     * <li>Run tests step by step to see the changes.
     * </ol>
     */
    int pid = -1;

    @Test
    @Ignore
    public void testLoadJar() throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException
    {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");

        final File jarFile = ClassTools.createTemporaryJar("loadJarTest", manifest, TestLoadRemoteJarClass.class.getName());
        jarFile.deleteOnExit();

        final byte[] jarBytes = Files.readAllBytes(Paths.get(jarFile.toURI()));
        AgentTools.loadJar(pid, "loadJarTest", jarBytes); // class loaded every second
    }

    @Test
    @Ignore
    public void testResetRemote() throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException
    {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putIfAbsent(Attributes.Name.MANIFEST_VERSION, "1.0");

        final File ctf = ClassTools.createTemporaryJar("ctfDependency", manifest, Onomatopoeia.class.getName());
        final File jnaPlatform = ClassTools.findJarOf(Kernel32.class);
        final File javassist = ClassTools.findJarOf(CtClass.class);

        ctf.deleteOnExit();

        AgentTools.loadJar(pid, "ctfDependency", Files.readAllBytes(Paths.get(ctf.toURI())));
        AgentTools.loadJar(pid, "jna-4.0.0", Files.readAllBytes(Paths.get(jnaPlatform.toURI())));
        AgentTools.loadJar(pid, "javassist-3.18.1-GA", Files.readAllBytes(Paths.get(javassist.toURI())));

        final Onomatopoeia meow = new Onomatopoeia("meow", "org/testremote/Cat");
        final Onomatopoeia woof = new Onomatopoeia("woof-woof", "org/testremote/Dog");

        AgentTools.retransform(pid, woof, "org.testremote.Dog"); // woof-woof every second
        AgentTools.remove(pid, woof);
        AgentTools.reset(pid, "org.testremote.Dog"); // no more woofs
        AgentTools.retransform(pid, meow, "org.testremote.Cat"); // meow every second
    }
}
