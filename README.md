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

Method parameters must be `Request`, `Response`, followed by the parameters of the operations in the order they are declared in the OpenAPI specification. Parameter names do not need to match (they are unavailable at runtime).

If a parameter expects a model, use the model of your choice or a `Map`. Sparkling retrieves the runtime type from the method parameter.

A file parameter will be passed as two parameters `String` and `InputStream` in this order.

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