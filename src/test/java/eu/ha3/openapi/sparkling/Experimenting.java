package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.routing.ISparklingDeserializer;
import eu.ha3.openapi.sparkling.specific.CommonSparkConsumer;
import eu.ha3.openapi.sparkling.specific.CommonSparklingInteractor;
import eu.ha3.openapi.sparkling.specific.CommonSparklingParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import spark.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
@Disabled
public class Experimenting {
    @Test
    void experimenting() throws IOException {
        Service http = Service.ignite();
        EnumSet<CommonSparkConsumer> commonSparkConsumers = EnumSet.allOf(CommonSparkConsumer.class);
        ArrayList<CommonSparkConsumer> consumers = new ArrayList<>(commonSparkConsumers);
        CommonSparklingInteractor spark = new CommonSparklingInteractor(http, consumers, new ISparklingDeserializer() {
            @Override
            public List<?> deserializePart(DeserializeInto type, ArrayType arrayType, InputStream part) {
                return null;
            }

            @Override
            public List<?> deserializeSimple(DeserializeInto type, ArrayType arrayType, String content) {
                if (type == DeserializeInto.INT) {
                    return Arrays.asList(Integer.parseInt(content));

                } else if (type == DeserializeInto.LONG) {
                    return Arrays.asList(Long.parseLong(content));

                }
                return null;
            }
        }, Arrays.asList(new StoreController()));

        try (InputStream openApiStream = Files.newInputStream(pathFromResource("petstore.json"))) {
            CommonSparklingParser.apply(openApiStream, spark);
        }

        try {
            Thread.sleep(10_000_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
