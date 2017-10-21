package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface SparklingDeserializer {
    List<Object> deserializeMultiValuedPart(DeserializeInto type, ArrayType arrayType, InputStream part, Map<String, List<String>> stringListMap);
    Object deserializeSingleValuedPart(DeserializeInto type, InputStream part, Map<String, List<String>> partHeaders);
    List<Object> deserializeMultiValued(DeserializeInto type, ArrayType arrayType, String content);
    Object deserializeSingleValued(DeserializeInto type, String content);
}
