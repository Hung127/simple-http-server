package com.example.web.http;

public class HTTPRequest extends HTTPMessage {
    private HTTPMethod method;
    private String target;
    private String originalVersion; // literal httpversion from request
    private HTTPVersion bestCompatibleHTTPVersion;

    HTTPRequest() {
        // do nothing
    }

    HTTPRequest(HTTPMethod method, String target, String version) throws BadHTTPVersionException {
        this.method = method;
        this.target = target;
        this.originalVersion = version;
        this.bestCompatibleHTTPVersion = HTTPVersion.getBestCompatibleVersion(version);
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

    @Override
    public String toString() {
        return "HTTPRequest{method='" + method + "', target='" + target + "', version='" + originalVersion + "'}";
    }
}
