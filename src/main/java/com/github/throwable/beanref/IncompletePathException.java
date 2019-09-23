package com.github.throwable.beanref;

public class IncompletePathException extends RuntimeException {
    public IncompletePathException() {
    }

    public IncompletePathException(String message) {
        super(message);
    }

    public IncompletePathException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompletePathException(Throwable cause) {
        super(cause);
    }

    public IncompletePathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
