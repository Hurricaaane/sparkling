package eu.ha3.openapi.sparkling.petstore;

import com.google.gson.Gson;
import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-10-05
 *
 * @author Ha3
 */
public class PetController {
    public SparklingResponseContext addPet(Request request, Response response, Pet pet) {
        return new SparklingResponseContext().entity(pet);
    }

    public SparklingResponseContext updatePet(Request request, Response response, Pet pet) {
        return new SparklingResponseContext().entity(pet);
    }

    public SparklingResponseContext findPetsByStatus(Request request, Response response, List<String> query) {
        return new SparklingResponseContext()
                .entity(new Gson().toJson(Arrays.asList(new Pet(0, new Category(0, "string"), "string", Arrays.asList("string"), Arrays.asList(new Tag(0, "string")), "string"))))
                .contentType("application/json");
    }
}
