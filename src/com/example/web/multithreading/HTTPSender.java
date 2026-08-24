package com.example.web.multithreading;

import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

public class HTTPSender implements Runnable {
    private final String CRLF = "\r\n";
    private final OutputStream outStream;
    private final InputStream inStream;

    private String getHTML() {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head><title>Test Server</title></head>" +
                "<body>" +
                "<h1>Hello from Java!</h1>" +
                "<p>This is a test HTTP server.</p>" +
                "</body>" +
                "</html>";
        return html;
    }

    public String makeHTTPResponse(String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String header = "HTTP/1.1 200 OK" + CRLF +
                "Content-Type: text/html; charset=UTF-8" + CRLF +
                "Content-Length: " + bodyBytes.length + CRLF +
                "Connection: close" + CRLF +
                CRLF;

        return header + body;
    }

    public HTTPSender(InputStream inStream, OutputStream outStream) {
        this.inStream = inStream;
        this.outStream = outStream;
    }

    @Override
    public void run() {
        String responseBody = this.getHTML();
        String response = this.makeHTTPResponse(responseBody);
        try {
            outStream.write(response.getBytes());
        } catch (IOException e) {
            System.out.println("Cannot send response to client");
        }
    }
}
