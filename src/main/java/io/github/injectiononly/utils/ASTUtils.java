package io.github.injectiononly.utils;

import com.sun.source.tree.NewClassTree;
import com.sun.source.util.*;

public class ASTUtils {

    public static boolean containsNew(TreePath path, Trees trees) {
        final boolean[] found = {false};

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitNewClass(NewClassTree newClassTree, Void unused) {
                found[0] = true;
                return super.visitNewClass(newClassTree, unused);
            }
        }.scan(path, null);

        return found[0];
    }
}
