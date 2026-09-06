package com.example.web.http;

import java.io.OutputStream;
import java.net.Socket;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.example.web.utils.Router;

import java.io.IOException;

public class HTTPWorker implements Runnable {
    private final OutputStream outStream;
    private final InputStream inStream;
    private final Socket socket;
    private final Router router;

    public HTTPWorker(Socket socket, Router router) throws IOException {
        this.socket = socket;
        this.inStream = socket.getInputStream();
        this.outStream = socket.getOutputStream();
        this.router = router;
    }

    @Override
    public void run() {
        HTTPParser requestParser = new HTTPParser();
        HTTPRequest request = new HTTPRequest();
        HTTPResponse response = new HTTPResponse();

        try {
            request = requestParser.parseHTTPRequest(this.inStream);
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        } catch (HTTPParsingException e) {
            response.setHeaderValue("Content-Type", "text/html; charset=UTF-8");
            this.setErrorHtmlBody(response, e.getErrorCode());
        } finally {
            response.setVersion(request.getBestCompatibleHTTPVersion());
        }

        // parse success, try to serve file/api
        if (response.getStatusCode() == HTTPStatusCode.SUCCESS_200) {
            response = this.router.route(request);
        }

        if (response.getBody() == null) {
            if (!response.hasField("Content-Type")) {
                response.setHeaderValue("Content-Type", "text/html; charset=UTF-8");
            } else {
                response.updateField("Content-Type", "text/html; charset=UTF-8");
            }

            this.setErrorHtmlBody(response, response.getStatusCode());
        }

        response.setHeaderValue("Connection", "close");
        response.setHeaderValue("Content-Length", String.valueOf(response.getBody().length));

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

    private void setErrorHtmlBody(HTTPResponse response, HTTPStatusCode status) {
        if (status == null) {
            status = HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR;
        }
        response.setStatusCode(status);

        String errorMsg = status.STATUS_CODE + " " + status.MESSAGE;
        String errorHtml = "<html><body><h1>" + errorMsg + "</h1></body></html>";
        response.setBody(errorHtml.getBytes(StandardCharsets.UTF_8));
    }
}
