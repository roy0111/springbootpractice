package com.learn.restapi.service;

import com.learn.restapi.model.Post;
import com.learn.restapi.model.PostComment;
import com.learn.restapi.model.PostWithComments;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Traditional (blocking) circuit breaker service using:
 * <ul>
 *   <li>{@link RestTemplate} — classic blocking HTTP client (no reactive types)</li>
 *   <li>{@code @CircuitBreaker} annotation — Resilience4j AOP-based approach</li>
 * </ul>
 *
 * <h2>How the @CircuitBreaker annotation works</h2>
 * <p>Resilience4j wraps each annotated method in a Spring AOP proxy. The proxy:
 * <ol>
 *   <li>Checks if the named circuit breaker is CLOSED → lets the call through</li>
 *   <li>Records success / failure after the method returns or throws</li>
 *   <li>If failure rate ≥ threshold → opens the circuit</li>
 *   <li>When OPEN → calls the {@code fallbackMethod} directly without invoking the real method</li>
 * </ol>
 *
 * <h2>Fallback method rules</h2>
 * <pre>
 *   // Annotated method:
 *   &#64;CircuitBreaker(name = "externalApi", fallbackMethod = "getAllPostsFallback")
 *   public List&lt;Post&gt; getAllPosts() { ... }
 *
 *   // Fallback — SAME return type, SAME params + Exception at the end:
 *   private List&lt;Post&gt; getAllPostsFallback(Exception ex) { ... }
 * </pre>
 *
 * <h2>Two CB instances (configured in application.properties)</h2>
 * <ul>
 *   <li>{@code externalApi} — for JSONPlaceholder HTTP calls</li>
 *   <li>{@code blockingApi} — for slow/blocking operations</li>
 * </ul>
 */
