package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
class InternalSparklingRoute implements Route {
    private final ControllerInvoker reflectedMethod;
    private final Gson gson;
    private final ModelExtractor modelExtractor;

    public InternalSparklingRoute(Map<SparklingParameter, Type> reflectedTypes, ControllerInvoker reflectedMethod, Gson gson, ParameterAggregator aggregator, Modelizer modelizer) {
        modelExtractor = new ModelExtractor(reflectedTypes, aggregator, modelizer);

        this.gson = gson;
        this.reflectedMethod = reflectedMethod;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String contentType = request.contentType();

        Map<SparklingParameter, Object> models = modelExtractor.extractModels(request);

        Object returnedObject = reflectedMethod.submit(request, response, models);

        return applyResponse(request, response, returnedObject);
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
                return gson.toJson(returnedObject);

            } else {
                // FIXME: How to handle other formats than JSON
                if (response.type() == null) {
                    response.type("application/json");
                }
                return gson.toJson(returnedObject);
            }
        }
    }
}
