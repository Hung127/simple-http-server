package com.example.web;

import com.example.web.configuration.ConfigurationManager;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.example.web.configuration.Configuration;

public class HTTPServer {
    private final String CRLF = "\r\n";

    public void start(int port) {

    }

    public void stop() {

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

    private void readRequest(InputStream inputStream) throws IOException {
        // make a inputstreamreader to make inputstream bytes into utf8 characters
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
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

    public String makeHTTPResponse(String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String header = "HTTP/1.1 200 OK" + CRLF +
                "Content-Type: text/html; charset=UTF-8" + CRLF +
                "Content-Length: " + bodyBytes.length + CRLF +
                "Connection: close" + CRLF +
                CRLF;

        return header + body;
    }

    public static void main(String[] args) {
        System.out.println("hi, I am a server");
        ConfigurationManager configManager = ConfigurationManager.getInstance();

        try {
            configManager.loadConfigFile("src/com/resources/config.json");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Configuration config = configManager.getCurrentConfiguration();

        int port = config.getPort();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Creating socket with port " + port + "...");
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                HTTPServer httpServer = new HTTPServer();

                // TODO: Reading
                try {
                    httpServer.readRequest(inputStream);
                } catch (IOException e) {
                    System.out.println("Failed to read request header from client");
                    socket.close();
                    server.close();
                    scanner.close();
                    return;
                }

                // TODO: Writing
                String response = httpServer.makeHTTPResponse(httpServer.getHTML());
                outputStream.write(response.getBytes());

                System.out.println("Sent: " + response);

                System.out.print("Do you want to stop: ");
                String closed = scanner.nextLine();
                if (closed.compareToIgnoreCase("YES") == 0) {
                    System.out.println("Server is closing...");
                    socket.close();
                    server.close();
                    System.out.println("Server closed");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e.toString());
        }
        scanner.close();
    }
}
