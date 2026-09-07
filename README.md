# Simple HTTP Server

A simple HTTP server built from scratch **without Maven, Gradle, or Spring** — it compiles with plain `javac`; external dependencies (Jackson, JUnit, SLF4J/Logback) are kept as jars in `lib/`.

It is intended as a hands-on learning project for understanding how an HTTP server really works: the server is written by hand on a raw `ServerSocket`, with a hand-written bounded thread pool instead of a thread per connection — no JDK `HttpServer` anywhere.

## Modules

| Module | Package | Status |
|---|---|---|
| Web Server | `com.example.web` | Working: accept loop, hand-written thread pool, config loading, HTTP parsing, response building, static file serving (with correct MIME types), request-body parsing |
| REST API | `com.example.api` + `com.example.web.utils` | Working: Router (method + path + path variables), JSON CRUD todo endpoints backed by an in-memory store |
| JSON | `com.example.json` | Thin wrapper around Jackson (parse / stringify), used for config loading and the API responses |

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
│   │   │       ├── api.html            # todos REST API page
│   │   │       ├── roadmap.html        # project roadmap page
│   │   │       ├── css/style.css       # shared stylesheet (incl. nav)
│   │   │       └── images/landscape.svg
│   │   └── example/
│   │       ├── api/
│   │       │   ├── Todo.java           # todo POJO (id, title, completed)
│   │       │   └── TodoStore.java      # in-memory store (ConcurrentHashMap + AtomicLong ids)
│   │       ├── json/
│   │       │   └── Json.java           # Jackson ObjectMapper wrapper (parse/stringify)
│   │       └── web/
│   │           ├── HTTPServer.java     # entry point: loads config, opens ServerSocket, registers API routes
│   │           ├── RequestHandler.java # accept loop, submits workers to the thread pool
│   │           ├── configuration/      # config.json loading (ConfigurationManager, Configuration, HTTPConfigurationException)
│   │           ├── http/
│   │           │   ├── HTTPMessage.java            # shared message base (body + header map)
│   │           │   ├── HTTPRequest.java            # parsed request data
│   │           │   ├── HTTPResponse.java           # built response (status line, headers, body)
│   │           │   ├── HTTPParser.java             # request-line + header + body parsing
│   │           │   ├── HTTPMethod.java             # enum GET / HEAD / POST / PUT / DELETE
│   │           │   ├── HTTPVersion.java            # enum + compatibility resolution
│   │           │   ├── HTTPStatusCode.java         # status code enum
│   │           │   ├── HTTPWorker.java             # per-connection worker (parses, routes, writes)
│   │           │   └── exceptions: HTTPParsingException, BadHTTPHeaderException, BadHTTPVersionException
│   │           └── utils/              # routing, static file serving, thread pool
│   │               ├── Router.java                 # dispatch (method, path) -> endpoint, captures {id}
│   │               ├── Route.java                  # one registered (method, path pattern, Endpoint)
│   │               ├── Endpoint.java               # handler contract: handle(variables, json)
│   │               ├── APIHandler.java             # /api/* endpoints echoed by the Router
│   │               ├── WebRootHandler.java         # serves files safely from webRoot + MIME types
│   │               ├── MyExecutorService.java      # hand-written bounded thread pool (start/addJob/stop)
│   │               └── BadRootPathException.java
├── test/
│   └── com/
│       └── example/
│           ├── api/
│           │   ├── TodoTest.java
│           │   └── TodoStoreTest.java
│           └── web/
│               ├── http/
│               │   ├── HTTPParserTest.java
│               │   ├── HTTPHeaderTest.java
│               │   ├── HTTPVersionTest.java
│               │   ├── HTTPResponseTest.java
│               │   └── HTTPWorkerTest.java
│               └── utils/
│                   ├── WebRootHandlerTest.java
│                   ├── RouterTest.java
│                   ├── APIHandlerTest.java
│                   └── MyExecutorServiceTest.java
├── lib/                                # Jackson, JUnit, SLF4J/Logback jars
└── out/                                # compiled .class files (git-ignored)
```

## JSON

`com.example.json.Json` wraps Jackson's `ObjectMapper` with simple `parse` / `stringify` helpers, used by `ConfigurationManager` to load `config.json`.

## Web Server

The server is written by hand on raw sockets — no framework, not even `com.sun.net.httpserver`:

- `HTTPServer` (entry point) loads `src/com/resources/config.json` via `ConfigurationManager`/`Json`, registers the `/api/*` todo routes on a shared `Router`, then opens a `ServerSocket` on the configured port.
- `RequestHandler.run()` starts a bounded thread pool (`MyExecutorService`) and, for each accepted socket, submits an `HTTPWorker` to the pool — no thread is spawned per connection.
- `HTTPWorker` parses the request with `HTTPParser`, routes `/api/*` targets through the `Router` (static targets to `WebRootHandler`), builds an `HTTPResponse` (status line, default headers, body), and writes it back over the socket.

### Concurrency

Accepting many requests at once is what motivated the thread pool: a burst of connections should not spawn unbounded threads. `MyExecutorService` is a hand-written pool — a `LinkedBlockingQueue<Runnable>` plus N worker threads, started with `start()` and drained on `stop()`. Each `HTTPWorker` runs on a pool thread, and the API's `TodoStore` is a `ConcurrentHashMap` whose ids come from an `AtomicLong`, so parallel `POST`s can never hand out the same id (same idea behind Spring's thread-per-request + @GeneratedValue sequences).

### How a request is handled

1. `HTTPParser` reads the request line (`GET / HTTP/1.1`) and headers, populating an `HTTPRequest` (header *names* keep their original casing with case-insensitive lookups; *values* keep their original case).
2. For non-`GET` methods the request body is read from the socket, bounded by `Content-Length` (up to `HTTPRequest.MAX_BODY_LENGTH` = 1024 bytes; missing/invalid `Content-Length` → `400`, oversized bodies → `413`).
3. On success the status is set to `200`; on a parse error it is set from the thrown `HTTPParsingException`.
4. `HTTPWorker` hands the request to the `Router`: targets under `/api/` are matched by method and path (with `{id}` variables captured), any JSON body is parsed, and the matching `APIHandler` endpoint is invoked; any other target goes straight to `WebRootHandler.readFile()`, which loads the requested file from `webRoot`, resolving real paths so `..` segments and symlinks cannot escape (guards against path traversal). Unmatched `/api/` paths get a `404`; a missing file gets a `404 Not Found` page.
5. `HTTPResponse` serializes: status line → headers → blank line → body. `HTTPWorker` attaches default headers:
   - `Content-Type` set to `application/json` for API responses, otherwise derived from the file extension via `WebRootHandler.getContentType()` (falls back to `text/html` for error responses)
   - `Content-Length` computed from the body
   - `Connection: close`
6. Non-`200` responses get a simple HTML error body.

The **static site** in `src/com/resources/web` (a small article-style multi-page site with shared navigation) exercises the server end to end, serving HTML, CSS, and SVG.

### REST API

Requests whose target begins with `/api/` never touch the file system: the `Router` matches the request's method and path against registered patterns, captures `{id}` path variables, parses any JSON body, and dispatches to an `APIHandler` endpoint. Todos live in a shared, thread-safe `TodoStore` (`ConcurrentHashMap` with `AtomicLong` ids), seeded with three todos at startup so ids `1`–`3` exist.

| Method | Path | Success |
|---|---|---|
| `GET` | `/api/todos` | `200` — array of todos |
| `GET` | `/api/todos/{id}` | `200` — one todo, or `404` |
| `POST` | `/api/todos` | `201` — created todo from body `{"title":...}` |
| `PUT` | `/api/todos/{id}` | `200` — partial update from body `{"title"...,"completed":...}`, or `404` |
| `DELETE` | `/api/todos/{id}` | `200` — removes the todo |

Malformed JSON or a missing required field returns `400`; unknown ids and unmatched paths return `404`. Because the parser reads a body for every non-GET method, `DELETE` clients must send `Content-Length: 0`. Try it with curl (non-GET bodies are read only when declared):

```bash
curl http://localhost:8080/api/todos
curl http://localhost:8080/api/todos/1
curl -X POST http://localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":"learn http"}'
curl -X PUT http://localhost:8080/api/todos/1 -H 'Content-Type: application/json' -d '{"title":"done","completed":true}'
curl -X DELETE http://localhost:8080/api/todos/1 -H 'Content-Length: 0'
```

The full learning path — request bodies, routing, JSON APIs, concurrency, then a browser frontend — lives in `STUDY_PLAN.md`.

Roadmap: routing, the JSON REST API, and the thread pool are done; next comes a browser frontend, keep-alive connections, and persistent storage.

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

> Note: `HTTPWorkerTest` spins up real socket pairs against a temp web root, so the full suite exercises the server wire end to end. `MyExecutorServiceTest` and the concurrency cases in `TodoStoreTest` cover pool lifecycle/parallel behavior and unique ids under load.

## Requirements

- JDK (check with `java --version` and `javac --version`)
- Jars in `lib/`: `jackson-core`, `jackson-databind`, `jackson-annotations`, `junit-platform-console-standalone`, `slf4j-api`, `logback-classic`, `logback-core`
- A text editor or IDE and a terminal
- `curl` and any modern browser for testing the server

Maven and Spring Boot will be introduced only after the underlying concepts are understood by hand.
