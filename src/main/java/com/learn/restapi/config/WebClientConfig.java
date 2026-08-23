package com.learn.restapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for reactive HTTP client (WebClient) and thread executor.
 *
 * <p><b>WebClient</b> — non-blocking, reactive HTTP client.
 * Replaces the old blocking {@code RestTemplate}.
 * Works natively with Project Reactor (Mono / Flux).
 *
 * <p><b>ExecutorService</b> — a dedicated thread pool for situations where you must
 * call a blocking/legacy API (e.g., old SDK, JDBC, gRPC sync stubs).
 * Wrap those calls with:
 * <pre>
 *   Mono.fromCallable(() -> blockingCall())
 *       .subscribeOn(Schedulers.fromExecutor(blockingTaskExecutor))
 * </pre>
 * This offloads the blocking work onto the dedicated pool, keeping the
 * Netty event-loop threads free.
 */
@Configuration
public class WebClientConfig {

    // ── WebClient ──────────────────────────────────────────────────────────

    /**
     * Preconfigured WebClient pointing at JSONPlaceholder.
     *
     * <p>Settings:
     * <ul>
     *   <li>Connect timeout: 5 s</li>
     *   <li>Read/write timeout: 10 s (via Netty handlers)</li>
     *   <li>Response timeout: 10 s (Reactor Netty level)</li>
     *   <li>Request/response logging filter (see {@link #loggingFilter()})</li>
     * </ul>
     */
    @Bean
    public WebClient jsonPlaceholderClient() {

        // Low-level Netty HTTP client with timeouts
        HttpClient nettyHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com")
                .clientConnector(new ReactorClientHttpConnector(nettyHttpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(loggingFilter())   // log every outbound request
                .build();
    }

    /**
     * Exchange filter that logs outbound request method + URL.
     * In production replace with a proper MDC-based logger.
     */
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            System.out.printf("[WebClient] --> %s %s%n",
                    request.method(), request.url());
            return Mono.just(request);
        });
    }

    // ── Thread Executor (for blocking integrations) ────────────────────────

    /**
     * A dedicated fixed-size thread pool to run blocking (non-reactive) code.
     *
     * <p>Usage pattern:
     * <pre>
     *   Mono.fromCallable(() -> legacyBlockingService.call())
     *       .subscribeOn(Schedulers.fromExecutor(blockingTaskExecutor))
     * </pre>
     *
     * <p>Alternative: use {@code Schedulers.boundedElastic()} which is Reactor's
     * built-in elastic pool designed exactly for this purpose.
     *
     * <p>Use a dedicated pool (this bean) when you want tighter control over
     * thread count, queue depth, and naming.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService blockingTaskExecutor() {
        // 4 threads — tune based on expected blocking I/O concurrency
        return Executors.newFixedThreadPool(
                4,
                r -> {
                    Thread t = new Thread(r, "blocking-exec-" + System.nanoTime());
                    t.setDaemon(true);    // don't prevent JVM shutdown
                    return t;
                }
        );
    }

    // ── RestTemplate (blocking HTTP client for CB service) ─────────────────

    /**
     * Traditional blocking HTTP client.
     *
     * <p>Used by {@link com.learn.restapi.service.CircuitBreakerService} alongside
     * the {@code @CircuitBreaker} annotation — the classic, non-reactive pattern.
     *
     * <p>RestTemplate is still supported in Spring Boot 3.x.
     * For new reactive code, prefer {@link WebClient} instead.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri("https://jsonplaceholder.typicode.com")
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }
}
