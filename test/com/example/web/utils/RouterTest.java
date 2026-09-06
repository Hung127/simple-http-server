package com.example.web.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.example.web.http.HTTPMethod;
import com.example.web.http.HTTPRequest;
import com.example.web.http.HTTPResponse;
import com.example.web.http.HTTPStatusCode;
import com.fasterxml.jackson.databind.JsonNode;

class RouterTest {

    private Router router;

    @TempDir
    Path webRootDir;

    @BeforeEach
    void setUp() throws IOException {
        WebRootHandler webRoot = new WebRootHandler(webRootDir.toString());
        router = new Router(webRoot);
    }

    private static class RecordingEndpoint implements Endpoint {
        final HTTPResponse response = new HTTPResponse();
        Map<String, String> vars;
        JsonNode content;

        RecordingEndpoint() {
            this.response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        }

        @Override
        public HTTPResponse handle(Map<String, String> vars, JsonNode content) {
            this.vars = vars;
            this.content = content;
            return this.response;
        }
    }

    private HTTPRequest request(HTTPMethod method, String target) throws Exception {
        return new HTTPRequest(method, target, "HTTP/1.1");
    }

    private void register(HTTPMethod method, String path, Endpoint endpoint) {
        this.router.register(method, path, endpoint);
    }

    // ---------- Route metadata ----------

    @Test
    void routeStripsLeadingSlash() {
        Route route = new Route(HTTPMethod.GET, "/api/todos", new RecordingEndpoint());
        assertEquals("api/todos", route.getRoute());
    }

    @Test
    void routeKeepsPathWithoutLeadingSlash() {
        Route route = new Route(HTTPMethod.GET, "api/todos", new RecordingEndpoint());
        assertEquals("api/todos", route.getRoute());
    }

    @Test
    void routeSplitsIntoSegments() {
        Route route = new Route(HTTPMethod.GET, "/api/todos/{id}", new RecordingEndpoint());
        assertArrayEquals(new String[] { "api", "todos", "{id}" }, route.getRouteSegments());
    }

    @Test
    void routeRejectsNullMethod() {
        assertThrows(NullPointerException.class,
                () -> new Route(null, "/api/todos", new RecordingEndpoint()));
    }

