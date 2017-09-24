package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;

import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface ISparklingRequestTransformer {
    List<String> getApplicableContentTypes();

    List<?> transform(Request request, SparklingParameter parameter, ISparklingDeserializer deserializer);
}
