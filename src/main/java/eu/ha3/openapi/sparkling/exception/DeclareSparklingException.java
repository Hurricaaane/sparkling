package eu.ha3.openapi.sparkling.exception;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class DeclareSparklingException extends SparklingException {
    public DeclareSparklingException() {
    }

    public DeclareSparklingException(String message) {
        super(message);
    }

    public DeclareSparklingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeclareSparklingException(Throwable cause) {
        super(cause);
    }

    public DeclareSparklingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
