package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ArrayType;

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
}
