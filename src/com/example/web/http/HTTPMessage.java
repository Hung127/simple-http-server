package com.example.web.http;

import java.util.HashMap;
import java.util.Map;

public abstract class HTTPMessage {

    protected byte[] body;
    protected HashMap<String, String> header;

    protected HTTPMessage() {
        this.body = null;
        this.header = new HashMap<>();
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setHeaderValue(String fieldName, String fieldValue) throws BadHTTPHeaderException {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        for (String key : this.header.keySet()) {
            if (key.equalsIgnoreCase(fieldName)) {
                throw new BadHTTPHeaderException();
            }
        }

        this.header.put(fieldName, fieldValue);
    }

    public String getHeaderValue(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Invalid fieldName");
        }

        for (Map.Entry<String, String> entry : this.header.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(fieldName)) {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Field name not found");
    }

    public void updateField(String fieldName, String fieldValue) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        if (fieldValue == null || fieldValue.isEmpty()) {
            throw new BadHTTPHeaderException();
        }

        if (!this.hasField(fieldName)) {
            throw new BadHTTPHeaderException();
        }

        this.header.replace(fieldName, fieldValue);
    }

    public boolean hasField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("Invalid fieldName");
        }

        for (String key : this.header.keySet()) {
            if (key.equalsIgnoreCase(fieldName)) {
                return true;
            }
        }

        return false;
    }
}
