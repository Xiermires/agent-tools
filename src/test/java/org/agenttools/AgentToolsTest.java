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
import java.net.URISyntaxException;

import org.junit.Ignore;
import org.junit.Test;

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
        final Onomatopoeia transformer = new Onomatopoeia("meow");
        AgentTools.load(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        AgentTools.remove(transformer);
        AgentTools.reset(Cat.class.getName());
        final Dog dog = new Dog();
        dog.bark(); // silence
    }

    @Test
    public void testReset() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow");
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
        final Onomatopoeia transformer = new Onomatopoeia("woof-woof"); 
        AgentTools.load(transformer);

        final Dog dog = new Dog();
        dog.bark(); // woof-woof

        AgentTools.remove(transformer);
        AgentTools.reset(Dog.class.getName());

        dog.bark(); // silence

        AgentTools.retransform(new Onomatopoeia("bup bup"), Dog.class.getName());
        dog.bark(); // bup bup (Catalan)
    }
    
    @Test
    public void testRedefine() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, InterruptedException
    {
        final Onomatopoeia transformer = new Onomatopoeia("meow");
        AgentTools.load(transformer);

        final Cat cat = new Cat();
        cat.meow(); // meow

        AgentTools.remove(transformer);
        cat.meow(); // meow

        AgentTools.load(new Onomatopoeia("marrameu"));
        AgentTools.redefine(Cat.class.getName());
        cat.meow(); // marrameu (Catalan)
    }
    
    @Test
    @Ignore // white test only.
    public void createRemoteJar() throws IOException, URISyntaxException, ClassNotFoundException
    {
        AgentBootstrap.createRemoteBootstrapJar();
    }
}
