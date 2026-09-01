package com.example.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPParser.class);
    private static final int SP = (int) ' ';
    private static final int CR = (int) '\r';
    private static final int LF = (int) '\n';

    private void parseRequestLine(InputStreamReader reader, HTTPRequest request)
            throws HTTPParsingException {
        // format: method SP target SP version CRLF
        int _byte;
        StringBuilder processingBuffer = new StringBuilder();
        boolean gotMethod = false;
        boolean gotTarget = false;

        try {
            while ((_byte = reader.read()) >= 0) {
                if (_byte == CR) {
                    if ((_byte = reader.read()) == HTTPParser.LF) {
                        if (!gotMethod || !gotTarget) { // Missing any fields
                            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                        }

                        String version = processingBuffer.toString();
                        LOGGER.debug("Processing: " + version);
                        processingBuffer.setLength(0); // clear builder
                        try {
                            request.setHTTPVersion(version);
                        } catch (BadHTTPVersionException e) {
                            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                        } catch (HTTPParsingException e) { // 505 http version not supported
                            throw e;
                        }
                        return;
                    } else { // have CR but no LF
                        throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }

                }

                if (_byte == HTTPParser.SP) {
                    String currentObj = processingBuffer.toString();
                    LOGGER.debug("Processing: " + currentObj);
                    processingBuffer.setLength(0); // clear builder

                    if (!gotMethod) {
                        request.setMethod(currentObj);
                        gotMethod = true;
                    } else if (!gotTarget) {
                        request.setTarget(currentObj);
                        gotTarget = true;
                    } else { // more than 2 SP -> invalid format
                        throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }

                } else {
                    if (!gotMethod) { // method with too long name
                        if (processingBuffer.length() > HTTPMethod.MAX_LENGTH) {
                            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
                        }
                    }
                    if (_byte == HTTPParser.LF) { // LF without CR
                        throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }
                    processingBuffer.append((char) _byte);
                }
            }
        } catch (IOException e) {
            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
    }

    private void parseRequestHeader(InputStreamReader reader, HTTPRequest request) {

    }

    private void parseRequestBody(InputStreamReader reader, HTTPRequest request) {

    }

    public HTTPRequest parseHTTPRequest(InputStream inStream) throws HTTPParsingException {
        InputStreamReader reader = new InputStreamReader(inStream, StandardCharsets.US_ASCII);

        HTTPRequest request = new HTTPRequest();

        // TODO: Builder pattern
        this.parseRequestLine(reader, request);
        this.parseRequestHeader(reader, request);
        this.parseRequestBody(reader, request);

        return request;
    }
}
