package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ParameterLocation;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.SparklingRequestAggregator;
import spark.Service;
import spark.Spark;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class CommonSparkling implements Sparkling {
    private final Service http;
    private final List<? extends SparklingRequestAggregator> availableConsumers;
    private final SparklingDeserializer deserializer;
    private final ImplementationMatcher implementationMatcher;

    public CommonSparkling(Service http, List<? extends SparklingRequestAggregator> availableConsumers, SparklingDeserializer deserializer, List<?> controllers) {
        this.http = http;
        this.availableConsumers = availableConsumers;
        this.deserializer = deserializer;
        implementationMatcher = new ImplementationMatcher(controllers);
    }

    public static CommonSparkling setup(Service http, List<?> controllers) {
        ArrayList<CommonSparklingRequestAggregator> consumers = new ArrayList<>(EnumSet.allOf(CommonSparklingRequestAggregator.class));
        return new CommonSparkling(http, consumers, new CommonDeserializer(), controllers);
    }

    public static CommonSparkling setup(List<?> controllers) {
        ArrayList<CommonSparklingRequestAggregator> consumers = new ArrayList<>(EnumSet.allOf(CommonSparklingRequestAggregator.class));
        return new CommonSparkling(null, consumers, new CommonDeserializer(), controllers);
    }

    @Override
    public void newRoute(RouteDefinition routeDefinition) {
        int bodyLocation = routeDefinition.getParameters().stream()
                .filter(parameter -> parameter.getLocation() == ParameterLocation.BODY)
                .findFirst()
                .map(routeDefinition.getParameters()::indexOf)
                .orElse(-1);

        ReflectedMethodDescriptor descriptor = implementationMatcher.resolveControllerImplementation(routeDefinition.getActionName(), routeDefinition.getTag(), routeDefinition.getParameters());
        List<SparklingRequestAggregator> allowedConsumers = findAvailableConsumersApplicableForThisDeclaration(routeDefinition.getConsumes());

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getImplementation(), allowedConsumers, routeDefinition.getParameters(), deserializer);
        addRouteToSpark(routeDefinition.getPost(), routeDefinition.getSparkPath(), route);
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

    private List<SparklingRequestAggregator> findAvailableConsumersApplicableForThisDeclaration(List<String> consumes) {
        return availableConsumers.stream()
                .filter(sparkConsumer -> consumes.stream().anyMatch(declaredConsumer -> sparkConsumer.getApplicableContentTypes().contains(declaredConsumer)))
                .collect(Collectors.toList());
    }
}
