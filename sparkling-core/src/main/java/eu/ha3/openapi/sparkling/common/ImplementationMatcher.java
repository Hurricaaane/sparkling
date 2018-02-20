package eu.ha3.openapi.sparkling.common;

import com.google.gson.reflect.TypeToken;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.vo.Question;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-10-08
 *
 * @author Ha3
 */
class ImplementationMatcher {
    static final int FIRST_PARAMETER_INDEX = 2;

    private final List<?> controllers;

    public ImplementationMatcher(List<?> controllers) {
        this.controllers = controllers;
    }

    public ControllerInvoker resolveControllerImplementation(String operationId, String controllerHint, List<SparklingParameter> parameters) {
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

            return new NoImplementationControllerInvoker(parameters, "No controller matching " + controllerHint + " available for operation " + operationId);
        }
    }

    private static String debuggableParameterString(SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return parameter.getType().name() + "@" + parameter.getLocation() + " " + parameter.getName();

        } else {
            return "List<" + parameter.getType().name() + ">" + parameter.getArrayType().name() + "@" + parameter.getLocation() + " " + parameter.getName();
        }
    }

    private ControllerInvoker whenControllerExists(String operationId, List<SparklingParameter> parameters, Object controller) {
        List<Method> matchingMethods = resolveMatchingMethodsByName(operationId, controller);
        if (!matchingMethods.isEmpty()) {
            return whenMethodExists(operationId, parameters, controller, matchingMethods);

        } else {
            System.out.println("    WARNING: No method " + operationId + " found in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);

            return new NoImplementationControllerInvoker(parameters, "No method " + operationId + " available in controller " + controller.getClass().getSimpleName() + " to call operation " + operationId);
        }
    }

    private ControllerInvoker whenMethodExists(String operationId, List<SparklingParameter> parameters, Object controller, List<Method> methods) {
        Optional<Method> questionMethod = methods.stream()
                .filter(method -> method.getParameters().length == 1)
                .findFirst();

        return questionMethod
                .map(method -> withQuestionMethod(operationId, parameters, controller, method))
                .orElseGet(() -> withOrderedMethod(operationId, parameters, controller, methods));
    }

    private ControllerInvoker withOrderedMethod(String operationId, List<SparklingParameter> parameters, Object controller, List<Method> methods) {
        Optional<Method> orderedMethod = methods.stream()
                .filter(method -> method.getParameters().length > 1)
                .findFirst();

        return orderedMethod
                .map(method -> withOrderedMethod(operationId, parameters, controller, method))
                .orElseGet(() -> {
                    System.out.println("    ERROR: No method candidate for " + controller.getClass().getSimpleName() + "." + operationId + " to call operation " + operationId);

                    return new NoImplementationControllerInvoker(parameters, "No method candidate for " + controller.getClass().getSimpleName() + "." + operationId + " to call operation " + operationId);
                });
    }

    private ControllerInvoker withQuestionMethod(String operationId, List<SparklingParameter> parameters, Object controller, Method method) {
        if (method.getParameterTypes()[0] != Question.class) {
            System.out.println("    ERROR: First and only parameter is not Question in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new NoImplementationControllerInvoker(parameters, "First and only parameter is not Question in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
        }


        List<Type> methodTypes = resolveQuestionRuntimeTypes(method);
        ParameterizedType type = (ParameterizedType) methodTypes.get(0);
        Type dataType = type.getActualTypeArguments()[0];

        TypeToken<?> questionType = TypeToken.get(dataType);
        Class<?> rawQuestionType = questionType.getRawType();

        Field[] declaredFields = rawQuestionType.getDeclaredFields();

        Map<SparklingParameter, Type> reflectedTypes = new LinkedHashMap<>();
        List<String> missingFields = new ArrayList<>();
        for (SparklingParameter parameter : parameters) {
            Optional<Field> dataTypeField = Arrays.stream(declaredFields)
                    .filter(field -> field.getName().equals(parameter.getName()))
                    .findFirst();
            if (dataTypeField.isPresent()) {
                Field field = dataTypeField.get();
                Type genericType = field.getGenericType();
                reflectedTypes.put(parameter, genericType);

            } else {
                missingFields.add(parameter.getName());
            }
        }

        if (!missingFields.isEmpty()) {
            System.out.println("    ERROR: In data type " + dataType + ", missing fields (" + missingFields.stream().collect(Collectors.joining(", ")) + ") for method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new NoImplementationControllerInvoker(parameters, "First two parameters are not Request, Response in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);
        }

        System.out.println("    OK: Resolving question " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with parameters: " + reflectedTypes);

        return new QuestionControllerInvoker(operationId, controller, method, parameters, reflectedTypes, dataType);
    }

    private ControllerInvoker withOrderedMethod(String operationId, List<SparklingParameter> parameters, Object controller, Method method) {
        if (method.getParameters().length < 2 || method.getParameterTypes()[0] != Request.class || method.getParameterTypes()[1] != Response.class) {
            System.out.println("    ERROR: First two parameters are not Request, Response in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new NoImplementationControllerInvoker(parameters, "First two parameters are not Request, Response in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

        } else if (parameters.size() != (method.getParameters().length - 2)) {
            System.out.println("    ERROR: Expected " + (parameters.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

            return new NoImplementationControllerInvoker(parameters, "Expected " + (parameters.size() + 2) +  " parameters but found incorrect count of " + method.getParameters().length + " in method " + controller.getClass().getSimpleName() + "." + method.getName() + " to call operation " + operationId);

        } else {
            return whenOrderedMethodMatches(operationId, controller, method, parameters);
        }
    }

    private ControllerInvoker whenOrderedMethodMatches(String operationId, Object controller, Method method, List<SparklingParameter> parameters) {
        List<Type> argumentTypes = resolveOrderedArgumentTypes(method);
        System.out.println("    OK: Resolved " + controller.getClass().getSimpleName() + "." + method.getName() + "(" + Arrays.stream(method.getParameters()).map(parameter -> parameter.getType().getSimpleName()).collect(Collectors.joining(", ")) + ") with parameters: " + argumentTypes);

        Map<SparklingParameter, Type> reflectedTypes = new LinkedHashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            SparklingParameter parameter = parameters.get(i);
            Type reflectedType = argumentTypes.get(i);

            reflectedTypes.put(parameter, reflectedType);
        }

        return new OrderedControllerInvoker(operationId, controller, method, parameters, reflectedTypes);
    }

    private List<Type> resolveQuestionRuntimeTypes(Method method) {
        List<Type> types = Arrays.asList(method.getGenericParameterTypes());

        return types;
    }

    private List<Type> resolveOrderedArgumentTypes(Method method) {
        List<Type> types = Arrays.asList(method.getGenericParameterTypes());
        List<Type> expectedTypes = new ArrayList<>(types.subList(FIRST_PARAMETER_INDEX, types.size()));

        return expectedTypes;
    }

    private List<Method> resolveMatchingMethodsByName(String operationId, Object controller) {
        return Arrays.stream(controller.getClass().getMethods())
                .filter(m -> operationId.equals(m.getName()))
                .collect(Collectors.toList());
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
