package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.routing.RequestConverter;
import eu.ha3.openapi.sparkling.routing.ResponseConverter;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import spark.Service;
import spark.Spark;

import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class CommonSparkling implements Sparkling {
    private final Service http;
    private final ImplementationMatcher implementationMatcher;
    private final ParameterAggregator aggregator;
    private final Modelizer modelizer;
    private final ResponseConverter responseConverter;

    public CommonSparkling(Service http, List<?> controllers) {
        Gson gson = new Gson();
        RequestConverter requestConverter = gson::fromJson;

        this.http = http;
        this.responseConverter = gson::toJson;
        implementationMatcher = new ImplementationMatcher(controllers, requestConverter);
        this.aggregator = new ParameterAggregator(new CommonDeserializer());
        this.modelizer = new Modelizer(requestConverter);
    }

    public CommonSparkling(Service http, List<?> controllers, RequestConverter requestConverter, ResponseConverter responseConverter) {
        this.http = http;
        this.responseConverter = responseConverter;
        implementationMatcher = new ImplementationMatcher(controllers, requestConverter);
        this.aggregator = new ParameterAggregator(new CommonDeserializer());
        this.modelizer = new Modelizer(requestConverter);
    }

    public static CommonSparkling setup(Service http, List<?> controllers) {
        return new CommonSparkling(http, controllers);
    }

    public static CommonSparkling setup(List<?> controllers) {
        return new CommonSparkling(null, controllers);
    }

    @Override
    public void newRoute(RouteDefinition routeDefinition) {
        ControllerInvoker descriptor = implementationMatcher.resolveControllerImplementation(routeDefinition.getActionName(), routeDefinition.getTag(), routeDefinition.getParameters());

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getReflectedTypeMap(), descriptor, responseConverter, aggregator, modelizer);
        addRouteToSpark(routeDefinition.getVerb(), routeDefinition.getSparkPath(), route);
    }

    private void addRouteToSpark(SparklingVerb method, String sparkPath, InternalSparklingRoute route) {
        if (http != null) {
            switch (method) {
                case GET:
                    http.get(sparkPath, route);
                    break;
                case POST:
                    http.post(sparkPath, route);
                    break;
                case PUT:
                    http.put(sparkPath, route);
                    break;
                case PATCH:
                    http.patch(sparkPath, route);
                    break;
                case DELETE:
                    http.delete(sparkPath, route);
                    break;
                case HEAD:
                    http.head(sparkPath, route);
                    break;
                case TRACE:
                    http.trace(sparkPath, route);
                    break;
                case OPTIONS:
                    http.options(sparkPath, route);
                    break;
            }
        } else {
            switch (method) {
                case GET:
                    Spark.get(sparkPath, route);
                    break;
                case POST:
                    Spark.post(sparkPath, route);
                    break;
                case PUT:
                    Spark.put(sparkPath, route);
                    break;
                case PATCH:
                    Spark.patch(sparkPath, route);
                    break;
                case DELETE:
                    Spark.delete(sparkPath, route);
                    break;
                case HEAD:
                    Spark.head(sparkPath, route);
                    break;
                case TRACE:
                    Spark.trace(sparkPath, route);
                    break;
                case OPTIONS:
                    Spark.options(sparkPath, route);
                    break;
            }
        }
    }
}
