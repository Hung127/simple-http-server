# Java Learning Project

A simple Java project built from scratch **without Maven, Gradle, Spring, or any other build tool** — everything compiles with plain `javac`.

It is intended as a hands-on learning project for understanding **Java fundamentals**: classes, packages, enums, collections, constructors, encapsulation, and manual compilation. The next milestone is turning the Room Manager into a web backend built with only the JDK.

## Modules

| Module | Package | Status |
|---|---|---|
| Task Manager | `com.example.taskmanager` | Console app, complete |
| Room Manager | `com.example.roommanager` | Domain model complete → web backend in progress |

## Project Structure

```text
basic-watch/
├── src/
│   └── com/example/
│       ├── taskmanager/
│       │   ├── Main.java              # entry point, manual testing
│       │   ├── TaskManager.java       # manages tasks
│       │   └── task/
│       │       ├── Task.java          # id, name, completed, priority
│       │       └── TaskPriority.java  # enum LOW / MEDIUM / HIGH
│       │
│       └── roommanager/
│           ├── RoomManager.java       # registry of rooms
│           ├── Room.java              # members + host management
│           └── User.java              # participant record
│
├── web/                               # (planned) static frontend
└── out/                               # compiled .class files
```

## Task Manager

A console application for managing tasks.

- `Task` — data holder: ID, name, completion status, priority (`TaskPriority` enum: `LOW(1)`, `MEDIUM(2)`, `HIGH(3)`).
- `TaskManager` — stores tasks in `Map<Integer, Task>` (`HashMap`) with O(1) lookup by ID; supports add, search by ID/name, list, complete/uncomplete, remove.
- `Main` — entry point used to exercise the manager manually.

## Room Manager

A model of chat-room-style membership:

- `User` — participant with an immutable ID and a name.
- `Room` — holds its members in `Map<Integer, User>`, enforces unique member IDs, requires the host to be a member, and automatically reassigns the host when they leave.
- `RoomManager` — top-level registry of rooms (`Map<Integer, Room>`); routes join/leave actions to the right room.

### Next step: web backend

The room manager is being turned into a simple web application using only the JDK's built-in `com.sun.net.httpserver` — no frameworks. The roadmap: HTTP basics → first server → static files → REST API (JSON) → simple browser frontend. The domain classes stay framework-free; a new `web/` package will handle HTTP on top of them.

## Compilation & Running

No build system is used intentionally — the workflow is:

```text
src/*.java → javac → out/*.class → java → JVM
```

Compile from the project root:

```bash
javac -d out $(find src -name "*.java")
```

Run the task manager:

```bash
java -cp out com.example.taskmanager.Main
```

Clean:

```bash
rm -rf out && mkdir out
```

## Requirements

- JDK (check with `java --version` and `javac --version`)
- A text editor or IDE
- A terminal
- For the web phase: `curl` and any modern browser

Maven and Spring Boot will be introduced only after the underlying concepts are understood by hand.
