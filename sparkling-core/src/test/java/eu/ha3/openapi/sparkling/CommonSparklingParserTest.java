package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.common.CommonSparklingParser;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
class CommonSparklingParserTest {
    @Test
    void creating() throws IOException {
        try (InputStream openApiStream = Files.newInputStream(pathFromResource("petstore.json"))) {
            CommonSparklingParser.createRoutes(new NoSparkling(), openApiStream, StandardCharsets.UTF_8);
        }
    }

    private Path pathFromResource(String resource) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
            if (url != null) {
                return Paths.get(url.toURI());

            } else {
                throw new IllegalStateException("Resource not found");
            }

        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
    private static class NoSparkling implements Sparkling {
        @Override
        public void newRoute(RouteDefinition routeDefinition) {

        }
    }
}