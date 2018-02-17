# sparkling

Sparkling reads an OpenAPI 2.0 specification and creates routes using [Spark](http://sparkjava.com/). These routes are mapped to implementations you provide.

There are no configuration files nor annotations.

## Declaration

```java
Service http = Service.ignite();

List<?> implementations = Arrays.asList(new PetController(), new StoreController());
Sparkling sparkling = Sparkling.setup(http, implementations);

http.path("/v2", () -> {
    sparkling.createRoutes(readFile("petstore.json"), StandardCharsets.UTF_8);
});
```

You are free to use Spark as usual (add filters, add other routes...)

## Implementation

There are two ways of implementing the individual controller methods.

### Using a data class

Parameters will be resolved based on the **names of the parameters** of the specification, and will be mapped to a data class.

Method parameters must be exactly one parameter: `Question<YourDataClass>`. The `Question` object contains references to the `Request` and `Response` objects. Your data class must contain field names equal to the parameter names.

If a parameter expects a model, use the model of your choice or a `Map`. Sparkling retrieves the runtime type from the field.

A file parameter will be passed as two parameters for the file name and the content, `String` and `InputStream`. Sparkling will look for a field composed of the parameter name, followed by `Name`, case sensitive. For instance, if your parameter name is `file`, then the field will be `fileName`.

```java
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
```

### Using the method's parameters order

Parameters will be based on the **parameter order** of the specification. This has a huge downside: If the parameter order changes in the specification, the method's parameter names will become out-of-sync, and could continue running without causing runtime errors.

Method parameters must be `Request`, `Response`, followed by the parameters of the operations in the order they are declared in the OpenAPI specification. Parameter names do not need to match (they are unavailable at runtime).

If a parameter expects a model, use the model of your choice or a `Map`. Sparkling retrieves the runtime type from the method parameter.

A file parameter will be passed as two parameters for the file name and the content, `String` and `InputStream`, in this order.

```java 
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
```

### Controller class

The controller class name must begin with the first tag name of the operation, and then preferably followed by the word `controller`.

For example, the operation `addPet` has `pet` as the first tag, therefore it is called `PetController`.

```yaml
paths:
  /pet:
    post:
      tags:
        - pet
      summary: Add a new pet to the store
      description: ''
      operationId: addPet
```

## Declaration without OpenAPI

It is also possible to declare routes without an OpenAPI specification, although you'll probably be better off using Spark directly.

```java 
Service http = Service.ignite();

List<?> implementations = Arrays.asList(new PetController(), new StoreController());
Sparkling sparkling = Sparkling.setup(http, implementations);

sparkling.newRoute(new RouteDefinition(
        "pet", // Binds to PetController
        "addPet",
        SparklingVerb.POST,
        "/pet",
        Arrays.asList("application/json", "application/xml"),
        Arrays.asList("application/json", "application/xml"),
        Arrays.asList(new SparklingParameter(
                "body",
                ParameterLocation.BODY,
                ArrayType.NONE,
                DeserializeInto.STRING,
                SparklingRequirement.REQUIRED
        ))
));
```
