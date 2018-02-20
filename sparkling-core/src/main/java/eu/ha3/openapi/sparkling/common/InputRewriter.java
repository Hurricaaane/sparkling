package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * (Default template)
 * Created on 2018-02-18
 *
 * @author Ha3
 */
@Deprecated
public class InputRewriter {
    // FIXME: What was the point of this? Isn't this already achieved in the Modelizer ?
    public static List<Object> rewriteAllInputsToBeCloserToMethodTypes(List<Object> inputs, List<Type> reflectedTypes, List<SparklingParameter> parameters) {
        List<Object> objects = rewriteInputsToBeCloserToMethodTypes(inputs, reflectedTypes, parameters);
        return objects;
    }

    private static List<Object> rewriteInputsToBeCloserToMethodTypes(List<Object> inputs, List<Type> reflectedTypes, List<SparklingParameter> parameters) {
        return IntStream.range(0, parameters.size())
                .mapToObj(i -> rewriteInputToBeCloserToMethodTypes(inputs.get(i), reflectedTypes.get(i), parameters.get(i)))
                .collect(Collectors.toList());
    }

    private static Object rewriteInputToBeCloserToMethodTypes(Object input, Type runtimeType, SparklingParameter parameter) {
        if (parameter.getType() == DeserializeInto.STRING) {
            if (input != null) {
                return rewriteInputIfApplicable(input, parameter, runtimeType);
            }
        }

        return input;
    }

    private static Object rewriteInputIfApplicable(Object input, SparklingParameter parameter, Type runtimeTypeInController) {
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

    private static boolean isNotStringType(Type runtimeTypeInController) {
        return runtimeTypeInController instanceof ParameterizedType && ((ParameterizedType) runtimeTypeInController).getRawType() != String.class;
    }

    private static boolean isNotStringCollection(Type runtimeTypeInController) {
        return runtimeTypeInController instanceof ParameterizedType && ((ParameterizedType) runtimeTypeInController).getActualTypeArguments()[0] != String.class;
    }
}
