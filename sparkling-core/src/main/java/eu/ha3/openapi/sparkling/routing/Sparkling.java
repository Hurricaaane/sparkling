package eu.ha3.openapi.sparkling.routing;

import eu.ha3.openapi.sparkling.common.CommonSparkling;
import eu.ha3.openapi.sparkling.common.CommonSparklingParser;
import spark.Service;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public interface Sparkling {
    void newRoute(RouteDefinition routeDefinition);

    static Sparkling setup(Service http, List<?> controllers) {
        return CommonSparkling.setup(http, controllers);
    }

    static Sparkling setup(List<?> controllers) {
        return CommonSparkling.setup(controllers);
    }

    default Sparkling createRoutes(InputStream openApi, Charset charset) {
        CommonSparklingParser.createRoutes(this, openApi, charset);
        return this;
    }

    default Sparkling createRoutes(String openApi) {
        CommonSparklingParser.createRoutes(this, openApi);
        return this;
    }

    default Sparkling createRoutes(java.nio.file.Path openApiFile, Charset charset){
        CommonSparklingParser.createRoutes(this, openApiFile, charset);
        return this;
    }

}
