# Simple HTTP Server

A simple HTTP server built from scratch **without Maven, Gradle, or Spring** — it compiles with plain `javac`; external dependencies (Jackson, JUnit, SLF4J/Logback) are kept as jars in `lib/`.

It is intended as a hands-on learning project for understanding how an HTTP server really works: the server is written by hand on a raw `ServerSocket`, one thread per connection, instead of using the JDK's built-in `HttpServer`.

## Modules

| Module | Package | Status |
|---|---|---|
| Web Server | `com.example.web` | In progress: accept loop, thread-per-connection, config loading, HTTP parsing, static file serving |
| JSON | `com.example.json` | Thin wrapper around Jackson (parse / stringify), used for config loading |

## Project Structure

```text
basic-watch/
├── src/
│   ├── com/
│   │   ├── resources/
│   │   │   ├── config.json             # runtime config (port, webRoot)
│   │   │   └── web/
│   │   │       └── index.html          # served web root (static files)
│   │   └── example/
│   │       ├── json/
│   │       │   └── Json.java           # Jackson ObjectMapper wrapper (parse/stringify)
│   │       └── web/
│   │           ├── HTTPServer.java     # entry point: loads config, opens ServerSocket
│   │           ├── RequestHandler.java # accept loop, one thread per connection
│   │           ├── configuration/      # config.json loading (ConfigurationManager, Configuration, HTTPConfigurationException)
│   │           ├── http/               # HTTP parsing
│   │           │   ├── HTTPParser.java             # request-line + header parsing
│   │           │   ├── HTTPRequest.java            # parsed request data
│   │           │   ├── HTTPMessage.java            # shared message base
│   │           │   ├── HTTPMethod.java             # enum GET / POST / ...
│   │           │   ├── HTTPVersion.java            # enum + compatibility resolution
│   │           │   ├── HTTPStatusCode.java         # status code enum
│   │           │   ├── HTTPWorker.java             # per-connection response thread
│   │           │   └── exceptions: HTTPParsingException, BadHTTPHeaderException, BadHTTPVersionException
│   │           └── utils/              # static file serving
│   │               ├── WebRootHandler.java         # serves files safely from webRoot
│   │               └── BadRootPathException.java
├── test/
│   └── com/
│       └── example/
│           └── web/
│               ├── http/
│               │   ├── HTTPParserTest.java
│               │   ├── HTTPHeaderTest.java
│               │   └── HTTPVersionTest.java
│               └── utils/
│                   └── WebRootHandlerTest.java
├── lib/                                # Jackson, JUnit, SLF4J/Logback jars
└── out/                                # compiled .class files
```

## JSON

`com.example.json.Json` wraps Jackson's `ObjectMapper` with simple `parse` / `stringify` helpers, used by `ConfigurationManager` to load `config.json`.

## Web Server (in progress)

The server is written by hand on raw sockets — no framework, not even `com.sun.net.httpserver`:

- `HTTPServer` (entry point) loads `src/com/resources/config.json` via `ConfigurationManager`/`Json`, then opens a `ServerSocket` on the configured port.
- `RequestHandler.run()` accepts connections in a loop (stopping after a few, as a learning demo) and hands each socket to an `HTTPWorker` on its own thread.
- `HTTPWorker` reads the client's raw bytes and replies with a hard-coded HTML page.

So far the **HTTP parsing** layer (`com.example.web.http`: `HTTPParser`, `HTTPRequest`, `HTTPMethod`, `HTTPVersion`, `HTTPStatusCode`, exceptions) and **static file serving** (`com.example.web.utils.WebRootHandler`, which resolves real paths so symlinks/traversal cannot escape `webRoot`) are built and unit-tested, but not yet wired into `HTTPWorker`.

Roadmap: wire `HTTPParser` + `WebRootHandler` into `HTTPWorker` → routing → REST API → simple browser frontend.

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
