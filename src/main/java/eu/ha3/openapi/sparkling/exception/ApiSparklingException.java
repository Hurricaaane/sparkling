package eu.ha3.openapi.sparkling.exception;

import eu.ha3.openapi.sparkling.enums.ApiSparklingCode;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class ApiSparklingException extends SparklingException {
    private final ApiSparklingCode code;

    public ApiSparklingException(ApiSparklingCode code) {
        this.code = code;
    }

    public ApiSparklingCode getCode() {
        return code;
    }
}
