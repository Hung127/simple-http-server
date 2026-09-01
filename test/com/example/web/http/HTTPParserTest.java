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
public class HTTPParserTest {
    private HTTPParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HTTPParser();
    }

    @Test
    void parseHTTPMethodRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateValidGETTestCase());
            assertEquals(request.getMethod(), HTTPMethod.GET);
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPBadMethodRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateBadMethodTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
        }
    }

    @Test
    void parseHTTPLongMethodRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateLongMethodTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
        }
    }

    @Test
    void parseHTTPInvalidNumberItemRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateInvalidNumberItemTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPEmptyRequestLine() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateEmptyRequestLineTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPValidHEADRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateValidHEADTestCase());
            assertEquals(request.getMethod(), HTTPMethod.HEAD);
            assertEquals(request.getTarget(), "/index.html");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPValidGETChecksTarget() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateValidGETTestCase());
            assertEquals(request.getMethod(), HTTPMethod.GET);
            assertEquals(request.getTarget(), "/");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPLFWithoutCR() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateLFWithoutCRTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPCRWithoutLF() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateCRWithoutLFTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPMissingTargetRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMissingTargetTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPEmptyMethodRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateMissingMethodTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
        }
    }

    @Test
    void parseHTTPValidVersion() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateValidGETTestCase());
            assertEquals(request.getOriginalVersion(), "HTTP/1.1");
            assertEquals(request.getBestCompatibleHTTPVersion(), HTTPVersion.HTTP_1_1);
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPHigherMinorVersion() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateHigherMinorVersionTestCase());
            assertEquals(request.getOriginalVersion(), "HTTP/1.2");
            assertEquals(request.getBestCompatibleHTTPVersion(), HTTPVersion.HTTP_1_1);
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPUnsupportedLowerMinorVersion() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateLowerMinorVersionTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
        }
    }

    @Test
    void parseHTTPUnsupportedHigherMajorVersion() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateHigherMajorVersionTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED);
        }
    }

    @Test
    void parseHTTPBadVersionFormat() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateBadVersionFormatTestCase());
            fail();
        } catch (HTTPParsingException e) {
            assertEquals(e.getErrorCode(), HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    @Test
    void parseHTTPCompleteValidRequest() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateCompleteValidRequestTestCase());
            assertEquals(request.getMethod(), HTTPMethod.HEAD);
            assertEquals(request.getTarget(), "/submit");
            assertEquals(request.getOriginalVersion(), "HTTP/1.1");
            assertEquals(request.getBestCompatibleHTTPVersion(), HTTPVersion.HTTP_1_1);
            assertEquals(request.getHeaderValue("host"), "example.com");
            assertEquals(request.getHeaderValue("user-agent"), "test-client");
            assertEquals(request.getHeaderValue("accept"), "*/*");
            assertEquals(request.getHeaderValue("content-length"), "0");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    @Test
    void parseHTTPFullValidGETRequestWithHeaders() {
        try {
            HTTPRequest request = this.httpParser.parseHTTPRequest(this.generateFullValidGETWithHeadersTestCase());
            assertEquals(request.getMethod(), HTTPMethod.GET);
            assertEquals(request.getTarget(), "/about");
            assertEquals(request.getOriginalVersion(), "HTTP/1.1");
            assertEquals(request.getBestCompatibleHTTPVersion(), HTTPVersion.HTTP_1_1);
            assertEquals(request.getHeaderValue("host"), "example.com");
            assertEquals(request.getHeaderValue("accept-language"), "en-us");
        } catch (HTTPParsingException e) {
            fail();
        }
    }

    private InputStream generateValidGETTestCase() {
        String rawData = "GET / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateBadMethodTestCase() {
        String rawData = "Get / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateLongMethodTestCase() {
        String rawData = "GETTTTT / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateInvalidNumberItemTestCase() {
        String rawData = "GET / SIU HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateEmptyRequestLineTestCase() {
        String rawData = "\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateValidHEADTestCase() {
        String rawData = "HEAD /index.html HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateLFWithoutCRTestCase() {
        String rawData = "GET / HTTP/1.1\n" +
                "Host: localhost\n" +
                "\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateCRWithoutLFTestCase() {
        String rawData = "GET / HTTP/1.1\r" +
                "Host: localhost\r" +
                "\r";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateMissingTargetTestCase() {
        String rawData = "GET \r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateMissingMethodTestCase() {
        String rawData = " / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateHigherMinorVersionTestCase() {
        String rawData = "GET / HTTP/1.2\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateLowerMinorVersionTestCase() {
        String rawData = "GET / HTTP/1.0\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateHigherMajorVersionTestCase() {
        String rawData = "GET / HTTP/2.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateBadVersionFormatTestCase() {
        String rawData = "GET / HttP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateCompleteValidRequestTestCase() {
        String rawData = "HEAD /submit HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "User-Agent: test-client\r\n" +
                "Accept: */*\r\n" +
                "Content-Length: 0\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }

    private InputStream generateFullValidGETWithHeadersTestCase() {
        String rawData = "GET /about HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "Accept-Language: en-US\r\n" +
                "\r\n";
        InputStream instream = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
        return instream;
    }
}
