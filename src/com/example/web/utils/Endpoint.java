package com.example.web.utils;

import java.util.Map;

import com.example.web.http.HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface Endpoint {
    HTTPResponse handle(Map<String, String> vars, JsonNode content);
}