    @Test
    void routeRejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> new Route(HTTPMethod.GET, null, new RecordingEndpoint()));
    }

    @Test
    void routeRejectsNullEndpoint() {
        assertThrows(NullPointerException.class,
                () -> new Route(HTTPMethod.GET, "/api/todos", null));
    }

    @Test
    void routeToStringNamesMethodAndPath() {
        Route route = new Route(HTTPMethod.POST, "/api/todos", new RecordingEndpoint());
        assertEquals("POST api/todos", route.toString());
    }

    // ---------- Registration ----------

    @Test
    void constructorRejectsNullWebRootHandler() {
        assertThrows(NullPointerException.class, () -> new Router(null));
    }

    @Test
    void registeredRouteDispatchesToEndpoint() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos", endpoint);

        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/api/todos"));

        assertSame(endpoint.response, result);
        assertTrue(endpoint.vars.isEmpty());
    }

    @Test
    void rejectsDuplicatePathForSameMethod() {
        register(HTTPMethod.GET, "/api/todos", new RecordingEndpoint());
        assertThrows(IllegalArgumentException.class,
                () -> register(HTTPMethod.GET, "/api/todos", new RecordingEndpoint()));
    }

    @Test
    void allowsSamePathUnderDifferentMethods() throws Exception {
        RecordingEndpoint listEndpoint = new RecordingEndpoint();
        RecordingEndpoint createEndpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos", listEndpoint);
        register(HTTPMethod.POST, "/api/todos", createEndpoint);

        HTTPResponse getResult = this.router.route(request(HTTPMethod.GET, "/api/todos"));
        HTTPResponse postResult = this.router.route(request(HTTPMethod.POST, "/api/todos"));

        assertSame(listEndpoint.response, getResult);
        assertSame(createEndpoint.response, postResult);
    }

    @Test
    void putAndDeleteDispatchedByMethod() throws Exception {
        RecordingEndpoint updateEndpoint = new RecordingEndpoint();
        RecordingEndpoint deleteEndpoint = new RecordingEndpoint();
        register(HTTPMethod.PUT, "/api/todos/{id}", updateEndpoint);
        register(HTTPMethod.DELETE, "/api/todos/{id}", deleteEndpoint);

        assertSame(updateEndpoint.response, this.router.route(request(HTTPMethod.PUT, "/api/todos/1")));
        assertEquals(Map.of("id", "1"), updateEndpoint.vars);

        assertSame(deleteEndpoint.response, this.router.route(request(HTTPMethod.DELETE, "/api/todos/2")));
        assertEquals(Map.of("id", "2"), deleteEndpoint.vars);
    }

    @Test
    void unknownPathYields404AndEndpointNotCalled() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos", endpoint);

        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/api/nope"));

        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, result.getStatusCode());
        assertNull(endpoint.vars);
    }

    @Test
    void unregisteredMethodYields404() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos", endpoint);

        HTTPResponse result = this.router.route(request(HTTPMethod.HEAD, "/api/todos"));

        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, result.getStatusCode());
        assertNull(endpoint.vars);
    }

    // ---------- Matching ----------

    @Test
    void literalRoutesResolveIndependently() throws Exception {
        RecordingEndpoint a = new RecordingEndpoint();
        RecordingEndpoint b = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/a", a);
        register(HTTPMethod.GET, "/api/b", b);

        assertSame(a.response, this.router.route(request(HTTPMethod.GET, "/api/a")));
        assertSame(b.response, this.router.route(request(HTTPMethod.GET, "/api/b")));
    }

    @Test
    void capturesPathVariable() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos/{id}", endpoint);

        this.router.route(request(HTTPMethod.GET, "/api/todos/42"));

        assertEquals(Map.of("id", "42"), endpoint.vars);
    }

    @Test
    void capturesMultiplePathVariables() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/{kind}/todos/{id}", endpoint);

        this.router.route(request(HTTPMethod.GET, "/api/foo/todos/7"));

        assertEquals(Map.of("kind", "foo", "id", "7"), endpoint.vars);
    }

    @Test
    void literalRouteBeatsPathVariableRoute() throws Exception {
        RecordingEndpoint literal = new RecordingEndpoint();
        RecordingEndpoint variable = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos/{id}", variable);
        register(HTTPMethod.GET, "/api/todos/status", literal);

        assertSame(literal.response, this.router.route(request(HTTPMethod.GET, "/api/todos/status")));
        assertSame(variable.response, this.router.route(request(HTTPMethod.GET, "/api/todos/42")));
    }

    @Test
    void partialLiteralMismatchYields404() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos/{id}", endpoint);

        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/api/other/7"));

        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, result.getStatusCode());
        assertNull(endpoint.vars);
    }

    @Test
    void wrongDepthYields404() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos/{id}", endpoint);

        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/api/todos/42/extra"));

        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, result.getStatusCode());
        assertNull(endpoint.vars);
    }

    // ---------- Request body -> JsonNode ----------

    @Test
    void parsesJsonBodyIntoContent() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.POST, "/api/todos", endpoint);

        HTTPRequest req = request(HTTPMethod.POST, "/api/todos");
        req.setBody("{\"title\":\"learn http\"}".getBytes(StandardCharsets.UTF_8));
        this.router.route(req);

        assertEquals("learn http", endpoint.content.get("title").asText());
    }

    @Test
    void passesNullContentWhenRequestHasNoBody() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.GET, "/api/todos", endpoint);

        HTTPRequest req = request(HTTPMethod.GET, "/api/todos");
        req.setBody(null);
        this.router.route(req);

        assertNull(endpoint.content);
    }

    @Test
    void passesNullContentWhenBodyIsMalformedJson() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint();
        register(HTTPMethod.POST, "/api/todos", endpoint);

        HTTPRequest req = request(HTTPMethod.POST, "/api/todos");
        req.setBody("not json at all".getBytes(StandardCharsets.UTF_8));
        this.router.route(req);

        assertNull(endpoint.content);
    }

    // ---------- Non-API fallback to WebRootHandler ----------

    @Test
    void nonApiRequestFallsBackToWebRootHandler() throws Exception {
        Files.writeString(webRootDir.resolve("index.html"), "HOME_INDEX");

        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/index.html"));

        assertEquals(HTTPStatusCode.SUCCESS_200, result.getStatusCode());
        assertEquals("HOME_INDEX", new String(result.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    void nonApiMissingFileReturns404FromWebRootHandler() throws Exception {
        HTTPResponse result = this.router.route(request(HTTPMethod.GET, "/nope.html"));

        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, result.getStatusCode());
    }
}