package com.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TodoTest {

    @Test
    void todoHasIdTitleAndCompleted() {
        Todo todo = new Todo(7, "learn http", true);

        assertEquals(7, todo.getId());
        assertEquals("learn http", todo.getTitle());
        assertTrue(todo.isCompleted());
    }

    @Test
    void todoIsIncompleteByDefault() {
        Todo todo = new Todo(1, "todo", false);
        assertFalse(todo.isCompleted());
    }

    @Test
    void todoSettersUpdateFields() {
        Todo todo = new Todo(1, "a", false);
        todo.setId(2);
        todo.setTitle("b");
        todo.setCompleted(true);

        assertEquals(2, todo.getId());
        assertEquals("b", todo.getTitle());
        assertTrue(todo.isCompleted());
    }
}
