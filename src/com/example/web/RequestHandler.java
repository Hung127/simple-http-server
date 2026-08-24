package com.example.web;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import com.example.web.multithreading.HTTPSender;

public class RequestHandler {
    private ServerSocket server;

    RequestHandler(ServerSocket server) {
        if (server == null) {
            throw new IllegalArgumentException("Invalid server socket");
        }
        this.server = server;
    }

    public void begin() throws IOException {
        System.out.println("Server is listening on port " + this.server.getLocalPort());
        int i = 0;
        while (true) {
            Socket socket = this.server.accept();
            HTTPSender sender = new HTTPSender(socket);
            Thread thread = new Thread(sender);
            thread.start();

            if (i++ >= 5) {
                break;
            }
        }
    }
}
