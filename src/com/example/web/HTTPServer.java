package com.example.web;

import com.example.web.configuration.ConfigurationManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.example.json.Json;
import com.example.web.configuration.Configuration;

public class HTTPServer {
    public void start(int port) {

    }

    public void stop() {

    }

    public static void main(String[] args) {
        System.out.println("hi, I am a server");
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        configManager.loadConfigFile("src/com/resources/config.json");
        Configuration config = configManager.getCurrentConfiguration();
        try {
            System.out.println(Json.stringify(config, true));
        } catch (Exception e) {
            System.out.println("ERROR!!");
            // System.out.println(e.toString());
        }
        System.out.println("hi, I am NOT a server anymore");

    }
}
