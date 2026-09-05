package io.github.injectiononly.model;

import javax.lang.model.element.Element;

public class Violation {
    private final Element element;
    private final String message;

    public Violation(Element element, String message) {
        this.element = element;
        this.message = message;
    }

    public Element getElement() {
        return element;
    }

    public String getMessage() {
        return message;
    }
}
