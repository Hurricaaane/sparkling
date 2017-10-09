package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.SparklingRequestAggregator;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.InputStream;
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
    private final Function<List<Object>, ?> implementation;
    private final List<SparklingRequestAggregator> availableConsumers;
    private final List<SparklingParameter> parameters;
    private final SparklingDeserializer deserializer;

    public InternalSparklingRoute(Function<List<Object>, ?> implementation, List<SparklingRequestAggregator> availableConsumers, List<SparklingParameter> parameters, SparklingDeserializer deserializer) {
        this.implementation = implementation;
        this.availableConsumers = availableConsumers;
        this.parameters = parameters;
        this.deserializer = deserializer;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String contentType = request.contentType();
        SparklingRequestAggregator aggregator = getMatchingAggregator(contentType);

        List<Object> extractedParameters = extractParameters(request, response, aggregator);

        Object returnedObject = implementation.apply(extractedParameters);
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

        } else if (returnedObject instanceof InputStream) {
            return returnedObject;

        } else {
            // FIXME: Transform response
            String acceptHeader = request.headers("Accept");
            if (acceptHeader == null || "application/json".equals(acceptHeader)) {
                if (response.type() == null) {
                    response.type("application/json");
                }
                return new Gson().toJson(returnedObject);

            } else {
                // FIXME: How to handle other formats than JSON
                if (response.type() == null) {
                    response.type("application/json");
                }
                return new Gson().toJson(returnedObject);
            }
        }
    }

    private List<Object> extractParameters(Request request, Response response, SparklingRequestAggregator aggregator) {
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
                    item = aggregator.transform(request, parameter, deserializer);
                    break;
                case FORM:
                    item = aggregator.transform(request, parameter, deserializer);
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

    private SparklingRequestAggregator getMatchingAggregator(String contentType) {
        return availableConsumers.stream()
                .filter(availableConsumer -> availableConsumer.getApplicableContentTypes().contains(contentType))
                .findFirst()
                // FIXME: Handle non consumable requests
//              .orElseThrow(() -> new ApiSparklingException(ApiSparklingCode.NOT_ACCEPTABLE));
                .orElse(CommonSparklingRequestAggregator.APPLICATION_JSON);
    }
}
