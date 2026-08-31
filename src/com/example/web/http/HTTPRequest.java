package com.example.web.http;

public class HTTPRequest extends HTTPMessage {
    private HTTPMethod method;
    private String target;
    private String version;

    HTTPRequest() {
        // do nothing
    }

    HTTPRequest(HTTPMethod method, String target, String version) {
        this.method = method;
        this.target = target;
        this.version = version;
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

    void setTarget(String target) {
        this.target = target;
    }

    String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "HTTPRequest{method='" + method + "', target='" + target + "', version='" + version + "'}";
    }
}
