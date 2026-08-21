package com.example.roommanager;

import java.util.Map;
import java.util.HashMap;

public class RoomManager {
    private final Map<Integer, Room> rooms;

    public RoomManager() {
        this.rooms = new HashMap<>();
    }

    public void addRoom(Room room) {
        int id = room.getID();
        if (this.rooms.containsKey(id)) {
            throw new IllegalArgumentException(
                    "There is already a room with id " + id);
        }
        this.rooms.put(id, room);
    }

    public void removeRoom(int roomID) {
        if (!this.rooms.containsKey(roomID)) {
            throw new IllegalArgumentException(
                    "There is no room with id " + roomID);
        }
        this.rooms.remove(roomID);
    }

    public Room getRoom(int roomID) {
        if (hasRoomID(roomID)) {
            return this.rooms.get(roomID);
        }
        throw new IllegalArgumentException(
                "There is no room with id " + roomID);
    }

    public boolean hasRoomID(int roomID) {
        if (this.rooms.containsKey(roomID)) {
            return true;
        }
        return false;
    }

    public void joinRoom(int roomID, User user) {
        // TODO: Try catch
        Room room = this.getRoom(roomID);
        room.addUser(user);
    }

    public void leaveRoom(int roomID, int userID) {
        // TODO: Try catch
        Room room = this.getRoom(roomID);
        room.removeUser(userID);
    }
}
