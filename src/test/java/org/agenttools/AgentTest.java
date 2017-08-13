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
public class AgentTest
{
    @Test
    public void testAddRemove() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat", "org/agenttools/Dog");
        Agents.add(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        Agents.remove(transformer);
        final Dog dog = new Dog();
        dog.bark(); // silence
    }

    @Test
    public void testReset() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat");
        Agents.add(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        Agents.remove(transformer);
        Agents.reset(Cat.class.getName());
        cat.meow(); // silence
    }

    @Test
    public void testRetransform() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("woof-woof", "org/agenttools/Dog");
        Agents.add(transformer);

        final Dog dog = new Dog();
        dog.bark(); // woof-woof

        Agents.remove(transformer);
        Agents.reset(Dog.class.getName());

        dog.bark(); // silence

        Agents.retransform(new Onomatopoeia("bup bup", "org/agenttools/Dog"), Dog.class.getName());
        dog.bark(); // bup bup (Catalan)
    }

    @Test
    public void testRedefine() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow", "org/agenttools/Cat");
        Agents.add(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        Agents.remove(transformer);
        cat.meow(); // meow

        Agents.add(new Onomatopoeia("marrameu", "org/agenttools/Cat"));
        Agents.redefine(Cat.class.getName());
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
        Agents.loadJar(pid, "loadJarTest", jarBytes); // class loaded every second
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

        Agents.loadJar(pid, "ctfDependency", Files.readAllBytes(Paths.get(ctf.toURI())));
        Agents.loadJar(pid, "jna-4.0.0", Files.readAllBytes(Paths.get(jnaPlatform.toURI())));
        Agents.loadJar(pid, "javassist-3.18.1-GA", Files.readAllBytes(Paths.get(javassist.toURI())));

        final Onomatopoeia meow = new Onomatopoeia("meow", "org/testremote/Cat");
        final Onomatopoeia woof = new Onomatopoeia("woof-woof", "org/testremote/Dog");

        Agents.retransform(pid, woof, "org.testremote.Dog"); // woof-woof every second
        Agents.remove(pid, woof);
        Agents.reset(pid, "org.testremote.Dog"); // no more woofs
        Agents.retransform(pid, meow, "org.testremote.Cat"); // meow every second
    }
}
