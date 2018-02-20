package eu.ha3.openapi.sparkling.exception;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class ParseSparklingException extends SparklingException {
    public ParseSparklingException() {
    }

    public ParseSparklingException(String message) {
        super(message);
    }

    public ParseSparklingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseSparklingException(Throwable cause) {
        super(cause);
    }

    public ParseSparklingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
