package eu.ha3.openapi.sparkling.exception;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class SparklingException extends RuntimeException {
    public SparklingException() {
    }

    public SparklingException(String message) {
        super(message);
    }

    public SparklingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SparklingException(Throwable cause) {
        super(cause);
    }

    public SparklingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
