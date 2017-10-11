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
