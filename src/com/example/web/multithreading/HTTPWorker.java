package com.example.web.multithreading;

import java.io.OutputStream;
import java.net.Socket;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

public class HTTPWorker implements Runnable {
    private final String CRLF = "\r\n";
    private final OutputStream outStream;
    private final InputStream inStream;
    private final Socket socket;

    private void readRequest() throws IOException {
        // make a inputstreamreader to make inputstream bytes into utf8 characters
        InputStreamReader inputStreamReader = new InputStreamReader(this.inStream,
                StandardCharsets.UTF_8);
        // we cannot use inputStream to read all bytes, because the readAllBytes stops
        // iff the connection is close, maybe the browser wants to keep the connection
        // -> cant read all bytes at once

        // make buffer reader to read inputreader line by line
        BufferedReader bufferReader = new BufferedReader(inputStreamReader);

        String line = null;
        // line = null means there is no more data (closed connection)
        while ((line = bufferReader.readLine()) != null) {
            // problem: maybe the browser just keep the connection
            // so we want a way to stop when a request header is sent -> the end of a
            // request header is a blank line -> isEmpty() ("")
            if (line.isEmpty()) {
                break;
            }
            System.out.println("Received: " + line);
        }
    }

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

    public HTTPWorker(Socket socket) throws IOException {
        this.socket = socket;
        this.inStream = socket.getInputStream();
        this.outStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        try {
            this.readRequest();
        } catch (IOException e) {
            System.out.println("Cannot read request message");
        }

        String responseBody = this.getHTML();
        String response = this.makeHTTPResponse(responseBody);

        try {
            this.outStream.write(response.getBytes());
        } catch (IOException e) {
            System.out.println("Cannot send read request or send response to client");
        } finally {
            try {
                this.inStream.close();
            } catch (IOException e) {
            }
            try {
                this.outStream.close();
            } catch (IOException e) {

            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }
}
