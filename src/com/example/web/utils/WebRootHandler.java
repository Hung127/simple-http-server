package com.example.web.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebRootHandler {
    private final Path root;

    private Path completePath(String relativePath) throws IOException {
        Path result = this.root.resolve(relativePath).toRealPath().normalize();
        return result;
    }

    private boolean isChild(Path root, Path path) {
        Path relative = root.relativize(path);
        boolean isOutside = relative.startsWith("..");
        return !isOutside;
    }

    public WebRootHandler(String root) throws IOException {
        if (root == null) {
            throw new NullPointerException();
        }

        Path path = Path.of(root).toRealPath();

        if (Files.isDirectory(path)) {
            this.root = path.normalize();
        } else {
            throw new IllegalArgumentException();
        }

    }

    // for http request like /index.html -> {root}/index.html or so
    private boolean relativePathExistsFile(String relativePath) {
        try {
            Path fullPath = this.completePath(relativePath);
            return (Files.isRegularFile(fullPath) && this.isChild(this.root, fullPath));
        } catch (IOException e) {
            return false;
        }
    }

    public String getTextFileContent(String requestPath) throws IOException, BadRootPathException {
        if (requestPath == null) {
            throw new NullPointerException();
        }

        String normalizedRelativePath = requestPath;

        if (requestPath.endsWith("/")) {
            normalizedRelativePath = requestPath + "index.html";
        }

        normalizedRelativePath = normalizedRelativePath.startsWith("/")
                ? normalizedRelativePath.substring(1) // skip first char if it is /
                : normalizedRelativePath;

        Path fullPath = null;

        try {
            fullPath = this.completePath(normalizedRelativePath);
        } catch (IOException e) {
            throw new BadRootPathException();
        }

        if (!this.relativePathExistsFile(normalizedRelativePath)) {
            throw new BadRootPathException();
        }

        return Files.readString(fullPath);
    }

    public byte[] readFile(String requestPath) throws IOException, BadRootPathException {
        if (requestPath == null) {
            throw new NullPointerException();
        }

        String normalizedRelativePath = requestPath;

        if (requestPath.endsWith("/")) {
            normalizedRelativePath = requestPath + "index.html";
        }

        normalizedRelativePath = normalizedRelativePath.startsWith("/")
                ? normalizedRelativePath.substring(1) // skip first char if it is /
                : normalizedRelativePath;

        Path fullPath = null;

        try {
            fullPath = this.completePath(normalizedRelativePath);
        } catch (IOException e) {
            throw new BadRootPathException();
        }

        if (!this.relativePathExistsFile(normalizedRelativePath)) {
            throw new BadRootPathException();
        }

        return Files.readAllBytes(fullPath);
    }

    public String getContentType(String requestPath) throws BadRootPathException {
        if (requestPath == null) {
            throw new NullPointerException();
        }

        String normalizedRelativePath = requestPath;

        if (requestPath.endsWith("/")) {
            normalizedRelativePath = requestPath + "index.html";
        }

        normalizedRelativePath = normalizedRelativePath.startsWith("/")
                ? normalizedRelativePath.substring(1)
                : normalizedRelativePath;

        Path fullPath = null;

        try {
            fullPath = this.completePath(normalizedRelativePath);
        } catch (IOException e) {
            throw new BadRootPathException();
        }

        if (!this.relativePathExistsFile(normalizedRelativePath)) {
            throw new BadRootPathException();
        }

        String fileName = fullPath.getFileName().toString();
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        } else if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (lower.endsWith(".js")) {
            return "application/javascript";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".json")) {
            return "application/json";
        } else if (lower.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        } else if (lower.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lower.endsWith(".woff2")) {
            return "font/woff2";
        } else if (lower.endsWith(".woff")) {
            return "font/woff";
        }

        return "application/octet-stream";
    }

}
