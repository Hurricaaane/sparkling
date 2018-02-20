package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
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
    private final Gson gson;
    private final ImplementationMatcher implementationMatcher;
    private final ParameterAggregator aggregator;
    private final Modelizer modelizer;

    public CommonSparkling(Service http, List<?> controllers) {
        this.http = http;

        gson = new Gson();
        implementationMatcher = new ImplementationMatcher(controllers, gson);
        this.aggregator = new ParameterAggregator(new CommonDeserializer());
        this.modelizer = new Modelizer(gson);
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

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getReflectedTypeMap(), descriptor, gson, aggregator, modelizer);
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
