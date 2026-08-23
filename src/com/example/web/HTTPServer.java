package com.example.web;

import com.example.web.configuration.ConfigurationManager;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.InputStream;
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

        // try {
        // // System.out.println(Json.stringify(config, true));
        // } catch (Exception e) {
        // System.out.println(e.toString());
        // }

        int port = config.getPort();
        System.out.println("Creating socket with port " + port + "...");
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);
            Socket socket = server.accept();
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            // TODO: Reading
            // DO IT

            // TODO: Writing
            HTTPServer httpServer = new HTTPServer();
            String response = httpServer.makeHTTPResponse(httpServer.getHTML());
            outputStream.write(response.getBytes());

            System.out.println("Sent: " + response);

            System.out.println("Server is closing...");
            socket.close();
            server.close();
            System.out.println("Server closed");

        } catch (IOException e) {
            System.out.println("Exception: " + e.toString());
        }
    }
}
