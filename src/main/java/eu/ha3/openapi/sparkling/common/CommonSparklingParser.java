package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ParameterLocation;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
import eu.ha3.openapi.sparkling.exception.ParseSparklingException;
import eu.ha3.openapi.sparkling.routing.Sparkling;
import eu.ha3.openapi.sparkling.routing.RouteDefinition;
import eu.ha3.openapi.sparkling.vo.SparklingParameter;
import eu.ha3.openapi.sparkling.vo.SparklingParameterHandler;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class CommonSparklingParser {
    private static final Pattern SPEC_PATH_TEMPLATING = Pattern.compile("\\{(.*?)\\}");
    private final String openApi;

    /**
     * Convenience method that creates a parser and declares all routes to the Sparkling implementation.
     */
    public static void createRoutes(Sparkling sparkling, InputStream openApi, Charset charset) {
        try {
            createRoutes(sparkling, IOUtils.toString(openApi, charset));

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Convenience method that creates a parser and declares all routes to the Sparkling implementation.
     */
    public static void createRoutes(Sparkling sparkling, String openApi) {
        CommonSparklingParser commonSparklingParser = new CommonSparklingParser(openApi);
        List<RouteDefinition> parse = commonSparklingParser.parse();
        for (RouteDefinition routeDefinition : parse) {
            sparkling.newRoute(routeDefinition);
        }
    }

    /**
     * Convenience method that creates a parser and declares all routes to the Sparkling implementation.
     */
    public static void createRoutes(Sparkling sparkling, java.nio.file.Path openApiFile, Charset charset) {
        try (InputStream inputStream = Files.newInputStream(openApiFile)) {
            createRoutes(sparkling, inputStream, charset);

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public CommonSparklingParser(String openApi) {
        this.openApi = openApi;
    }

    private List<RouteDefinition> parse() {
        try {
            return doParse();

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private List<RouteDefinition> doParse() throws IOException {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = Files.createTempFile("oapi", ".json");
            Files.write(tempFile, openApi.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

            Swagger swagger = parseOpenApiWithReferencesFlattened(tempFile);

            return findAllRoutes(swagger);

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private Swagger parseOpenApiWithReferencesFlattened(java.nio.file.Path tempFile) {
        return new SwaggerParser().read(tempFile.toAbsolutePath().toString(), Collections.emptyList(), true);
    }

    private List<RouteDefinition> findAllRoutes(Swagger swagger) {
        return swagger.getPaths().entrySet().stream()
                .map(pathToItem -> expandPath(pathToItem.getKey(), pathToItem.getValue()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<RouteDefinition> expandPath(String swaggerPath, Path pathDefinition) {
        String sparkPath = convertPathParametersToSparkFormat(swaggerPath);
        Map<HttpMethod, Operation> operationMap = pathDefinition.getOperationMap();

        return expandMethods(sparkPath, operationMap);
    }

    private List<RouteDefinition> expandMethods(String sparkPath, Map<HttpMethod, Operation> operationMap) {
        return operationMap.entrySet().stream()
                .map(methodToOperation -> declareOperation(sparkPath, methodToOperation.getKey(), methodToOperation.getValue()))
                .collect(Collectors.toList());
    }

    private RouteDefinition declareOperation(String sparkPath, HttpMethod method, Operation operation) {
        List<SparklingParameter> parameters = parseParameters(operation);

        List<String> tags = operation.getTags();
        String controllerHint = tags.size() > 0 ? tags.get(0) : "Unspecified";

        List<String> consumes = operation.getConsumes();
        if (consumes == null) {
            consumes = Arrays.asList("application/json");
        }

        SparklingVerb verb = toVerb(method);
        return new RouteDefinition(controllerHint, operation.getOperationId(), verb, sparkPath, consumes, operation.getProduces(), parameters);
    }

    private SparklingVerb toVerb(HttpMethod method) {
        SparklingVerb verb;
        switch (method) {
            case POST:
                verb = SparklingVerb.POST;
                break;
            case GET:
                verb = SparklingVerb.GET;
                break;
            case PUT:
                verb = SparklingVerb.PUT;
                break;
            case PATCH:
                verb = SparklingVerb.PATCH;
                break;
            case DELETE:
                verb = SparklingVerb.DELETE;
                break;
            case HEAD:
                verb = SparklingVerb.HEAD;
                break;
            case OPTIONS:
                verb = SparklingVerb.OPTIONS;
                break;
            default:
                throw new ParseSparklingException("Unexpected method");
        }
        return verb;
    }

    private List<SparklingParameter> parseParameters(Operation operation) {
        List<SparklingParameter> parameters = new ArrayList<>();
        for (Parameter parameter : operation.getParameters()) {
            if (parameter instanceof SerializableParameter) {
                SparklingParameter sparklingParameter = SparklingParameterHandler.from((SerializableParameter) parameter);
                if (sparklingParameter.getType() == DeserializeInto.FILE) {
                    parameters.add(new SparklingParameter(sparklingParameter.getName() + "_filename", ParameterLocation.FORM, ArrayType.NONE, DeserializeInto.STRING_FILENAME, sparklingParameter.getRequirement()));
                }
                parameters.add(sparklingParameter);

            } else {
                parameters.add(SparklingParameterHandler.ofBody(parameter));
            }
        }
        return parameters;
    }

    private String convertPathParametersToSparkFormat(String specPath) {
        return SPEC_PATH_TEMPLATING.matcher(specPath).replaceAll(":$1");
    }

    private List<String> findAllPathParameters(String specPath) {
        Matcher m = SPEC_PATH_TEMPLATING.matcher(specPath);
        List<String> pathParameters = new ArrayList<>();
        while (m.find()) {
            pathParameters.add(m.group(1));
        }
        return pathParameters;
    }
}
