# Simple HTTP Server

A simple HTTP server built from scratch **without Maven, Gradle, or Spring** — it compiles with plain `javac`; external dependencies (Jackson, JUnit, SLF4J/Logback) are kept as jars in `lib/`.

It is intended as a hands-on learning project for understanding how an HTTP server really works: the server is written by hand on a raw `ServerSocket`, one thread per connection, instead of using the JDK's built-in `HttpServer`.

## Modules

| Module | Package | Status |
|---|---|---|
| Web Server | `com.example.web` | Working: accept loop, thread-per-connection, config loading, HTTP parsing, response building, static file serving (with correct MIME types) |
| JSON | `com.example.json` | Thin wrapper around Jackson (parse / stringify), used for config loading |

## Project Structure

```text
simple-http-server/
├── src/
│   ├── com/
│   │   ├── resources/
│   │   │   ├── config.json             # runtime config (port, webRoot)
│   │   │   └── web/                    # served web root (static files)
│   │   │       ├── index.html          # article landing page
│   │   │       ├── methods.html        # HTTP methods page
│   │   │       ├── status-codes.html   # status codes page
│   │   │       ├── roadmap.html        # project roadmap page
│   │   │       ├── css/style.css       # shared stylesheet (incl. nav)
│   │   │       └── images/landscape.svg
│   │   └── example/
│   │       ├── json/
│   │       │   └── Json.java           # Jackson ObjectMapper wrapper (parse/stringify)
│   │       └── web/
│   │           ├── HTTPServer.java     # entry point: loads config, opens ServerSocket
│   │           ├── RequestHandler.java # accept loop, one thread per connection
│   │           ├── configuration/      # config.json loading (ConfigurationManager, Configuration, HTTPConfigurationException)
│   │           ├── http/
│   │           │   ├── HTTPMessage.java            # shared message base
│   │           │   ├── HTTPRequest.java            # parsed request data
│   │           │   ├── HTTPResponse.java           # built response (status line, headers, body)
│   │           │   ├── HTTPParser.java             # request-line + header parsing
│   │           │   ├── HTTPMethod.java             # enum GET / HEAD / POST
│   │           │   ├── HTTPVersion.java            # enum + compatibility resolution
│   │           │   ├── HTTPStatusCode.java         # status code enum
│   │           │   ├── HTTPWorker.java             # per-connection response thread
│   │           │   └── exceptions: HTTPParsingException, BadHTTPHeaderException, BadHTTPVersionException
│   │           └── utils/              # static file serving
│   │               ├── WebRootHandler.java         # serves files safely from webRoot + MIME types
│   │               └── BadRootPathException.java
├── test/
│   └── com/
│       └── example/
│           └── web/
│               ├── http/
│               │   ├── HTTPParserTest.java
│               │   ├── HTTPHeaderTest.java
│               │   ├── HTTPVersionTest.java
│               │   ├── HTTPResponseTest.java
│               │   └── HTTPWorkerTest.java
│               └── utils/
│                   └── WebRootHandlerTest.java
├── lib/                                # Jackson, JUnit, SLF4J/Logback jars
└── out/                                # compiled .class files (git-ignored)
```

## JSON

`com.example.json.Json` wraps Jackson's `ObjectMapper` with simple `parse` / `stringify` helpers, used by `ConfigurationManager` to load `config.json`.

## Web Server

The server is written by hand on raw sockets — no framework, not even `com.sun.net.httpserver`:

- `HTTPServer` (entry point) loads `src/com/resources/config.json` via `ConfigurationManager`/`Json`, then opens a `ServerSocket` on the configured port.
- `RequestHandler.run()` accepts connections in a loop and hands each socket to an `HTTPWorker` on its own thread.
- `HTTPWorker` parses the request with `HTTPParser`, builds an `HTTPResponse` (status line, default headers, body), and writes it back over the socket.

### How a request is handled

1. `HTTPParser` reads the request line (`GET / HTTP/1.1`) and headers, populating an `HTTPRequest` (header *names* are lowercased, *values* keep their original case).
2. For non-`GET` methods the request body is read from the socket, bounded by `Content-Length` (up to `HTTPRequest.MAX_BODY_LENGTH` = 1024 bytes; missing/invalid `Content-Length` → `400`, oversized bodies → `413`).
3. On success the status is set to `200`; on a parse error it is set from the thrown `HTTPParsingException`.
4. `WebRootHandler.readFile()` loads the requested file from `webRoot`, resolving real paths so `..` segments and symlinks cannot escape (guards against path traversal). A missing file gets a `404 Not Found` page; `POST` requests are answered with a placeholder `Hello` body.
5. `HTTPResponse` serializes: status line → headers → blank line → body. `HTTPWorker` attaches default headers:
   - `Content-Type` derived from the file extension via `WebRootHandler.getContentType()` (falls back to `text/html` for error responses)
   - `Content-Length` computed from the body
   - `Connection: close`
6. Non-`200` responses get a simple HTML error body.

The **static site** in `src/com/resources/web` (a small article-style multi-page site with shared navigation) exercises the server end to end, serving HTML, CSS, and SVG.

Roadmap: routing / `PUT` & `DELETE` → JSON REST API → simple browser frontend → thread pooling / connection keep-alive.

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

The server keeps serving until stopped (Ctrl+C closes the accept loop).

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
  --select-class com.example.web.http.HTTPResponseTest
```

> Note: `HTTPWorkerTest` spins up real socket pairs against a temp web root, so the full suite exercises the server wire end to end.

## Requirements

- JDK (check with `java --version` and `javac --version`)
- Jars in `lib/`: `jackson-core`, `jackson-databind`, `jackson-annotations`, `junit-platform-console-standalone`, `slf4j-api`, `logback-classic`, `logback-core`
- A text editor or IDE and a terminal
- `curl` and any modern browser for testing the server

Maven and Spring Boot will be introduced only after the underlying concepts are understood by hand.
