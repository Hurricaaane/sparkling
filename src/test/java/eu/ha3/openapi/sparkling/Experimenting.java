package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.petstore.PetController;
import eu.ha3.openapi.sparkling.petstore.StoreController;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import org.junit.jupiter.api.Disabled;
import spark.Service;

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
 * Created on 2017-09-24
 *
 * @author Ha3
 */
@Disabled
public class Experimenting implements Runnable {
    public static void main(String[] args) {
        new Experimenting().run();
    }

    @Override
    public void run() {
        Service http = Service.ignite();
        http.options("*", (request, response) -> "");
        http.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "Content-Type,*");
        });
        List<?> implementations = Arrays.asList(new PetController(), new StoreController());
        Sparkling sparkling = Sparkling.setup(http, implementations);

        try (InputStream openApiStream = Files.newInputStream(pathFromResource("petstore.json"))) {
            http.path("/v2", () -> {
                sparkling.createRoutes(openApiStream, StandardCharsets.UTF_8);
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

//    public static void main(String[] args) {
//        Service http = Service.ignite();
//        http.options("*", (request, response) -> "");
//        http.before((request, response) -> {
//            response.header("Access-Control-Allow-Origin", "*");
//            response.header("Access-Control-Request-Method", "*");
//            response.header("Access-Control-Allow-Headers", "Content-Type,*");
//        });
//        List<?> implementations = Arrays.asList(new PetController(), new StoreController());
//        Sparkling sparkling = Sparkling.setup(http, implementations);
//
//        sparkling.newRoute(new RouteDefinition(
//                "pet", // Binds to PetController
//                "addPet",
//                SparklingVerb.POST,
//                "/pet",
//                Arrays.asList("application/json", "application/xml"),
//                Arrays.asList("application/json", "application/xml"),
//                Arrays.asList(new SparklingParameter(
//                        "body",
//                        ParameterLocation.BODY,
//                        ArrayType.NONE,
//                        DeserializeInto.STRING,
//                        SparklingRequirement.REQUIRED
//                ))
//        ));
//    }

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
