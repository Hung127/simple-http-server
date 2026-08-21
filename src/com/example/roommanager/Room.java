package com.example.roommanager;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

public class Room {
    private final int id;
    private final Map<Integer, User> users;
    private User host;

    private boolean isHost(int userID) {
        if (this.host == null) {
            return false;
        }
        if (this.host.getID() == userID) {
            return true;
        }
        return false;
    }

    private boolean hasUserID(int userID) {
        if (this.users.containsKey(userID)) {
            return true;
        }
        return false;
    }

    public Room() {
        this.id = 0;
        this.host = null;
        this.users = new HashMap<>();
    }

    public void addUser(User user) {
        int id = user.getID();
        if (this.users.containsKey(id)) {
            throw new IllegalArgumentException(
                    "There is already a user with the id " + id);
        }
        this.users.put(id, user);
    }

    public void setHost(User user) {
        User member = this.users.get(user.getID());

        if (member == null) {
            throw new IllegalArgumentException(
                    "User is not a member of this room");
        }

        this.host = member;
    }

    public int getID() {
        return this.id;
    }

    public User getHost() {
        return this.host;
    }

    public void removeUser(int userID) {
        if (this.hasUserID(userID)) {
            this.users.remove(userID);
            if (this.isHost(userID)) {
                if (this.users.isEmpty()) {
                    this.host = null;
                } else {
                    // set a new host, just a random member in the room
                    for (User user : this.users.values()) {
                        if (userID != user.getID()) {
                            this.host = user;
                            break;
                        }
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException(
                "There is no room with id " + id);
    }

    public User getUser(int userID) {
        if (this.hasUserID(userID)) {
            return this.users.get(userID);
        }
        throw new IllegalArgumentException(
                "There is already a user with the id " + id);
    }

    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(this.users.values());
    }

    public boolean isEmpty() {
        return this.users.isEmpty();
    }
}
