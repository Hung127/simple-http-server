package com.example.web.http;

import java.io.OutputStream;
import java.net.Socket;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.example.web.utils.BadRootPathException;
import com.example.web.utils.WebRootHandler;

import java.io.IOException;

public class HTTPWorker implements Runnable {
    private final String CRLF = "\r\n";
    private final OutputStream outStream;
    private final InputStream inStream;
    private final Socket socket;
    private final WebRootHandler fileHandler;

    public String makeHTTPResponse(String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String header = "HTTP/1.1 200 OK" + CRLF +
                "Content-Type: text/html; charset=UTF-8" + CRLF +
                "Content-Length: " + bodyBytes.length + CRLF +
                "Connection: close" + CRLF +
                CRLF;

        return header + body;
    }

    public HTTPWorker(Socket socket, WebRootHandler fileHandler) throws IOException {
        this.socket = socket;
        this.inStream = socket.getInputStream();
        this.outStream = socket.getOutputStream();
        this.fileHandler = fileHandler;
    }

    @Override
    public void run() {
        HTTPParser requestParser = new HTTPParser();
        HTTPRequest request = new HTTPRequest();
        HTTPResponse response = new HTTPResponse();

        try {
            request = requestParser.parseHTTPRequest(this.inStream, this.fileHandler);
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        } catch (HTTPParsingException e) {
            response.setStatusCode(e.getErrorCode());
        } finally {
            response.setRequest(request);
        }

        // TODO: method, API, static files serving handler
        if (response.getStatusCode() == HTTPStatusCode.SUCCESS_200) { // parse success, try to serve file
            try {
                if (request.getMethod() == HTTPMethod.POST) {
                    byte[] body = new String("Hello").getBytes(StandardCharsets.US_ASCII);
                    response.setBody(body);
                } else {
                    byte[] body = this.fileHandler.readFile(request.getTarget());
                    response.setBody(body);
                }
            } catch (IOException e) { // problem in server reading files
                response.setStatusCode(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
            } catch (BadRootPathException e) { // cannot serve file
                response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND);
            }
        }

        if (response.getStatusCode() != HTTPStatusCode.SUCCESS_200) {
            String errorMsg = response.getStatusCode().STATUS_CODE + " " + response.getStatusCode().MESSAGE;
            String errorHtml = "<html><body><h1>" + errorMsg + "</h1></body></html>";
            response.setBody(errorHtml.getBytes(StandardCharsets.UTF_8));
        }

        String contentType;
        try {
            contentType = this.fileHandler.getContentType(request.getTarget());
        } catch (BadRootPathException e) {
            contentType = "text/html; charset=UTF-8";
        }

        try {
            response.setHeaderValue("Content-Type", contentType);
            response.setHeaderValue("Connection", "close");
            response.setHeaderValue("Content-Length", String.valueOf(response.getBody().length));
        } catch (BadHTTPHeaderException e) {
        }

        try {
            this.outStream.write(response.toByteArray());
        } catch (IOException e) {
            System.out.println("Cannot send read request or send response to client");
        } finally {
            try {
                this.inStream.close();
            } catch (IOException e) {
            }
            try {
                this.outStream.close();
            } catch (IOException e) {

            }
            try {
                this.socket.close();
            } catch (IOException e) {
            }
        }
    }
}
