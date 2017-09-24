package eu.ha3.openapi.sparkling.exception;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class UnavailableControllerSparklingException extends SparklingException {
    public UnavailableControllerSparklingException() {
    }

    public UnavailableControllerSparklingException(String message) {
        super(message);
    }

    public UnavailableControllerSparklingException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnavailableControllerSparklingException(Throwable cause) {
        super(cause);
    }

    public UnavailableControllerSparklingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
