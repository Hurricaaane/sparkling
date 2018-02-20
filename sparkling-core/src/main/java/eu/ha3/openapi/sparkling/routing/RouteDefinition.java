package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;

import java.util.List;

public class RouteDefinition {
    private final String tag;
    private final String actionName;
    private final SparklingVerb verb;
    private final String sparkPath;
    private final List<String> consumes;
    private final List<String> produces;
    private final List<SparklingParameter> parameters;

    public RouteDefinition(String tag, String actionName, SparklingVerb verb, String sparkPath, List<String> consumes, List<String> produces, List<SparklingParameter> parameters) {
        this.tag = tag;
        this.actionName = actionName;
        this.verb = verb;
        this.sparkPath = sparkPath;
        this.consumes = consumes;
        this.produces = produces;
        this.parameters = parameters;
    }

    public String getTag() {
        return tag;
    }

    public String getActionName() {
        return actionName;
    }

    public SparklingVerb getVerb() {
        return verb;
    }

    public String getSparkPath() {
        return sparkPath;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public List<String> getProduces() {
        return produces;
    }

    public List<SparklingParameter> getParameters() {
        return parameters;
    }
}
