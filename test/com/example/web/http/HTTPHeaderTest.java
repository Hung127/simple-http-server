package com.example.web.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HTTPHeaderTest {
    private HTTPParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HTTPParser();
    }

    @Test
    void parseSingleHeader() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateSingleHeaderTestCase());
            assertEquals(request.getHeaderValue("host"), "localhost");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseMultipleHeaders() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMultipleHeadersTestCase());
            assertEquals(request.getHeaderValue("host"), "example.com");
            assertEquals(request.getHeaderValue("user-agent"), "test-client");
            assertEquals(request.getHeaderValue("accept"), "*/*");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHeaderWithOptionalWhitespace() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateWhitespaceHeaderTestCase());
            assertEquals(request.getHeaderValue("host"), "localhost");
            assertEquals(request.getHeaderValue("x-test"), "value");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHeaderNameAndValueLowercased() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMixedCaseHeaderTestCase());
            assertEquals(request.getHeaderValue("user-agent"), "test-client");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHeaderValueLowercased() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMixedCaseHeaderTestCase());
            assertEquals(request.getHeaderValue("x-mode"), "strict");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHeaderEmptyValue() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateEmptyValueHeaderTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHeaderMissingColon() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMissingColonHeaderTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHeaderSpaceBeforeColon() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateSpaceBeforeColonHeaderTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseDuplicateHeader() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateDuplicateHeaderTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    private InputStream generateSingleHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateMultipleHeadersTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "User-Agent: test-client\r\n" +
                "Accept: */*\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateWhitespaceHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host:   localhost\r\n" +
                "X-Test: value\t \r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateMixedCaseHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "User-Agent: Test-Client\r\n" +
                "X-Mode: STRICT\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateEmptyValueHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host:\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateMissingColonHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "ThisIsNotAHeader\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateSpaceBeforeColonHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host : localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateDuplicateHeaderTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Host: example.com\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }
}