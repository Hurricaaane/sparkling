package eu.ha3.openapi.sparkling.specific;

import eu.ha3.openapi.sparkling.routing.ISparklingInteractor;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparkConsumer;
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
    private final List<? extends ISparkConsumer> availableConsumers;
    private final ISparklingDeserializer deserializer;
    private final List<?> controllers;

    public CommonSparklingInteractor(Service http, List<? extends ISparkConsumer> availableConsumers, ISparklingDeserializer deserializer, List<?> controllers) {
        this.http = http;
        this.availableConsumers = availableConsumers;
        this.deserializer = deserializer;
        this.controllers = controllers;
    }

    @Override
    public void declare(String controllerHint, String operationId, SparklingVerb method, String sparkPath, List<String> consumes, List<String> produces, List<SparklingParameter> parameters) {
        Function<Object[], SparklingResponseContext> reflector = resolveControllerAndMethod(operationId, controllerHint);
        List<ISparkConsumer> allowedConsumers = findAvailableConsumersApplicableForThisDeclaration(consumes);

        switch (method) {
            case GET:
                http.get(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case POST:
                http.post(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case PUT:
                http.put(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case PATCH:
                http.patch(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case DELETE:
                http.delete(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case HEAD:
                http.head(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case TRACE:
                http.trace(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
            case OPTIONS:
                http.options(sparkPath, new InternalSparklingRoute(reflector, allowedConsumers, parameters, deserializer));
                break;
        }
    }

    private List<ISparkConsumer> findAvailableConsumersApplicableForThisDeclaration(List<String> consumes) {
        return availableConsumers.stream()
                .filter(sparkConsumer -> consumes.stream().anyMatch(declaredConsumer -> sparkConsumer.getApplicableContentTypes().contains(declaredConsumer)))
                .collect(Collectors.toList());
    }

    private Function<Object[], SparklingResponseContext> resolveControllerAndMethod(String operationId, String controllerHint) {
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
            if (isClassNameMatching(controller, controllerHint)) {
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
