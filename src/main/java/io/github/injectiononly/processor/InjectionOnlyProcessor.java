package io.github.injectiononly.processor;

import io.github.injectiononly.annotation.InjectionOnly;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("io.github.injectiononly.annotation.InjectionOnly")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InjectionOnlyProcessor extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (trees == null) {
            return false;
        }

        // @InjectionOnly means: this class must obtain its dependencies via dependency
        // injection, and may not manually construct THEM itself. It does NOT mean the class
        // can't build a plain ArrayList or throw a new IllegalArgumentException - those aren't
        // managed dependencies, they're everyday object construction, and banning them makes
        // the rule unusable on real code. So a "new" is only a violation if:
        //   1. its nearest enclosing class is the annotated class, AND
        //   2. the type being constructed is not exempt (JDK types, or explicitly allow()-ed).
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(InjectionOnly.class)) {

            TreePath classPath = trees.getPath(annotatedElement);
            if (classPath == null) continue;

            CompilationUnitTree unit = classPath.getCompilationUnit();
            Set<String> allowedQualifiedNames = readAllowList(annotatedElement);

            new TreePathScanner<Void, Void>() {

                @Override
                public Void visitNewClass(NewClassTree newClassTree, Void unused) {

                    TreePath parent = getCurrentPath().getParentPath();

                    while (parent != null) {
                        if (parent.getLeaf() instanceof ClassTree) {

                            Element enclosingElement = trees.getElement(parent);

                            if (enclosingElement != null
                                    && enclosingElement.equals(annotatedElement)
                                    && !isExempt(getCurrentPath(), allowedQualifiedNames)) {

                                trees.printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "Manual instantiation using 'new' is not allowed in @InjectionOnly classes.",
                                        newClassTree,
                                        unit
                                );
                            }
                            break;
                        }
                        parent = parent.getParentPath();
                    }

                    return super.visitNewClass(newClassTree, unused);
                }

            }.scan(unit, null);
        }

        return false;
    }

    /**
     * True if the type constructed by the "new" expression at newClassPath should be allowed
     * even inside an @InjectionOnly class: anything in java.* / javax.* (can't be user-annotated
     * anyway), or anything explicitly named in the class's @InjectionOnly(allow = ...) list.
     * Anonymous classes (no qualified name available) are treated conservatively as NOT exempt.
     */
    private boolean isExempt(TreePath newClassPath, Set<String> allowedQualifiedNames) {

        Element constructor = trees.getElement(newClassPath);
        if (!(constructor instanceof ExecutableElement)) {
            return false;
        }

        Element constructedType = constructor.getEnclosingElement();
        if (!(constructedType instanceof TypeElement)) {
            return false;
        }

        String qualifiedName = ((TypeElement) constructedType).getQualifiedName().toString();
        if (qualifiedName.isEmpty()) {
            // Anonymous or local class - no reliable qualified name, don't exempt it.
            return false;
        }

        return qualifiedName.startsWith("java.")
                || qualifiedName.startsWith("javax.")
                || allowedQualifiedNames.contains(qualifiedName);
    }

    /**
     * Reads the Class<?>[] allow() attribute off an @InjectionOnly annotation. Class<?> values
     * can't be read directly in an annotation processor (that would force-load the class), so
     * we go through the annotation mirror and pull out the TypeMirror for each entry instead.
     */
    private Set<String> readAllowList(Element annotatedElement) {

        Set<String> result = new HashSet<>();

        for (AnnotationMirror mirror : annotatedElement.getAnnotationMirrors()) {

            if (!mirror.getAnnotationType().toString().equals(InjectionOnly.class.getName())) {
                continue;
            }

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
                    : mirror.getElementValues().entrySet()) {

                if (!entry.getKey().getSimpleName().contentEquals("allow")) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<? extends AnnotationValue> values =
                        (List<? extends AnnotationValue>) entry.getValue().getValue();

                for (AnnotationValue value : values) {
                    TypeMirror typeMirror = (TypeMirror) value.getValue();
                    if (typeMirror instanceof DeclaredType) {
                        Element element = ((DeclaredType) typeMirror).asElement();
                        if (element instanceof TypeElement) {
                            result.add(((TypeElement) element).getQualifiedName().toString());
                        }
                    }
                }
            }
        }

        return result;
    }
}

/*package io.github.injectiononly.processor;

import io.github.injectiononly.annotation.InjectionOnly;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.util.Set;

@SupportedAnnotationTypes("io.github.injectiononly.annotation.InjectionOnly")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InjectionOnlyProcessor extends AbstractProcessor {

    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (trees == null) {
            return false;
        }

        // @InjectionOnly means: this class must obtain everything via dependency injection, and
        // may not manually construct objects itself. So we forbid ANY "new" expression whose
        // nearest enclosing class is the annotated class - regardless of what type is being
        // constructed. This must be checked within the annotated class's own compilation unit,
        // since a class body can't span multiple files.
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(InjectionOnly.class)) {

            TreePath classPath = trees.getPath(annotatedElement);
            if (classPath == null) continue;

            CompilationUnitTree unit = classPath.getCompilationUnit();

            new TreePathScanner<Void, Void>() {

                @Override
                public Void visitNewClass(NewClassTree newClassTree, Void unused) {

                    TreePath parent = getCurrentPath().getParentPath();

                    while (parent != null) {
                        if (parent.getLeaf() instanceof ClassTree) {

                            Element enclosingElement = trees.getElement(parent);

                            if (enclosingElement != null && enclosingElement.equals(annotatedElement)) {
                                trees.printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "Manual instantiation using 'new' is not allowed in @InjectionOnly classes.",
                                        newClassTree,
                                        unit
                                );
                            }
                            break;
                        }
                        parent = parent.getParentPath();
                    }

                    return super.visitNewClass(newClassTree, unused);
                }

            }.scan(unit, null);
        }

        return false;
    }
}*/
