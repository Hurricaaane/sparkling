package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;

import java.io.InputStream;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface ISparklingDeserializer {
    List<?> deserializePart(DeserializeInto type, ArrayType arrayType, InputStream part);
    List<?> deserializeSimple(DeserializeInto type, ArrayType arrayType, String content);
    <T> T deserializeSchema(String body, SparklingParameter parameter, Class<T> target);
}
