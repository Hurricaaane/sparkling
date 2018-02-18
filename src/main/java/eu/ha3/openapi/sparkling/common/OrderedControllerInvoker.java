package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
class OrderedControllerInvoker implements ControllerInvoker {
    private final String operationId;
    private final Object controller;
    private final Method method;
    private final List<SparklingParameter> parameters;
    private final Map<SparklingParameter, Type> reflectedTypes;

    public OrderedControllerInvoker(String operationId, Object controller, Method method, List<SparklingParameter> parameters, Map<SparklingParameter, Type> reflectedTypes) {
        this.operationId = operationId;
        this.controller = controller;
        this.method = method;
        this.parameters = parameters;
        this.reflectedTypes = reflectedTypes;
    }

    @Override
    public Map<SparklingParameter, Type> getReflectedTypeMap() {
        return reflectedTypes;
    }

    @Override
    public Object submit(Request request, Response response, Map<SparklingParameter, Object> models) {
        try {
            List<Object> invocationArguments = compileInvocationArguments(request, response, models);

            return method.invoke(controller, invocationArguments.toArray());

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId, e);
        }
    }

    private List<Object> compileInvocationArguments(Request request, Response response, Map<SparklingParameter, Object> models) {
        return Stream.concat(
                parameters.stream().map(models::get),
                Stream.of(request, response)
        ).collect(Collectors.toList());
    }
}