@Service
public class CircuitBreakerService {

    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * {@code RestTemplate} for blocking HTTP calls.
     * {@code CircuitBreakerRegistry} for reading metrics in {@link #getStatus()}.
     */
    public CircuitBreakerService(RestTemplate restTemplate,
                                 CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-1 — Get all posts with @CircuitBreaker
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches all posts using a blocking {@code RestTemplate} GET call.
     *
     * <p>{@code @CircuitBreaker(name = "externalApi", fallbackMethod = "getAllPostsFallback")}
     * wraps this method in an AOP proxy. When the circuit is OPEN, the proxy skips this
     * method entirely and calls {@code getAllPostsFallback} instead.
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getAllPostsFallback")
    public List<Post> getAllPosts() {
        // getForObject makes a blocking HTTP GET and deserializes the JSON array
        Post[] posts = restTemplate.getForObject("/posts", Post[].class);
        return posts != null ? Arrays.asList(posts) : List.of();
    }

    /**
     * Fallback for {@link #getAllPosts()}.
     * Called automatically when the circuit is OPEN or when an exception is thrown.
     *
     * <p>Signature rules: same return type + same parameters + {@code Exception} at the end.
     */
    public List<Post> getAllPostsFallback(Exception ex) {
        System.err.println("[CB:externalApi] getAllPosts fallback triggered: " + ex.getMessage());
        return List.of(new Post(0, 0,
                "[FALLBACK] External API unavailable",
                "Circuit breaker active. Reason: " + ex.getClass().getSimpleName()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-2 — Get single post with @CircuitBreaker
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single post by ID with circuit breaker protection.
     *
     * <p>If the post is not found (HTTP 404), {@code RestTemplate} throws
     * {@link HttpClientErrorException.NotFound}. Since 404 is in {@code ignore-exceptions}
     * in {@code application.properties}, it does NOT count toward the failure rate.
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getPostByIdFallback")
    public Optional<Post> getPostById(int id) {
        try {
            Post post = restTemplate.getForObject("/posts/{id}", Post.class, id);
            return Optional.ofNullable(post);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();  // 404 → empty optional, not an error
        }
    }

    /**
     * Fallback for {@link #getPostById(int)}.
     * Note: fallback includes the same parameter {@code id} as the original method.
     */
    public Optional<Post> getPostByIdFallback(int id, Exception ex) {
        System.err.printf("[CB:externalApi] getPostById(%d) fallback: %s%n", id, ex.getMessage());
        return Optional.of(new Post(0, id,
                "[FALLBACK] Post " + id + " unavailable",
                "Circuit is OPEN. Retry after wait duration."));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-3 — Get posts by user with @CircuitBreaker
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches all posts for a user. If this call fails, the CB records a failure.
     * When failure rate crosses the threshold, the circuit opens and all subsequent
     * calls go straight to {@code getPostsByUserFallback}.
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getPostsByUserFallback")
    public List<Post> getPostsByUser(int userId) {
        Post[] posts = restTemplate.getForObject("/posts?userId={userId}", Post[].class, userId);
        return posts != null ? Arrays.asList(posts) : List.of();
    }

    public List<Post> getPostsByUserFallback(int userId, Exception ex) {
        System.err.printf("[CB:externalApi] getPostsByUser(%d) fallback: %s%n", userId, ex.getMessage());
        return List.of(new Post(0, 0,
                "[FALLBACK] Posts for user " + userId + " unavailable",
                "Circuit breaker is OPEN."));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-4 — Get post + comments (two blocking calls, one CB)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Makes two sequential blocking HTTP calls and combines the results.
     *
     * <p>Unlike the reactive version (which used {@code Mono.zip} for concurrency),
     * here the calls are sequential: post first, then comments.
     * Total latency = t_post + t_comments.
     *
     * <p>Both calls are inside the same {@code @CircuitBreaker} scope —
     * if either call fails, the CB records a failure.
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getPostWithCommentsFallback")
    public PostWithComments getPostWithComments(int postId) {
        // Call 1: fetch the post
        Post post = restTemplate.getForObject("/posts/{id}", Post.class, postId);

        // Call 2: fetch the comments
        PostComment[] comments = restTemplate.getForObject(
                "/posts/{id}/comments", PostComment[].class, postId);

        return new PostWithComments(post,
                comments != null ? Arrays.asList(comments) : List.of());
    }

    public PostWithComments getPostWithCommentsFallback(int postId, Exception ex) {
        System.err.printf("[CB:externalApi] getPostWithComments(%d) fallback: %s%n", postId, ex.getMessage());
        Post fallbackPost = new Post(0, postId, "[FALLBACK] Post unavailable", "Circuit is OPEN.");
        return new PostWithComments(fallbackPost, List.of());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-5 — Fetch multiple posts (sequential, one CB wraps all)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fetches multiple posts by ID sequentially (blocking, no parallelism).
     *
     * <p>Compare with the reactive version which used {@code flatMap(concurrency=5)}
     * for parallel fetching. Here, each call blocks until the previous one returns.
     *
     * <p>If ANY call in the loop fails, the exception propagates and the CB records it.
     * Failed IDs are skipped (logged and skipped via try-catch).
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getPostsByIdsFallback")
    public List<Post> getPostsByIds(List<Integer> ids) {
        return ids.stream()
                .map(id -> {
                    try {
                        return restTemplate.getForObject("/posts/{id}", Post.class, id);
                    } catch (Exception ex) {
                        System.err.println("[CB] Skipping post id=" + id + ": " + ex.getMessage());
                        return null;
                    }
                })
                .filter(post -> post != null)
                .toList();
    }

    public List<Post> getPostsByIdsFallback(List<Integer> ids, Exception ex) {
        System.err.println("[CB:externalApi] getPostsByIds fallback: " + ex.getMessage());
        return List.of(new Post(0, 0, "[FALLBACK] Batch fetch unavailable", "Circuit is OPEN."));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-6 — Slow/blocking call with a SEPARATE CB instance (blockingApi)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Simulates a slow blocking operation (e.g., a legacy JDBC or SDK call)
     * with its own dedicated {@code blockingApi} circuit breaker.
     *
     * <p>Using a <b>separate CB instance</b> for blocking vs HTTP calls
     * isolates the failure domains — a slow blocking system won't trip
     * the HTTP circuit breaker, and vice-versa.
     */
    @CircuitBreaker(name = "blockingApi", fallbackMethod = "fetchFromBlockingSourceFallback")
    public Map<String, Object> fetchFromBlockingSource(long simulatedDelayMs) throws InterruptedException {
        String thread = Thread.currentThread().getName();
        System.out.printf("[CB:blockingApi] Blocking call on thread: %s%n", thread);

        // Simulate a slow legacy operation
        Thread.sleep(simulatedDelayMs);

        return Map.of(
                "source",           "legacy-blocking-api",
                "executedOnThread", thread,
                "delayMs",          simulatedDelayMs,
                "note",             "Real blocking call — no reactive wrappers"
        );
    }

    public Map<String, Object> fetchFromBlockingSourceFallback(long simulatedDelayMs, Exception ex) {
        System.err.println("[CB:blockingApi] fetchFromBlockingSource fallback: " + ex.getMessage());
        return Map.of(
                "source",         "FALLBACK (cached response)",
                "circuitBreaker", "blockingApi is OPEN — blocking system NOT contacted",
                "reason",         ex.getClass().getSimpleName() + ": " + ex.getMessage()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CB-7 — Live status metrics for both instances
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns a live snapshot of both circuit breaker instances.
     *
     * <p>Also available at:
     * <ul>
     *   <li>{@code /actuator/health} — shows DOWN when a CB is OPEN</li>
     *   <li>{@code /actuator/circuitbreakers} — Resilience4j Actuator endpoint</li>
     * </ul>
     */
    public Map<String, Object> getStatus() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker extCB =
                circuitBreakerRegistry.circuitBreaker("externalApi");
        io.github.resilience4j.circuitbreaker.CircuitBreaker blkCB =
                circuitBreakerRegistry.circuitBreaker("blockingApi");

        return Map.of(
                "externalApi", buildMetricsMap(extCB),
                "blockingApi", buildMetricsMap(blkCB)
        );
    }

    private Map<String, Object> buildMetricsMap(
            io.github.resilience4j.circuitbreaker.CircuitBreaker cb) {
        var m = cb.getMetrics();
        return Map.of(
                "state",             cb.getState().toString(),
                "failureRate%",      m.getFailureRate(),
                "slowCallRate%",     m.getSlowCallRate(),
                "bufferedCalls",     m.getNumberOfBufferedCalls(),
                "failedCalls",       m.getNumberOfFailedCalls(),
                "successfulCalls",   m.getNumberOfSuccessfulCalls(),
                "notPermittedCalls", m.getNumberOfNotPermittedCalls()
        );
    }
}
