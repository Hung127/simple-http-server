package com.example.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TodoStoreTest {

    private TodoStore store;

    @BeforeEach
    void setUp() {
        this.store = new TodoStore();
    }

    @Test
    void storeSeedsSomeTodos() {
        assertTrue(store.size() > 0);
    }

    @Test
    void newTodoAssignsUniqueIds() {
        Todo a = store.add(new Todo(0, "a", false));
        Todo b = store.add(new Todo(0, "b", false));

        assertTrue(a.getId() != b.getId());
        assertTrue(a.getId() > 0);
        assertTrue(b.getId() > 0);
    }

    @Test
    void getReturnsTodoByExistingId() {
        Todo created = store.add(new Todo(0, "find me", false));
        Todo found = store.get(created.getId());

        assertNotNull(found);
        assertEquals("find me", found.getTitle());
    }

    @Test
    void getReturnsNullForUnknownId() {
        assertNull(store.get(999999L));
    }

    @Test
    void getAllReturnsAllTodos() {
        int before = store.size();
        store.add(new Todo(0, "a", false));
        store.add(new Todo(0, "b", false));

        assertEquals(before + 2, store.getAll().size());
    }

    @Test
    void updateReturnsNullForUnknownId() {
        assertNull(store.update(999999L, new Todo(0, "x", false)));
    }

    @Test
    void updateChangesExistingTodo() {
        Todo created = store.add(new Todo(0, "old", false));
        Todo updated = store.update(created.getId(), new Todo(0, "new", true));

        assertNotNull(updated);
        assertEquals("new", updated.getTitle());
        assertTrue(updated.isCompleted());
        assertEquals("new", store.get(created.getId()).getTitle());
    }

    @Test
    void deleteRemovesTodoAndReportsTrue() {
        Todo created = store.add(new Todo(0, "bye", false));

        assertTrue(store.delete(created.getId()));
        assertNull(store.get(created.getId()));
    }

    @Test
    void deleteUnknownIdReportsFalse() {
        assertFalse(store.delete(999999L));
    }

    @Test
    void containsChecksById() {
        Todo created = store.add(new Todo(0, "here", false));
        assertTrue(store.contains(created.getId()));
        assertFalse(store.contains(999999L));
    }
}
