package com.example.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.example.web.configuration.Configuration;

import com.example.web.multithreading.HTTPWorker;

public class RequestHandler implements Runnable {
    private final ServerSocket server;

    private final Configuration config;

    RequestHandler(Configuration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Invalid server socket");
        }
        this.config = config;
        int port = this.config.getPort();
        this.server = new ServerSocket(port);
    }

    @Override
    public void run() {
        System.out.println("Server is listening on port " + this.server.getLocalPort());
        int i = 0;
        try {
            while (true) {
                Socket socket = this.server.accept();
                HTTPWorker sender = new HTTPWorker(socket);
                Thread thread = new Thread(sender);
                thread.start();

                if (i++ >= 5) {
                    break;
                }
            }
        } catch (IOException e) {
        }
    }
}
