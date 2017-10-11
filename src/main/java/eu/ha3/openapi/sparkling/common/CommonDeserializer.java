package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class CommonDeserializer implements SparklingDeserializer {
    public CommonDeserializer() {
    }

    @Override
    public List<Object> deserializeMultiValuedPart(DeserializeInto type, ArrayType arrayType, InputStream part) {
        try (InputStream closingInputStream = part) {
            // FIXME: Suspicious stream to string encoding, Where should encoding come from? (possible passed as a parameter)
            String content = IOUtils.toString(closingInputStream, StandardCharsets.UTF_8);
            return deserializeMultiValued(type, arrayType, content);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Object deserializeSingleValuedPart(DeserializeInto type, InputStream part) {
        if (type == DeserializeInto.BYTE_STREAM) {
            // Stream must not be closed
            return part;

        } else if (type == DeserializeInto.FILE) {
            // Stream must not be closed
            return part;

        } else {
            try (InputStream closingInputStream = part) {
                // FIXME: Suspicious stream to string encoding, Where should encoding come from? (possible passed as a parameter)
                String content = IOUtils.toString(closingInputStream, StandardCharsets.UTF_8);
                return deserializeSingleValued(type, content);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public List<Object> deserializeMultiValued(DeserializeInto type, ArrayType arrayType, String content) {
        return handleArrayTypeAsStream(type, arrayType, content)
                .map(s -> deserializeSingleValued(type, content))
                .collect(Collectors.toList());
    }

    @Override
    public Object deserializeSingleValued(DeserializeInto type, String content) {
        if (type == DeserializeInto.INT) {
            return Integer.parseInt(content);

        } else if (type == DeserializeInto.LONG) {
            return Long.parseLong(content);

        } else if (type == DeserializeInto.FLOAT) {
            return Float.parseFloat(content);

        } else if (type == DeserializeInto.DOUBLE) {
            return Double.parseDouble(content);

        } else if (type == DeserializeInto.STRING) {
            return content;

        } else if (type == DeserializeInto.BYTE_ARRAY) {
            return Base64.getDecoder().decode(content);

        } else if (type == DeserializeInto.BYTE_STREAM) {
            throw new TransformationFailedInternalSparklingException("Internal error, Byte stream deserialization must be treated as an input stream");

        } else if (type == DeserializeInto.BOOLEAN) {
            return Boolean.parseBoolean(content);

        } else if (type == DeserializeInto.DATE) {
            // If we're using Spark, we're using Java 8 => java.time is available
            return LocalDateTime.parse(content);

        } else if (type == DeserializeInto.OFFSET_DATE_TIME) {
            // If we're using Spark, we're using Java 8 => java.time is available
            return OffsetDateTime.parse(content);

        } else if (type == DeserializeInto.STRING_CONFIDENTIAL) {
            return content.toCharArray();

        } else if (type == DeserializeInto.FILE) {
            throw new TransformationFailedInternalSparklingException("Internal error, File deserialization must be treated as an input stream");

        } else {
            throw new TransformationFailedInternalSparklingException("Unsupported deserialization type");
        }
    }

    private Stream<String> handleArrayTypeAsStream(DeserializeInto type, ArrayType arrayType, String content) {
        Stream<String> stream;
        if (arrayType == ArrayType.NONE) {
            stream = Stream.of(content);

        } else if (arrayType == ArrayType.CSV) {
            stream = Arrays.stream(content.split(","));

        } else if (arrayType == ArrayType.SSV) {
            stream = Arrays.stream(content.split(" "));

        } else if (arrayType == ArrayType.TSV) {
            stream = Arrays.stream(content.split("\t"));

        } else if (arrayType == ArrayType.PIPES) {
            stream = Arrays.stream(content.split("\\|"));

        } else if (arrayType == ArrayType.MULTI) {
            // Multi is handled by the caller, which will call the deserializer multiple times and flat map the arrays
            stream = Stream.of(content);

        } else {
            throw new TransformationFailedInternalSparklingException("Invalid array type");
        }

        return stream;
    }
}
