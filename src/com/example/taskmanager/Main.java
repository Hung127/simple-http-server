package com.example.taskmanager;

import com.example.taskmanager.task.Task;
import com.example.taskmanager.task.TaskPriority;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager();

        // =========================
        // Add tasks
        // =========================

        Task task1 = new Task(
                1,
                "Learn Java",
                TaskPriority.HIGH,
                false);

        Task task2 = new Task(
                2,
                "Learn HashMap",
                TaskPriority.MEDIUM,
                false);

        Task task3 = new Task(
                3,
                "Build Task Manager",
                TaskPriority.LOW,
                false);

        System.out.println("=== ADD TASKS ===");

        System.out.println("Task 1: " + manager.addTask(task1));
        System.out.println("Task 2: " + manager.addTask(task2));
        System.out.println("Task 3: " + manager.addTask(task3));

        // =========================
        // Duplicate ID
        // =========================

        System.out.println("\n=== DUPLICATE ID ===");

        Task duplicate = new Task(
                1,
                "Another task",
                TaskPriority.LOW,
                false);

        System.out.println(
                "Adding duplicate: " +
                        manager.addTask(duplicate));

        // =========================
        // List tasks
        // =========================

        System.out.println("\n=== ALL TASKS ===");

        List<Task> tasks = manager.listTasks();

        for (Task task : tasks) {
            System.out.println(task);
        }

        // =========================
        // Search by ID
        // =========================

        System.out.println("\n=== SEARCH ID ===");

        Task found = manager.search(2);

        System.out.println("Found: " + found);

        found = manager.search(999);

        System.out.println("ID 999: " + found);

        // =========================
        // Search by name
        // =========================

        System.out.println("\n=== SEARCH NAME ===");

        List<Task> results = manager.search("java");

        for (Task task : results) {
            System.out.println(task);
        }

        // =========================
        // Complete task
        // =========================

        System.out.println("\n=== COMPLETE TASK ===");

        System.out.println(
                "Complete task 1: " +
                        manager.completeTask(1));

        System.out.println(manager.search(1));

        // =========================
        // Uncomplete task
        // =========================

        System.out.println("\n=== UNCOMPLETE TASK ===");

        System.out.println(
                "Uncomplete task 1: " +
                        manager.uncompleteTask(1));

        System.out.println(manager.search(1));

        // =========================
        // Remove task
        // =========================

        System.out.println("\n=== REMOVE TASK ===");

        System.out.println(
                "Remove task 2: " +
                        manager.removeTask(2));

        // =========================
        // Final list
        // =========================

        System.out.println("\n=== FINAL TASKS ===");

        for (Task task : manager.listTasks()) {
            System.out.println(task);
        }
    }
}
