package com.example.web.http;

public enum HTTPMethod {
    GET, HEAD;

    public static final int MAX_LENGTH;

    static {
        // MAX_LENGTH → direct reference to the field being initialized
        // HTTPMethod.MAX_LENGTH → qualified reference through the class/enum name
        // Java doesnt allow to use qualified reference through the class/enum name for
        // static cases

        int maxLen = -1;
        for (HTTPMethod method : HTTPMethod.values()) {
            maxLen = Math.max(maxLen, method.name().length());
        }
        MAX_LENGTH = maxLen;
    }
}
