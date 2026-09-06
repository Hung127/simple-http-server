package com.example.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.json.Json;
import com.example.web.http.HTTPMethod;
import com.example.web.http.HTTPRequest;
import com.example.web.http.HTTPResponse;
import com.example.web.http.HTTPStatusCode;
import com.fasterxml.jackson.databind.JsonNode;

class APIHandlerTest {

    private APIHandler handler;

    @TempDir
    Path webRootDir;

    @BeforeEach
    void setUp() throws IOException {
        handler = new APIHandler();
    }

    private String body(HTTPResponse response) {
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    private JsonNode json(HTTPResponse response) throws IOException {
        return Json.parse(body(response));
    }

    private JsonNode content(String s) throws IOException {
        return Json.parse(s);
    }

    private void assertError(HTTPResponse response, HTTPStatusCode status, String message) throws IOException {
        assertEquals(status, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals(status.MESSAGE, body.get("error").asText());
        assertEquals(message, body.get("message").asText());
        assertEquals(status.STATUS_CODE, body.get("statusCode").asInt());
        assertTrue(body.has("statusCode"));
    }

    private Map<String, String> vars(String id) {
        return Map.of("id", id);
    }

    // ---------- getAllTodos ----------

    @Test
    void getAllTodosReturnsSeededTodos() throws Exception {
        HTTPResponse response = this.handler.getAllTodos(Map.of(), null);

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        assertEquals("text/json", response.getHeaderValue("Content-Type"));

        JsonNode body = json(response);
        assertTrue(body.isArray());
        assertEquals(3, body.size());
        assertEquals("learn http", body.get(0).get("title").asText());
        assertEquals(1, body.get(0).get("id").asLong());
    }

    // ---------- createTodo ----------

    @Test
    void createTodoAssignsIdAndReturnsCreated() throws Exception {
        HTTPResponse response = this.handler.createTodo(Map.of(), content("{\"title\":\"shop\",\"completed\":true}"));

        assertEquals(HTTPStatusCode.CREATED_201, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("shop", body.get("title").asText());
        assertTrue(body.get("completed").asBoolean());
        assertEquals(4, body.get("id").asLong());

        JsonNode all = json(this.handler.getAllTodos(Map.of(), null));
        assertEquals(4, all.size());
    }

    @Test
    void createTodoDefaultsCompletedToFalse() throws Exception {
        HTTPResponse response = this.handler.createTodo(Map.of(), content("{\"title\":\"shop\"}"));

        assertEquals(HTTPStatusCode.CREATED_201, response.getStatusCode());
        assertFalse(json(response).get("completed").asBoolean());
    }

    @Test
    void createTodoMissingTitleReturns400() throws Exception {
        HTTPResponse response = this.handler.createTodo(Map.of(), content("{\"completed\":true}"));

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Field 'title' is required");
    }

    @Test
    void createTodoNullContentReturns400() throws Exception {
        HTTPResponse response = this.handler.createTodo(Map.of(), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Field 'title' is required");
    }

    // ---------- getTodo ----------

    @Test
    void getTodoReturnsExistingTodo() throws Exception {
        HTTPResponse response = this.handler.getTodo(vars("1"), null);

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals(1, body.get("id").asLong());
        assertEquals("learn http", body.get("title").asText());
    }

    @Test
    void getTodoMissingIdReturns400() throws Exception {
        HTTPResponse response = this.handler.getTodo(Map.of(), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
    }

    @Test
    void getTodoNonNumericIdReturns400() throws Exception {
        HTTPResponse response = this.handler.getTodo(vars("abc"), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
    }

    @Test
    void getTodoUnknownIdReturns404() throws Exception {
        HTTPResponse response = this.handler.getTodo(vars("999"), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
    }

    // ---------- updateTodo ----------

    @Test
    void updateTodoChangesTitleOnly() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("1"), content("{\"title\":\"learn https\"}"));

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("learn https", body.get("title").asText());
        assertFalse(body.get("completed").asBoolean());

        JsonNode fetched = json(this.handler.getTodo(vars("1"), null));
        assertEquals("learn https", fetched.get("title").asText());
    }

    @Test
    void updateTodoChangesCompletedOnly() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("1"), content("{\"completed\":true}"));

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        JsonNode body = json(response);
        assertTrue(body.get("completed").asBoolean());
        assertEquals("learn http", body.get("title").asText());
    }

    @Test
    void updateTodoChangesBothFields() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("1"),
                content("{\"title\":\"learn https\",\"completed\":true}"));

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        JsonNode body = json(response);
        assertEquals("learn https", body.get("title").asText());
        assertTrue(body.get("completed").asBoolean());
    }

    @Test
    void updateTodoNullContentReturns400() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("1"), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Request body is required");
    }

