package com.example.web.utils;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.api.Todo;
import com.example.api.TodoStore;
import com.example.web.http.HTTPResponse;
import com.example.web.http.HTTPStatusCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class APIHandler {
    private final TodoStore todos;
    private final ObjectMapper mapper;

    public APIHandler() {
        this.todos = new TodoStore();
        this.mapper = new ObjectMapper();
    }

    private HTTPResponse errorResponse(HTTPStatusCode status, String message) {
        HTTPResponse response = new HTTPResponse();
        response.setHeaderValue("Content-Type", "text/json");

        try {
            String body = this.mapper.writeValueAsString(Map.of(
                    "error", status.MESSAGE,
                    "message", message == null ? "" : message,
                    "statusCode", status.STATUS_CODE));
            response.setBody(body.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            response.setBody(status.MESSAGE.getBytes(StandardCharsets.UTF_8));
        }

        response.setStatusCode(status);
        return response;
    }

    public HTTPResponse getAllTodos(Map<String, String> vars, JsonNode content) {
        List<Todo> allTodos = this.todos.getAll();
        HTTPResponse response = new HTTPResponse();

        try {
            response.setHeaderValue("Content-Type", "text/json");
            String result = mapper.writeValueAsString(allTodos);
            response.setBody(result.getBytes(StandardCharsets.UTF_8));
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);
        } catch (JsonProcessingException e) {
            return errorResponse(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Failed to serialize todos");
        }

        return response;
    }

    public HTTPResponse createTodo(Map<String, String> vars, JsonNode content) {
        HTTPResponse response = new HTTPResponse();

        String title = content == null ? null : content.path("title").asText(null);
        boolean completed = content == null ? false : content.path("completed").asBoolean(false);

        if (title == null) { // invalid request
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Field 'title' is required");
        }

        Todo newTodo = new Todo(0, title, completed);

        Todo result = this.todos.add(newTodo);

        try {
            String json = this.mapper.writeValueAsString(result);
            response.setHeaderValue("Content-Type", "text/json");
            response.setBody(json.getBytes(StandardCharsets.UTF_8));
            response.setStatusCode(HTTPStatusCode.CREATED_201);
        } catch (JsonProcessingException e) {
            return errorResponse(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Failed to serialize todo");
        }

        return response;
    }

    public HTTPResponse getTodo(Map<String, String> vars, JsonNode content) {
        HTTPResponse response = new HTTPResponse();
        if (!vars.containsKey("id")) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
        }

        try {
            long id = Long.parseLong(vars.get("id"));
            Todo targetTodo = this.todos.get(id);
            if (targetTodo == null) {
                return errorResponse(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
            }

            String json = this.mapper.writeValueAsString(targetTodo);
            response.setHeaderValue("Content-Type", "text/json");
            response.setBody(json.getBytes(StandardCharsets.UTF_8));
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);

        } catch (JsonProcessingException e) {
            return errorResponse(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Failed to serialize todo");
        } catch (NumberFormatException e) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
        }

        return response;
    }

    public HTTPResponse updateTodo(Map<String, String> vars, JsonNode content) {
        HTTPResponse response = new HTTPResponse();

        if (content == null) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Request body is required");
        }

        if (!vars.containsKey("id")) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
        }

        boolean hasCompleted = content.hasNonNull("completed");
        boolean hasTitle = content.hasNonNull("title");

        if (!hasCompleted && !hasTitle) { // corrupted format
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST,
                    "At least one of 'title' or 'completed' is required");
        }

        try {
            long id = Long.parseLong(vars.get("id"));
            Todo targetTodo = this.todos.get(id);
            if (targetTodo == null) {
                return errorResponse(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
            }

            if (hasTitle) {
                targetTodo.setTitle(content.get("title").asText());
            }
            if (hasCompleted) {
                targetTodo.setCompleted(content.get("completed").asBoolean());
            }

            Todo updated = this.todos.update(id, targetTodo);

            String json = this.mapper.writeValueAsString(updated);
            response.setBody(json.getBytes(StandardCharsets.UTF_8));
            response.setHeaderValue("Content-Type", "text/json");
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);

        } catch (JsonProcessingException e) {
            return errorResponse(HTTPStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR, "Failed to serialize todo");
        } catch (NumberFormatException e) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
        }

        return response;
    }

    public HTTPResponse deleteTodo(Map<String, String> vars, JsonNode content) {
        HTTPResponse response = new HTTPResponse();

        if (!vars.containsKey("id")) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Missing 'id' path variable");
        }

        try {
            long id = Long.parseLong(vars.get("id"));
            if (!this.todos.contains(id)) {
                return errorResponse(HTTPStatusCode.CLIENT_ERROR_404_NOT_FOUND, "Todo not found");
            }

            this.todos.delete(id);
            response.setBody(new byte[0]);
            response.setHeaderValue("Content-Type", "text/json");
            response.setStatusCode(HTTPStatusCode.SUCCESS_200);

        } catch (NumberFormatException e) {
            return errorResponse(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Invalid id format");
        }

        return response;
    }

}
