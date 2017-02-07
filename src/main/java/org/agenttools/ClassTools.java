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
import java.lang.instrument.ClassDefinition;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ClassTools
{
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
    
    static Path getPath(Package pckg) throws URISyntaxException
    {
        final String path = "/".concat(pckg.getName().replace('.', '/'));
        final URL url = ClassTools.class.getResource(path);
        return Paths.get(url.toURI());
    }
    
    static URI getClassURI(String className) throws URISyntaxException
    {
        return ClassTools.class.getResource("/".concat(className).replace('.', '/').concat(".class")).toURI();
    };

    // This method is a workaround due to 'user.dir' while running from tests fixated there. 
    // Hence reading the package contents, reads the tests files.
    // We force the package of the target files by using a target class URI as pattern.
    static URI getPckgURIFromClassURI(URI classUri)
    {
        final Matcher matcher = CLASS_NAME.matcher(classUri.getPath());
        while (matcher.find())
        {
            return new File(matcher.group(1)).toURI(); // FIXME : jars.
        }
        throw new AgentLoadingException(String.format("Unable to get package uri from path :'%s'", classUri.getPath()));
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
        if (matcher.matches())
        {
            return matcher.group(2);
        }
        throw new AgentLoadingException(String.format("Unable to get class name from path :'%s'", path));
    }
}
