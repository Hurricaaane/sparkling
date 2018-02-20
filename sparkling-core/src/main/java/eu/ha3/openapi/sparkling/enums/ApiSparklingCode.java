package eu.ha3.openapi.sparkling.enums;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public enum ApiSparklingCode {
    NOT_ACCEPTABLE(405),;

    private final int restCode;

    ApiSparklingCode(int restCode) {
        this.restCode = restCode;
    }

    public int getRestCode() {
        return restCode;
    }
}
