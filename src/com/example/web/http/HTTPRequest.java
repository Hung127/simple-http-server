package com.example.web.http;

import java.util.HashMap;
import java.util.Map;

public class HTTPRequest extends HTTPMessage {
    private HTTPMethod method = HTTPMethod.GET;
    private String target = "/";
    private String originalVersion; // literal httpversion from request
    private HTTPVersion bestCompatibleHTTPVersion = HTTPVersion.HTTP_1_1;

    private HashMap<String, String> header;

    HTTPRequest() {
        // do nothing
        this.header = new HashMap<>();
    }

    HTTPRequest(HTTPMethod method, String target, String version) throws BadHTTPVersionException {
        this.method = method;
        this.target = target;
        this.originalVersion = version;
        this.bestCompatibleHTTPVersion = HTTPVersion.getBestCompatibleVersion(version);
        this.header = new HashMap<>();
    }

    HTTPMethod getMethod() {
        return method;
    }

    void setMethod(String methodName) throws HTTPParsingException {
        for (HTTPMethod method : HTTPMethod.values()) {
            if (method.name().equals(methodName)) {
                this.method = method;
                return;
            }
        }
        throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
    }

    String getTarget() {
        return target;
    }

    void setTarget(String target) throws HTTPParsingException {
        if (target == null || target.length() == 0) {
            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
        this.target = target;
    }

    String getOriginalVersion() {
        return originalVersion;
    }

    void setHTTPVersion(String version) throws BadHTTPVersionException, HTTPParsingException {
        this.originalVersion = version;
        this.bestCompatibleHTTPVersion = HTTPVersion.getBestCompatibleVersion(version);
        if (this.bestCompatibleHTTPVersion == null) {
            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
        }
    }

    HTTPVersion getBestCompatibleHTTPVersion() {
        return this.bestCompatibleHTTPVersion;
    }

    void setHeaderValue(String fieldName, String fieldValue) throws BadHTTPHeaderException {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        if (this.header.containsKey(fieldName)) {
            throw new BadHTTPHeaderException();
        }

        this.header.put(fieldName, fieldValue);
    }

    String getHeaderValue(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Invalid fieldName");
        }

        if (!this.header.containsKey(fieldName)) {
            throw new IllegalArgumentException("Field name not found");
        }

        String fieldValue = this.header.get(fieldName);
        return fieldValue;
    }

    @Override
    public String toString() {
        return "HTTPRequest{method='" + method + "', target='" + target + "', version='" + originalVersion + "'}";
    }
}
