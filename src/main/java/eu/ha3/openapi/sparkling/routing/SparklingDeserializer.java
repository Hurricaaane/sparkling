package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;

import javax.servlet.http.Part;
import java.io.InputStream;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface SparklingDeserializer {
    List<Object> deserializeMultiValuedPart(DeserializeInto type, ArrayType arrayType, InputStream part);
    Object deserializeSingleValuedPart(DeserializeInto type, InputStream part, Part part1);
    List<Object> deserializeMultiValued(DeserializeInto type, ArrayType arrayType, String content);
    Object deserializeSingleValued(DeserializeInto type, String content);
}
