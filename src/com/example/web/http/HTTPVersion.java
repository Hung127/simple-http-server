package com.example.web.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum HTTPVersion {
    HTTP_1_1("HTTP/1.1", 1, 1);

    public final String LITERAL;
    public final int MAJOR;
    public final int MINOR;

    HTTPVersion(String literal, int major, int minor) {
        this.LITERAL = literal;
        this.MAJOR = major;
        this.MINOR = minor;
    }

    public static final Pattern httpVersionPattern = Pattern.compile("^HTTP/(?<major>\\d+).(?<minor>\\d+)");

    public static HTTPVersion getBestCompatibleVersion(String literalVersion) throws BadHTTPVersionException {
        Matcher matcher = httpVersionPattern.matcher(literalVersion);
        if (!matcher.find() || matcher.groupCount() != 2) {
            throw new BadHTTPVersionException();
        }
        HTTPVersion bestCompatibleVersion = null;

        int major = Integer.parseInt(matcher.group("major"));
        int minor = Integer.parseInt(matcher.group("minor"));

        for (HTTPVersion version : HTTPVersion.values()) {
            if (version.LITERAL == literalVersion) {
                return version;
            } else if (version.MAJOR == major) {
                if (version.MINOR <= minor) {
                    bestCompatibleVersion = version;
                }
            }
        }
        return bestCompatibleVersion;
    }
}
