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

import java.io.IOException;
import java.io.Serializable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

class Onomatopoeia implements ClassFileTransformer, Serializable
{
    private static final long serialVersionUID = 1L;

    private final String sound;
    private final String[] acceptTheseOnly;

    Onomatopoeia(String sound, String... acceptTheseOnly)
    {
        this.sound = sound;
        this.acceptTheseOnly = acceptTheseOnly;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        if (Objects.nonNull(className) && accept(className))
        {
            CtClass clazz = null;
            try
            {
                clazz = getCtClass(className.replace('/', '.'), classfileBuffer);
                if (clazz.isFrozen())
                {
                    clazz.defrost();
                }
                makeSound(clazz);
                return clazz.toBytecode();
            }
            catch (NotFoundException | IOException | CannotCompileException e)
            {
                System.err.println(e.getMessage());
            }
            finally
            {
                clazz.detach();
            }
        }
        return classfileBuffer;
    }

    private CtClass getCtClass(String className, byte[] classByteCode) throws NotFoundException
    {
        final ClassPool classPool = new ClassPool();
        classPool.appendSystemPath();
        return classPool.get(className);
    }

    private void makeSound(CtClass clazz)
    {
        for (CtMethod method : clazz.getDeclaredMethods())
        {
            try
            {
                method.insertBefore("System.out.println(\"" + sound + "\");");
            }
            catch (CannotCompileException e)
            {
                System.err.println(e.getMessage());
            }
        }
    }
    
    // Using a predicate parameter throws serialization errors due to the owner of the anonymous class.
    // Use a lazy initialization approach to filter.
    
    /*private Predicate<String> remoteClasses()
    {
        return (Predicate<String> & Serializable) cn -> "org/testremote/Cat".equals(cn) || "org/testremote/Dog".equals(cn);
    }*/
    
    private Set<String> set = null;
    
    private boolean accept(String className)
    {
        if (set == null)
        {
            set = new HashSet<>(Arrays.asList(acceptTheseOnly));
        }
        return set.contains(className);
    }
}
