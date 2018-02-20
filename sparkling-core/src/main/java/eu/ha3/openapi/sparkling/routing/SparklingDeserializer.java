package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;

import java.io.InputStream;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface SparklingDeserializer {
    List<Object> deserializeMultiValuedPart(DeserializeInto type, ArrayType arrayType, InputStream part, String partFilename, String possiblePartEncoding);
    Object deserializeSingleValuedPart(DeserializeInto type, InputStream part, String partFilename, String possiblePartEncoding);
    List<Object> deserializeMultiValued(DeserializeInto type, ArrayType arrayType, String content);
    Object deserializeSingleValued(DeserializeInto type, String content);
}
