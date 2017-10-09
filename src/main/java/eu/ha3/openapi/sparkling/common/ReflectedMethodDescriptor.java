package eu.ha3.openapi.sparkling.common;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
class ReflectedMethodDescriptor {
    private final Function<List<Object>, ?> implementation;
    private final List<Type> expectedRequestParameters;

    public ReflectedMethodDescriptor(Function<List<Object>, ?> implementation, List<Type> expectedRequestParameters) {
        this.implementation = implementation;
        this.expectedRequestParameters = expectedRequestParameters;
    }

    public Function<List<Object>, ?> getImplementation() {
        return implementation;
    }

    public List<Type> getExpectedRequestParameters() {
        return expectedRequestParameters;
    }
}
