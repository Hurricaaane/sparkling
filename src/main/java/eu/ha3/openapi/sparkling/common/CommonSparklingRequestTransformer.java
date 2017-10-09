package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.routing.SparklingRequestTransformer;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public enum CommonSparklingRequestTransformer implements SparklingRequestTransformer {
    FORM_URL_ENCODED {
        @Override
        public List<?> transform(Request request, SparklingParameter parameter, SparklingDeserializer deserializer, Class<?> bodyPojoClass) {
            if (parameter.getArrayType() != ArrayType.MULTI) {
                String[] queryParamsValues = request.queryParamsValues(parameter.getName());
                if (queryParamsValues != null) {
                    return Arrays.stream(queryParamsValues)
                            .map(queryParam -> deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), queryParam))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());

                } else {
                    return Collections.emptyList();
                }
            } else {
                return deserializer.deserializeSimple(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));
            }
        }

        @Override
        public List<String> getApplicableContentTypes() {
            return Arrays.asList("application/x-www-form-urlencoded");
        }
    },
    MULTIPART_FORM_DATA {
        private final Object lock = new Object();
        private volatile MultipartConfigElement multipartConfig;

        @Override
        public List<Object> transform(Request request, SparklingParameter parameter, SparklingDeserializer deserializer, Class<?> bodyPojoClass) {
            try {
                if (multipartConfig == null) {
                    resolveMultipartConfig();
                }

                request.attribute("org.eclipse.jetty.multipartConfig", multipartConfig);
                return request.raw().getParts().stream()
                        .filter(part -> parameter.getName().equals(part.getName()))
                        .map(part -> deserialize(deserializer, parameter, part))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

            } catch (IOException | ServletException e) {
                throw new TransformationFailedInternalSparklingException(e);
            }
        }

        private List<?> deserialize(SparklingDeserializer deserializer, SparklingParameter parameter, Part part) {
            try {
                return deserializer.deserializePart(parameter.getType(), parameter.getArrayType(), part.getInputStream());

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void setMultipartConfig(MultipartConfigElement multipartConfig) {
            synchronized (lock) {
                this.multipartConfig = multipartConfig;
            }
        }

        private void resolveMultipartConfig() {
            synchronized (lock) {
                try {
                    if (multipartConfig != null) {
                        return;
                    }

                    String property = System.getProperty("sparkling.multipart.temp");
                    if (property == null) {
                        property = System.getenv("sparkling.multipart.temp");
                    }
                    if (property == null) {
                        Path path = Files.createTempDirectory("sparkling");
                        property = path.toAbsolutePath().toString();
                    }

                    multipartConfig = new MultipartConfigElement(property);

                } catch (IOException e) {
                    throw new ExceptionInInitializerError();
                }
            }
        }

        @Override
        public List<String> getApplicableContentTypes() {
            return Arrays.asList("multipart/form-data");
        }
    },
    APPLICATION_JSON {
        @Override
        public List<Object> transform(Request request, SparklingParameter parameter, SparklingDeserializer deserializer, Class<?> bodyPojoClass) {
            return Arrays.asList(deserializer.deserializeSchema(request.body(), parameter, bodyPojoClass));
        }

        @Override
        public List<String> getApplicableContentTypes() {
            return Arrays.asList("application/json");
        }
    }
}
