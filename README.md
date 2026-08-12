# Task Manager

A simple Java task management application built from scratch without Maven, Gradle, Spring, or other build tools.

The project is intended as a learning project for understanding **Java fundamentals**, including classes, packages, enums, collections, constructors, encapsulation, and manual compilation.

## Project Goals

This project is mainly for learning how Java applications are structured and executed.

It currently focuses on:

- Java classes and objects
- Packages and imports
- Constructors
- Access modifiers
- Encapsulation
- Interfaces and implementations
- `Map` and `HashMap`
- `ArrayList`
- Java `enum`
- Manual compilation with `javac`
- Running Java programs with `java`
- Basic object management

No external framework or build system is used.

---

## Project Structure

```text
task-manager/
├── src/
│   └── com/
│       └── example/
│           └── taskmanager/
│               ├── Main.java
│               ├── TaskManager.java
│               │
│               └── task/
│                   ├── Task.java
│                   └── TaskPriority.java
│
└── out/
```

### `Main`

Entry point of the application.

Used to manually test the functionality of the task manager.

### `TaskManager`

Responsible for managing tasks.

Main responsibilities:

- Add tasks
- Search tasks by ID
- Search tasks by name
- Remove tasks
- List all tasks
- Complete tasks
- Uncomplete tasks

### `Task`

Represents an individual task.

A task currently contains:

- ID
- Name
- Completion status
- Priority

### `TaskPriority`

An enum representing task priority:

```text
LOW
MEDIUM
HIGH
```

Each priority has an associated numeric value:

```text
LOW    → 1
MEDIUM → 2
HIGH   → 3
```

---

## Task Storage

Tasks are stored using:

```java
Map<Integer, Task>
```

with:

```java
HashMap<Integer, Task>
```

as the implementation.

The task ID is used as the map key:

```text
ID → Task
```

For example:

```text
1 → Learn Java
2 → Learn HashMap
3 → Build Task Manager
```

This allows tasks to be retrieved directly by ID:

```java
Task task = tasks.get(2);
```

Average-case lookup using `HashMap` is `O(1)`.

---

## Example

Creating a task:

```java
Task task = new Task(
    1,
    "Learn Java",
    TaskPriority.HIGH,
    false
);
```

Adding it to the manager:

```java
manager.addTask(task);
```

Searching by ID:

```java
Task result = manager.search(1);
```

Completing a task:

```java
manager.completeTask(1);
```

Removing a task:

```java
manager.removeTask(1);
```

Searching by name:

```java
List<Task> results = manager.search("java");
```

---

## Compilation

This project intentionally does not use Maven or Gradle.

Compile the project manually using `javac`.

From the project root:

```bash
javac -d out $(find src -name "*.java")
```

### What does this do?

```text
src/
 ↓
javac
 ↓
out/
```

The `-d out` option tells Java to put compiled `.class` files inside the `out` directory.

Because the project uses packages, the compiler creates the corresponding directory structure automatically.

For example:

```text
src/com/example/taskmanager/TaskManager.java
```

becomes:

```text
out/com/example/taskmanager/TaskManager.class
```

---

## Running

After compilation:

```bash
java -cp out com.example.taskmanager.Main
```

The `-cp` option specifies the **classpath**.

Here:

```text
out/
```

is the root of the compiled package hierarchy.

The fully qualified name of the main class is:

```text
com.example.taskmanager.Main
```

---

## Packages

The project uses packages to organize related classes.

For example:

```java
package com.example.taskmanager;
```

and:

```java
package com.example.taskmanager.task;
```

The package structure corresponds to the directory structure:

```text
com.example.taskmanager
        ↓
com/example/taskmanager/

com.example.taskmanager.task
        ↓
com/example/taskmanager/task/
```

Classes from another package can be imported:

```java
import com.example.taskmanager.task.Task;
import com.example.taskmanager.task.TaskPriority;
```

A class or enum that needs to be accessed from another package must be declared `public`.

For example:

```java
public enum TaskPriority {
    LOW(1),
    MEDIUM(2),
    HIGH(3);
}
```

---

## Current Features

### Add Task

Adds a task using its ID as the key.

```java
manager.addTask(task);
```

Duplicate IDs are rejected.

```text
ID 1 → Task A

Attempt to add another task with ID 1
        ↓
Rejected
```

### Search by ID

```java
manager.search(1);
```

Uses the `HashMap` key for direct lookup.

### Search by Name

```java
manager.search("java");
```

Returns tasks whose names contain the search string.

### List Tasks

```java
manager.listTasks();
```

Returns all tasks currently stored by the manager.

### Complete Task

```java
manager.completeTask(1);
```

Changes:

```text
completed = false
```

to:

```text
completed = true
```

### Uncomplete Task

```java
manager.uncompleteTask(1);
```

Changes:

```text
completed = true
```

to:

```text
completed = false
```

### Remove Task

```java
manager.removeTask(1);
```

Removes the task from the `HashMap`.

---

## Design

The current design separates the responsibility of representing a task from managing tasks.

```text
Task
 │
 ├── id
 ├── name
 ├── priority
 └── completed


TaskManager
 │
 ├── Map<Integer, Task>
 │
 ├── addTask()
 ├── search()
 ├── removeTask()
 ├── listTasks()
 ├── completeTask()
 └── uncompleteTask()
```

The `Task` class represents data and behavior belonging to an individual task.

The `TaskManager` manages the collection of tasks.

---

## Why `Map<Integer, Task>`?

A `Map` represents a relationship between a key and a value:

```text
Map<Key, Value>
```

In this project:

```text
Map<Integer, Task>
     │       │
     │       └── Task
     └────────── ID
```

This is preferable to a `Set<Task>` because the main lookup requirement is:

```text
Task ID → Task
```

A `HashMap` provides efficient average-case lookup by key.

---

## Learning Approach

The project is intentionally being developed without a build system.

The current workflow is:

```text
Write .java files
      ↓
javac
      ↓
.class files
      ↓
java
      ↓
JVM
```

The goal is to understand what Java build tools such as Maven eventually automate.

Future topics may include:

- File persistence
- Exception handling
- Interfaces
- Abstract classes
- Generics
- Java Collections
- Streams
- Unit testing
- JAR files
- Classpath management
- Manual dependency management
- Maven
- Eventually Spring/Spring Boot

Maven and Spring will be introduced only after the underlying Java concepts are understood.

---

## Requirements

- Java Development Kit (JDK)
- A text editor or IDE
- Terminal

Check the installation with:

```bash
java --version
javac --version
```

No Maven or Gradle is currently required.

---

## Running the Project

Compile:

```bash
javac -d out $(find src -name "*.java")
```

Run:

```bash
java -cp out com.example.taskmanager.Main
```

Clean compiled files:

```bash
rm -rf out
```

Then compile again:

```bash
mkdir out
javac -d out $(find src -name "*.java")
```
