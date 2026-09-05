package io.github.injectiononly.processor;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Wraps a Java source string as a JavaFileObject so tests can hand javac a class body
 * without writing an actual .java file to disk.
 */
final class InMemorySource extends SimpleJavaFileObject {

    private final String source;

    InMemorySource(String qualifiedClassName, String source) {
        super(URI.create("string:///" + qualifiedClassName.replace('.', '/') + Kind.SOURCE.extension),
                Kind.SOURCE);
        this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }
}