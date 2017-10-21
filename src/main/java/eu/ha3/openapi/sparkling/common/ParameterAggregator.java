package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-10-11
 *
 * @author Ha3
 */
class ParameterAggregator {
    private final SparklingDeserializer deserializer;

    ParameterAggregator(SparklingDeserializer deserializer) {
        this.deserializer = deserializer;
    }

    public Object aggregateParameter(Request request, SparklingParameter parameter, FormType formType, Collection<Part> parts) {
        Object item;
        switch (parameter.getLocation()) {
            case PATH:
                item = deserializePath(request, parameter);
                break;
            case QUERY:
                item = deserializeQuery(request, parameter);
                break;
            case HEADER:
                item = deserializeHeader(request, parameter);
                break;
            case BODY:
                item = deserializeBody(request, parameter);
                break;
            case FORM:
                item = deserializeForm(request, parameter, formType, parts);
                break;
            default:
                throw new TransformationFailedInternalSparklingException("Unknown location");
        }
        return item;
    }

    private Object deserializePath(Request request, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return deserializer.deserializeSingleValued(parameter.getType(), request.params(parameter.getName()));

        } else {
            return deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.params(parameter.getName()));
        }
    }

    private Object deserializeQuery(Request request, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            String[] queryParamsValues = request.queryParamsValues(parameter.getName());
            if (queryParamsValues != null) {
                return Arrays.stream(queryParamsValues)
                                .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                                .collect(Collectors.toList());
            } else {
                // FIXME: Possible semantic difference between an optional query and a query with zero items
                return new ArrayList<>();
            }

        } else {
            return deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));
        }
    }

    private Object deserializeHeader(Request request, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            return Collections.list(request.raw().getHeaders(parameter.getName())).stream()
                        .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                        .collect(Collectors.toList());
        } else {
            return deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.headers(parameter.getName()));
        }
    }

    private Object deserializeBody(Request request, SparklingParameter parameter) {
        return request.body();
    }

    private Object deserializeForm(Request request, SparklingParameter parameter, FormType formType, Collection<Part> parts) {
        // FIXME Need some sort of content type check on "consumes"
        if (formType == FormType.MULTIPART) {
            return deserializeMultiForm(parts, parameter);

        } else if (formType == FormType.URL_ENCODED) {
            return deserializeApplicationForm(request, parameter);

        } else {
            // FIXME Non matching content type! What to do?
            return null;
        }
    }

    private Object deserializeMultiForm(Collection<Part> parts, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return parts.stream()
                    .filter(part -> parameter.getName().equals(part.getName()))
                    .findFirst()
                    .map(part -> deserializeSingleValuedPart(parameter, part))
                    .orElse(null);

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            return parts.stream().filter(part -> parameter.getName().equals(part.getName()))
                    .map(part -> deserializeSingleValuedPart(parameter, part))
                    .collect(Collectors.toList());

        } else {
            return parts.stream()
                    .filter(part -> parameter.getName().equals(part.getName()))
                    .findFirst()
                    .map(part -> deserializeMultiValuedPart(parameter, part))
                    .orElse(null);
        }
    }

    private Object deserializeApplicationForm(Request request, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            String[] queryParamsValues = request.queryParamsValues(parameter.getName());
            if (queryParamsValues != null) {
                return Arrays.stream(queryParamsValues)
                        .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                        .collect(Collectors.toList());

            } else {
                return Collections.emptyList();
            }

        } else {
            return deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));
        }
    }

    private List<Object> deserializeMultiValuedPart(SparklingParameter parameter, Part part) {
        try {
            return deserializer.deserializeMultiValuedPart(parameter.getType(), parameter.getArrayType(), part.getInputStream(), toHeadersMap(part));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Object deserializeSingleValuedPart(SparklingParameter parameter, Part part) {
        try {
            return deserializer.deserializeSingleValuedPart(parameter.getType(), part.getInputStream(), toHeadersMap(part));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, List<String>> toHeadersMap(Part part) {
        return part.getHeaderNames().stream()
                .collect(Collectors.toMap(o -> o, o -> new ArrayList<>(part.getHeaders(o))));
    }
}
