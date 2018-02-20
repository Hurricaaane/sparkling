package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
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
    private final SparklingDeserializer deserializer;
    private final ImplementationMatcher implementationMatcher;

    public CommonSparkling(Service http, SparklingDeserializer deserializer, List<?> controllers) {
        this.http = http;
        this.deserializer = deserializer;
        implementationMatcher = new ImplementationMatcher(controllers);
    }

    public static CommonSparkling setup(Service http, List<?> controllers) {
        return new CommonSparkling(http, new CommonDeserializer(), controllers);
    }

    public static CommonSparkling setup(List<?> controllers) {
        return new CommonSparkling(null, new CommonDeserializer(), controllers);
    }

    @Override
    public void newRoute(RouteDefinition routeDefinition) {
        ControllerInvoker descriptor = implementationMatcher.resolveControllerImplementation(routeDefinition.getActionName(), routeDefinition.getTag(), routeDefinition.getParameters());

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getReflectedTypeMap(), descriptor, deserializer);
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
