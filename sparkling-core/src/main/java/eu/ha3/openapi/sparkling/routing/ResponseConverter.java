package eu.ha3.openapi.sparkling.routing;

/**
 * (Default template)
 * Created on 2018-02-21
 *
 * @author Ha3
 */
@FunctionalInterface
public interface ResponseConverter {
    String convertResponse(Object serializableThing) throws Exception;
}
