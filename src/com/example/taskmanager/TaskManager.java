package com.example.taskmanager;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import com.example.taskmanager.task.Task;

public class TaskManager {
    private Map<Integer, Task> tasks;

    private boolean hasTask(int taskID) {
        return this.tasks.containsKey(taskID);
    }

    public TaskManager() {
        this.tasks = new HashMap<>();
    }

    public Task search(int taskID) {
        Task result;
        if (this.hasTask(taskID)) {
            result = this.tasks.get(taskID);
            return result;
        }
        return null;
    }

    public boolean removeTask(int taskID) {
        if (this.hasTask(taskID)) {
            this.tasks.remove(taskID);
            return true;
        }
        return false;
    }

    public List<Task> search(String taskName) {
        taskName = taskName.toLowerCase();
        ArrayList<Task> result = new ArrayList<>();
        for (Task task : this.tasks.values()) {
            if (task.getName().indexOf(taskName) != -1) {
                result.add(task);
            }
        }
        return result;
    }

    public List<Task> listTasks() {
        ArrayList<Task> result = new ArrayList<>();
        for (Task task : this.tasks.values()) {
            result.add(task);
        }
        return result;
    }

    public boolean addTask(Task t) {
        if (this.tasks.containsKey(t.getID())) {
            return false;
        }
        this.tasks.put(t.getID(), t);
        return true;
    }

    public boolean completeTask(int taskID) {
        if (hasTask(taskID) && !this.tasks.get(taskID).getCompleteStatus()) {
            this.tasks.get(taskID).setCompleteStatus(true);
            return true;
        }
        return false;
    }

    public boolean uncompleteTask(int taskID) {
        if (hasTask(taskID) && this.tasks.get(taskID).getCompleteStatus()) {
            this.tasks.get(taskID).setCompleteStatus(false);
            return true;
        }
        return false;
    }

    public void saveTasks() {
        System.out.println("Saved");
    }
}
