package com.example.web;

import com.example.web.configuration.ConfigurationManager;
import java.io.IOException;
import java.net.ServerSocket;

import com.example.web.configuration.Configuration;

public class HTTPServer {

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
        System.out.println("Creating socket with port " + port + "...");
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            RequestHandler handler = new RequestHandler(server);
            handler.begin();
        } catch (IOException e) {
            System.out.println("Exception: " + e.toString());
        } finally {
            try {
                System.out.println("Closing server...");
                server.close();
            } catch (IOException e) {
            }
        }
    }
}
