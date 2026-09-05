package com.example.web.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.web.utils.WebRootHandler;

public class HTTPParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPParser.class);
    private static final int SP = (int) ' ';
    private static final int CR = (int) '\r';
    private static final int LF = (int) '\n';

    private static final Pattern HEADER_LINE_PATTERN = Pattern.compile(
            "^(?<name>[!#$%&'*+\\-.^_`|~0-9A-Za-z]+):[ \\t]*(?<value>.*?)[ \\t]*$");

    private void parseRequestLine(InputStream in, HTTPRequest request, WebRootHandler webRoot)
            throws HTTPParsingException {
        // format: method SP target SP version CRLF
        int _byte;
        StringBuilder processingBuffer = new StringBuilder();
        boolean gotMethod = false;
        boolean gotTarget = false;

        try {
            while ((_byte = in.read()) >= 0) {
                if (_byte == CR) {
                    if ((_byte = in.read()) == HTTPParser.LF) {
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

    private void parseRequestHeader(InputStream in, HTTPRequest request) throws HTTPParsingException {
        try {
            StringBuilder buffer = new StringBuilder();
            int _byte;
            while ((_byte = in.read()) >= 0) {
                if (_byte == HTTPParser.CR) {
                    if ((_byte = in.read()) != HTTPParser.LF) { // CR but no LF
                        throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }
                    if (buffer.length() == 0) { // complete
                        return;
                    }

                    String line = buffer.toString();
                    this.parseSingleLineHeader(line, request);
                    buffer.setLength(0); // clear buffer
                    continue; // keep LF out of the buffer
                } else if (_byte == HTTPParser.LF) { // LF but no CR
                    throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                buffer.append((char) _byte);
            }
        } catch (IOException e) {
            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
    }

    private void parseSingleLineHeader(String line, HTTPRequest request) throws HTTPParsingException {
        Matcher matcher = HEADER_LINE_PATTERN.matcher(line);
        if (!matcher.find() || matcher.groupCount() != 2) {
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
        String fieldName = matcher.group("name").toLowerCase();
        String fieldValue = matcher.group("value");
        LOGGER.debug("Processing name: " + fieldName + " with value: " + fieldValue);
        try {
            request.setHeaderValue(fieldName, fieldValue);
        } catch (BadHTTPHeaderException e) {
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    private void parseRequestBody(InputStream in, HTTPRequest request) throws HTTPParsingException {
        HTTPMethod requestMethod = request.getMethod();
        if (requestMethod == HTTPMethod.GET) { // no need to read body for GET
            return;
        }

        int contentLength = HTTPRequest.MAX_BODY_LENGTH;

        try {
            contentLength = Integer.parseInt(request.getHeaderValue("content-length"));
            if (contentLength < 0) {
                throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
            } else if (contentLength > HTTPRequest.MAX_BODY_LENGTH) {
                throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_413_CONTENT_TOO_LARGE);
            }
        } catch (IllegalArgumentException e) { // wrong format or error in header
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }

        byte[] byteArray = new byte[contentLength];
        try {
            for (int i = 0; i < contentLength; i++) {
                int _byte = in.read();
                if (_byte >= 0) {
                    byteArray[i] = (byte) _byte;
                } else { // shorter than expected
                    throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
            }

            request.setBody(byteArray);

        } catch (IOException e) {
            throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
    }

    public HTTPRequest parseHTTPRequest(InputStream inStream, WebRootHandler webRootHandler)
            throws HTTPParsingException {
        HTTPRequest request = new HTTPRequest();

        // TODO: Builder pattern
        this.parseRequestLine(inStream, request, webRootHandler);
        this.parseRequestHeader(inStream, request);
        this.parseRequestBody(inStream, request);

        return request;
    }
}
