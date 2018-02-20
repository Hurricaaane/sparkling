package eu.ha3.openapi.sparkling.vo;

import spark.Request;
import spark.Response;

/**
 * (Default template)
 * Created on 2018-02-17
 *
 * @author Ha3
 */
public class Question<D> {
    private final Request request;
    private final Response response;
    private final D data;

    public Question(Request request, Response response, D data) {
        this.request = request;
        this.response = response;
        this.data = data;
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public D getData() {
        return data;
    }
}
