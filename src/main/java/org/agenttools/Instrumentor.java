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

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public class Instrumentor implements InstrumentorMBean
{
    private final Instrumentation instrumentation;

    Instrumentor(Instrumentation instrumentation)
    {
        this.instrumentation = instrumentation;
    }

    @Override
    public void load(ClassFileTransformer transformer)
    {
        instrumentation.addTransformer(transformer);
    }

    @Override
    public void remove(ClassFileTransformer transformer)
    {
        instrumentation.removeTransformer(transformer);
    }

    @Override
    public void reset(String... classNames)
    {
        retransform(new ResetChangesTransformer(), classNames);
    }

    @Override
    public void retransform(ClassFileTransformer transformer, String... classNames)
    {
        instrumentation.addTransformer(transformer, true);
        try
        {
            final List<Class<?>> cs = new ArrayList<Class<?>>();
            for (String s : classNames)
            {
                cs.add(Class.forName(s));
            }
            instrumentation.retransformClasses(cs.toArray(new Class[cs.size()]));
        }
        catch (Exception e)
        {
            throw new AgentLoadingException(String.format("All or some of the following classes couldn't be transformed { %s }.", classNames.toString()), e);
        }
        finally
        {
            instrumentation.removeTransformer(transformer);
        }
    }

    @Override
    public void redefine(String... classNames)
    {
        try
        {
            final List<ClassDefinition> cds = new ArrayList<ClassDefinition>();
            for (String c : classNames)
            {
                cds.add(new ClassDefinition(Class.forName(c), ClassTools.getClassBytes(c)));
            }
            instrumentation.redefineClasses(cds.toArray(new ClassDefinition[cds.size()]));
        }
        catch (Exception e)
        {
            throw new AgentLoadingException(String.format("All or some of the following classes couldn't be redefined { %s }.", classNames.toString()), e);
        }
    }
}
