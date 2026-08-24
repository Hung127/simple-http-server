package com.example.web;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.example.web.multithreading.HTTPSender;

import java.io.BufferedReader;

public class RequestHandler {
    private ServerSocket server;
    private InputStream inStream = null;
    private OutputStream outStream = null;

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

    RequestHandler(ServerSocket server) {
        if (server == null) {
            throw new IllegalArgumentException("Invalid server socket");
        }
        this.server = server;
    }

    public void begin() throws IOException {
        System.out.println("Server is listening on port " + this.server.getLocalPort());
        Socket socket = null;
        int i = 0;
        while (true) {
            socket = this.server.accept();
            this.inStream = socket.getInputStream();
            this.outStream = socket.getOutputStream();

            // TODO: parse request then create thread
            this.readRequest();

            if (true) {
                HTTPSender sender = new HTTPSender(this.inStream, this.outStream);
                Thread thread = new Thread(sender);
                thread.start();
            }
            if (i++ >= 5) {
                break;
            }
        }
        if (socket != null) {
            socket.close();
        }
    }
}
