package com.example.web.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HTTPResponse extends HTTPMessage {

    private HTTPRequest request;
    private HTTPStatusCode statusCode;
    private HashMap<String, String> header;
    private byte[] body = null;
    private static final String CRLF = "\r\n";

    public HTTPResponse() {
        this.header = new HashMap<>();
    }

    void setStatusCode(HTTPStatusCode statusCode) {
        if (statusCode == null) {
            throw new NullPointerException("Status code cannot be null");
        }

        this.statusCode = statusCode;
    }

    HTTPStatusCode getStatusCode() {
        return this.statusCode;
    }

    void setBody(byte[] body) {
        this.body = body;
    }

    byte[] getBody() {
        return this.body;
    }

    void setRequest(HTTPRequest request) {
        if (request == null) {
            throw new NullPointerException("HTTP request cannot be null");
        }
        this.request = request;
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

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        String statusLine = this.request.getBestCompatibleHTTPVersion().LITERAL + " " // status line
                + this.statusCode.STATUS_CODE + " "
                + this.statusCode.MESSAGE + HTTPResponse.CRLF;
        try {
            stream.write(statusLine.getBytes(StandardCharsets.US_ASCII));
            for (Map.Entry<String, String> entry : this.header.entrySet()) { // header
                String key = entry.getKey();
                String value = entry.getValue();

                String headerLine = key + ":" + value + CRLF;

                stream.write(headerLine.getBytes(StandardCharsets.US_ASCII));
            }

            stream.write(HTTPResponse.CRLF.getBytes(StandardCharsets.US_ASCII)); // end header

            if (this.body != null && this.body.length > 0) {
                stream.writeBytes(this.body);
            }

            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot make HTTP response");
        }

    }
}
