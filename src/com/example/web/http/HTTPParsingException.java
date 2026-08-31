package com.example.web.http;

public class HTTPParsingException extends Exception {
    private final HTTPStatusCode errorCode;

    public HTTPParsingException(HTTPStatusCode errorCode) {
        super(errorCode.MESSAGE);
        this.errorCode = errorCode;
    }

    public HTTPStatusCode getErrorCode() {
        return this.errorCode;
    }

}
