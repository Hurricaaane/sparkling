package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.ISparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
class InternalSparklingRoute implements Route {
    private final Function<Object[], ?> implementation;
    private final List<ISparklingRequestTransformer> availableConsumers;
    private final List<SparklingParameter> parameters;
    private final ISparklingDeserializer deserializer;
    private final Class<?> bodyPojoClass;

    public InternalSparklingRoute(Function<Object[], ?> implementation, List<ISparklingRequestTransformer> availableConsumers, List<SparklingParameter> parameters, ISparklingDeserializer deserializer, Class<?> bodyPojoClass) {
        this.implementation = implementation;
        this.availableConsumers = availableConsumers;
        this.parameters = parameters;
        this.deserializer = deserializer;
        this.bodyPojoClass = bodyPojoClass;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String contentType = request.contentType();
        ISparklingRequestTransformer consumer = getMatchingConsumer(contentType);

        List<Object> extractedParameters = extractParameters(request, response, consumer);

        Object returnedObject = implementation.apply(extractedParameters.toArray());
        Object entity = applyResponse(request, response, returnedObject);

        return entity;
    }

    private Object applyResponse(Request request, Response response, Object returnedObject) {
        if (returnedObject instanceof SparklingResponseContext) {
            SparklingResponseContext sparklingResponseContext = (SparklingResponseContext) returnedObject;

            response.status(sparklingResponseContext.getStatus());
            String contentType = sparklingResponseContext.getContentType();
            if (contentType != null) {
                response.type(contentType);
            }
            for (Map.Entry<String, List<String>> header : sparklingResponseContext.getHeaders().entrySet()) {
                List<String> headerValues = header.getValue();
                for (String value : headerValues) {
                    response.header(header.getKey(), value);
                }
            }

            // FIXME: Transform response
            return sparklingResponseContext.getEntity();

        } else {
            // FIXME: Transform response
            String acceptHeader = request.headers("Accept");
            if (acceptHeader != null && "application/json".equals(acceptHeader)) {
                return new Gson().toJson(returnedObject);

            } else {
                // FIXME: How to handle other formats than JSON
                return returnedObject;
            }
        }
    }

    private List<Object> extractParameters(Request request, Response response, ISparklingRequestTransformer consumer) {
        List<Object> extractedParameters = new ArrayList<>();
        extractedParameters.add(request);
        extractedParameters.add(response);

        for (SparklingParameter parameter : parameters) {
            Object item;
            switch (parameter.getLocation()) {
                case PATH:
                    item = deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), request.params(parameter.getName()));
                    break;
                case QUERY:
                    item = deserializeQuery(request, parameter);
                    break;
                case HEADER:
                    item = deserializeHeader(request, parameter);
                    break;
                case BODY:
                    item = consumer.transform(request, parameter, deserializer, bodyPojoClass);
                    break;
                case FORM:
                    item = consumer.transform(request, parameter, deserializer, bodyPojoClass);
                    break;
                default:
                    throw new TransformationFailedInternalSparklingException("Unknown location");
            }
            if (parameter.getArrayType() != ArrayType.NONE) {
                extractedParameters.add(item);

            } else {
                List itemAsList = (List) item;
                if (item != null && !itemAsList.isEmpty()) {
                    extractedParameters.add(itemAsList.get(0));

                } else {
                    extractedParameters.add(null);
                }
            }
        }
        return extractedParameters;
    }

    private Object deserializeQuery(Request request, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() != ArrayType.MULTI) {
            item = deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));

        } else {
            String[] queryParamsValues = request.queryParamsValues(parameter.getName());
            if (queryParamsValues != null) {
                item = Arrays.stream(queryParamsValues)
                        .map(queryParam -> deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), queryParam))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
            } else {
                // FIXME: Possible semantic difference between an optional query and a query with zero items
                item = new ArrayList<>();
            }
        }
        return item;
    }

    private Object deserializeHeader(Request request, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() != ArrayType.MULTI) {
            item = deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), request.headers(parameter.getName()));

        } else {
            item = Collections.list(request.raw().getHeaders(parameter.getName())).stream()
                    .map(queryParam -> deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), queryParam))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        return item;
    }

    private ISparklingRequestTransformer getMatchingConsumer(String contentType) {
        return availableConsumers.stream()
                .filter(availableConsumer -> availableConsumer.getApplicableContentTypes().contains(contentType))
                .findFirst()
                // FIXME: Handle non consumable requests
//              .orElseThrow(() -> new ApiSparklingException(ApiSparklingCode.NOT_ACCEPTABLE));
                .orElse(CommonSparklingRequestTransformer.APPLICATION_JSON);
    }
}
