package eu.ha3.openapi.sparkling.exception;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class TransformationFailedInternalSparklingException extends SparklingException {
    public TransformationFailedInternalSparklingException() {
    }

    public TransformationFailedInternalSparklingException(String message) {
        super(message);
    }

    public TransformationFailedInternalSparklingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationFailedInternalSparklingException(Throwable cause) {
        super(cause);
    }

    public TransformationFailedInternalSparklingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
