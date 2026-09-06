package com.example.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.web.http.HTTPResponse;
import com.example.web.http.HTTPStatusCode;

class WebRootHandlerTest {

    @TempDir
    Path webRootDir;

    @BeforeEach
    void setUpWebRoot() throws IOException {
        Files.writeString(webRootDir.resolve("index.html"), "HOME_INDEX");
        Files.writeString(webRootDir.resolve("foo.html"), "FOO_HTML");
        Files.createDirectories(webRootDir.resolve("css"));
        Files.writeString(webRootDir.resolve("css/style.css"), "CSS_CONTENT");
        Files.createDirectories(webRootDir.resolve("js"));
        Files.writeString(webRootDir.resolve("js/app.js"), "JS_CONTENT");
        Files.createDirectories(webRootDir.resolve("nested"));
        Files.writeString(webRootDir.resolve("nested/index.html"), "NESTED_INDEX");
    }

    private WebRootHandler handler() throws IOException {
        return new WebRootHandler(webRootDir.toString());
    }

    private HTTPResponse response(WebRootHandler handler, String path) {
        return handler.handle(path);
    }

    private String content(WebRootHandler handler, String path) {
        HTTPResponse resp = handler.handle(path);
        assertEquals(HTTPStatusCode.SUCCESS_200, resp.getStatusCode());
        return new String(resp.getBody(), StandardCharsets.UTF_8);
    }

