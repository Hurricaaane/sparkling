package eu.ha3.openapi.sparkling.petstore;

import java.io.InputStream;

/**
 * (Default template)
 * Created on 2018-02-17
 *
 * @author Ha3
 */
public class UploadFileRequest {
    private final long petId;
    private final String additionalMetadata;
    private final String fileName;
    private final InputStream file;

    public UploadFileRequest(long petId, String additionalMetadata, String fileName, InputStream file) {
        this.petId = petId;
        this.additionalMetadata = additionalMetadata;
        this.fileName = fileName;
        this.file = file;
    }

    public long getPetId() {
        return petId;
    }

    public String getAdditionalMetadata() {
        return additionalMetadata;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getFile() {
        return file;
    }
}
