package com.atlassian.dbexporter.node;

/**
 * @author Erik van Zijst
 */
public class ParseException extends RuntimeException {

    public ParseException() {
    }

    public ParseException(String s) {
        super(s);
    }

    public ParseException(String s, Throwable throwable) {
        super(s);
        initCause(throwable);
    }

    public ParseException(Throwable throwable) {
        initCause(throwable);
    }
}