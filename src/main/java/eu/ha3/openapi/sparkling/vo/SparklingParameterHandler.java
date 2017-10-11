package eu.ha3.openapi.sparkling.vo;

import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.ParameterLocation;
import eu.ha3.openapi.sparkling.exception.ParseSparklingException;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public final class SparklingParameterHandler {
    public SparklingParameterHandler() {
    }

    public static SparklingParameter ofBody(Parameter parameter) {
        if ("body".equals(parameter.getIn()) && parameter instanceof BodyParameter) {
            BodyParameter body = (BodyParameter) parameter;
            // FIXME: Support binary schemas

            return new SparklingParameter(parameter.getName(), ParameterLocation.BODY, ArrayType.NONE, DeserializeInto.STRING, toRequirement(parameter.getRequired(), parameter.getAllowEmptyValue()));

        } else {
            throw new ParseSparklingException("Unknown parameter, expected body");
        }
    }

    public static SparklingParameter from(SerializableParameter parameter) {
        DeserializeInto deserialize;

        ArrayType arrayType;
        if ("array".equals(parameter.getType())) {
            arrayType = toCollectionType(parameter.getCollectionFormat());
            deserialize = toDeserializationType(parameter.getItems().getFormat(), parameter.getItems().getType());

        } else {
            arrayType = ArrayType.NONE;
            deserialize = toDeserializationType(parameter.getFormat(), parameter.getType());
        }

        ParameterLocation parameterLocation = toLocation(parameter.getIn());

        return new SparklingParameter(parameter.getName(), parameterLocation, arrayType, deserialize, toRequirement(parameter.getRequired(), parameter.getAllowEmptyValue()));
    }

    private static SparklingRequirement toRequirement(boolean required, Boolean allowEmptyValue) {
        if (allowEmptyValue == null) {
            allowEmptyValue = false;
        }

        SparklingRequirement requirement;
        if (required) {
            if (allowEmptyValue) {
                requirement = SparklingRequirement.REQUIRED;

            } else {
                requirement = SparklingRequirement.REQUIRED_AND_NOT_EMPTY;
            }
        } else {
            requirement = SparklingRequirement.OPTIONAL;
        }

        return requirement;
    }

    private static ParameterLocation toLocation(String in) {
        ParameterLocation location;
        if ("path".equals(in)) {
            location = ParameterLocation.PATH;

        } else if ("query".equals(in)) {
            location = ParameterLocation.QUERY;

        } else if ("header".equals(in)) {
            location = ParameterLocation.HEADER;

        } else if ("body".equals(in)) {
            location = ParameterLocation.BODY;

        } else if ("formData".equals(in)) {
            location = ParameterLocation.FORM;

        } else {
            throw new ParseSparklingException("Parameter has an unknown location");
        }
        return location;
    }

    private static ArrayType toCollectionType(String collectionFormatNullable) {
        ArrayType arrayType;
        if (collectionFormatNullable == null || "csv".equals(collectionFormatNullable)) {
            arrayType = ArrayType.CSV;

        } else if ("ssv".equals(collectionFormatNullable)) {
            arrayType = ArrayType.SSV;

        } else if ("tsv".equals(collectionFormatNullable)) {
            arrayType = ArrayType.TSV;

        } else if ("pipes".equals(collectionFormatNullable)) {
            arrayType = ArrayType.PIPES;

        } else if ("multi".equals(collectionFormatNullable)) {
            arrayType = ArrayType.MULTI;

        } else {
            arrayType = ArrayType.UNSPECIFIED;
        }
        return arrayType;
    }

    private static DeserializeInto toDeserializationType(String format, String type) {
        DeserializeInto deserialize;
        if (type.equals("integer")) {
            deserialize = whenInteger(format);

        } else if (type.equals("number")) {
            deserialize = whenDouble(format);

        } else if (type.equals("string")) {
            deserialize = whenString(format);

        } else if (type.equals("boolean")) {
            deserialize = DeserializeInto.BOOLEAN;

        } else if (type.equals("file")) {
            deserialize = DeserializeInto.FILE;

        } else {
            throw new ParseSparklingException("Parameter has an unknown type");
        }
        return deserialize;
    }

    private static DeserializeInto whenInteger(String format) {
        DeserializeInto deserialize;
        if (format == null || "int32".equals(format)) {
            deserialize = DeserializeInto.INT;

        } else if ("int64".equals(format)) {
            deserialize = DeserializeInto.LONG;

        } else {
            throw unknownFormatException();
        }
        return deserialize;
    }

    private static DeserializeInto whenDouble(String format) {
        DeserializeInto deserialize;
        if (format == null || "float".equals(format)) {
            deserialize = DeserializeInto.FLOAT;

        } else if ("double".equals(format)) {
            deserialize = DeserializeInto.DOUBLE;

        } else {
            throw unknownFormatException();
        }
        return deserialize;
    }

    private static DeserializeInto whenString(String format) {
        DeserializeInto deserialize;
        if (format == null) {
            deserialize = DeserializeInto.STRING;

        } else if ("password".equals(format)) {
            deserialize = DeserializeInto.STRING_CONFIDENTIAL;

        } else if ("byte".equals(format)) {
            deserialize = DeserializeInto.BYTE_ARRAY;

        } else if ("binary".equals(format)) {
            deserialize = DeserializeInto.BYTE_STREAM;

        } else if ("date".equals(format)) {
            deserialize = DeserializeInto.DATE;

        } else if ("date-time".equals(format)) {
            deserialize = DeserializeInto.OFFSET_DATE_TIME;

        } else {
            throw unknownFormatException();
        }
        return deserialize;
    }

    private static ParseSparklingException unknownFormatException() {
        return new ParseSparklingException("Parameter has a known type associated with an unknown format");
    }
}
