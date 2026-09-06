package com.example.web.utils;

import com.example.web.http.HTTPMethod;

public class Route {
    private final HTTPMethod method;
    private final String route;
    private final Endpoint endpoint;

    public Route(HTTPMethod method, String route, Endpoint endpoint) {
        if (method == null || route == null || endpoint == null) {
            throw new NullPointerException("Method and route cannot be null");
        }
        this.method = method;
        this.endpoint = endpoint;
        if (route.startsWith("/")) { // skip 1 char
            this.route = route.substring(1);
        } else {
            this.route = route;
        }
    }

    String getRoute() {
        return this.route;
    }

    String[] getRouteSegments() {
        return this.route.split("/");
    }

    HTTPMethod getMethod() {
        return this.method;
    }

    Endpoint getEndpoint() {
        return this.endpoint;
    }

    @Override
    public String toString() {
        return this.method.name() + " " + this.route;
    }
}
