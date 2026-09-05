package io.github.injectiononly.processor;

import io.github.injectiononly.annotation.InjectionOnly;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Compiles a single in-memory Java source file with InjectionOnlyProcessor registered as an
 * annotation processor, and hands back the diagnostics so tests can assert on them.
 * <p>
 * Uses the real system javac via javax.tools.JavaCompiler - no reflection into javac internals,
 * so this needs no --add-opens/--add-exports flags, unlike libraries such as compile-testing.
 * <p>
 * IMPORTANT: an in-process JavaCompiler does NOT automatically inherit the classpath of the JVM
 * that invoked it - it only sees whatever is explicitly configured below. We point it at the
 * exact location InjectionOnly.class is already loaded from, so it doesn't matter whether
 * Surefire is forking a JVM or running embedded (forkCount=0) - either way, this always finds
 * the right classes.
 */
final class CompilationTestHelper {

    static Result compile(String qualifiedClassName, String source) throws IOException {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        Path outDir = Files.createTempDirectory("injection-only-test");
        Path annotationClasses = locationOf(InjectionOnly.class);

        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null);
        fileManager.setLocationFromPaths(
                StandardLocation.CLASS_OUTPUT, Collections.singletonList(outDir));
        fileManager.setLocationFromPaths(
                StandardLocation.CLASS_PATH, Collections.singletonList(annotationClasses));

        List<JavaFileObject> compilationUnits =
                Collections.singletonList(new InMemorySource(qualifiedClassName, source));

        StringWriter output = new StringWriter();

        // Also pass -classpath explicitly, since some javac versions read it from options
        // rather than (or in addition to) the file manager's CLASS_PATH location.
        List<String> options = Arrays.asList("-classpath", annotationClasses.toString());

        JavaCompiler.CompilationTask task = compiler.getTask(
                output, fileManager, diagnostics, options, null, compilationUnits);

        task.setProcessors(Collections.singletonList(new InjectionOnlyProcessor()));

        boolean success = task.call();
        fileManager.close();

        return new Result(success, diagnostics.getDiagnostics());
    }

    private static Path locationOf(Class<?> type) {
        try {
            return Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not resolve classpath location for " + type, e);
        }
    }

    static final class Result {
        final boolean success;
        final List<Diagnostic<? extends JavaFileObject>> diagnostics;

        Result(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
            this.success = success;
            this.diagnostics = diagnostics;
        }

        boolean hasErrorContaining(String messageFragment) {
            return diagnostics.stream()
                    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                            && d.getMessage(null).contains(messageFragment));
        }
    }
}