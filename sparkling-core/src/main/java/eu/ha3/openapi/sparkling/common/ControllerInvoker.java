package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * (Default template)
 * Created on 2018-02-18
 *
 * @author Ha3
 */
public interface ControllerInvoker {
    // FIXME: Law of Demeter violation
    Map<SparklingParameter, Type> getReflectedTypeMap();

    Object submit(Request request, Response response, Map<SparklingParameter, Object> models);
}
