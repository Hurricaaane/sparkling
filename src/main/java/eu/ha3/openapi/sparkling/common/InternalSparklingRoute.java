package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.enums.ArrayType;
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
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
class InternalSparklingRoute implements Route {
    private final ReflectedMethodDescriptor reflectedMethod;
    private final List<SparklingParameter> parameters;
    private final SparklingDeserializer deserializer;

    public InternalSparklingRoute(ReflectedMethodDescriptor reflectedMethod, List<SparklingParameter> parameters, SparklingDeserializer deserializer) {
        this.reflectedMethod = reflectedMethod;
        this.parameters = parameters;
        this.deserializer = deserializer;
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
        Collection<Part> parts = null;
        boolean isMultipart = false;
        if ("multipart/form-data".equals(request.contentType())) {
            isMultipart = true;
            parts = extractParts(request);
        }

        boolean isFormUrlEncoded = "application/x-www-form-urlencoded".equals(request.contentType());

        List<Object> models = new ArrayList<>();
        models.add(request);
        models.add(response);

        Iterator<SparklingParameter> parameterIterator = parameters.iterator();
        Iterator<Type> reflectedIterator = reflectedMethod.getExpectedRequestParameters().iterator();
        while (parameterIterator.hasNext()) {
            SparklingParameter sparklingParameter = parameterIterator.next();
            Type reflectedType = reflectedIterator.next();

            Object value = deserializeParameter(request, parts, isMultipart, isFormUrlEncoded, sparklingParameter);
            Object model = modelize(value, reflectedType);

            models.add(model);
        }
        return models;
    }

    private Collection<Part> extractParts(Request request) {
        Collection<Part> parts;
        if (multipartConfig == null) {
            resolveMultipartConfig();
        }

        request.attribute("org.eclipse.jetty.multipartConfig", multipartConfig);
        try {
            parts = request.raw().getParts();
        } catch (IOException | ServletException e) {
            throw new TransformationFailedInternalSparklingException(e);
        }
        return parts;
    }

    private Object deserializeParameter(Request request, Collection<Part> parts, boolean isMultipart, boolean isFormUrlEncoded, SparklingParameter parameter) {
        Object item;
        switch (parameter.getLocation()) {
            case PATH:
                item = deserializePath(request, parameter);
                break;
            case QUERY:
                item = deserializeQuery(request, parameter);
                break;
            case HEADER:
                item = deserializeHeader(request, parameter);
                break;
            case BODY:
                item = request.body();
                break;
            case FORM:
                // FIXME Need some sort of content type check
                if (isMultipart) {
                    item = deserializeMultiForm(parts, parameter);

                } else if (isFormUrlEncoded) {
                    item = deserializeApplicationForm(request, parameter);

                } else {
                    // FIXME Non matching content type! What to do?
                    item = null;
                }
                break;
            default:
                throw new TransformationFailedInternalSparklingException("Unknown location");
        }
        return item;
    }

    private Object modelize(Object item, Type reflectedType) {
        if (item instanceof List) {
            return modelizeListSource(item, reflectedType, (List) item);

        } else {
            return modelizeStringSource(item, reflectedType);
        }
    }

    private Object modelizeListSource(Object item, Type reflectedType, List list) {
        if (list.size() > 0
                && list.get(0) instanceof String
                && reflectedType instanceof ParameterizedType
                && ((ParameterizedType)reflectedType).getActualTypeArguments()[0] != String.class) {
            return ((List<String>) item).stream()
                            .map(o -> new Gson().fromJson((String) o, reflectedType))
                            .collect(Collectors.toList());

        } else {
            return item;
        }
    }

    private Object modelizeStringSource(Object item, Type reflectedType) {
        if (item instanceof String && reflectedType != String.class) {
            // reflectedType can be either a model, or a list of models, it will work the same
            return new Gson().fromJson((String) item, reflectedType);

        } else {
            return item;
        }
    }

    private Object deserializePath(Request request, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() == ArrayType.NONE) {
            item = deserializer.deserializeSingleValued(parameter.getType(), request.params(parameter.getName()));

        } else {
            item = deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.params(parameter.getName()));
        }
        return item;
    }

    private Object deserializeQuery(Request request, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() == ArrayType.NONE) {
            item = deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            String[] queryParamsValues = request.queryParamsValues(parameter.getName());
            if (queryParamsValues != null) {
                item = Arrays.stream(queryParamsValues)
                        .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                        .collect(Collectors.toList());
            } else {
                // FIXME: Possible semantic difference between an optional query and a query with zero items
                item = new ArrayList<>();
            }
        } else {
            item = deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));

        }
        return item;
    }

    private Object deserializeHeader(Request request, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() == ArrayType.NONE) {
            item = deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            item = Collections.list(request.raw().getHeaders(parameter.getName())).stream()
                    .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                    .collect(Collectors.toList());
        } else {
            item = deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.headers(parameter.getName()));

        }
        return item;
    }

    private Object deserializeMultiForm(Collection<Part> parts, SparklingParameter parameter) {
        Object item;
        if (parameter.getArrayType() == ArrayType.NONE) {
            Object collect = parts.stream()
                    .filter(part -> parameter.getName().equals(part.getName()))
                    .findFirst()
                    .map(part -> deserializeSingleValuedPart(parameter, part))
                    .orElse(null);
            item = collect;

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            List<Object> collect = parts.stream().filter(part -> parameter.getName().equals(part.getName()))
                    .map(part -> deserializeSingleValuedPart(parameter, part))
                    .collect(Collectors.toList());
            item = collect;

        } else {
            List<Object> collect = parts.stream()
                    .filter(part -> parameter.getName().equals(part.getName()))
                    .findFirst()
                    .map(part -> deserializeMultiValuedPart(parameter, part))
                    .orElse(null);
            item = collect;
        }
        return item;
    }

    private Object deserializeApplicationForm(Request request, SparklingParameter parameter) {
        if (parameter.getArrayType() == ArrayType.NONE) {
            return deserializer.deserializeSingleValued(parameter.getType(), request.queryParams(parameter.getName()));

        } else if (parameter.getArrayType() == ArrayType.MULTI) {
            String[] queryParamsValues = request.queryParamsValues(parameter.getName());
            if (queryParamsValues != null) {
                return Arrays.stream(queryParamsValues)
                        .map(queryParam -> deserializer.deserializeSingleValued(parameter.getType(), queryParam))
                        .collect(Collectors.toList());

            } else {
                return Collections.emptyList();
            }

        } else {
            return deserializer.deserializeMultiValued(parameter.getType(), parameter.getArrayType(), request.queryParams(parameter.getName()));
        }
    }

    private List<Object> deserializeMultiValuedPart(SparklingParameter parameter, Part part) {
        try {
            return deserializer.deserializeMultiValuedPart(parameter.getType(), parameter.getArrayType(), part.getInputStream());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Object deserializeSingleValuedPart(SparklingParameter parameter, Part part) {
        try {
            return deserializer.deserializeSingleValuedPart(parameter.getType(), part.getInputStream());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
