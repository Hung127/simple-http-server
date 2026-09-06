package com.example.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class TodoStore {
    private final ConcurrentMap<Long, Todo> todos = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public TodoStore() {
        seed();
    }

    private void seed() {
        add(new Todo(0, "learn http", false));
        add(new Todo(0, "build a server", false));
        add(new Todo(0, "write some json", true));
    }

    public Todo add(Todo todo) {
        long id = this.nextId.getAndIncrement();
        todo.setId(id);
        this.todos.put(id, todo);
        return todo;
    }

    public Todo get(long id) {
        return this.todos.get(id);
    }

    public List<Todo> getAll() {
        return new ArrayList<>(this.todos.values());
    }

    public Todo update(long id, Todo todo) {
        if (!this.todos.containsKey(id)) {
            return null;
        }
        todo.setId(id);
        this.todos.put(id, todo);
        return todo;
    }

    public boolean delete(long id) {
        return this.todos.remove(id) != null;
    }

    public boolean contains(long id) {
        return this.todos.containsKey(id);
    }

    public int size() {
        return this.todos.size();
    }
}
