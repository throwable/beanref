package com.github.throwable.beanref;

public class IllegalBeanPathException extends RuntimeException {
    public IllegalBeanPathException() {
    }

    public IllegalBeanPathException(String message) {
        super(message);
    }

    public IllegalBeanPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalBeanPathException(Throwable cause) {
        super(cause);
    }

    public IllegalBeanPathException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
