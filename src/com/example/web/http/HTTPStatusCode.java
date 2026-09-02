package com.example.web.http;

public enum HTTPStatusCode {
    // Client error
    CLIENT_ERROR_400_BAD_REQUEST(400, "Bad Request"),
    CLIENT_ERROR_401_METHOD_NOT_ALLOWED(401, "Method Not Allowed"),
    CLIENT_ERROR_414_URI_TOO_LONG(414, "URI Too Long"),

    // Server error
    SERVER_ERROR_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVER_ERROR_501_NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),

    // Success
    SUCCESS_200(200, "Success");

    public final String MESSAGE;
    public final int STATUS_CODE;

    HTTPStatusCode(int code, String message) {
        this.MESSAGE = message;
        this.STATUS_CODE = code;
    }
}
