package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.ParameterLocation;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.ISparklingInteractor;
import eu.ha3.openapi.sparkling.routing.ISparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
    public static final int FIRST_PARAMETER_INDEX = 2;
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

    public static CommonSparklingInteractor generic(Service http, List<?> controllers) {
        ArrayList<CommonSparklingRequestTransformer> consumers = new ArrayList<>(EnumSet.allOf(CommonSparklingRequestTransformer.class));
        return new CommonSparklingInteractor(http, consumers, new CommonDeserializer(), controllers);
    }

    @Override
    public void newRoute(RouteDefinition routeDefinition) {
        int bodyLocation = routeDefinition.getParameters().stream()
                .filter(parameter -> parameter.getLocation() == ParameterLocation.BODY)
                .findFirst()
                .map(routeDefinition.getParameters()::indexOf)
                .orElse(-1);

        ReflectedMethodDescriptor descriptor = resolveControllerImplementation(routeDefinition.getActionName(), routeDefinition.getTag(), bodyLocation, routeDefinition.getParameters());
        List<ISparklingRequestTransformer> allowedConsumers = findAvailableConsumersApplicableForThisDeclaration(routeDefinition.getConsumes());

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getImplementation(), allowedConsumers, routeDefinition.getParameters(), deserializer, descriptor.getPojoClass());
        addRouteToSpark(routeDefinition.getPost(), routeDefinition.getSparkPath(), route);
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

    private ReflectedMethodDescriptor resolveControllerImplementation(String operationId, String controllerHint, int bodyParameterIndex, List<SparklingParameter> parametersForDebugging) {
        if (parametersForDebugging.size() != 0) {
            System.out.println("Searching for method (" + controllerHint + "*)." + operationId + " with parameters able to accommodate input parameters: " + parametersForDebugging.stream().map(CommonSparklingInteractor::debuggableParameterString).collect(Collectors.joining(", ")));

        } else {
            System.out.println("Searching for method (" + controllerHint + "*)." + operationId + " with parameters able to accommodate zero input parameters...");
        }

        Object controller = resolveController(controllerHint);

        if (controller != null) {
            return whenControllerExists(operationId, bodyParameterIndex, parametersForDebugging, controller);

        } else {
            System.out.println("    WARNING: No controller found matching (" + controllerHint + "*) for operation " + operationId);

            return new ReflectedMethodDescriptor((Function<Object[], ?>) (Object[] items) -> {
                throw new UnavailableControllerSparklingException("No controller matching " + controllerHint + " available for operation " + operationId);
            }, String.class);
        }
    }

    private static String debuggableParameterString(SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return parameter.getType().name() + "@" + parameter.getLocation() + " " + parameter.getName();

        } else {
            return "List<" + parameter.getType().name() + ">" + parameter.getArrayType().name() + "@" + parameter.getLocation() + " " + parameter.getName();
        }
    }

    private ReflectedMethodDescriptor whenControllerExists(String operationId, int bodyParameterIndex, List<SparklingParameter> parametersForDebugging, Object controller) {
        Optional<Method> matchingMethod = resolveMatchingMethodByName(operationId, controller);
        if (matchingMethod.isPresent()) {
            return whenMethodExists(operationId, bodyParameterIndex, parametersForDebugging, controller, matchingMethod.get());

        } else {
            System.out.println("    WARNING: No method " + operationId + " found in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<Object[], ?>) items -> {
                throw new UnavailableControllerSparklingException("No method " + operationId + " available in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);
            }, String.class);
        }
    }

    private ReflectedMethodDescriptor whenMethodExists(String operationId, int bodyParameterIndex, List<SparklingParameter> parametersForDebugging, Object controller, Method method) {
        if (method.getParameters().length < 2 || method.getParameterTypes()[0] != Request.class || method.getParameterTypes()[1] != Response.class) {
            System.out.println("    ERROR: First two parameters are not Request, Respoinse in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<Object[], ?>) items -> {
                throw new UnavailableControllerSparklingException("First two parameters are not Request, Respoinse in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
            }, String.class);

        } else if (parametersForDebugging.size() == (method.getParameters().length - 2)) {
            return whenMethodMatches(operationId, bodyParameterIndex, controller, method);

        } else {
            System.out.println("    ERROR: Expected " + (parametersForDebugging.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<Object[], ?>) items -> {
                throw new UnavailableControllerSparklingException("Expected " + (parametersForDebugging.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
            }, String.class);
        }
    }

    private ReflectedMethodDescriptor whenMethodMatches(String operationId, int bodyParameterIndex, Object controller, Method method) {
        Function<Object[], ?> implementation = items -> invokeController(operationId, controller, method, items);
        Class<?> bodyPojoClass = resolveBodyPojoClass(bodyParameterIndex, controller, method);

        return new ReflectedMethodDescriptor(implementation, bodyPojoClass);
    }

    private Class<?> resolveBodyPojoClass(int bodyParameterIndex, Object controller, Method method) {
        Class<?> bodyPojoClass;
        if (bodyParameterIndex != -1) {
            bodyPojoClass = method.getParameterTypes()[bodyParameterIndex + FIRST_PARAMETER_INDEX];
            System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with a body class: " + bodyPojoClass.getSimpleName());

        } else {
            bodyPojoClass = String.class;
            System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") without a body class");
        }

        return bodyPojoClass;
    }

    private Optional<Method> resolveMatchingMethodByName(String operationId, Object controller) {
        return Arrays.stream(controller.getClass().getMethods())
                .filter(m -> operationId.equals(m.getName()))
                .findFirst();
    }

    private Object invokeController(String operationId, Object controller, Method method, Object[] items) {
        try {
            return method.invoke(controller, items);

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
