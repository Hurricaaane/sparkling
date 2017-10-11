package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.exception.TransformationFailedInternalSparklingException;
import eu.ha3.openapi.sparkling.routing.SparklingDeserializer;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
class InternalSparklingRoute implements Route {
    private final ReflectedMethodDescriptor reflectedMethod;
    private final List<SparklingParameter> parameters;
    private final ParameterAggregator aggregator;
    private final Modelizer modelizer;

    public InternalSparklingRoute(ReflectedMethodDescriptor reflectedMethod, List<SparklingParameter> parameters, SparklingDeserializer deserializer) {
        this.reflectedMethod = reflectedMethod;
        this.parameters = parameters;
        aggregator = new ParameterAggregator(deserializer);
        modelizer = new Modelizer();
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String contentType = request.contentType();

        List<Object> models = extractModels(request, response);

        Object returnedObject = reflectedMethod.getImplementation().apply(models);

        return applyResponse(request, response, returnedObject);
    }

    private Object applyResponse(Request request, Response response, Object returnedObject) {
        if (returnedObject instanceof SparklingResponseContext) {
            SparklingResponseContext sparklingResponseContext = (SparklingResponseContext) returnedObject;

            response.status(sparklingResponseContext.getStatus());
            String contentType = sparklingResponseContext.getContentType();
            if (contentType != null) {
                response.type(contentType);
            }
            for (Map.Entry<String, List<String>> header : sparklingResponseContext.getHeaders().entrySet()) {
                List<String> headerValues = header.getValue();
                for (String value : headerValues) {
                    response.header(header.getKey(), value);
                }
            }

            // FIXME: Transform response
            return sparklingResponseContext.getEntity();

        } else if (returnedObject instanceof InputStream) {
            return returnedObject;

        } else {
            // FIXME: Transform response
            String acceptHeader = request.headers("Accept");
            if (acceptHeader == null || "application/json".equals(acceptHeader)) {
                if (response.type() == null) {
                    response.type("application/json");
                }
                return new Gson().toJson(returnedObject);

            } else {
                // FIXME: How to handle other formats than JSON
                if (response.type() == null) {
                    response.type("application/json");
                }
                return new Gson().toJson(returnedObject);
            }
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

    private List<Object> extractModels(Request request, Response response) {
        FormType formType = resolveFormType(request);

        Collection<Part> parts = null;
        if (formType == FormType.MULTIPART) {
            parts = extractParts(request);
        }

        List<Object> models = new ArrayList<>();
        models.add(request);
        models.add(response);

        Iterator<SparklingParameter> parameterIterator = parameters.iterator();
        Iterator<Type> reflectedIterator = reflectedMethod.getExpectedRequestParameters().iterator();
        while (parameterIterator.hasNext()) {
            SparklingParameter sparklingParameter = parameterIterator.next();
            Type reflectedType = reflectedIterator.next();

            Object value = aggregator.aggregateParameter(request, sparklingParameter, formType, parts);
            Object model = modelizer.modelize(value, reflectedType);

            models.add(model);
        }
        return models;
    }

    private FormType resolveFormType(Request request) {
        FormType formType;
        if ("multipart/form-data".equals(request.contentType())) {
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
}
