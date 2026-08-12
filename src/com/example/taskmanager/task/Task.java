package com.example.taskmanager.task;

public class Task {
    private int id;
    private boolean completed;
    private String name;
    private TaskPriority priority;

    public Task() {
        this.id = 0;
        this.completed = false;
        this.name = "Default";
        this.priority = TaskPriority.LOW;
    }

    public Task(int id, String name, TaskPriority priority, boolean completed) {
        this.id = id;
        this.completed = completed;
        this.name = name;
        this.priority = priority;
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean getCompleteStatus() {
        return this.completed;
    }

    public void setCompleteStatus(boolean value) {
        this.completed = value;
    }

    public TaskPriority getPriority() {
        return this.priority;
    }

    public void setPriority(TaskPriority value) {
        this.priority = value;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", completed=" + completed +
                '}';
    }
}
