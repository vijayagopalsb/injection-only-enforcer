package io.github.injectiononly.utils;

import io.github.injectiononly.model.Violation;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class ViolationReporter {

    public static void report(Messager messager, Violation violation) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                violation.getMessage(),
                violation.getElement()
        );
    }
}