    private void assertNotFound(WebRootHandler handler, String path) {
        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, handler.handle(path).getStatusCode());
    }

    // ---------- Root initialization ----------

    @Test
    void acceptsValidExistingDirectoryRoot() throws IOException {
        WebRootHandler handler = handler();
        org.junit.jupiter.api.Assertions.assertNotNull(handler);
    }

    @Test
    void rejectsNonexistentRoot() {
        Path missing = webRootDir.resolve("does-not-exist");
        assertThrows(IOException.class, () -> new WebRootHandler(missing.toString()));
    }

    @Test
    void rejectsRegularFileAsRoot() throws IOException {
        Path file = webRootDir.resolve("foo.html");
        assertThrows(IllegalArgumentException.class, () -> new WebRootHandler(file.toString()));
    }

    @Test
    void rejectsNullRoot() {
        assertThrows(NullPointerException.class, () -> new WebRootHandler(null));
    }

    @Test
    void acceptsRelativeRootPath() throws IOException {
        String relName = "webroot-relative-test";
        Path relativeRoot = Path.of(relName);
        try {
            Files.createDirectories(relativeRoot);
            Files.writeString(relativeRoot.resolve("index.html"), "RELATIVE_HOME");
            WebRootHandler handler = new WebRootHandler(relName);
            assertEquals("RELATIVE_HOME", content(handler, "/index.html"));
        } finally {
            Files.deleteIfExists(relativeRoot.resolve("index.html"));
            Files.deleteIfExists(relativeRoot);
        }
    }

    @Test
    void behavesCorrectlyWithTrailingSlashRoot() throws IOException {
        String withSlash = webRootDir.toString() + "/";
        String withoutSlash = webRootDir.toString();
        WebRootHandler a = new WebRootHandler(withSlash);
        WebRootHandler b = new WebRootHandler(withoutSlash);
        org.junit.jupiter.api.Assertions.assertNotNull(a);
        org.junit.jupiter.api.Assertions.assertNotNull(b);
        assertEquals("HOME_INDEX", content(a, "/index.html"));
        assertEquals("HOME_INDEX", content(b, "/index.html"));
    }

    // ---------- Basic HTTP paths ----------

    @Test
    void servesIndexHtml() throws IOException {
        assertEquals("HOME_INDEX", content(handler(), "/index.html"));
    }

    @Test
    void servesFooHtml() throws IOException {
        assertEquals("FOO_HTML", content(handler(), "/foo.html"));
    }

    @Test
    void servesCssStyle() throws IOException {
        assertEquals("CSS_CONTENT", content(handler(), "/css/style.css"));
    }

    @Test
    void servesNestedIndexHtml() throws IOException {
        assertEquals("NESTED_INDEX", content(handler(), "/nested/index.html"));
    }

    // ---------- Directory index behavior ----------

    @Test
    void servesRootPathIndexHtml() throws IOException {
        assertEquals("HOME_INDEX", content(handler(), "/"));
    }

    @Test
    void servesNestedDirectoryIndexHtml() throws IOException {
        assertEquals("NESTED_INDEX", content(handler(), "/nested/"));
    }

    @Test
    void pathEndingInSlashAppendsIndexHtml() throws IOException {
        assertEquals("NESTED_INDEX", content(handler(), "/nested/"));
    }

    @Test
    void rejectsDirectoryWithoutIndexHtml() throws IOException {
        Files.createDirectories(webRootDir.resolve("empty-dir"));
        assertNotFound(handler(), "/empty-dir/");
    }

    // ---------- Relative request paths (no leading slash) ----------

    @Test
    void supportsRequestPathWithoutLeadingSlash() throws IOException {
        WebRootHandler handler = handler();
        assertEquals("HOME_INDEX", content(handler, "index.html"));
        assertEquals("CSS_CONTENT", content(handler, "css/style.css"));
    }

    // ---------- Content-Type headers ----------

    @Test
    void setsContentTypeHeader() throws IOException {
        WebRootHandler handler = handler();
        assertEquals("text/html; charset=UTF-8", response(handler, "/index.html").getHeaderValue("content-type"));
        assertEquals("text/css; charset=UTF-8", response(handler, "/css/style.css").getHeaderValue("content-type"));
        assertEquals("application/javascript", response(handler, "/js/app.js").getHeaderValue("content-type"));
    }

    // ---------- Path normalization ----------

    @Test
    void resolvesHarmlessDotSegmentInMiddle() throws IOException {
        Files.createDirectories(webRootDir.resolve("foo"));
        assertEquals("HOME_INDEX", content(handler(), "/foo/../index.html"));
    }

    @Test
    void rejectsDotSegmentWithNonExistentIntermediateDirectory() throws IOException {
        // toRealPath() requires every directory in the path to exist on disk,
        // so a dot-segment through a missing directory is rejected.
        assertNotFound(handler(), "/nope/../index.html");
    }

    @Test
    void resolvesParentSegmentStayingInsideWebRoot() throws IOException {
        assertEquals("FOO_HTML", content(handler(), "/nested/../foo.html"));
    }

    // ---------- Path traversal protection ----------

    @Test
    void rejectsPathTraversalOutsideWebRoot() throws IOException {
        Path secret = webRootDir.getParent().resolve("secret.txt");
        Files.writeString(secret, "SECRET");
        try {
            assertNotFound(handler(), "/../secret.txt");
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void rejectsDoubleParentTraversal() throws IOException {
        assertNotFound(handler(), "/../../secret.txt");
    }

    @Test
    void rejectsNestedTraversalEscape() throws IOException {
        Path secret = webRootDir.getParent().resolve("secret2.txt");
        Files.writeString(secret, "SECRET");
        try {
            assertNotFound(handler(), "/nested/../../secret2.txt");
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void rejectsEscapeToEtcPasswd() throws IOException {
        assertNotFound(handler(), "/foo/../../../etc/passwd");
    }

    // ---------- Boundary cases ----------

    @Test
    void rejectsNonexistentFile() throws IOException {
        assertNotFound(handler(), "/nope.html");
    }

    @Test
    void rejectsEmptyRequestPath() throws IOException {
        // Empty path resolves to the root directory, which is not a regular file.
        assertNotFound(handler(), "");
    }

    @Test
    void rejectsNullRequestPath() throws IOException {
        assertThrows(NullPointerException.class, () -> response(handler(), null));
    }

    @Test
    void servesFilePathSlash() throws IOException {
        assertEquals("HOME_INDEX", content(handler(), "/"));
    }

    @Test
    void rejectsDirectoryAsFile() throws IOException {
        Files.createDirectories(webRootDir.resolve("real-dir"));
        Files.writeString(webRootDir.resolve("real-dir/other.html"), "OTHER");
        // /real-dir is a directory, not a regular file -> rejected.
        assertNotFound(handler(), "/real-dir");
    }

    @Test
    void servesFilenameWithSpaces() throws IOException {
        Files.writeString(webRootDir.resolve("my page.html"), "PAGE_WITH_SPACE");
        assertEquals("PAGE_WITH_SPACE", content(handler(), "/my page.html"));
    }

    @Test
    void servesUnicodeFilename() throws IOException {
        Files.writeString(webRootDir.resolve("café.html"), "CAFE");
        assertEquals("CAFE", content(handler(), "/café.html"));
    }

    @Test
    void servesDeeplyNestedPath() throws IOException {
        Path deep = webRootDir.resolve("a/b/c/d.txt");
        Files.createDirectories(deep.getParent());
        Files.writeString(deep, "DEEP");
        assertEquals("DEEP", content(handler(), "/a/b/c/d.txt"));
    }

    // ---------- File contents and errors ----------

    @Test
    void returnsExactFileContents() throws IOException {
        WebRootHandler handler = handler();
        assertEquals("FOO_HTML", content(handler, "/foo.html"));
        assertEquals("CSS_CONTENT", content(handler, "/css/style.css"));
    }

    @Test
    void invalidOutsidePathReturns404() throws IOException {
        assertNotFound(handler(), "/../../does-not-matter");
    }

    @Test
    void malformedPathReturns404() throws IOException {
        assertNotFound(handler(), "/foo/../../../secret.txt");
    }

    // ---------- Symlink security ----------

    @Test
    void rejectsSymlinkEscapeOutOfWebRoot() throws IOException {
        Path outsideSecret = webRootDir.getParent().resolve("outside-secret.txt");
        Files.writeString(outsideSecret, "OUTSIDE_SECRET_CONTENT");
        Path link = webRootDir.resolve("evil-link");
        boolean symlinkCreated = false;
        try {
            try {
                Files.createSymbolicLink(link, outsideSecret);
                symlinkCreated = true;
            } catch (IOException | UnsupportedOperationException e) {
                Assumptions.abort("symlinks not supported on this filesystem");
            }
            // A symlink pointing outside the root must not expose the outside file.
            assertNotFound(handler(), "/evil-link");
        } finally {
            if (symlinkCreated) {
                Files.deleteIfExists(link);
            }
            Files.deleteIfExists(outsideSecret);
        }
    }

    @Test
    void servesFileThroughSymlinkInsideWebRoot() throws IOException {
        String targetName = "shared.txt";
        Files.writeString(webRootDir.resolve(targetName), "SHARED_CONTENT");
        Path link = webRootDir.resolve("alias-link");
        boolean symlinkCreated = false;
        try {
            try {
                Files.createSymbolicLink(link, webRootDir.resolve(targetName));
                symlinkCreated = true;
            } catch (IOException | UnsupportedOperationException e) {
                Assumptions.abort("symlinks not supported on this filesystem");
            }
            assertEquals("SHARED_CONTENT", content(handler(), "/alias-link"));
        } finally {
            if (symlinkCreated) {
                Files.deleteIfExists(link);
            }
        }
    }
}