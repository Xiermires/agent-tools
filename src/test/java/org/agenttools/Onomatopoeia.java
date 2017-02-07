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
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Objects;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Onomatopoeia implements ClassFileTransformer
{
    private final String sound;

    Onomatopoeia(String sound)
    {
        this.sound = sound;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        if (Objects.nonNull(className) && ("org/agenttools/cat".equals(className.toLowerCase()) || "org/agenttools/dog".equals(className.toLowerCase())))
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
}
