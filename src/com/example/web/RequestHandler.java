package com.example.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import com.example.web.configuration.Configuration;

import com.example.web.http.HTTPWorker;
import com.example.web.utils.WebRootHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private final ServerSocket server;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    private final Configuration config;

    RequestHandler(Configuration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Invalid server socket");
        }
        this.config = config;
        int port = this.config.getPort();
        LOGGER.debug("Creating socket with port " + port + "...");
        this.server = new ServerSocket(port);
    }

    @Override
    public void run() {
        System.out.println("Server is listening on port " + this.server.getLocalPort());
        try {
            while (true) {
                Socket socket = this.server.accept();
                HTTPWorker sender = new HTTPWorker(socket, new WebRootHandler(config.getWebRoot()));
                Thread thread = new Thread(sender);
                thread.start();
            }
        } catch (IOException e) {
        } finally {
            try {
                this.server.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }
}
