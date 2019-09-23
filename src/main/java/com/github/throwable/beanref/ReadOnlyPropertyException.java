package com.github.throwable.beanref;

public class ReadOnlyPropertyException extends RuntimeException {
    public ReadOnlyPropertyException() {
        super();
    }

    public ReadOnlyPropertyException(String message) {
        super(message);
    }

    public ReadOnlyPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadOnlyPropertyException(Throwable cause) {
        super(cause);
    }

    protected ReadOnlyPropertyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