    @Test
    void updateTodoMissingIdReturns400() throws Exception {
        HTTPResponse response = this.handler.updateTodo(Map.of(), content("{\"title\":\"x\"}"));

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
    }

    @Test
    void updateTodoNoUpdatableFieldReturns400() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("1"), content("{\"unrelated\":true}"));

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST,
                "At least one of 'title' or 'completed' is required");
    }

    @Test
    void updateTodoUnknownIdReturns404() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("999"), content("{\"title\":\"x\"}"));

        assertError(response, HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
    }

    @Test
    void updateTodoNonNumericIdReturns400() throws Exception {
        HTTPResponse response = this.handler.updateTodo(vars("abc"), content("{\"title\":\"x\"}"));

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
    }

    // ---------- deleteTodo ----------

    @Test
    void deleteTodoRemovesExistingTodo() throws Exception {
        HTTPResponse response = this.handler.deleteTodo(vars("1"), null);

        assertEquals(HTTPStatusCode.SUCCESS_200, response.getStatusCode());
        assertEquals(0, response.getBody().length);

        HTTPResponse fetched = this.handler.getTodo(vars("1"), null);
        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, fetched.getStatusCode());

        JsonNode all = json(this.handler.getAllTodos(Map.of(), null));
        assertEquals(2, all.size());
    }

    @Test
    void deleteTodoUnknownIdReturns404() throws Exception {
        HTTPResponse response = this.handler.deleteTodo(vars("999"), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
    }

    @Test
    void deleteTodoMissingIdReturns400() throws Exception {
        HTTPResponse response = this.handler.deleteTodo(Map.of(), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
    }

    @Test
    void deleteTodoNonNumericIdReturns400() throws Exception {
        HTTPResponse response = this.handler.deleteTodo(vars("abc"), null);

        assertError(response, HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
    }

    // ---------- Error body shape ----------

    @Test
    void errorBodyHasAllFields() throws Exception {
        HTTPResponse response = this.handler.getTodo(vars("999"), null);

        JsonNode body = json(response);
        assertEquals(3, body.size());
        assertTrue(body.has("error"));
        assertTrue(body.has("message"));
        assertTrue(body.has("statusCode"));
    }

    // ---------- Registration through Router ----------

    @Test
    void handlersRegisterThroughRouter() throws Exception {
        WebRootHandler webRoot = new WebRootHandler(webRootDir.toString());
        Router router = new Router(webRoot);
        router.register(HTTPMethod.GET, "/api/todos", this.handler::getAllTodos);
        router.register(HTTPMethod.GET, "/api/todos/{id}", this.handler::getTodo);
        router.register(HTTPMethod.POST, "/api/todos", this.handler::createTodo);
        router.register(HTTPMethod.PUT, "/api/todos/{id}", this.handler::updateTodo);
        router.register(HTTPMethod.DELETE, "/api/todos/{id}", this.handler::deleteTodo);

        HTTPResponse list = router.route(request(HTTPMethod.GET, "/api/todos"));
        assertEquals(HTTPStatusCode.SUCCESS_200, list.getStatusCode());
        assertEquals(3, json(list).size());

        HTTPResponse one = router.route(request(HTTPMethod.GET, "/api/todos/2"));
        assertEquals(HTTPStatusCode.SUCCESS_200, one.getStatusCode());
        assertEquals("build a server", json(one).get("title").asText());

        HTTPRequest create = request(HTTPMethod.POST, "/api/todos");
        create.setBody("{\"title\":\"via router\"}".getBytes(StandardCharsets.UTF_8));
        HTTPResponse created = router.route(create);
        assertEquals(HTTPStatusCode.CREATED_201, created.getStatusCode());
        assertEquals("via router", json(created).get("title").asText());

        HTTPRequest update = request(HTTPMethod.PUT, "/api/todos/1");
        update.setBody("{\"title\":\"rerouted\"}".getBytes(StandardCharsets.UTF_8));
        HTTPResponse updated = router.route(update);
        assertEquals(HTTPStatusCode.SUCCESS_200, updated.getStatusCode());
        assertEquals("rerouted", json(updated).get("title").asText());

        HTTPResponse deleted = router.route(request(HTTPMethod.DELETE, "/api/todos/2"));
        assertEquals(HTTPStatusCode.SUCCESS_200, deleted.getStatusCode());

        HTTPResponse gone = router.route(request(HTTPMethod.GET, "/api/todos/2"));
        assertEquals(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, gone.getStatusCode());
    }

    private HTTPRequest request(HTTPMethod method, String target) throws Exception {
        return new HTTPRequest(method, target, "HTTP/1.1");
    }
}
