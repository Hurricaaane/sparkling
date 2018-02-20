package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.exception.UnavailableControllerSparklingException;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2018-02-18
 *
 * @author Ha3
 */
public class NoImplementationControllerInvoker implements ControllerInvoker {
    private final Map<SparklingParameter, Type> reflectedTypeMap;
    private final String reason;

    public NoImplementationControllerInvoker(List<SparklingParameter> parameters, String reason) {
        this.reason = reason;

        reflectedTypeMap = parameters.stream().collect(Collectors.toMap(o -> o, o -> (Type)Object.class));
    }

    @Override
    public Map<SparklingParameter, Type> getReflectedTypeMap() {
        return reflectedTypeMap;
    }

    @Override
    public Object submit(Request request, Response response, Map<SparklingParameter, Object> models) {
        throw new UnavailableControllerSparklingException(reason);
    }
}
