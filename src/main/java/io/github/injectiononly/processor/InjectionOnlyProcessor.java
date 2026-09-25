package io.github.injectiononly.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
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
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(InjectionOnly.class)) {
            TreePath classPath = trees.getPath(annotatedElement);
            if (classPath == null) {
                continue;
            }

            CompilationUnitTree compilationUnit = classPath.getCompilationUnit();
            Set<String> allowedTypes = readAllowList(annotatedElement);

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitNewClass(NewClassTree newClassTree, Void unused) {
                    if (isDirectlyInsideAnnotatedType(getCurrentPath(), annotatedElement)
                            && !isExempt(getCurrentPath(), allowedTypes)) {
                        trees.printMessage(
                                Diagnostic.Kind.ERROR,
                                "Manual instantiation using 'new' is not allowed in @InjectionOnly classes.",
                                newClassTree,
                                compilationUnit
                        );
                    }
                    return super.visitNewClass(newClassTree, unused);
                }
            }.scan(classPath, null);
        }

        return false;
    }

    private boolean isDirectlyInsideAnnotatedType(TreePath newClassPath, Element annotatedElement) {
        for (TreePath parent = newClassPath.getParentPath(); parent != null; parent = parent.getParentPath()) {
            if (parent.getLeaf() instanceof ClassTree) {
                Element enclosingType = trees.getElement(parent);
                return annotatedElement.equals(enclosingType);
            }
        }
        return false;
    }

    private boolean isExempt(TreePath newClassPath, Set<String> allowedTypes) {
        Element constructor = trees.getElement(newClassPath);
        if (!(constructor instanceof ExecutableElement)) {
            return false;
        }

        Element constructedType = constructor.getEnclosingElement();
        if (!(constructedType instanceof TypeElement)) {
            return false;
        }

        String qualifiedName = ((TypeElement) constructedType).getQualifiedName().toString();
        return qualifiedName.startsWith("java.")
                || qualifiedName.startsWith("javax.")
                || allowedTypes.contains(qualifiedName);
    }

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
                    if (typeMirror instanceof DeclaredType declaredType
                            && declaredType.asElement() instanceof TypeElement typeElement) {
                        result.add(typeElement.getQualifiedName().toString());
                    }
                }
            }
        }

        return result;
    }
}
