# Java Learning Project

A simple Java project built from scratch **without Maven, Gradle, or Spring** — it compiles with plain `javac`; external dependencies (Jackson, JUnit, SLF4J/Logback) are kept as jars in `lib/`.

It is intended as a hands-on learning project for understanding **Java fundamentals** (classes, packages, enums, collections, constructors, encapsulation, manual compilation) and how an HTTP server really works: the web server is written by hand on a raw `ServerSocket`, one thread per connection, instead of using the JDK's built-in `HttpServer`.

## Modules

| Module | Package | Status |
|---|---|---|
| Task Manager | `com.example.taskmanager` | Console app, complete |
| Room Manager | `com.example.roommanager` | Domain model complete → will back the REST API |
| Web Server | `com.example.web` | In progress: accept loop, thread-per-connection, config loading |
| JSON | `com.example.json` | Thin wrapper around Jackson (parse / stringify) |

## Project Structure

```text
basic-watch/
├── src/
│   └── com/
│       └── example/
│           ├── json/
│           │   └── Json.java           # Jackson ObjectMapper wrapper (parse/stringify)
│           ├── roommanager/
│           │   ├── RoomManager.java    # registry of rooms
│           │   ├── Room.java           # members + host management
│           │   └── User.java           # participant record
│           ├── taskmanager/
│           │   ├── Main.java           # entry point, manual testing
│           │   ├── TaskManager.java    # manages tasks
│           │   └── task/
│           │       ├── Task.java       # id, name, completed, priority
│           │       └── TaskPriority.java  # enum LOW / MEDIUM / HIGH
│           └── web/
│               ├── Main.java           # placeholder stub (unused)
│               ├── HTTPServer.java     # entry point: loads config, opens ServerSocket
│               ├── RequestHandler.java # accept loop, one thread per connection
│               ├── configuration/      # config.json loading (ConfigurationManager)
│               └── http/
│                   ├── HTTPParser.java
│                   └── HTTPWorker.java
├── test/
│   └── com/
│       └── example/
│           └── web/
│               └── http/
│                   └── HTTPParserTest.java
├── lib/                                # Jackson, JUnit, SLF4J/Logback jars
├── web/                                # (planned) static frontend
└── out/                                # compiled .class files
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

## Web Server (in progress)

The server is written by hand on raw sockets — no framework, not even `com.sun.net.httpserver`:

- `HTTPServer` (entry point) loads `src/com/resources/config.json` via `ConfigurationManager`/`Json`, then opens a `ServerSocket` on the configured port.
- `RequestHandler.begin()` accepts connections in a loop and hands each socket to an `HTTPSender` runnable on its own thread.
- `HTTPSender` reads the request head line-by-line (a blank line ends the headers) and replies with a hard-coded HTML page.

Roadmap: request parsing → routing → static files from `webRoot` → REST API backed by `roommanager` → simple browser frontend.

## Compilation & Running

No build system is used intentionally — the workflow is:

```text
lib/*.jar + src/*.java + test/*.java → javac → out/*.class → java → JVM
```

Run all commands from the project root. Dependencies live in `lib/`, so every `javac`/`java` call needs `-cp 'lib/*'` (plus `out` when running):

```bash
# compile source + tests
javac -d out -cp 'lib/*' $(find src test -name "*.java")

# clean
rm -rf out && mkdir out
```

Run the task manager:

```bash
java -cp 'out:lib/*' com.example.taskmanager.Main
```

Run the web server (must be launched from the repo root — it loads `src/com/resources/config.json` by relative path):

```bash
java -cp 'out:lib/*' com.example.web.HTTPServer

# from another terminal:
curl -v http://localhost:8080
```

Note: `RequestHandler` currently stops accepting after a handful of connections (learning demo), so restart the server once the accept loop exits.

### Testing

Tests use JUnit 6 (via `junit-platform-console-standalone` in `lib/`). To run:

```bash
# compile source + tests
javac -d out -cp 'lib/*' $(find src test -name "*.java")

# run all tests
java -jar lib/junit-platform-console-standalone-6.1.3.jar execute \
  --class-path "out:$(ls lib/*.jar | tr '\n' ':')" \
  --scan-class-path out
```

To run a specific test class:

```bash
java -jar lib/junit-platform-console-standalone-6.1.3.jar execute \
  --class-path "out:$(ls lib/*.jar | tr '\n' ':')" \
  --select-class com.example.web.http.HTTPParserTest
```

## Requirements

- JDK (check with `java --version` and `javac --version`)
- Jars in `lib/`: `jackson-core`, `jackson-databind`, `jackson-annotations`, `junit-platform-console-standalone`, `slf4j-api`, `logback-classic`, `logback-core`
- A text editor or IDE and a terminal
- `curl` and any modern browser for testing the server

Maven and Spring Boot will be introduced only after the underlying concepts are understood by hand.
