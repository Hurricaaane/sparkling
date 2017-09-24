package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final Function<Object[], SparklingResponseContext> implementation;
    private final List<ISparklingRequestTransformer> availableConsumers;
    private final List<SparklingParameter> parameters;
    private final ISparklingDeserializer deserializer;

    public InternalSparklingRoute(Function<Object[], SparklingResponseContext> implementation, List<ISparklingRequestTransformer> availableConsumers, List<SparklingParameter> parameters, ISparklingDeserializer deserializer) {
        this.implementation = implementation;
        this.availableConsumers = availableConsumers;
        this.parameters = parameters;
        this.deserializer = deserializer;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String contentType = request.contentType();
        ISparklingRequestTransformer consumer = getMatchingConsumer(contentType);

        List<Object> extractedParameters = extractParameters(request, consumer);

        SparklingResponseContext sparklingResponseContext = implementation.apply(extractedParameters.toArray());
        Object entity = applyResponse(sparklingResponseContext, response);

        return entity;
    }

    private Object applyResponse(SparklingResponseContext sparklingResponseContext, Response response) {
        response.status(sparklingResponseContext.getStatus());
        MediaType contentType = sparklingResponseContext.getContentType();
        if (contentType != null) {
            response.type(contentType.getType());
        }
        for (Map.Entry<String, List<String>> header : sparklingResponseContext.getHeaders().entrySet()) {
            List<String> headerValues = header.getValue();
            for (String value : headerValues) {
                response.header(header.getKey(), value);
            }
        }

        // FIXME: Transform response
        return sparklingResponseContext.getEntity();
    }

    private List<Object> extractParameters(Request request, ISparklingRequestTransformer consumer) {
        List<Object> extractedParameters = new ArrayList<>();
        extractedParameters.add(request);

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
                    item = consumer.transform(request, parameter, deserializer);
                    break;
                case FORM:
                    item = consumer.transform(request, parameter, deserializer);
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
