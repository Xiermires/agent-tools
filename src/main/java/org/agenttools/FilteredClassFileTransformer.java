package org.agenttools;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.function.Predicate;

public class FilteredClassFileTransformer implements ClassFileTransformer
{
    private final ClassFileTransformer cft;
    private final Predicate<String> classNameFilter;
    
    public FilteredClassFileTransformer (ClassFileTransformer cft, Predicate<String> classNameFilter)
    {
        assert cft != null;
        assert classNameFilter != null;
        
        this.cft = cft;
        this.classNameFilter = classNameFilter;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        if (classNameFilter.test(className))
        {
            return cft.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        }
        return classfileBuffer;
    }
}
