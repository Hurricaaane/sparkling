package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

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
 * Created on 2017-10-08
 *
 * @author Ha3
 */
class ImplementationMatcher {
    private static final int FIRST_PARAMETER_INDEX = 2;

    private final List<?> controllers;

    public ImplementationMatcher(List<?> controllers) {
        this.controllers = controllers;
    }


    public ReflectedMethodDescriptor resolveControllerImplementation(String operationId, String controllerHint, int bodyParameterIndex, List<SparklingParameter> parametersForDebugging) {
        if (parametersForDebugging.size() != 0) {
            System.out.println("Searching for method (" + controllerHint + "*)." + operationId + " with parameters able to accommodate input parameters: " + parametersForDebugging.stream().map(ImplementationMatcher::debuggableParameterString).collect(Collectors.joining(", ")));

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
            Class<?> possibleBodyPojoClass = method.getParameterTypes()[bodyParameterIndex + FIRST_PARAMETER_INDEX];
//            if (List.class.isAssignableFrom(possibleBodyPojoClass)) {
//                // FIXME: This is a huge mess that will not work because of application/json body requiring the Container of POJO, and multipart/form-data requiring the POJO
//                // FIXME: Possible bad class resolution
//                Type type = ((ParameterizedType) method.getGenericParameterTypes()[bodyParameterIndex + FIRST_PARAMETER_INDEX])
//                        .getActualTypeArguments()[0];
//                if (type instanceof Class) {
//                    bodyPojoClass = (Class<?>) type;
//                    System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with a complex list body class: " + bodyPojoClass.getSimpleName());
//
//                } else {
//                    bodyPojoClass = String.class;
//                    System.out.println("    ERROR: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + "), but body class is a List which generic type parameter isn't a class; will default to " + bodyPojoClass.getSimpleName() + " instead");
//                }
//
//            } else {
                bodyPojoClass = possibleBodyPojoClass;
                System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with a body class: " + bodyPojoClass.getSimpleName());
//            }

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

    private Object invokeController(String operationId, Object controller, Method method, Object[] items) {
        try {
            return method.invoke(controller, items);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId, e);
        }
    }
}
