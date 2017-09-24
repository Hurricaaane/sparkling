package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;

import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface ISparklingInteractor {
    void declare(String tag, String operationId, SparklingVerb post, String sparkPath, List<String> consumes, List<String> produces, List<SparklingParameter> parameters);
}
