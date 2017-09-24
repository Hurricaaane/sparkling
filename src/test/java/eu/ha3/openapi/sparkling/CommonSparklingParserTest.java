package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.routing.ISparklingInteractor;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import eu.ha3.openapi.sparkling.specific.CommonSparklingParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
            CommonSparklingParser.apply(openApiStream, new NoSparklingInteractor());
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
    private static class NoSparklingInteractor implements ISparklingInteractor {
        @Override
        public void declare(String tag, String operationId, SparklingVerb post, String sparkPath, List<String> consumes, List<String> produces, List<SparklingParameter> parameters) {

        }
    }
}