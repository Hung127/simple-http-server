package com.example.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.web.configuration.Configuration;
import com.example.web.http.HTTPMethod;
import com.example.web.http.HTTPWorker;
import com.example.web.utils.Endpoint;
import com.example.web.utils.MyExecutorService;
import com.example.web.utils.Router;
import com.example.web.utils.WebRootHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
    private final ServerSocket server;
    private final Router router;
    private final MyExecutorService executorService;

    RequestHandler(Configuration config, int threadPool)
            throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Invalid server socket");
        }

        int port = config.getPort();
        this.executorService = new MyExecutorService(threadPool);

        LOGGER.debug("Creating socket with port " + port + "...");

        this.server = new ServerSocket(port);
        this.router = new Router(new WebRootHandler(config.getWebRoot()));
    }

    public void register(HTTPMethod method, String path, Endpoint endpoint) {
        this.router.register(method, path, endpoint);
    }

    @Override
    public void run() {
        this.executorService.start();
        System.out.println("Server is listening on port " + this.server.getLocalPort());
        try {
            while (true) {
                Socket socket = this.server.accept();
                HTTPWorker sender = new HTTPWorker(socket, this.router);
                this.executorService.addJob(sender);
            }
        } catch (IOException e) {
            // do nothing
        } finally {
            try {
                this.server.close();
                this.executorService.stop();
            } catch (IOException e) {
                // do nothing
            }
        }
    }
}
