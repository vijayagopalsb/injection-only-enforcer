package io.github.injectiononly.processor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectionOnlyProcessorTest {

    @Test
    void flagsManualInstantiationOfNonExemptType() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                @InjectionOnly
                class DemoService {
                    static class Helper {}
                    void test() {
                        new Helper();
                    }
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertFalse(result.success, "compilation should fail on manual 'new' of a non-exempt type");
        assertTrue(result.hasErrorContaining("not allowed in @InjectionOnly classes"), result.diagnostics.toString());
    }

    @Test
    void allowsJdkTypesWithoutAnAllowList() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                import java.util.ArrayList;
                @InjectionOnly
                class DemoService {
                    void test() {
                        new ArrayList<String>();
                        new StringBuilder();
                        throw new IllegalArgumentException("x");
                    }
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertTrue(result.success, "JDK types should be exempt automatically: " + result.diagnostics);
    }

    @Test
    void allowsTypesExplicitlyListedInAllow() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                @InjectionOnly(allow = { DemoService.Helper.class })
                class DemoService {
                    static class Helper {}
                    void test() {
                        new Helper();
                    }
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertTrue(result.success, "type listed in allow() should compile clean: " + result.diagnostics);
    }

    @Test
    void stillFlagsTypesNotInTheAllowList() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                @InjectionOnly(allow = { DemoService.Allowed.class })
                class DemoService {
                    static class Allowed {}
                    static class NotAllowed {}
                    void test() {
                        new Allowed();
                        new NotAllowed();
                    }
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertFalse(result.success, "NotAllowed is not in allow(), so this should still fail");
        assertTrue(result.hasErrorContaining("not allowed in @InjectionOnly classes"), result.diagnostics.toString());
    }

    @Test
    void classWithoutAnnotationIsUntouched() throws IOException {

        String source = """
                package t;
                class PlainService {
                    static class Helper {}
                    void test() {
                        new Helper();
                    }
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.PlainService", source);

        assertTrue(result.success, "a class with no @InjectionOnly should never be touched by the processor");
    }

    @Test
    void doesNotApplyToNestedTypeBodies() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                @InjectionOnly
                class DemoService {
                    static class NestedHelper {
                        void create() {
                            new Helper();
                        }
                    }
                    static class Helper {}
                }
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertTrue(result.success,
                "nested type bodies are outside the annotated type's direct enforcement scope: "
                        + result.diagnostics);
    }

    @Test
    void doesNotScanSiblingTypesInTheSameCompilationUnit() throws IOException {

        String source = """
                package t;
                import io.github.injectiononly.annotation.InjectionOnly;
                @InjectionOnly
                class DemoService {}
                class PlainService {
                    void create() {
                        new Helper();
                    }
                }
                class Helper {}
                """;

        CompilationTestHelper.Result result = CompilationTestHelper.compile("t.DemoService", source);

        assertTrue(result.success,
                "only the annotated type's source tree should be checked: " + result.diagnostics);
    }
}