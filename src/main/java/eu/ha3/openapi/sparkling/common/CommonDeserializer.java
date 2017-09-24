package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class CommonDeserializer implements ISparklingDeserializer {
    @Override
    public List<?> deserializePart(DeserializeInto type, ArrayType arrayType, InputStream part) {
        return null;
    }

    @Override
    public List<?> deserializeSimple(DeserializeInto type, ArrayType arrayType, String content) {
        if (type == DeserializeInto.INT) {
            return Arrays.asList(Integer.parseInt(content));

        } else if (type == DeserializeInto.LONG) {
            return Arrays.asList(Long.parseLong(content));

        }
        return null;
    }
}
