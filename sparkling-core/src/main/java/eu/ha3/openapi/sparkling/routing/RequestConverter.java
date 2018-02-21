package eu.ha3.openapi.sparkling.routing;

import java.lang.reflect.Type;

/**
 * (Default template)
 * Created on 2018-02-21
 *
 * @author Ha3
 */
@FunctionalInterface
public interface RequestConverter {
    <T> T convertRequest(String deserializableThing, Type type) throws Exception;
}
