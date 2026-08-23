package com.example.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class Json {
    private static ObjectMapper objectMapper = Json.getObjectMapper();

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // allow skipping fields that class does not have in the fromJson function
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }

    private static String generateJson(Object obj, boolean pretty) throws JsonProcessingException {
        ObjectWriter objectWriter = Json.objectMapper.writer();

        if (pretty) {
            objectWriter = objectWriter.with(SerializationFeature.INDENT_OUTPUT);
        }

        return objectWriter.writeValueAsString(obj);
    }

    public static JsonNode parse(String s) throws IOException {
        return Json.objectMapper.readTree(s);
    }

    // JSON to POJO
    public static <T> T fromJson(JsonNode node, Class<T> clazz) throws IOException {
        return Json.objectMapper.treeToValue(node, clazz);
    }

    public static <T> T fromString(String jsonString, Class<T> clazz) throws IOException {
        JsonNode node = Json.parse(jsonString);
        return Json.fromJson(node, clazz);
    }

    public static <T> T fromJson(JsonNode node, TypeReference<T> typeRef) {
        return objectMapper.convertValue(node, typeRef);
    }

    public static <T> T fromString(String jsonString, TypeReference<T> typeRef) throws IOException {
        JsonNode node = Json.parse(jsonString);
        return Json.fromJson(node, typeRef);
    }

    public static JsonNode toJson(Object obj) {
        return Json.objectMapper.valueToTree(obj);
    }

    public static String stringify(JsonNode node, boolean pretty) throws JsonProcessingException {
        return Json.generateJson(node, pretty);
    }

    public static String stringify(Object obj, boolean pretty) throws JsonProcessingException {
        return Json.generateJson(Json.toJson(obj), pretty);
    }
}
