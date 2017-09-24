package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.routing.ISparklingInteractor;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class CommonSparklingInteractor implements ISparklingInteractor {
    private final Service http;
    private final List<? extends ISparklingRequestTransformer> availableConsumers;
    private final ISparklingDeserializer deserializer;
    private final List<?> controllers;

    public CommonSparklingInteractor(Service http, List<? extends ISparklingRequestTransformer> availableConsumers, ISparklingDeserializer deserializer, List<?> controllers) {
        this.http = http;
        this.availableConsumers = availableConsumers;
        this.deserializer = deserializer;
        this.controllers = controllers;
    }

    @Override
    public void newRoute(String controllerHint, String operationId, SparklingVerb method, String sparkPath, List<String> consumes, List<String> produces, List<SparklingParameter> parameters) {
        Function<Object[], SparklingResponseContext> implentation = resolveControllerImplementation(operationId, controllerHint);
        List<ISparklingRequestTransformer> allowedConsumers = findAvailableConsumersApplicableForThisDeclaration(consumes);

        InternalSparklingRoute route = new InternalSparklingRoute(implentation, allowedConsumers, parameters, deserializer);
        addRouteToSpark(method, sparkPath, route);
    }

    private void addRouteToSpark(SparklingVerb method, String sparkPath, InternalSparklingRoute route) {
        switch (method) {
            case GET:
                http.get(sparkPath, route);
                break;
            case POST:
                http.post(sparkPath, route);
                break;
            case PUT:
                http.put(sparkPath, route);
                break;
            case PATCH:
                http.patch(sparkPath, route);
                break;
            case DELETE:
                http.delete(sparkPath, route);
                break;
            case HEAD:
                http.head(sparkPath, route);
                break;
            case TRACE:
                http.trace(sparkPath, route);
                break;
            case OPTIONS:
                http.options(sparkPath, route);
                break;
        }
    }

    private List<ISparklingRequestTransformer> findAvailableConsumersApplicableForThisDeclaration(List<String> consumes) {
        return availableConsumers.stream()
                .filter(sparkConsumer -> consumes.stream().anyMatch(declaredConsumer -> sparkConsumer.getApplicableContentTypes().contains(declaredConsumer)))
                .collect(Collectors.toList());
    }

    private Function<Object[], SparklingResponseContext> resolveControllerImplementation(String operationId, String controllerHint) {
        Object controller = resolveController(controllerHint);

        Function<Object[], SparklingResponseContext> reflector;
        if (controller != null) {
            reflector = resolveMatchingMethodByName(operationId, controller)
                    .<Function<Object[], SparklingResponseContext>>map(method -> items -> invokeController(operationId, controller, method, items))
                    .orElseGet(() -> items -> {
                        throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId);
                    });
        } else {
            reflector = (Object[] items) -> {
                throw new UnavailableControllerSparklingException("No controller available for this operation " + operationId);
            };
        }
        return reflector;
    }

    private Optional<Method> resolveMatchingMethodByName(String operationId, Object controller) {
        return Arrays.stream(controller.getClass().getMethods())
                .filter(m -> operationId.equals(m.getName()))
                .findFirst();
    }

    private SparklingResponseContext invokeController(String operationId, Object controller, Method method, Object[] items) {
        try {
            return (SparklingResponseContext) method.invoke(controller, items);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId, e);
        }
    }

    private Object resolveController(String controllerHint) {
        for (Object controller : controllers) {
            if (isClassNameMatching(controller, controllerHint)) {
                return controller;
            }
        }
        // Do it in two passes so that weak matching occurs only after exact matching fails
        for (Object controller : controllers) {
            if (isClassNameWeaklyMatching(controller, controllerHint)) {
                return controller;
            }
        }

        return null;
    }

    private boolean isClassNameMatching(Object controller, String controllerHint) {
        String controllerName = controller.getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
        String hint = controllerHint.toLowerCase(Locale.ENGLISH);
        return (hint + "controller").equals(controllerName);
    }

    private boolean isClassNameWeaklyMatching(Object controller, String controllerHint) {
        String controllerName = controller.getClass().getSimpleName().toLowerCase(Locale.ENGLISH);
        String hint = controllerHint.toLowerCase(Locale.ENGLISH);
        return controllerName.startsWith(hint);
    }
}
