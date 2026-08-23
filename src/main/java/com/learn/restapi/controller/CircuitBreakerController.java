package com.learn.restapi.controller;

import com.learn.restapi.model.Post;
import com.learn.restapi.model.PostWithComments;
import com.learn.restapi.service.CircuitBreakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Circuit Breaker controller — traditional blocking REST API style.
 *
 * <p>Base URL: {@code /api/cb}
 *
 * <p><b>No reactive types here.</b> Every method returns a plain {@code ResponseEntity<T>}.
 * The circuit breaker is handled entirely by the {@code @CircuitBreaker} annotation
 * on the service layer — this controller just calls the service like any normal REST controller.
 *
 * <h2>Circuit Breaker state machine</h2>
 * <pre>
 *   CLOSED ──(failures ≥ threshold)──▶  OPEN  ──(waitDuration)──▶  HALF_OPEN
 *   HALF_OPEN ──(probes pass)─────────▶ CLOSED
 *   HALF_OPEN ──(probes fail)─────────▶ OPEN
 * </pre>
 *
 * <h2>Key difference from reactive approach</h2>
 * <table border="1">
 *   <caption>Reactive vs Annotation-based CB</caption>
 *   <tr><th></th><th>Reactive (operator)</th><th>Annotation (this controller)</th></tr>
 *   <tr><td>HTTP Client</td><td>WebClient (non-blocking)</td><td>RestTemplate (blocking)</td></tr>
 *   <tr><td>CB applied via</td><td>{@code transformDeferred(CB)}</td><td>{@code @CircuitBreaker}</td></tr>
 *   <tr><td>Return type</td><td>{@code Mono<T>} / {@code Flux<T>}</td><td>{@code T} / {@code ResponseEntity<T>}</td></tr>
 *   <tr><td>Fallback</td><td>{@code onErrorResume()} in pipeline</td><td>Separate fallback method</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/cb")
@Tag(
    name        = "Circuit Breaker Patterns",
    description = "Traditional blocking REST API with Resilience4j `@CircuitBreaker` annotation. " +
                  "Uses `RestTemplate` (not WebClient). No reactive types — pure Spring MVC style. " +
                  "Monitor at `/api/cb/status`, `/actuator/health`, `/actuator/circuitbreakers`."
)
public class CircuitBreakerController {

    private final CircuitBreakerService service;

    public CircuitBreakerController(CircuitBreakerService service) {
        this.service = service;
    }

    // ── CB-1: Get all posts ────────────────────────────────────────────────

