package com.example.web.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HTTPResponseTest {

    private HTTPRequest createDefaultRequest() throws BadHTTPVersionException {
        return new HTTPRequest(HTTPMethod.GET, "/", "HTTP/1.1");
    }

    @Test
    void setAndGetStatusCode() throws BadHTTPVersionException {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
    }

    @Test
    void setStatusCodeThrowsOnNull() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(NullPointerException.class, () -> response.setStatusCode(null));
    }

    @Test
    void setAndGetBody() {
        HTTPResponse response = new HTTPResponse();
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        response.setBody(body);
        assertArrayEquals(body, response.getBody());
    }

    @Test
    void bodyDefaultsToNull() {
        HTTPResponse response = new HTTPResponse();
        assertEquals(null, response.getBody());
    }

    @Test
    void setRequestThrowsOnNull() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(NullPointerException.class, () -> response.setRequest(null));
    }

    @Test
    void setHeaderValueSetsCorrectly() throws BadHTTPHeaderException {
        HTTPResponse response = new HTTPResponse();
        response.setHeaderValue("Content-Type", "text/html");
        assertEquals("text/html", response.getHeaderValue("Content-Type"));
    }

    @Test
    void setHeaderValueThrowsOnDuplicate() throws BadHTTPHeaderException {
        HTTPResponse response = new HTTPResponse();
        response.setHeaderValue("Content-Type", "text/html");
        assertThrows(BadHTTPHeaderException.class, () -> response.setHeaderValue("Content-Type", "application/json"));
    }

    @Test
    void setHeaderValueThrowsOnNullFieldName() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(BadHTTPHeaderException.class, () -> response.setHeaderValue(null, "value"));
    }

    @Test
    void setHeaderValueThrowsOnEmptyFieldName() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(BadHTTPHeaderException.class, () -> response.setHeaderValue("", "value"));
    }

    @Test
    void setHeaderValueThrowsOnNullFieldValue() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(BadHTTPHeaderException.class, () -> response.setHeaderValue("X-Test", null));
    }

    @Test
    void setHeaderValueThrowsOnEmptyFieldValue() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(BadHTTPHeaderException.class, () -> response.setHeaderValue("X-Test", ""));
    }

    @Test
    void getHeaderValueThrowsOnNullFieldName() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(IllegalArgumentException.class, () -> response.getHeaderValue(null));
    }

    @Test
    void getHeaderValueThrowsOnMissingField() {
        HTTPResponse response = new HTTPResponse();
        assertThrows(IllegalArgumentException.class, () -> response.getHeaderValue("Nonexistent"));
    }

    @Test
    void toByteArrayWithBodyAndHeaders() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        response.setHeaderValue("Content-Type", "text/html");
        response.setHeaderValue("Connection", "close");
        byte[] body = "<h1>Hello</h1>".getBytes(StandardCharsets.UTF_8);
        response.setBody(body);
        response.setHeaderValue("Content-Length", String.valueOf(body.length));

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(output.contains("Content-Type:text/html\r\n"));
        assertTrue(output.contains("Connection:close\r\n"));
        assertTrue(output.contains("Content-Length:14\r\n"));
        assertTrue(output.contains("\r\n\r\n"));
        assertTrue(output.endsWith("<h1>Hello</h1>"));
    }

    @Test
    void toByteArrayWithEmptyBody() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        response.setHeaderValue("Content-Type", "text/html");
        response.setHeaderValue("Content-Length", "0");

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(output.contains("Content-Length:0\r\n"));
        assertTrue(output.endsWith("\r\n\r\n"));
    }

    @Test
    void toByteArrayNoHeaders() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertEquals("HTTP/1.1 200 Success\r\n\r\n", output);
    }

    @Test
    void toByteArrayErrorStatus() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        byte[] body = "<html><body><h1>400 Bad Request</h1></body></html>".getBytes(StandardCharsets.UTF_8);
        response.setBody(body);
        response.setHeaderValue("Content-Type", "text/html");
        response.setHeaderValue("Connection", "close");
        response.setHeaderValue("Content-Length", String.valueOf(body.length));

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.startsWith("HTTP/1.1 400 Bad Request\r\n"));
        assertTrue(output.contains("Content-Type:text/html\r\n"));
        assertTrue(output.endsWith("<html><body><h1>400 Bad Request</h1></body></html>"));
    }

    @Test
    void toByteArray500Status() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.startsWith("HTTP/1.1 500 Internal Server Error\r\n"));
    }

    @Test
    void headerValuesPreservedExactly() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        response.setHeaderValue("X-Custom-Header", "my-custom-value");

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.contains("X-Custom-Header:my-custom-value\r\n"));
    }

    @Test
    void multipleHeadersSerialized() throws Exception {
        HTTPResponse response = new HTTPResponse();
        response.setRequest(createDefaultRequest());
        response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        response.setHeaderValue("Content-Type", "text/plain");
        response.setHeaderValue("X-Foo", "bar");
        response.setHeaderValue("X-Bar", "baz");

        byte[] result = response.toByteArray();
        String output = new String(result, StandardCharsets.US_ASCII);

        assertTrue(output.contains("Content-Type:text/plain\r\n"));
        assertTrue(output.contains("X-Foo:bar\r\n"));
        assertTrue(output.contains("X-Bar:baz\r\n"));
    }

}
