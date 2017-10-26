package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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


    public ReflectedMethodDescriptor resolveControllerImplementation(String operationId, String controllerHint, List<SparklingParameter> parameters) {
        if (parameters.size() != 0) {
            System.out.println("Searching for method (" + controllerHint + "*)." + operationId + " with parameters able to accommodate input parameters: " + parameters.stream().map(ImplementationMatcher::debuggableParameterString).collect(Collectors.joining(", ")));

        } else {
            System.out.println("Searching for method (" + controllerHint + "*)." + operationId + " with parameters able to accommodate zero input parameters...");
        }

        Object controller = resolveController(controllerHint);

        if (controller != null) {
            return whenControllerExists(operationId, parameters, controller);

        } else {
            System.out.println("    WARNING: No controller found matching (" + controllerHint + "*) for operation " + operationId);

            return new ReflectedMethodDescriptor((Function<List<Object>, ?>) (List<Object> items) -> {
                throw new UnavailableControllerSparklingException("No controller matching " + controllerHint + " available for operation " + operationId);
            }, Collections.emptyList());
        }
    }

    private static String debuggableParameterString(SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return parameter.getType().name() + "@" + parameter.getLocation() + " " + parameter.getName();

        } else {
            return "List<" + parameter.getType().name() + ">" + parameter.getArrayType().name() + "@" + parameter.getLocation() + " " + parameter.getName();
        }
    }

    private ReflectedMethodDescriptor whenControllerExists(String operationId, List<SparklingParameter> parameters, Object controller) {
        Optional<Method> matchingMethod = resolveMatchingMethodByName(operationId, controller);
        if (matchingMethod.isPresent()) {
            return whenMethodExists(operationId, parameters, controller, matchingMethod.get());

        } else {
            System.out.println("    WARNING: No method " + operationId + " found in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<List<Object>, ?>) items -> {
                throw new UnavailableControllerSparklingException("No method " + operationId + " available in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);
            }, Collections.emptyList());
        }
    }

    private ReflectedMethodDescriptor whenMethodExists(String operationId, List<SparklingParameter> parameters, Object controller, Method method) {
        if (method.getParameters().length < 2 || method.getParameterTypes()[0] != Request.class || method.getParameterTypes()[1] != Response.class) {
            System.out.println("    ERROR: First two parameters are not Request, Respoinse in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<List<Object>, ?>) items -> {
                throw new UnavailableControllerSparklingException("First two parameters are not Request, Respoinse in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
            }, Collections.emptyList());

        } else if (parameters.size() == (method.getParameters().length - 2)) {
            return whenMethodMatches(operationId, controller, method, parameters);

        } else {
            System.out.println("    ERROR: Expected " + (parameters.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new ReflectedMethodDescriptor((Function<List<Object>, ?>) items -> {
                throw new UnavailableControllerSparklingException("Expected " + (parameters.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
            }, Collections.emptyList());
        }
    }

    private ReflectedMethodDescriptor whenMethodMatches(String operationId, Object controller, Method method, List<SparklingParameter> parameters) {
        List<Type> runtimeTypes = resolveRuntimeTypes(controller, method);
        Function<List<Object>, ?> implementation = inputs -> invokeController(operationId, controller, method, inputs, runtimeTypes, parameters);

        return new ReflectedMethodDescriptor(implementation, runtimeTypes);
    }

    private List<Type> resolveRuntimeTypes(Object controller, Method method) {
        List<Type> types = Arrays.asList(method.getGenericParameterTypes());
        List<Type> expectedTypes = new ArrayList<>(types.subList(FIRST_PARAMETER_INDEX, types.size()));

        System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with parameters: " + expectedTypes);

        return expectedTypes;
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

    private Object invokeController(String operationId, Object controller, Method method, List<Object> inputs, List<Type> runtimeTypes, List<SparklingParameter> parameters) {
        try {
            List<Object> typedInputs = rewriteAllInputsToBeCloserToMethodTypes(inputs, runtimeTypes, parameters);

            return method.invoke(controller, typedInputs.toArray());

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId, e);
        }
    }

    private List<Object> rewriteAllInputsToBeCloserToMethodTypes(List<Object> inputs, List<Type> runtimeTypes, List<SparklingParameter> parameters) {
        List<Object> objects = rewriteInputsToBeCloserToMethodTypes(inputs.subList(FIRST_PARAMETER_INDEX, inputs.size()), runtimeTypes, parameters);
        objects.add(0, inputs.get(0));
        objects.add(1, inputs.get(1));
        return objects;
    }

    private List<Object> rewriteInputsToBeCloserToMethodTypes(List<Object> inputs, List<Type> runtimeTypes, List<SparklingParameter> parameters) {
        return IntStream.range(0, parameters.size())
                .mapToObj(i -> rewriteInputToBeCloserToMethodTypes(inputs.get(i), runtimeTypes.get(i), parameters.get(i)))
                .collect(Collectors.toList());
    }

    private Object rewriteInputToBeCloserToMethodTypes(Object input, Type runtimeType, SparklingParameter parameter) {
        if (parameter.getType() == DeserializeInto.STRING) {
            if (input != null) {
                return rewriteInputIfApplicable(input, parameter, runtimeType);
            }
        }

        return input;
    }

    private Object rewriteInputIfApplicable(Object input, SparklingParameter parameter, Type runtimeTypeInController) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            if (isNotStringType(runtimeTypeInController)) {
                return new Gson().fromJson((String)input, runtimeTypeInController);
            }

        } else {
            if (isNotStringCollection(runtimeTypeInController)) {
                List<String> inputAsList = (List<String>) input;
                return inputAsList.stream()
                        .map(s -> new Gson().fromJson(s, ((ParameterizedType) runtimeTypeInController).getActualTypeArguments()[0]))
                        .collect(Collectors.toList());
            }
        }

        return input;
    }

    private boolean isNotStringType(Type runtimeTypeInController) {
        return runtimeTypeInController instanceof ParameterizedType && ((ParameterizedType) runtimeTypeInController).getRawType() != String.class;
    }

    private boolean isNotStringCollection(Type runtimeTypeInController) {
        return runtimeTypeInController instanceof ParameterizedType && ((ParameterizedType) runtimeTypeInController).getActualTypeArguments()[0] != String.class;
    }
}
