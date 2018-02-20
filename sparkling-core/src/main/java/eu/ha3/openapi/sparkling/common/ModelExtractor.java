package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * (Default template)
 * Created on 2018-02-19
 *
 * @author Ha3
 */
public class ModelExtractor {
    private static final String DEFAULT_MULTIPART_CHARSET = "UTF-8";
    private final Map<SparklingParameter, Type> reflectedTypes;
    private final ParameterAggregator aggregator;
    private final Modelizer modelizer;

    public ModelExtractor(SparklingDeserializer deserializer, Gson gson, Map<SparklingParameter, Type> reflectedTypes) {
        this.reflectedTypes = reflectedTypes;

        aggregator = new ParameterAggregator(deserializer);
        modelizer = new Modelizer(gson);
    }

    public Map<SparklingParameter, Object> extractModels(Request request) {
        FormType formType = resolveFormType(request);

        Collection<Part> parts = null;
        String possiblePartEncoding = null;
        if (formType == FormType.MULTIPART) {
            parts = extractParts(request);

            possiblePartEncoding = request.raw().getCharacterEncoding();
            if (possiblePartEncoding == null) {
                possiblePartEncoding = DEFAULT_MULTIPART_CHARSET;
            }
        }

        Map<SparklingParameter, Object> models = new LinkedHashMap<>();
        for (Map.Entry<SparklingParameter, Type> reflectedTypeEntry : reflectedTypes.entrySet()) {
            SparklingParameter sparklingParameter = reflectedTypeEntry.getKey();

            Object value = aggregator.aggregateParameter(request, sparklingParameter, formType, parts, possiblePartEncoding);
            Object model = modelizer.modelize(value, reflectedTypeEntry.getValue());

            models.put(sparklingParameter, model);
        }

        return models;
    }

    private FormType resolveFormType(Request request) {
        FormType formType;
        if ("multipart/form-data".equals(request.contentType()) || request.contentType() != null && request.contentType().startsWith("multipart/form-data;")) {
            formType = FormType.MULTIPART;

        } else if ("application/x-www-form-urlencoded".equals(request.contentType())) {
            formType = FormType.URL_ENCODED;

        } else {
            formType = FormType.NOT_A_FORM;
        }
        return formType;
    }

    private Collection<Part> extractParts(Request request) {
        if (multipartConfig == null) {
            resolveMultipartConfig();
        }

        request.attribute("org.eclipse.jetty.multipartConfig", multipartConfig);

        try {
            return request.raw().getParts();

        } catch (IOException | ServletException e) {
            throw new TransformationFailedInternalSparklingException(e);
        }
    }

    private static final Object lock = new Object();
    private static volatile MultipartConfigElement multipartConfig;
    private static void resolveMultipartConfig() {
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
}
