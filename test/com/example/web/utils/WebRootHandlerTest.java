package com.example.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    // ---------- Root initialization ----------

    @Test
    void acceptsValidExistingDirectoryRoot() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
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
    void acceptsRelativeRootPath() throws IOException, BadRootPathException {
        String relName = "webroot-relative-test";
        Path relativeRoot = Path.of(relName);
        try {
            Files.createDirectories(relativeRoot);
            Files.writeString(relativeRoot.resolve("index.html"), "RELATIVE_HOME");
            WebRootHandler handler = new WebRootHandler(relName);
            assertEquals("RELATIVE_HOME", handler.getTextFileContent("/index.html"));
        } finally {
            Files.deleteIfExists(relativeRoot.resolve("index.html"));
            Files.deleteIfExists(relativeRoot);
        }
    }

    @Test
    void behavesCorrectlyWithTrailingSlashRoot() throws IOException, BadRootPathException {
        String withSlash = webRootDir.toString() + "/";
        String withoutSlash = webRootDir.toString();
        WebRootHandler a = new WebRootHandler(withSlash);
        WebRootHandler b = new WebRootHandler(withoutSlash);
        org.junit.jupiter.api.Assertions.assertNotNull(a);
        org.junit.jupiter.api.Assertions.assertNotNull(b);
        assertEquals("HOME_INDEX", a.getTextFileContent("/index.html"));
        assertEquals("HOME_INDEX", b.getTextFileContent("/index.html"));
    }

    // ---------- Basic HTTP paths ----------

    @Test
    void servesIndexHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("HOME_INDEX", handler.getTextFileContent("/index.html"));
    }

    @Test
    void servesFooHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("FOO_HTML", handler.getTextFileContent("/foo.html"));
    }

    @Test
    void servesCssStyle() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("CSS_CONTENT", handler.getTextFileContent("/css/style.css"));
    }

    @Test
    void servesNestedIndexHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("NESTED_INDEX", handler.getTextFileContent("/nested/index.html"));
    }

    // ---------- Directory index behavior ----------

    @Test
    void servesRootPathIndexHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("HOME_INDEX", handler.getTextFileContent("/"));
    }

    @Test
    void servesNestedDirectoryIndexHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("NESTED_INDEX", handler.getTextFileContent("/nested/"));
    }

    @Test
    void pathEndingInSlashAppendsIndexHtml() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        String result = handler.getTextFileContent("/nested/");
        assertEquals("NESTED_INDEX", result);
    }

    @Test
    void rejectsDirectoryWithoutIndexHtml() throws IOException {
        Files.createDirectories(webRootDir.resolve("empty-dir"));
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/empty-dir/"));
    }

    // ---------- Relative request paths (no leading slash) ----------

    @Test
    void supportsRequestPathWithoutLeadingSlash() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("HOME_INDEX", handler.getTextFileContent("index.html"));
        assertEquals("CSS_CONTENT", handler.getTextFileContent("css/style.css"));
    }

    // ---------- Path normalization ----------

    @Test
    void resolvesHarmlessDotSegmentInMiddle() throws IOException, BadRootPathException {
        Files.createDirectories(webRootDir.resolve("foo"));
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("HOME_INDEX", handler.getTextFileContent("/foo/../index.html"));
    }

    @Test
    void rejectsDotSegmentWithNonExistentIntermediateDirectory() throws IOException {
        // toRealPath() requires every directory in the path to exist on disk,
        // so a dot-segment through a missing directory is rejected.
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/nope/../index.html"));
    }

    @Test
    void resolvesParentSegmentStayingInsideWebRoot() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("FOO_HTML", handler.getTextFileContent("/nested/../foo.html"));
    }

    // ---------- Path traversal protection ----------

    @Test
    void rejectsPathTraversalOutsideWebRoot() throws IOException {
        Path secret = webRootDir.getParent().resolve("secret.txt");
        Files.writeString(secret, "SECRET");
        try {
            WebRootHandler handler = new WebRootHandler(webRootDir.toString());
            assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/../secret.txt"));
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void rejectsDoubleParentTraversal() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/../../secret.txt"));
    }

    @Test
    void rejectsNestedTraversalEscape() throws IOException {
        Path secret = webRootDir.getParent().resolve("secret2.txt");
        Files.writeString(secret, "SECRET");
        try {
            WebRootHandler handler = new WebRootHandler(webRootDir.toString());
            assertThrows(BadRootPathException.class,
                    () -> handler.getTextFileContent("/nested/../../secret2.txt"));
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void rejectsEscapeToEtcPasswd() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class,
                () -> handler.getTextFileContent("/foo/../../../etc/passwd"));
    }

    // ---------- Boundary cases ----------

    @Test
    void rejectsNonexistentFile() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/nope.html"));
    }

    @Test
    void rejectsEmptyRequestPath() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        // Empty path resolves to the root directory, which is not a regular file.
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent(""));
    }

    @Test
    void rejectsNullRequestPath() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(NullPointerException.class, () -> handler.getTextFileContent(null));
    }

    @Test
    void servesFilePathSlash() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("HOME_INDEX", handler.getTextFileContent("/"));
    }

    @Test
    void rejectsDirectoryAsFile() throws IOException {
        Files.createDirectories(webRootDir.resolve("real-dir"));
        Files.writeString(webRootDir.resolve("real-dir/other.html"), "OTHER");
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        // /real-dir is a directory, not a regular file -> rejected.
        assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/real-dir"));
    }

    @Test
    void servesFilenameWithSpaces() throws IOException, BadRootPathException {
        Files.writeString(webRootDir.resolve("my page.html"), "PAGE_WITH_SPACE");
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("PAGE_WITH_SPACE", handler.getTextFileContent("/my page.html"));
    }

    @Test
    void servesUnicodeFilename() throws IOException, BadRootPathException {
        Files.writeString(webRootDir.resolve("café.html"), "CAFE");
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("CAFE", handler.getTextFileContent("/café.html"));
    }

    @Test
    void servesDeeplyNestedPath() throws IOException, BadRootPathException {
        Path deep = webRootDir.resolve("a/b/c/d.txt");
        Files.createDirectories(deep.getParent());
        Files.writeString(deep, "DEEP");
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("DEEP", handler.getTextFileContent("/a/b/c/d.txt"));
    }

    // ---------- File contents and errors ----------

    @Test
    void returnsExactFileContents() throws IOException, BadRootPathException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertEquals("FOO_HTML", handler.getTextFileContent("/foo.html"));
        assertEquals("CSS_CONTENT", handler.getTextFileContent("/css/style.css"));
    }

    @Test
    void invalidOutsidePathThrowsBadRootPathException() throws IOException {
        WebRootHandler handler = new WebRootHandler(webRootDir.toString());
        assertThrows(BadRootPathException.class,
                () -> handler.getTextFileContent("/../../does-not-matter"));
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
            WebRootHandler handler = new WebRootHandler(webRootDir.toString());
            // A symlink pointing outside the root must not expose the outside file.
            assertThrows(BadRootPathException.class, () -> handler.getTextFileContent("/evil-link"));
        } finally {
            if (symlinkCreated) {
                Files.deleteIfExists(link);
            }
            Files.deleteIfExists(outsideSecret);
        }
    }

    @Test
    void servesFileThroughSymlinkInsideWebRoot() throws IOException, BadRootPathException {
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
            WebRootHandler handler = new WebRootHandler(webRootDir.toString());
            assertEquals("SHARED_CONTENT", handler.getTextFileContent("/alias-link"));
        } finally {
            if (symlinkCreated) {
                Files.deleteIfExists(link);
            }
        }
    }
}
