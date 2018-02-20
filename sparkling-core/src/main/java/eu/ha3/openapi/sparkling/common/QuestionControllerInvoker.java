package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.ha3.openapi.sparkling.exception.DeclareSparklingException;
import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.vo.Question;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
class QuestionControllerInvoker implements ControllerInvoker {
    private final String operationId;
    private final Object controller;
    private final Method method;
    private final Map<SparklingParameter, Type> reflectedTypes;
    private final Type dataType;
    private final Map<SparklingParameter, Field> reflectedFields;

    public QuestionControllerInvoker(String operationId, Object controller, Method method, List<SparklingParameter> parameters, Map<SparklingParameter, Type> reflectedTypes, Type dataType) {
        this.operationId = operationId;
        this.controller = controller;
        this.method = method;
        this.reflectedTypes = reflectedTypes;
        this.dataType = dataType;

        TypeToken<?> typeToken = TypeToken.get(dataType);
        Class<?> rawType = typeToken.getRawType();

        Field[] declaredFields = rawType.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (!declaredField.isAccessible()) {
                declaredField.setAccessible(true);
            }
        }

        reflectedFields = parameters.stream().collect(Collectors.toMap(o -> o, o -> Arrays.stream(declaredFields)
                .filter(field -> field.getName().equals(o.getName()))
                .findAny()
                .orElseThrow(DeclareSparklingException::new)));
    }

    @Override
    public Map<SparklingParameter, Type> getReflectedTypeMap() {
        return reflectedTypes;
    }

    @Override
    public Object submit(Request request, Response response, Map<SparklingParameter, Object> models) {
        try {
            Object dataType = convertInputsToDataType(models);

            Question<?>[] questionVarArgs = { new Question<>(request, response, dataType) };

            return method.invoke(controller, (Object[]) questionVarArgs);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UnavailableControllerSparklingException("Controller has failed to call this operation: " + operationId, e);
        }
    }

    private Object convertInputsToDataType(Map<SparklingParameter, Object> models) {
        try {
            Object dataObject = new Gson().fromJson("{}", dataType);

            for (Map.Entry<SparklingParameter, Object> model : models.entrySet()) {
                reflectedFields.get(model.getKey()).set(dataObject, model.getValue());
            }

            return dataObject;

        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