    @Operation(
        summary     = "CB-1 — Get all posts with @CircuitBreaker",
        description = """
            **`@CircuitBreaker(name = "externalApi", fallbackMethod = "getAllPostsFallback")`**
            
            Resilience4j AOP wraps this service call. When the circuit is:
            - **CLOSED** → `RestTemplate` calls JSONPlaceholder, result returned normally
            - **OPEN** → `getAllPostsFallback()` is called directly, no HTTP request made
            - **HALF_OPEN** → a probe call is made; success → CLOSED, failure → OPEN
            
            To trip the circuit: call this endpoint 3+ times while the external API is unreachable.
            """
    )
    @ApiResponses(@ApiResponse(
        responseCode = "200", description = "Posts (or fallback list if CB is OPEN)",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                           array = @ArraySchema(schema = @Schema(implementation = Post.class)))
    ))
    @GetMapping("/posts")
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(service.getAllPosts());
    }

    // ── CB-2: Get single post ──────────────────────────────────────────────

    @Operation(
        summary     = "CB-2 — Get post by ID with @CircuitBreaker",
        description = """
            **`@CircuitBreaker(name = "externalApi", fallbackMethod = "getPostByIdFallback")`**
            
            - HTTP 404 (post not found) → returns `404 Not Found` to caller
              → does NOT trip the circuit (listed in `ignore-exceptions`)
            - Network error / timeout → fallback post returned, failure recorded in CB
            - CB OPEN → fallback returned immediately without calling the API
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post found or fallback"),
        @ApiResponse(responseCode = "404", description = "Post does not exist (not a CB issue)")
    })
    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPostById(
            @Parameter(description = "Post ID (1–100)", example = "1") @PathVariable int id) {
        return service.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── CB-3: Get posts by user ────────────────────────────────────────────

    @Operation(
        summary     = "CB-3 — Get posts by user with @CircuitBreaker",
        description = """
            **`@CircuitBreaker(name = "externalApi", fallbackMethod = "getPostsByUserFallback")`**
            
            Fetches all posts for a user. Each failed call records a failure in the
            `externalApi` circuit breaker. When the failure rate crosses the threshold,
            subsequent calls — even to other `/api/cb/` endpoints using the same CB — will
            short-circuit and return their respective fallbacks.
            """
    )
    @GetMapping("/posts/user/{userId}")
    public ResponseEntity<List<Post>> getPostsByUser(
            @Parameter(description = "User ID (1–10)", example = "1") @PathVariable int userId) {
        return ResponseEntity.ok(service.getPostsByUser(userId));
    }

    // ── CB-4: Get post with comments ───────────────────────────────────────

    @Operation(
        summary     = "CB-4 — Get post + comments with @CircuitBreaker (two sequential calls)",
        description = """
            **Two blocking `RestTemplate` calls inside one `@CircuitBreaker` scope.**
            
            Note: unlike the reactive version (which used `Mono.zip` for concurrency),
            these are **sequential** — post first, then comments.
            
            Total latency = t_post + t_comments (additive, not concurrent).
            
            If EITHER call fails, the CB records a failure and the fallback is returned.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post with comments (or fallback)",
                     content = @Content(schema = @Schema(implementation = PostWithComments.class))),
        @ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/posts/{id}/with-comments")
    public ResponseEntity<PostWithComments> getPostWithComments(
            @Parameter(description = "Post ID (1–100)", example = "3") @PathVariable int id) {
        PostWithComments result = service.getPostWithComments(id);
        if (result.getPost() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    // ── CB-5: Batch fetch ──────────────────────────────────────────────────

    @Operation(
        summary     = "CB-5 — Batch fetch with @CircuitBreaker",
        description = """
            **Sequential blocking fetch of multiple post IDs, wrapped in one CB.**
            
            - Each post is fetched one at a time (blocking loop)
            - Individual 404s are skipped silently
            - If a systemic error occurs (network down), the whole batch fails → CB records it
            - When CB is OPEN → fallback list returned immediately
            
            **Try with:** `[1, 5, 10, 20, 50]`
            """
    )
    @PostMapping("/posts/batch")
    public ResponseEntity<List<Post>> getPostsByIds(
            @Parameter(description = "List of post IDs", required = true)
            @RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(service.getPostsByIds(ids));
    }

    // ── CB-6: Blocking call with separate CB instance ─────────────────────

    @Operation(
        summary     = "CB-6 — Blocking call with separate 'blockingApi' circuit breaker",
        description = """
            **`@CircuitBreaker(name = "blockingApi", fallbackMethod = "...")`**
            
            Uses a **different** CB instance from all the HTTP endpoints above.
            
            Why? Fault isolation:
            - If the legacy blocking system is flaky → `blockingApi` CB opens
            - `externalApi` CB is unaffected and continues working normally
            - Each system gets its own independent failure window
            
            When `blockingApi` is OPEN:
            - The `Thread.sleep(...)` is NEVER called — the fallback fires immediately
            - This also protects the thread pool from being exhausted by slow calls
            
            `?delayMs=500` — set the simulated blocking delay in milliseconds.
            """
    )
    @GetMapping("/blocking")
    public ResponseEntity<Map<String, Object>> blockingCall(
            @Parameter(description = "Simulated blocking delay (ms)", example = "500")
            @RequestParam(defaultValue = "500") long delayMs) {
        try {
            return ResponseEntity.ok(service.fetchFromBlockingSource(delayMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Thread interrupted"));
        }
    }

    // ── CB-7: Live status ──────────────────────────────────────────────────

    @Operation(
        summary     = "CB-7 — Live circuit breaker status (both instances)",
        description = """
            Returns real-time metrics for both CB instances:
            
            | Field | Meaning |
            |---|---|
            | `state` | `CLOSED` / `OPEN` / `HALF_OPEN` |
            | `failureRate%` | % of sliding-window calls that failed |
            | `slowCallRate%` | % of calls slower than `slowCallDurationThreshold` |
            | `bufferedCalls` | Total calls recorded in window |
            | `failedCalls` | Calls ended in error |
            | `successfulCalls` | Calls completed OK |
            | `notPermittedCalls` | Calls rejected while OPEN |
            
            Same data at `/actuator/health` (OPEN → status: DOWN) and `/actuator/circuitbreakers`.
            """
    )
    @ApiResponse(responseCode = "200", description = "CB metrics snapshot")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(service.getStatus());
    }
}
