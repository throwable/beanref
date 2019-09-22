package com.github.throwable.beanutil;

public class InaccessiblePathException extends RuntimeException {
    public InaccessiblePathException() {
    }

    public InaccessiblePathException(String message) {
        super(message);
    }

    public InaccessiblePathException(String message, Throwable cause) {
        super(message, cause);
    }

    public InaccessiblePathException(Throwable cause) {
        super(cause);
    }

    public InaccessiblePathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
