package com.learn.restapi.controller;

import com.learn.restapi.model.Post;
import com.learn.restapi.model.PostWithComments;
import com.learn.restapi.service.ReactivePatternService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reactive Programming patterns controller.
 *
 * <p>Base URL: {@code /api/reactive}
 *
 * <p>Each endpoint demonstrates exactly ONE reactive pattern — no circuit breakers,
 * no resilience4j. Pure Project Reactor + WebClient.
 *
 * <table border="1">
 *   <caption>Endpoint → Pattern mapping</caption>
 *   <tr><th>Endpoint</th><th>Pattern</th></tr>
 *   <tr><td>GET /posts</td><td>Basic Flux (HTTP stream)</td></tr>
 *   <tr><td>GET /posts/{id}</td><td>Mono + timeout + onStatus</td></tr>
 *   <tr><td>GET /posts/user/{userId}</td><td>retryWhen (backoff + jitter)</td></tr>
 *   <tr><td>GET /posts/{id}/with-comments</td><td>Mono.zip (concurrent)</td></tr>
 *   <tr><td>POST /posts/batch</td><td>flatMap with concurrency cap</td></tr>
 *   <tr><td>GET /blocking</td><td>fromCallable + subscribeOn(customExecutor)</td></tr>
 *   <tr><td>GET /blocking/bounded-elastic</td><td>fromCallable + subscribeOn(boundedElastic)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/reactive")
@Tag(
    name        = "Reactive Patterns",
    description = "Pure reactive programming with WebClient + Project Reactor. " +
                  "No circuit breakers — each endpoint shows exactly one pattern. " +
                  "External API: **https://jsonplaceholder.typicode.com**"
)
public class ReactivePatternController {

    private final ReactivePatternService service;

    public ReactivePatternController(ReactivePatternService service) {
        this.service = service;
    }

    // ── Pattern 1: Basic Flux ───────────────────────────────────────────────

