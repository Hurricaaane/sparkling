# sparkling

Sparkling reads an OpenAPI 2.0 specification and creates routes using [Spark](http://sparkjava.com/). These routes are mapped to implementations you provide.

There is no code generation involved nor configuration, nor annotations.

    Service http = Service.ignite();

    List<?> implementations = Arrays.asList(new PetController(), new StoreController());
    ISparkling sparkling = CommonSparkling.generic(http, implementations);

    http.path("/v2", () -> {
        CommonSparklingParser.apply(readFile("petstore.json"), sparkling, StandardCharsets.UTF_8);
    });

