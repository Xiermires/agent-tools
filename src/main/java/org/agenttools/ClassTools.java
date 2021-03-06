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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.ClassDefinition;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class ClassTools
{
    public static File createTemporaryJar(String jarName, Manifest manifest, String... classNames) throws IOException, URISyntaxException, ClassNotFoundException
    {
        final File tmpJar = File.createTempFile(jarName, ".jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tmpJar)))
        {
            if (manifest != null)
            {
                final ZipEntry ze = new ZipEntry(JarFile.MANIFEST_NAME);
                jos.putNextEntry(ze);
                manifest.write(jos);
                jos.closeEntry();
            }

            if (classNames.length > 0)
            {
                appendToJarStream(jos, getClassDefinition(classNames));
            }

            return tmpJar;
        }
    }

    static List<ClassDefinition> getClassDefinition(String... classNames) throws ClassNotFoundException, IOException, URISyntaxException
    {
        final List<ClassDefinition> cds = new ArrayList<ClassDefinition>();
        for (String c : classNames)
        {
            cds.add(new ClassDefinition(Class.forName(c), ClassTools.getClassBytes(c)));
        }
        return cds;
    }

    static List<Class<?>> getClass(String... classNames) throws ClassNotFoundException
    {
        final List<Class<?>> cs = new ArrayList<Class<?>>();
        for (String s : classNames)
        {
            cs.add(Class.forName(s));
        }
        return cs;
    }

    static void appendToJarStream(JarOutputStream jos, List<ClassDefinition> cds) throws IOException
    {
        for (ClassDefinition cd : cds)
        {
            final ZipEntry z = new ZipEntry(cd.getDefinitionClass().getName().replace('.', '/').concat(".class"));
            jos.putNextEntry(z);
            jos.write(cd.getDefinitionClassFile());
            jos.closeEntry();
        }
    }

    static List<ClassDefinition> getPckgClassBytes(URI pckg, String pckgName) throws IOException, URISyntaxException, ClassNotFoundException
    {
        final List<ClassDefinition> result = new ArrayList<ClassDefinition>();
        final DirectoryStream<Path> paths = Files.newDirectoryStream(Paths.get(pckg));
        for (Path p : paths)
        {
            result.add(new ClassDefinition(Class.forName(pckgName.toString().concat(".").concat(getClassName(p))), Files.readAllBytes(p)));
        }
        paths.close();
        return result;
    }

    static byte[] getClassBytes(String className) throws IOException, URISyntaxException
    {
        return Files.readAllBytes(getPath(className));
    }

    public static File findJarOf(Class<?> clazz) throws UnsupportedEncodingException
    {
        final String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        return new File(URLDecoder.decode(path, "UTF-8"));
    }
    
    private static Path getPath(String className) throws URISyntaxException
    {
        final String path = "/".concat(className.replace('.', '/').concat(".class"));
        final URL url = ClassTools.class.getResource(path);
        return Paths.get(url.toURI());
    }

    // This should work for folder / jar / different os (TODO: test).
    private static final Pattern CLASS_NAME = Pattern.compile("(.+)\\/(.*)\\.class");

    private static String getClassName(Path p)
    {
        final String path = p.toUri().getPath(); // '/' separator
        final Matcher matcher = CLASS_NAME.matcher(path);
        if (matcher.matches()) { return matcher.group(2); }
        throw new AgentLoadingException(String.format("Unable to get class name from path :'%s'", path));
    }
}
