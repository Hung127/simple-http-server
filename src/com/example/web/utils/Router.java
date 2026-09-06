package com.example.web.utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.web.http.HTTPMethod;
import com.example.web.http.HTTPRequest;
import com.example.web.http.HTTPResponse;
import com.example.web.http.HTTPStatusCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Router {
    private final WebRootHandler webRootHandler;
    private final ConcurrentHashMap<HTTPMethod, ConcurrentHashMap<String, Route>> routes; // Method -> route string ->
                                                                                          // Route Obj
    private final ObjectMapper mapper;

    public Router(WebRootHandler webRootHandler) {
        this.routes = new ConcurrentHashMap<>();
        this.mapper = new ObjectMapper();

        if (webRootHandler == null) {
            throw new NullPointerException("WebRootHandler cannot be null");
        }

        this.webRootHandler = webRootHandler;
    }

    public void register(HTTPMethod method, String path, Endpoint endpoint) {

        Route route = new Route(method, path, endpoint);

        ConcurrentHashMap<String, Route> methodRoutes = this.routes.get(method);
        if (methodRoutes == null) {
            methodRoutes = new ConcurrentHashMap<>();
            this.routes.put(method, methodRoutes);
        }

        if (methodRoutes.containsKey(path)) {
            throw new IllegalArgumentException("Path existed");
        }

        methodRoutes.put(path, route);
    }

    public HTTPResponse route(HTTPRequest request) {
        if (request == null) {
            throw new NullPointerException("Request cannot be null");
        }

        String target = request.getTarget();
        if (!target.startsWith("/api/")) {
            return this.webRootHandler.handle(request.getTarget());
        }

        target = target.substring(1);
        String[] requestRouteSegments = target.split("/");
        HTTPMethod requestMethod = request.getMethod();
        Route route = this.bestBestAPIHandler(requestMethod, requestRouteSegments);

        if (route == null) {
            HTTPResponse response = new HTTPResponse();
            response.setVersion(request.getBestCompatibleHTTPVersion());
            response.setStatusCode(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND);
            return response;
        }

        Map<String, String> variables = this.getVariables(route, requestRouteSegments);

        byte[] body = request.getBody();
        JsonNode json = null;
        if (body != null && body.length > 0) {
            String jsonString = new String(body, StandardCharsets.UTF_8);
            try {
                json = this.mapper.readTree(jsonString);
            } catch (JsonProcessingException e) {
                json = null; // malformed json -> let the handler decide
            }
        }

        HTTPResponse response = route.getEndpoint().handle(variables, json);
        response.setVersion(request.getBestCompatibleHTTPVersion());

        return response;
    }

    private Map<String, String> getVariables(Route route, String[] requestRouteSegments) {
        HashMap<String, String> result = new HashMap<>();
        String[] handlerSegments = route.getRouteSegments();
        int len = handlerSegments.length;

        for (int i = 0; i < len; i++) {
            if (handlerSegments[i].startsWith("{")) {
                int begin = 1;
                int end = handlerSegments[i].lastIndexOf("}");
                String key = handlerSegments[i].substring(begin, end);
                result.put(key, requestRouteSegments[i]);
            }
        }

        return result;
    }

    private Route bestBestAPIHandler(HTTPMethod requestMethod, String[] requestRouteSegments) {
        int bestScore = 0;
        Route bestMatchRoute = null;

        ConcurrentHashMap<String, Route> methodRoutes = this.routes.get(requestMethod);
        if (methodRoutes == null) {
            return null;
        }

        for (Route route : methodRoutes.values()) {
            String[] routeSegments = route.getRouteSegments();
            if (routeSegments.length != requestRouteSegments.length) {
                continue;
            }

            int score = 0;

            for (int i = 0; i < routeSegments.length; i++) {
                if (routeSegments[i].startsWith("{")) {
                    score++;
                } else if (routeSegments[i].equals(requestRouteSegments[i])) {
                    score += 2;
                } else { // doesnt match anything
                    score = 0;
                    break;
                }

            }

            if (score > bestScore) {
                bestMatchRoute = route;
                bestScore = score;
            }
        }

        return bestMatchRoute;
    }
}
