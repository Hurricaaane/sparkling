package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;

import java.util.function.Function;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
class ReflectedMethodDescriptor {
    private final Function<Object[], SparklingResponseContext> implementation;
    private final Class<?> pojoClass;

    public ReflectedMethodDescriptor(Function<Object[], SparklingResponseContext> implementation, Class<?> pojoClass) {
        this.implementation = implementation;
        this.pojoClass = pojoClass;
    }

    public Function<Object[], SparklingResponseContext> getImplementation() {
        return implementation;
    }

    public Class<?> getPojoClass() {
        return pojoClass;
    }
}
