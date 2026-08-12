package com.example.taskmanager.task;

public enum TaskPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int value;

    private TaskPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
