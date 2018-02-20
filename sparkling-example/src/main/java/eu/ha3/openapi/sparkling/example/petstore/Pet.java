package eu.ha3.openapi.sparkling.example.petstore;

import java.util.List;

/**
 * (Default template)
 * Created on 2017-10-05
 *
 * @author Ha3
 */
public class Pet {
    private final long id;
    private final Category category;
    private final String name;
    private final List<String> photoUrls;
    private final List<Tag> tags;
    private final String status;

    public Pet(long id, Category category, String name, List<String> photoUrls, List<Tag> tags, String status) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.photoUrls = photoUrls;
        this.tags = tags;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public String getStatus() {
        return status;
    }
}
