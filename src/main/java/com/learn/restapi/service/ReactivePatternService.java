package com.learn.restapi.service;

import com.learn.restapi.model.Post;
import com.learn.restapi.model.PostComment;
import com.learn.restapi.model.PostWithComments;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Pure reactive service — no circuit breakers, no resilience4j.
 *
 * <p>Each method teaches exactly one reactive pattern using WebClient + Project Reactor.
 * This is a clean learning reference for reactive programming with Spring WebFlux.
 *
 * <h2>Reactive patterns covered</h2>
 * <ol>
 *   <li>{@link #getAllPosts()}                      — basic {@code Flux} HTTP stream</li>
 *   <li>{@link #getPostById(int)}                   — {@code Mono} + timeout + error mapping</li>
 *   <li>{@link #getPostsByUser(int)}                — retry with exponential backoff + jitter</li>
 *   <li>{@link #getPostWithComments(int)}           — concurrent composition with {@code Mono.zip}</li>
 *   <li>{@link #getPostsByIds(List)}                — parallel fan-out via {@code flatMap(concurrency)}</li>
 *   <li>{@link #fetchFromBlockingSource(long)}      — blocking interop with custom executor</li>
 *   <li>{@link #fetchFromBlockingSourceBoundedElastic()} — blocking interop with boundedElastic</li>
 * </ol>
 */
@Service
public class ReactivePatternService {

    private final WebClient webClient;
    private final ExecutorService blockingTaskExecutor;

    public ReactivePatternService(WebClient jsonPlaceholderClient,
                                  ExecutorService blockingTaskExecutor) {
        this.webClient = jsonPlaceholderClient;
        this.blockingTaskExecutor = blockingTaskExecutor;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 1 — Basic Flux: stream of items from HTTP
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches the first 10 posts as a reactive stream.
     *
     * <p><b>Key operators:</b>
     * <ul>
     *   <li>{@code retrieve().bodyToFlux(T)} — deserialises JSON array to a Flux of objects</li>
     *   <li>{@code take(n)} — limits emission to the first n elements</li>
     *   <li>{@code onErrorResume} — graceful fallback on any failure</li>
     * </ul>
     *
     * <p>WebClient is non-blocking: the Netty thread is released after the request
     * is sent and comes back only when data arrives from the network.
     */
    public Flux<Post> getAllPosts() {
        return webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)     // stream of Post objects
                .take(10)                   // take only first 10
                .onErrorResume(ex -> {
                    System.err.println("[Reactive] getAllPosts failed: " + ex.getMessage());
                    return Flux.empty();
                });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 2 — Mono + timeout + onStatus (error mapping)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single post by ID.
     *
     * <p><b>Key operators:</b>
     * <ul>
     *   <li>{@code onStatus} — maps specific HTTP status codes to typed exceptions
     *       before the body is read</li>
     *   <li>{@code timeout(Duration)} — aborts the Mono if no item arrives in time;
     *       emits a {@code TimeoutException} downstream</li>
     *   <li>{@code onErrorResume} — transforms any error into an empty Mono
     *       (controller maps that to HTTP 404)</li>
     * </ul>
     */
    public Mono<Post> getPostById(int id) {
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        resp -> Mono.error(new RuntimeException("Post " + id + " not found")))
                .onStatus(status -> status.is5xxServerError(),
                        resp -> Mono.error(new RuntimeException("External API 5xx error")))
                .bodyToMono(Post.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(ex -> {
                    System.err.printf("[Reactive] getPostById(%d) failed: %s%n", id, ex.getMessage());
                    return Mono.empty();  // controller turns empty Mono into 404
                });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 3 — Retry with exponential backoff + jitter
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches all posts for a user, automatically retrying on transient failures.
     *
     * <p><b>Key operator — {@code retryWhen(Retry.backoff(...))}:</b>
     * <ul>
     *   <li>Retries up to 3 times</li>
     *   <li>Exponential backoff: 500 ms → 1 s → 2 s</li>
     *   <li>±50% random jitter: avoids retry storms in distributed systems</li>
     *   <li>{@code filter}: skip retry for 404 (resource doesn't exist)</li>
     *   <li>{@code onRetryExhaustedThrow}: wrap final failure into a clear exception</li>
     * </ul>
     *
     * <p>Sequence:  attempt → fail → wait(500ms±jitter) → attempt → fail
     *              → wait(1s±jitter) → attempt → fail → throw exhausted
     */
    public Flux<Post> getPostsByUser(int userId) {
        return webClient.get()
                .uri("/posts?userId={userId}", userId)
                .retrieve()
                .bodyToFlux(Post.class)
                .timeout(Duration.ofSeconds(8))
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(500))
                             .jitter(0.5)
                             .filter(ex -> !(ex instanceof WebClientResponseException.NotFound))
                             .onRetryExhaustedThrow((spec, signal) ->
                                     new RuntimeException("All retries exhausted", signal.failure()))
                )
                .onErrorResume(ex -> {
                    System.err.println("[Reactive] getPostsByUser failed: " + ex.getMessage());
                    return Flux.empty();
                });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 4 — Concurrent composition with Mono.zip
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches a post AND its comments <b>concurrently</b>, then combines them.
     *
     * <p><b>Key operator — {@code Mono.zip(monoA, monoB)}:</b>
     * <ul>
     *   <li>Subscribes to both Monos <em>at the same time</em></li>
     *   <li>Waits for both to complete, then combines via {@code .map(tuple -> ...)}</li>
     *   <li>Total latency ≈ {@code max(t_post, t_comments)} — NOT {@code t_post + t_comments}</li>
     * </ul>
     *
     * <p>If you used sequential {@code flatMap} instead, latency would be additive.
     * {@code Mono.zip} is the correct pattern for "fetch N independent things in parallel".
     */
    public Mono<PostWithComments> getPostWithComments(int postId) {
        // Both HTTP calls are built but NOT yet subscribed
        Mono<Post> postMono = webClient.get()
                .uri("/posts/{id}", postId)
                .retrieve()
                .bodyToMono(Post.class);

        Mono<List<PostComment>> commentsMono = webClient.get()
                .uri("/posts/{id}/comments", postId)
                .retrieve()
                .bodyToFlux(PostComment.class)
                .collectList();  // Flux<PostComment> → Mono<List<PostComment>>

        // zip subscribes to BOTH simultaneously
        return Mono.zip(postMono, commentsMono)
                .map(tuple -> new PostWithComments(tuple.getT1(), tuple.getT2()))
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(ex -> {
                    System.err.println("[Reactive] getPostWithComments failed: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 5 — Parallel fan-out: flatMap with concurrency control
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches multiple posts by their IDs, all in parallel, with bounded concurrency.
     *
     * <p><b>Key operator — {@code flatMap(fn, maxConcurrency)}:</b>
     * <ul>
     *   <li>{@code flatMap} subscribes to the inner Monos concurrently (unlike {@code concatMap})</li>
     *   <li>The second argument {@code 5} is the concurrency cap:
     *       at most 5 HTTP requests are in-flight at once</li>
     *   <li>Acts like a semaphore — if 5 are running, new requests queue until one finishes</li>
     *   <li>Failed IDs are silently skipped via {@code onErrorResume(Mono.empty())}</li>
     * </ul>
     *
     * <p>Compare: {@code flatMap} (parallel), {@code concatMap} (sequential), {@code mergeMap} (alias for flatMap).
     */
    public Flux<Post> getPostsByIds(List<Integer> ids) {
        return Flux.fromIterable(ids)
                .flatMap(
                        id -> webClient.get()
                                .uri("/posts/{id}", id)
                                .retrieve()
                                .bodyToMono(Post.class)
                                .onErrorResume(ex -> Mono.empty()),  // skip failed IDs
                        5   // max concurrency
                );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 6 — Blocking interop: custom ExecutorService
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Safely calls a blocking/legacy API from a reactive pipeline.
     *
     * <p><b>Problem:</b> Blocking a Netty event-loop thread stalls the whole server —
     * Netty has very few threads (≈ 2 × CPU cores) and they must never block.
     *
     * <p><b>Solution — two key operators:</b>
     * <ul>
     *   <li>{@code Mono.fromCallable(() -> blockingFn())} — wraps a blocking {@code Callable}
     *       into a Mono; the lambda is only called when subscribed</li>
     *   <li>{@code subscribeOn(Schedulers.fromExecutor(pool))} — tells Reactor which thread
     *       pool to subscribe (and therefore execute the callable) on</li>
     * </ul>
     *
     * <p>The result: blocking code runs on a dedicated thread from {@code blockingTaskExecutor},
     * the Netty thread is never touched.
     *
     * @param simulatedDelayMs simulates a slow blocking call (e.g., legacy JDBC)
     */
    public Mono<Map<String, Object>> fetchFromBlockingSource(long simulatedDelayMs) {
        return Mono.fromCallable(() -> {
                    String thread = Thread.currentThread().getName();
                    System.out.printf("[Reactive] Blocking call on thread: %s%n", thread);

                    Thread.sleep(simulatedDelayMs); // simulates a slow blocking operation

                    return Map.<String, Object>of(
                            "pattern",          "Mono.fromCallable + subscribeOn(customExecutor)",
                            "executedOnThread", thread,
                            "delayMs",          simulatedDelayMs
                    );
                })
                .subscribeOn(Schedulers.fromExecutor(blockingTaskExecutor)) // ← runs on our pool
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(ex -> Mono.just(Map.of("error", ex.getMessage())));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PATTERN 7 — Blocking interop: Schedulers.boundedElastic()
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Same blocking interop, using Reactor's built-in {@code Schedulers.boundedElastic()}.
     *
     * <p><b>Comparison:</b>
     * <table border="1">
     *   <tr><th>Approach</th><th>When to use</th></tr>
     *   <tr>
     *     <td>{@code Schedulers.fromExecutor(pool)}</td>
     *     <td>Custom thread count, named threads, metrics on the pool</td>
     *   </tr>
     *   <tr>
     *     <td>{@code Schedulers.boundedElastic()}</td>
     *     <td>Quick setup; Reactor manages the pool (capped at 10 × CPU cores)</td>
     *   </tr>
     * </table>
     */
    public Mono<Map<String, Object>> fetchFromBlockingSourceBoundedElastic() {
        return Mono.fromCallable(() -> {
                    String thread = Thread.currentThread().getName();
                    Thread.sleep(100);
                    return Map.<String, Object>of(
                            "pattern",          "Mono.fromCallable + subscribeOn(boundedElastic)",
                            "executedOnThread", thread,
                            "scheduler",        "Schedulers.boundedElastic()"
                    );
                })
                .subscribeOn(Schedulers.boundedElastic()); // ← Reactor's built-in elastic pool
    }
}
