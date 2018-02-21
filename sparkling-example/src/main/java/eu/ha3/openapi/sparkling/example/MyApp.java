package eu.ha3.openapi.sparkling.example;

import eu.ha3.openapi.sparkling.example.petstore.PetController;
import eu.ha3.openapi.sparkling.example.petstore.StoreController;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import spark.Spark;
import spark.servlet.SparkApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * (Default template)
 * Created on 2018-02-20
 *
 * @author Ha3
 */
public class MyApp implements SparkApplication {
    @Override
    public void init() {
        try {
            tryInit("some default string");

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void tryInit(String defaultString) {
        Spark.options("*", (request, response) -> "");
        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,*");
        });

        List<?> implementations = Arrays.asList(new PetController(defaultString), new StoreController());
        Sparkling sparkling = Sparkling.setup(implementations);

        try (InputStream openApiStream = Files.newInputStream(pathFromResource("petstore.json"))) {
            Spark.path("/v2", () -> {
                sparkling.createRoutes(openApiStream, StandardCharsets.UTF_8);
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
}
