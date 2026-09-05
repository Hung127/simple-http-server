# Study Plan: Raw-Socket HTTP Server → Web Backend Fundamentals

A self-paced roadmap that turns your hand-written `com.example.web` HTTP server into a working
JSON web API **built on raw `ServerSocket`s** — no Maven, no Spring, no framework.

The whole point: when you later meet Spring Boot + Maven, you'll recognize exactly what each
piece is doing for you, because you built every layer by hand.

---

## Why this plan exists

You already understand Java fundamentals and you've built a working raw-socket server that serves
static files. The remaining layers to learn are:

1. **Request bodies** — reading `POST`/`PUT` payloads off the wire
2. **Routing** — mapping `(method, path)` to a handler, with path variables like `/todos/{id}`
3. **JSON APIs** — producing and consuming JSON responses (status discipline: 200/201/204/400/404/409)
4. **Concurrency** — a server handling many requests at once (thread pools)
5. **A browser frontend** — `fetch()` against your API

Learning these by hand is exactly the lens Spring Boot uses: `@RequestBody`,
`@RequestMapping`+`@PathVariable`, `@ResponseBody`, `@ResponseStatus`, thread-per-request.

## Ground rules

- **You write every line yourself.** This doc is direction, not copy-paste.
- **One phase = one commit** (or more). Small commits make debugging easier.
- When stuck: read the JDK Javadoc for the class first, then ask for a hint.
- Keep it **simple**: add only what each phase asks for. Resist gold-plating.

## Current state (Phase A)

Your server (a **different** architecture than the original plan — better, because harder):

| What | Where |
|---|---|
| Raw `ServerSocket` accept loop, thread-per-connection | `RequestHandler.java` |
| HTTP request-line + header parsing | `HTTPParser.java` |
| Response building (status line, headers, body) | `HTTPResponse.java` |
| Status-code enum | `HTTPStatusCode.java` |
| Static file serving with MIME types + path-traversal guard | `WebRootHandler.java` |
| JSON wrapper over Jackson | `Json.java` |

**Done:** parsing methods/headers, static files, correct status/error handling, tests.

**Missing:** method enum has only `GET/HEAD`; `parseRequestBody()` is an empty stub; no routing;
no `/api/*`; no domain data.

---

## Target architecture

```text
Browser                     JVM process
  │                          │
  │  GET /, /style.css       │   RequestHandler ── accept loop ── ServerSocket :8080
  │ ───────────────────────► │        │
  │                          │        ▼  (one thread per connection)
  │                          │   HTTPWorker
  │  fetch /api/...          │     ├─ HTTPParser        (request line, headers, BODY)
  │ ───────────────────────► │     ├─ Router            (method + path → handler)
  │                          │     │     ├─ ApiHandler  (/api/* → JSON)
  │                          │     │     └─ WebRootHandler (static files)
  │                          │     ├─ TodoStore + Todo  (in-memory data)
  │                          │     └─ HTTPResponse       (status + headers + body)
  └──────────────────────────┘
```

Design rule: **the web layer owns HTTP; the API layer owns data.** Keep them decoupled.

---

## Phase B — Request bodies, methods, status codes (build on your parser)

**Goal:** your server can `POST`/`PUT`/`DELETE` and read a request body.

### Learn

- **HTTP methods**: `GET` (read), `POST` (create), `PUT/PATCH` (update), `DELETE` (remove), `OPTIONS`.
- **Request body**: for non-GET requests, the body follows the blank line after headers.
  Its byte count is given by the `Content-Length` header. There's no other reliable terminator in HTTP/1.1.
- **Status codes to add**: `201 Created` (new resource with a body), `204 No Content` (delete,
  no body), `409 Conflict` (duplicate), `405 Method Not Allowed`, `413 Payload Too Large`.

### Build

