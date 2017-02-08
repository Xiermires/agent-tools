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
                traceMethods(clazz);
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

    private void traceMethods(CtClass clazz)
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
