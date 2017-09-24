package eu.ha3.openapi.sparkling.routing;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class SparklingResponseContext {
    private final MultivaluedMap<String, String> headers;
    private final MediaType contentType;
    private final int status;
    private final Object entity;

    public SparklingResponseContext() {
        this(new MultivaluedHashMap<>(), null, Response.Status.OK.getStatusCode(), null);
    }

    public SparklingResponseContext(MultivaluedMap<String, String> headers, MediaType contentType, int status, Object entity) {
        this.headers = headers;
        this.contentType = contentType;
        this.status = status;
        this.entity = entity;
    }

    public SparklingResponseContext header(String key, String value) {
        MultivaluedHashMap<String, String> newMap = new MultivaluedHashMap<>(headers);
        newMap.add(key, value);
        return new SparklingResponseContext(newMap, contentType, status, entity);
    }

    public SparklingResponseContext contentType(MediaType contentType) {
        return new SparklingResponseContext(headers, contentType, status, entity);
    }

    public SparklingResponseContext contentType(String contentType) {
        return new SparklingResponseContext(headers, MediaType.valueOf(contentType), status, entity);
    }

    public SparklingResponseContext status(Response.Status status) {
        return new SparklingResponseContext(headers, contentType, status.getStatusCode(), entity);
    }

    public SparklingResponseContext status(int status) {
        return new SparklingResponseContext(headers, contentType, status, entity);
    }

    public SparklingResponseContext entity(Object entity) {
        return new SparklingResponseContext(headers, contentType, status, entity);
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public Object getEntity() {
        return entity;
    }
}