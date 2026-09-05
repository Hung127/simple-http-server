package com.example.web.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import com.example.web.utils.WebRootHandler;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HTTPWorkerTest {

    @TempDir
    Path webRootDir;

    private void setUpWebRoot() throws IOException {
        Files.writeString(webRootDir.resolve("index.html"), "<h1>Home</h1>");
        Files.writeString(webRootDir.resolve("about.html"), "<h1>About</h1>");
    }

    private String sendRequest(String rawRequest) throws Exception {
        setUpWebRoot();
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());

        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Socket clientSocket = new Socket("localhost", port);
        Socket serverSocketConn = serverSocket.accept();

        OutputStream clientOut = clientSocket.getOutputStream();
        clientOut.write(rawRequest.getBytes(StandardCharsets.US_ASCII));
        clientOut.flush();

        HTTPWorker worker = new HTTPWorker(serverSocketConn, handler);
        worker.run();

        InputStream clientIn = clientSocket.getInputStream();
        ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = clientIn.read(buffer)) != -1) {
            responseBytes.write(buffer, 0, bytesRead);
        }

        clientSocket.close();
        serverSocket.close();

        return responseBytes.toString(StandardCharsets.US_ASCII);
    }

    @Test
    void validGETReturns200WithDefaultHeaders() throws Exception {
        String response = sendRequest("GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Content-Length:"));
        assertTrue(response.contains("\r\n\r\n"));
        assertTrue(response.contains("<h1>Home</h1>"));
    }

    @Test
    void validGETReturnsCorrectContentLength() throws Exception {
        String response = sendRequest("GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

        String body = response.split("\r\n\r\n", 2)[1];
        int contentLength = Integer.parseInt(
                response.lines()
                        .filter(l -> l.startsWith("Content-Length:"))
                        .findFirst()
                        .orElseThrow()
                        .split(":")[1].trim());
        assertEquals(body.length(), contentLength);
    }

    @Test
    void validGETRootServesIndexHtml() throws Exception {
        String response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("<h1>Home</h1>"));
    }

    @Test
    void postRequestReturns200WithHelloBody() throws Exception {
        String response = sendRequest("POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: 4\r\n\r\nname");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Hello"));
    }

    @Test
    void postRequestBodyParsed() throws Exception {
        String response = sendRequest("POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: 4\r\n\r\nname");

        String body = response.split("\r\n\r\n", 2)[1];
        assertEquals("Hello", body);
    }

    @Test
    void postOversizedBodyReturns413() throws Exception {
        StringBuilder body = new StringBuilder(HTTPRequest.MAX_BODY_LENGTH + 1);
        for (int i = 0; i < HTTPRequest.MAX_BODY_LENGTH + 1; i++) {
            body.append('a');
        }
        String response = sendRequest("POST /submit HTTP/1.1\r\nHost: localhost\r\nContent-Length: "
                + body.length() + "\r\n\r\n" + body);

        assertTrue(response.startsWith("HTTP/1.1 413 Content Too Large\r\n"));
        assertTrue(response.contains("<h1>413 Content Too Large</h1>"));
    }

    @Test
    void postWithoutContentLengthReturns400() throws Exception {
        String response = sendRequest("POST /submit HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 400 Bad Request\r\n"));
        assertTrue(response.contains("<h1>400 Bad Request</h1>"));
    }

    @Test
    void nonexistentFileReturns404WithDefaultHeaders() throws Exception {
        String response = sendRequest("GET /nope.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 404 Not Found\r\n"));
        assertTrue(response.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Content-Length:"));
        assertTrue(response.contains("<h1>404 Not Found</h1>"));
    }

    @Test
    void nonexistentFileReturns404WithErrorHtmlBody() throws Exception {
        String response = sendRequest("GET /nope.html HTTP/1.1\r\nHost: localhost\r\n\r\n");

        String body = response.split("\r\n\r\n", 2)[1];
        assertTrue(body.contains("<html>"));
        assertTrue(body.contains("<body>"));
        assertTrue(body.contains("<h1>404 Not Found</h1>"));
        assertTrue(body.contains("</body>"));
        assertTrue(body.contains("</html>"));
    }

    @Test
    void badRequestReturns400WithDefaultHeaders() throws Exception {
        String response = sendRequest("INVALID / HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 501 Not Implemented\r\n"));
        assertTrue(response.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Content-Length:"));
        assertTrue(response.contains("<h1>501 Not Implemented</h1>"));
    }

    @Test
    void badVersionReturns505WithErrorBody() throws Exception {
        String response = sendRequest("GET / HTTP/3.0\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 505 HTTP Version Not Supported\r\n"));
        assertTrue(response.contains("<h1>505 HTTP Version Not Supported</h1>"));
    }

    @Test
    void emptyRequestLineReturns400() throws Exception {
        String response = sendRequest("\r\n");

        assertTrue(response.startsWith("HTTP/1.1 400 Bad Request\r\n"));
        assertTrue(response.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(response.contains("<h1>400 Bad Request</h1>"));
    }

    @Test
    void connectionHeaderIsClose() throws Exception {
        String response = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.contains("Connection:close\r\n"));
    }

    @Test
    void defaultHeadersPresentOnAllStatusCodes() throws Exception {
        String success = sendRequest("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n");
        assertTrue(success.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(success.contains("Connection:close\r\n"));
        assertTrue(success.contains("Content-Length:"));

        String notFound = sendRequest("GET /missing.html HTTP/1.1\r\nHost: localhost\r\n\r\n");
        assertTrue(notFound.contains("Content-Type:text/html; charset=UTF-8\r\n"));
        assertTrue(notFound.contains("Connection:close\r\n"));
        assertTrue(notFound.contains("Content-Length:"));
    }

    @Test
    void cssFileServedWithCssContentType() throws Exception {
        Files.createDirectories(webRootDir.resolve("css"));
        Files.writeString(webRootDir.resolve("css/style.css"), "body { color: red; }");

        String response = sendRequest("GET /css/style.css HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("Content-Type:text/css; charset=UTF-8\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Content-Length:"));
        assertTrue(response.contains("body { color: red; }"));
    }

    @Test
    void svgFileServedWithImageContentType() throws Exception {
        Files.createDirectories(webRootDir.resolve("images"));
        Files.writeString(webRootDir.resolve("images/landscape.svg"), "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");

        String response = sendRequest("GET /images/landscape.svg HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("Content-Type:image/svg+xml\r\n"));
        assertTrue(response.contains("Connection:close\r\n"));
        assertTrue(response.contains("Content-Length:"));
        assertTrue(response.contains("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"));
    }

    @Test
    void unknownExtensionServedAsOctetStream() throws Exception {
        Files.writeString(webRootDir.resolve("data.bin"), "RAW");

        String response = sendRequest("GET /data.bin HTTP/1.1\r\nHost: localhost\r\n\r\n");

        assertTrue(response.startsWith("HTTP/1.1 200 Success\r\n"));
        assertTrue(response.contains("Content-Type:application/octet-stream\r\n"));
    }

}