    @Operation(
        summary     = "PATTERN 1 — Basic Flux: stream of posts",
        description = """
            **`webClient.get().retrieve().bodyToFlux(T)`**
            
            Performs a non-blocking GET and streams the JSON array as individual `Post` objects.
            The Netty event-loop thread is released immediately after the request is sent.
            Data is pushed downstream as it arrives from the network.
            
            Operators: `bodyToFlux`, `take(10)`, `onErrorResume`
            """
    )
    @ApiResponses(@ApiResponse(
        responseCode = "200", description = "Stream of posts",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                           array = @ArraySchema(schema = @Schema(implementation = Post.class)))
    ))
    @GetMapping("/posts")
    public Flux<Post> getAllPosts() {
        return service.getAllPosts();
    }

    // ── Pattern 2: Mono + timeout + onStatus ───────────────────────────────

    @Operation(
        summary     = "PATTERN 2 — Mono + timeout + onStatus error mapping",
        description = """
            **`bodyToMono(T).timeout(Duration).onErrorResume(...)`**
            
            Fetches a single item and adds:
            - `onStatus` — maps HTTP 404/5xx to typed exceptions before body is read
            - `timeout(5s)` — aborts with `TimeoutException` if no response arrives
            - `onErrorResume` — converts any error to an empty `Mono` (returns HTTP 404)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post found"),
        @ApiResponse(responseCode = "404", description = "Post not found or call timed out")
    })
    @GetMapping("/posts/{id}")
    public Mono<ResponseEntity<Post>> getPostById(
            @Parameter(description = "Post ID (1–100)", example = "1") @PathVariable int id) {
        return service.getPostById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ── Pattern 3: Retry with backoff ──────────────────────────────────────

    @Operation(
        summary     = "PATTERN 3 — retryWhen: exponential backoff + jitter",
        description = """
            **`retryWhen(Retry.backoff(3, 500ms).jitter(0.5))`**
            
            On transient failure, automatically retries up to 3 times:
            - Attempt 1 fails → wait ~500ms (±50% jitter)
            - Attempt 2 fails → wait ~1s (±50% jitter)
            - Attempt 3 fails → wait ~2s (±50% jitter)
            - All exhausted → `RuntimeException`
            
            Jitter prevents "thundering herd" when many clients retry simultaneously.
            HTTP 404 is NOT retried (filtered out).
            """
    )
    @GetMapping("/posts/user/{userId}")
    public Flux<Post> getPostsByUser(
            @Parameter(description = "User ID (1–10)", example = "1") @PathVariable int userId) {
        return service.getPostsByUser(userId);
    }

    // ── Pattern 4: Mono.zip (concurrent fetch) ─────────────────────────────

    @Operation(
        summary     = "PATTERN 4 — Mono.zip: concurrent composition",
        description = """
            **`Mono.zip(monoA, monoB).map(tuple -> combine(...))`**
            
            Makes two HTTP calls **simultaneously**:
            - `GET /posts/{id}` (post details)
            - `GET /posts/{id}/comments` (post comments)
            
            Both run at the same time. Total latency = max(t_post, t_comments),  
            NOT t_post + t_comments.
            
            Compare with sequential `flatMap` which would add latencies together.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post with comments",
                     content = @Content(schema = @Schema(implementation = PostWithComments.class))),
        @ApiResponse(responseCode = "404", description = "Not found or timed out")
    })
    @GetMapping("/posts/{id}/with-comments")
    public Mono<ResponseEntity<PostWithComments>> getPostWithComments(
            @Parameter(description = "Post ID (1–100)", example = "3") @PathVariable int id) {
        return service.getPostWithComments(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ── Pattern 5: Parallel fan-out with flatMap ────────────────────────────

    @Operation(
        summary     = "PATTERN 5 — flatMap: parallel fan-out with concurrency cap",
        description = """
            **`Flux.fromIterable(ids).flatMap(id -> fetch(id), concurrency=5)`**
            
            Fetches multiple posts by ID, all concurrently, but capped at 5 in-flight requests.
            - `flatMap` subscribes to inner Monos in parallel (unlike `concatMap`)
            - The concurrency arg acts as a semaphore
            - Failed IDs are silently skipped (`onErrorResume → Mono.empty()`)
            
            **Try with:** `[1, 5, 10, 20, 50, 99]`
            """
    )
    @PostMapping("/posts/batch")
    public Flux<Post> getPostsByIds(
            @Parameter(description = "List of post IDs", required = true)
            @RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Flux.empty();
        return service.getPostsByIds(ids);
    }

    // ── Pattern 6: Blocking interop — custom executor ──────────────────────

    @Operation(
        summary     = "PATTERN 6 — Blocking interop: Mono.fromCallable + custom ExecutorService",
        description = """
            **`Mono.fromCallable(() -> blockingFn()).subscribeOn(Schedulers.fromExecutor(pool))`**
            
            Safely integrates a blocking/legacy call without stalling the Netty event loop:
            1. `Mono.fromCallable` wraps the blocking lambda (not called yet)
            2. `subscribeOn(pool)` routes subscription to the custom thread pool
            3. Blocking code runs on `blocking-exec-*` thread — Netty is untouched
            
            The response includes the thread name so you can verify the correct pool was used.
            """
    )
    @GetMapping("/blocking")
    public Mono<Map<String, Object>> blockingWithCustomExecutor(
            @Parameter(description = "Simulated blocking delay (ms)", example = "500")
            @RequestParam(defaultValue = "500") long delayMs) {
        return service.fetchFromBlockingSource(delayMs);
    }

    // ── Pattern 7: Blocking interop — boundedElastic ──────────────────────

    @Operation(
        summary     = "PATTERN 7 — Blocking interop: Schedulers.boundedElastic()",
        description = """
            **`subscribeOn(Schedulers.boundedElastic())`** — Reactor's built-in elastic pool.
            
            | | Custom ExecutorService | boundedElastic |
            |---|---|---|
            | Thread count | You control | 10 × CPU cores (auto) |
            | Thread naming | Custom | `boundedElastic-N` |
            | Metrics | Your pool | Reactor internal |
            | Setup | A few lines | Zero config |
            
            Use `boundedElastic` for simplicity, custom executor for fine-grained control.
            """
    )
    @GetMapping("/blocking/bounded-elastic")
    public Mono<Map<String, Object>> blockingWithBoundedElastic() {
        return service.fetchFromBlockingSourceBoundedElastic();
    }
}
