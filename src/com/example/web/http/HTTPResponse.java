package com.example.web.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HTTPResponse extends HTTPMessage {

    private HTTPVersion version = HTTPVersion.HTTP_1_1;
    private HTTPStatusCode statusCode;
    private static final String CRLF = "\r\n";

    public HTTPResponse() {
    }

    public void setStatusCode(HTTPStatusCode statusCode) {
        if (statusCode == null) {
            throw new NullPointerException("Status code cannot be null");
        }

        this.statusCode = statusCode;
    }

    public HTTPStatusCode getStatusCode() {
        return this.statusCode;
    }

    public HTTPVersion getVersion() {
        return this.version;
    }

    public void setVersion(HTTPVersion version) {
        if (version == null) {
            throw new NullPointerException("Version cannot be null");
        }

        this.version = version;
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        String statusLine = this.version.LITERAL + " " // status line
                + this.statusCode.STATUS_CODE + " "
                + this.statusCode.MESSAGE + HTTPResponse.CRLF;
        try {
            stream.write(statusLine.getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : this.header.entrySet()) { // header
                String key = entry.getKey();
                String value = entry.getValue();

                String headerLine = key + ":" + value + CRLF;

                stream.write(headerLine.getBytes(StandardCharsets.UTF_8));
            }

            stream.write(HTTPResponse.CRLF.getBytes(StandardCharsets.UTF_8)); // end header

            if (this.body != null && this.body.length > 0) {
                stream.writeBytes(this.body);
            }

            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot make HTTP response");
        }

    }
}
