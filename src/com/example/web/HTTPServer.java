package com.example.web;

import com.example.web.configuration.ConfigurationManager;
import java.io.IOException;

import com.example.web.configuration.Configuration;
import com.example.web.http.HTTPMethod;
import com.example.web.utils.APIHandler;

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

        try {
            RequestHandler requestHandler = new RequestHandler(config);

            APIHandler api = new APIHandler();
            requestHandler.register(HTTPMethod.GET, "/api/todos", api::getAllTodos);
            requestHandler.register(HTTPMethod.GET, "/api/todos/{id}", api::getTodo);
            requestHandler.register(HTTPMethod.POST, "/api/todos", api::createTodo);
            requestHandler.register(HTTPMethod.PUT, "/api/todos/{id}", api::updateTodo);
            requestHandler.register(HTTPMethod.DELETE, "/api/todos/{id}", api::deleteTodo);

            Thread requestHandlerThread = new Thread(requestHandler);
            requestHandlerThread.start();
            requestHandlerThread.join();
        } catch (IOException e) {
            System.out.println("Exception: " + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Exception: " + e.toString());
        }
    }
}
