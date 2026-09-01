package com.example.web.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HTTPVersionTest {

    @Test
    void getBestCompatibleVersionExactMatch() {
        try {
            HTTPVersion bestCompatibleVersion = HTTPVersion.getBestCompatibleVersion("HTTP/1.1");
            assertEquals(bestCompatibleVersion, HTTPVersion.HTTP_1_1);
        } catch (BadHTTPVersionException e) {
            fail();
        }
    }

    @Test
    void getBestCompatibleVersionBadFormat() {
        try {
            HTTPVersion bestCompatibleVersion = HTTPVersion.getBestCompatibleVersion("HttP/1.1");
            fail();
        } catch (BadHTTPVersionException e) {
            // do nothing, pass
        }
    }

    @Test
    void getBestCompatibleVersionHigherMinorVersion() {
        try {
            HTTPVersion bestCompatibleVersion = HTTPVersion.getBestCompatibleVersion("HTTP/1.1");
            assertEquals(bestCompatibleVersion, HTTPVersion.HTTP_1_1);
        } catch (BadHTTPVersionException e) {
            fail();
        }
    }

    @Test
    void getBestCompatibleVersionHigherMajor() {
        try {
            HTTPVersion bestCompatibleVersion = HTTPVersion.getBestCompatibleVersion("HTTP/2.1");
            assertNull(bestCompatibleVersion);
        } catch (BadHTTPVersionException e) {
            fail();
        }
    }
}
