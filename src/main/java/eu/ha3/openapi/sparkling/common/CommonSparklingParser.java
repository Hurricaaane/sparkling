package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.exception.ParseSparklingException;
import eu.ha3.openapi.sparkling.routing.ISparklingInteractor;
import eu.ha3.openapi.sparkling.enums.SparklingVerb;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class CommonSparklingParser {
    private static final Pattern SPEC_PATH_TEMPLATING = Pattern.compile("\\{(.*?)\\}");
    private final String openApi;
    private final ISparklingInteractor sparkling;

    public static void apply(InputStream openApi, ISparklingInteractor spark, Charset charset) {
        try {
            apply(IOUtils.toString(openApi, charset), spark);

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void apply(String openApi, ISparklingInteractor spark) {
        CommonSparklingParser commonSparklingParser = new CommonSparklingParser(openApi, spark);
        commonSparklingParser.parsing();
    }

    public CommonSparklingParser(String openApi, ISparklingInteractor sparkling) {
        this.openApi = openApi;
        this.sparkling = sparkling;
    }

    public static void applyFile(java.nio.file.Path openApiFile, ISparklingInteractor spark, Charset charset) {
        try (InputStream inputStream = Files.newInputStream(openApiFile)) {
            apply(inputStream, spark, charset);

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void parsing() {
        try {
            doParse();

        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void doParse() throws IOException {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = Files.createTempFile("oapi", ".json");
            Files.write(tempFile, openApi.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

            Swagger swagger = parseOpenApiWithReferencesFlattened(tempFile);

            declareAllPaths(swagger);

        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private void declareAllPaths(Swagger swagger) {
        Map<String, Path> paths = swagger.getPaths();
        for (Map.Entry<String, Path> pathToItem : paths.entrySet()) {
            String specPath = pathToItem.getKey();

            List<String> pathParameters = findAllPathParameters(specPath);
            String sparkPath = replaceAllPathParametesWithSpark(specPath);

            Map<HttpMethod, Operation> operationMap = pathToItem.getValue().getOperationMap();

            declarePath(pathParameters, sparkPath, operationMap);
        }
    }

    private Swagger parseOpenApiWithReferencesFlattened(java.nio.file.Path tempFile) {
        return new SwaggerParser().read(tempFile.toAbsolutePath().toString(), Collections.emptyList(), true);
    }

    private void declarePath(List<String> pathParameters, String sparkPath, Map<HttpMethod, Operation> operationMap) {
        for (Map.Entry<HttpMethod, Operation> methodToOperation : operationMap.entrySet()) {
            HttpMethod method = methodToOperation.getKey();
            Operation operation = methodToOperation.getValue();

            List<SparklingParameter> parameters = parseParameters(operation);

            List<String> tags = operation.getTags();
            String controllerHint = tags.size() > 0 ? tags.get(0) : "Unspecified";

            List<String> consumes = operation.getConsumes();
            if (consumes == null) {
                consumes = Arrays.asList("application/json");
            }

            SparklingVerb verb = toVerb(method);
            sparkling.newRoute(controllerHint, operation.getOperationId(), verb, sparkPath, consumes, operation.getProduces(), parameters);
        }
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
                parameters.add(SparklingParameterHandler.from((SerializableParameter) parameter));

            } else {
                parameters.add(SparklingParameterHandler.ofBody(parameter));
            }
        }
        return parameters;
    }

    private String replaceAllPathParametesWithSpark(String specPath) {
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
