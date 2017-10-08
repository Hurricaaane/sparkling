package eu.ha3.openapi.sparkling.petstore;

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
    public Object addPet(Request request, Response response, Pet pet) {
        return new SparklingResponseContext().entity(pet);
    }

    public Object updatePet(Request request, Response response, Pet pet) {
        return new SparklingResponseContext().entity(pet);
    }

    public List<Pet> findPetsByStatus(Request request, Response response, List<String> query) {
        return Arrays.asList(new Pet(0, new Category(0, "string"), "string", Arrays.asList("string"), Arrays.asList(new Tag(0, "string")), "string"));
    }
}
