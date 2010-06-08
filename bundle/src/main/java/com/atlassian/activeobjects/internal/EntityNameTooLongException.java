package com.atlassian.activeobjects.internal;

public class EntityNameTooLongException extends IllegalArgumentException {

    public EntityNameTooLongException() {
    }

    public EntityNameTooLongException(String s) {
        super(s);
    }

    public EntityNameTooLongException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityNameTooLongException(Throwable cause) {
        super(cause);
    }
    
}
