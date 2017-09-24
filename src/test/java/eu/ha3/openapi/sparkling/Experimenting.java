package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.specific.CommonDeserializer;
import eu.ha3.openapi.sparkling.specific.CommonSparkConsumer;
import eu.ha3.openapi.sparkling.specific.CommonSparklingInteractor;
import eu.ha3.openapi.sparkling.specific.CommonSparklingParser;
import org.junit.jupiter.api.Disabled;
import spark.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

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
    public void run(){
        Service http = Service.ignite();
        EnumSet<CommonSparkConsumer> commonSparkConsumers = EnumSet.allOf(CommonSparkConsumer.class);
        ArrayList<CommonSparkConsumer> consumers = new ArrayList<>(commonSparkConsumers);
        CommonSparklingInteractor spark = new CommonSparklingInteractor(http, consumers, new CommonDeserializer(), Arrays.asList(new StoreController()));

        try (InputStream openApiStream = Files.newInputStream(pathFromResource("petstore.json"))) {
            CommonSparklingParser.apply(openApiStream, spark);

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
