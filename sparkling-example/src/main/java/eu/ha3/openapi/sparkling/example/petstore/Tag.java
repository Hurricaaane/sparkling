package eu.ha3.openapi.sparkling.example.petstore;

/**
 * (Default template)
 * Created on 2017-10-05
 *
 * @author Ha3
 */
public class Tag {
    private final long id;
    private final String name;

    public Tag(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
