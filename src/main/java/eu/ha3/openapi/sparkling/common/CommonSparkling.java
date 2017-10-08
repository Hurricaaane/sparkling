package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ParameterLocation;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.ISparkling;
import eu.ha3.openapi.sparkling.routing.ISparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import spark.Service;

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
public class CommonSparkling implements ISparkling {
    private final Service http;
    private final List<? extends ISparklingRequestTransformer> availableConsumers;
    private final ISparklingDeserializer deserializer;

    public CommonSparkling(Service http, List<? extends ISparklingRequestTransformer> availableConsumers, ISparklingDeserializer deserializer, List<?> controllers) {
        this.http = http;
        this.availableConsumers = availableConsumers;
        this.deserializer = deserializer;
        IMPMATCH = new ImplementationMatcher(controllers);
    }

    public static CommonSparkling generic(Service http, List<?> controllers) {
        ArrayList<CommonSparklingRequestTransformer> consumers = new ArrayList<>(EnumSet.allOf(CommonSparklingRequestTransformer.class));
        return new CommonSparkling(http, consumers, new CommonDeserializer(), controllers);
    }

    private final ImplementationMatcher IMPMATCH;

    @Override
    public void newRoute(RouteDefinition routeDefinition) {
        int bodyLocation = routeDefinition.getParameters().stream()
                .filter(parameter -> parameter.getLocation() == ParameterLocation.BODY)
                .findFirst()
                .map(routeDefinition.getParameters()::indexOf)
                .orElse(-1);

        ReflectedMethodDescriptor descriptor = IMPMATCH.resolveControllerImplementation(routeDefinition.getActionName(), routeDefinition.getTag(), bodyLocation, routeDefinition.getParameters());
        List<ISparklingRequestTransformer> allowedConsumers = findAvailableConsumersApplicableForThisDeclaration(routeDefinition.getConsumes());

        InternalSparklingRoute route = new InternalSparklingRoute(descriptor.getImplementation(), allowedConsumers, routeDefinition.getParameters(), deserializer, descriptor.getPojoClass());
        addRouteToSpark(routeDefinition.getPost(), routeDefinition.getSparkPath(), route);
    }

    private void addRouteToSpark(SparklingVerb method, String sparkPath, InternalSparklingRoute route) {
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
    }

    private List<ISparklingRequestTransformer> findAvailableConsumersApplicableForThisDeclaration(List<String> consumes) {
        return availableConsumers.stream()
                .filter(sparkConsumer -> consumes.stream().anyMatch(declaredConsumer -> sparkConsumer.getApplicableContentTypes().contains(declaredConsumer)))
                .collect(Collectors.toList());
    }
}
