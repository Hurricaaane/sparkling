package eu.ha3.openapi.sparkling.petstore;

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
    public Pet addPet(Request request, Response response, Pet pet) {
        return pet;
    }

    public Pet updatePet(Request request, Response response, Pet pet) {
        return pet;
    }

    public List<Pet> findPetsByStatus(Request request, Response response, List<String> query) {
        return Arrays.asList(new Pet(0, new Category(0, "string"), "string", Arrays.asList("string"), Arrays.asList(new Tag(0, "string")), "string"));
    }

    public Map<String, Object> uploadFile(Question<UploadFileRequest> uploadFileRequestQuestion) {
        return uploadFile(uploadFileRequestQuestion.getRequest(), uploadFileRequestQuestion.getResponse(),
                uploadFileRequestQuestion.getData().getPetId(),
                uploadFileRequestQuestion.getData().getAdditionalMetadata(),
                uploadFileRequestQuestion.getData().getFileName(),
                uploadFileRequestQuestion.getData().getFile()
        );
    }

    public Map<String, Object> uploadFile(Request request, Response response, long petId, String additionalMetadata, String filename, InputStream fileStream) {
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
}
