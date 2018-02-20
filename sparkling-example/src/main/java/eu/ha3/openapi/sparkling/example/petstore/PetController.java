package eu.ha3.openapi.sparkling.example.petstore;

import eu.ha3.openapi.sparkling.vo.Question;
import org.apache.commons.io.IOUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * (Default template)
 * Created on 2017-10-05
 *
 * @author Ha3
 */
public class PetController {
    public Pet addPet(Question<BodyPetRequest> question) {
        return question.getData().body;
    }

    public Pet updatePet(Question<BodyPetRequest> question) {
        return question.getData().body;
    }

    public List<Pet> findPetsByStatus(Question<FindPetsByStatusRequest> question) {
        return Arrays.asList(new Pet(0, new Category(0, "string"), "string", Arrays.asList("string"), Arrays.asList(new Tag(0, "string")), "string"));
    }

    public Map<String, Object> uploadFile(Question<UploadFileRequest> question) {
        InputStream fileStream = question.getData().file;
        String filename = question.getData().fileName;

        try (InputStream stream = fileStream) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", 0);
            map.put("type", "base64 with filename: " + filename);
            map.put("message", Base64.getEncoder().encodeToString(IOUtils.toByteArray(stream)));
            return map;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private class BodyPetRequest {
        Pet body;
    }

    private class FindPetsByStatusRequest {
        Request request;
        Response response;
        List<String> status;
    }

    public class UploadFileRequest {
        long petId;
        String additionalMetadata;
        String fileName;
        InputStream file;
    }
}