1. **`HTTPMethod`** — add `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`. (`MAX_LENGTH` auto-computes.)
2. **`HTTPStatusCode`** — add `201`, `204`, `409`, `413`; fix the mislabeled
   `CLIENT_ERROR_401_METHOD_NOT_ALLOWED` → `CLIENT_ERROR_405_METHOD_NOT_ALLOWED` (405 is the
   correct code), update any references.
3. **`parseRequestBody()`** (in `HTTPParser`) — the heart of this phase:
   - After headers are parsed, if `Content-Length` > 0, read that many raw bytes as the body.
   - Enforce a **max body size** (define a constant, e.g. 1 MB). Exceed it → `413`.
   - Store the body on `HTTPRequest`; add a `getBody()` accessor.
   - Keep `HEAD`/`GET` body-less (they shouldn't have one).
4. **Tests** — mirror the existing `HTTPParserTest` style: method enum, body reading, max-body/413.

### Check yourself

- [ ] `curl -v -X POST http://localhost:8080/api/ping -d 'hello'` shows the body was read.
- [ ] A request body longer than the max returns `413`, not a crash.
- [ ] `POST`, `DELETE` no longer return `501` from the method enum.

---

## Phase C — Routing + JSON API (data model: a simple Todo)

**Goal:** `curl` can CRUD todos through JSON endpoints — mirrors Spring's `@RequestMapping`,
`@PathVariable`, `@ResponseBody`, `@ResponseStatus`.

### Learn

- **Routing**: match a request's `(method, path)` against registered patterns. Patterns may have
  a path variable: `/api/todos/{id}` → capture `id` into a value map.
  This is exactly what Spring's `@RequestMapping("/api/todos/{id}")` + `@PathVariable` do.
- **404 vs 405**: if *no* route matches → `404`. If a route matches the path but not the method → `405`.
- **Status discipline**: `POST` → `201` + the created resource; `DELETE` → `204` **no body**;
  errors get a JSON body like `{"error":"..."}`.

### Build

1. **`Todo`** — tiny POJO: `long id`, `String title`, `boolean completed`. (Equals a `@Entity`/POJO.)
2. **`TodoStore`** — `ConcurrentHashMap<Long, Todo>`. This is your in-memory "database"
   (Spring would use a repository + DB). Start with a few seeded todos so responses aren't empty.
3. **`Router`** — register `(method, pathPattern, handler)`, dispatch by method+path, extract path vars.
4. **`ApiHandler`** — the endpoints:
   - `GET    /api/todos`        → 200, array of todos
   - `GET    /api/todos/{id}`   → 200, one todo or 404
   - `POST   /api/todos`        → body `{"title":"..."}`, 201 + created todo
   - `PUT    /api/todos/{id}`   → body `{"title":...,"completed":...}`, 200 or 404
   - `DELETE /api/todos/{id}`   → 204, no body
   - Malformed JSON / bad id → `400`; wrong method on a known path → `405`.
5. **Wire into `HTTPWorker`** — after parsing: if the path starts with `/api`, hand to `ApiHandler`;
   otherwise keep serving static files via `WebRootHandler`.

Start with a single `Router` used by `ApiHandler`; extend flow as needed.

### Check yourself

- [ ] Each of the 5 endpoints works via `curl` (create → get → update → delete round trip).
- [ ] Unknown todo id → `404` JSON, no stack trace.
- [ ] `GET /api/todos/abc` → `400`, not a crash.
- [ ] Wrong method on a valid path → `405`.
- [ ] Responses are valid JSON (paste into a validator).

---

## Phase D — Concurrency (thread pooling)

**Goal:** understand why servers need more than one thread, and how a pool is bounded.

### Learn

- Right now: **thread-per-connection** — each socket spawns a fresh `Thread`, no limit.
- Problem: a burst of connections spawns unbounded threads → resource exhaustion.
- Fix: an **`ExecutorService`** with a **bounded fixed pool** (e.g. `Executors.newFixedThreadPool(4)`).
  This is precisely how Spring handles thread-per-request.
- **Shared state**: your `TodoStore` is a `ConcurrentHashMap`. Reason about whether your handlers
  mutate shared data and whether the map alone is enough (yes for atomic single-key ops,
  maybe not for read-modify-write like "next id" — reach for `AtomicLong`).

### Build

1. Replace `new Thread(worker).start()` in `RequestHandler` with an `ExecutorService`.
2. Share **one `TodoStore`** (and one `Router`/`ApiHandler`) across all workers, not a fresh one
   per request.
3. Generate todo ids with an `AtomicLong` (not `Random`/timestamps) — this is the same reason
   Spring/JPA use a sequence/counter.
4. **Try to reproduce a race**: hammer two `POST`/`DELETE`s from two terminals and confirm the
   `AtomicLong` ids are unique.

### Check yourself

- [ ] Two parallel `POST /api/todos` never produce the same id.
- [ ] Many simultaneous requests don't exhaust memory/threads.
- [ ] You can explain why a pool is better than unbounded threads.

---

## Phase E — Browser frontend (optional but rewarding)

**Goal:** a single-page UI using `fetch()` against your API.

Powering this with a Todo UI is simplest — skip the old article pages if you want.

- `fetch(url).then(r => r.json())`
- `fetch(url, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(...)})`
- Render with `document.createElement` / `textContent` (avoid `innerHTML` with user input — XSS habit).
- Poll with `setInterval(loadTodos, 2000)`.

---

## Phase F — Spring Boot mapping (the payoff)

Once Phases B–D click, this list should feel like "I built that by hand":

| Your hand-written thing | Spring Boot equivalent |
|---|---|
| `Router` (method + path + path vars) | `@RequestMapping`, `@GetMapping`, ..., `@PathVariable` |
| `parseRequestBody()` + `Content-Length` | `@RequestBody` + embedded server |
| JSON wrapping (`Json.java`) | Jackson (auto-configured) |
| `HTTPStatusCode` discipline (201/204/404/...) | `ResponseEntity` / `@ResponseStatus` |
| `TodoStore` (in-memory, shared) | Repository + DB |
| `ExecutorService` pool | embedded server's thread pool |
| `AtomicLong` ids | JPA `@GeneratedValue` sequence |

After this you'll have earned Maven + Spring Boot: same endpoints, a fraction of the code — and
you'll know exactly what it's doing for you.

---

## Status codes quick reference

`200` read ok · `201` created · `204` deleted (no body) · `400` bad input · `404` missing ·
`405` method not allowed · `409` conflict · `413` too large · `500` server bug

## Curl reference

```bash
curl -v http://localhost:8080/api/todos
curl -v http://localhost:8080/api/todos/1
curl -v -X POST http://localhost:8080/api/todos -H 'Content-Type: application/json' -d '{"title":"learn http"}'
curl -v -X PUT http://localhost:8080/api/todos/1 -H 'Content-Type: application/json' -d '{"title":"done","completed":true}'
curl -v -X DELETE http://localhost:8080/api/todos/1
```

## Build & run (existing workflow)

```bash
javac -d out -cp 'lib/*' $(find src test -name "*.java")
java -cp 'out:lib/*' com.example.web.HTTPServer

# tests
java -jar lib/junit-platform-console-standalone-6.1.3.jar execute \
  --class-path "out:$(ls lib/*.jar | tr '\n' ':')" --scan-class-path out
```

## Milestone checklist

- [ ] Phase B: methods + body parsing + 413 → `POST`/`DELETE` accepted
- [ ] Phase C: `/api/todos` CRUD via curl with 200/201/204/400/404/405
- [ ] Phase D: executor pool + unique `AtomicLong` ids under parallel load
- [ ] Phase E: a browser UI hitting the API (optional)
- [ ] Phase F: I can name the Spring equivalent of each piece I built
