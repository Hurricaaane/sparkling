package eu.ha3.openapi.sparkling.common;

import java.util.function.Function;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
class ReflectedMethodDescriptor {
    private final Function<Object[], ?> implementation;
    private final Class<?> pojoClass;

    public ReflectedMethodDescriptor(Function<Object[], ?> implementation, Class<?> pojoClass) {
        this.implementation = implementation;
        this.pojoClass = pojoClass;
    }

    public Function<Object[], ?> getImplementation() {
        return implementation;
    }

    public Class<?> getPojoClass() {
        return pojoClass;
    }
}
