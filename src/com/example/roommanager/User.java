package com.example.roommanager;

public class User {
    private final int id;
    private String name;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public User(User other) {
        this.id = other.id;
        this.name = other.name;
    }

    public int getID() {
        return this.id;
    }
}
