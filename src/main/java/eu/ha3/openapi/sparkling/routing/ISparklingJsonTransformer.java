package eu.ha3.openapi.sparkling.routing;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public interface ISparklingJsonTransformer {
    <T> T fromJson(String json, Class<T> klass);
}
