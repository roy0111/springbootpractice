# 🏷️ Spring Boot Annotations & Application Properties Master Reference

A complete, categorized cheat-sheet guide covering both **Spring Boot Java Annotations** and **Configuration Properties** used in `application.properties` or `application.yml`.

---

## PART 1: SPRING BOOT ANNOTATIONS

### 📑 1. Core Framework & Bean Management

| Annotation | Description & Usage |
|---|---|
| `@SpringBootApplication` | Meta-annotation combining `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. Serves as app entry point. |
| `@Component` | Generic stereotype annotation marking a Java class as a Spring-managed bean. |
| `@Service` | Stereotype annotation marking a class in the business service layer. |
| `@Repository` | Stereotype annotation marking a DAO / Data Access component (enables automatic exception translation). |
| `@Controller` | Stereotype annotation marking a Spring MVC/WebFlux presentation layer controller. |
| `@RestController` | Combination of `@Controller` and `@ResponseBody`. Automatically serializes return values into JSON/XML. |
| `@Configuration` | Marks a class as a source of bean definitions created via `@Bean` methods. |
| `@Bean` | Marks a method in a `@Configuration` class to produce a Spring container bean. |
| `@Autowired` | Injects matching Spring bean dependencies automatically into fields, constructors, or setters. |
| `@Qualifier` | Specifies which exact bean name to inject when multiple beans of the same type exist. |
| `@Value` | Injects externalized configuration values or environment variables (e.g. `${server.port}`). |
| `@ConditionalOnProperty` | Conditionally enables a bean/configuration only if a specific property exists or equals a given value. |
| `@Lazy` | Delays bean initialization until it is first requested rather than at startup. |
| `@Scope` | Defines bean scope (e.g. `singleton`, `prototype`, `request`, `session`). |

---

### 🌐 2. Web, REST & Reactive Annotations (`WebFlux` / `MVC`)

| Annotation | Description & Usage |
|---|---|
| `@RequestMapping` | Maps HTTP request URLs to handler methods or classes. Supports method, path, headers, and params. |
| `@GetMapping` | Shortcut for `@RequestMapping(method = RequestMethod.GET)`. |
| `@PostMapping` | Shortcut for `@RequestMapping(method = RequestMethod.POST)`. |
| `@PutMapping` | Shortcut for `@RequestMapping(method = RequestMethod.PUT)`. |
| `@DeleteMapping` | Shortcut for `@RequestMapping(method = RequestMethod.DELETE)`. |
| `@PatchMapping` | Shortcut for `@RequestMapping(method = RequestMethod.PATCH)`. |
| `@PathVariable` | Binds a URI template variable (e.g. `/users/{id}`) to a method parameter. |
| `@RequestParam` | Extracts query parameters (e.g. `/users?name=John`) or form data. |
| `@RequestBody` | Binds incoming HTTP request body JSON/XML to a DTO or domain object. |
| `@ResponseBody` | Indicates that method return value should be written directly to HTTP response body. |
| `@ResponseStatus` | Specifies HTTP response status code returned by a controller method or custom exception class. |
| `@ExceptionHandler` | Marks a method to handle specific exceptions thrown by controller actions. |
| `@RestControllerAdvice` | Global advice component combining `@ControllerAdvice` and `@ResponseBody` for global exception handling. |
| `@CrossOrigin` | Enables Cross-Origin Resource Sharing (CORS) on controller classes or methods. |

---

### 🗄️ 3. Spring Data, R2DBC & Validation Annotations

| Annotation | Description & Usage |
|---|---|
| `@Table` | Specifies target relational/R2DBC database table name for an entity (e.g. `@Table("products")`). |
| `@Id` | Marks a field as the primary key of an entity. |
| `@Column` | Maps an entity field to a specific database column name. |
| `@CreatedDate` | Spring Data auditing annotation that automatically populates creation timestamp. |
| `@LastModifiedDate` | Spring Data auditing annotation that automatically updates modification timestamp. |
| `@Valid` | Triggers Bean Validation on nested objects or incoming `@RequestBody` DTOs. |
| `@NotNull` | Bean Validation rule ensuring a field is not null. |
| `@NotBlank` | Bean Validation rule ensuring a String is not null and contains at least one non-whitespace character. |
| `@Min` / `@Max` | Bean Validation rules setting numeric upper and lower bounds. |
| `@Size` | Bean Validation rule enforcing string length or collection size bounds. |

---

### ⚡ 4. Messaging, Resiliency & Testing Annotations

| Annotation | Description & Usage |
|---|---|
| `@EnableKafka` | Enables detection of `@KafkaListener` annotations on Spring beans. |
| `@KafkaListener` | Marks a method as a listener for receiving Kafka topic messages. |
| `@Header` | Injects a specific message header (e.g. partition, offset, topic) in Kafka or WebSocket handlers. |
| `@CircuitBreaker` | Resilience4j annotation wrapping a method execution in a Circuit Breaker state machine (`CLOSED`, `OPEN`, `HALF_OPEN`) with fallback execution. |
| `@Retry` | Resilience4j annotation automatically re-executing failed methods up to a configured max attempt threshold. |
| `@RateLimiter` | Resilience4j annotation enforcing rate limiting to cap incoming request executions over a specified time window. |
| `@Bulkhead` | Resilience4j annotation limiting concurrent executions to isolate resource usage (Semaphore or ThreadPool bulkhead). |
| `@TimeLimiter` | Resilience4j annotation enforcing execution timeout limits on asynchronous/reactive futures and mono/flux publishers. |
| `@Tag` | OpenAPI / Swagger annotation grouping endpoints under a specific tag category. |
| `@Operation` | OpenAPI / Swagger annotation documenting summary and description of an API operation. |
| `@Schema` | OpenAPI / Swagger annotation documenting model properties and example payloads. |
| `@SpringBootTest` | Spring Boot test annotation that bootstraps full application context for integration testing. |

---

## PART 2: APPLICATION PROPERTIES Cheat Sheet (`application.properties` / `application.yml`)

### 🐘 1. Spring Data JPA, R2DBC & Database Properties

| Property Key | Description & Typical Value |
|---|---|
| `spring.datasource.url` | JDBC Database connection URL (e.g., `jdbc:oracle:thin:@localhost:1521:xe` or `jdbc:h2:mem:testdb`). |
| `spring.datasource.username` | Database connection username (e.g., `sa`, `admin`). |
| `spring.datasource.password` | Database connection password. |
| `spring.datasource.driver-class-name` | Fully qualified JDBC driver class name (e.g. `oracle.jdbc.OracleDriver`). |
| `spring.jpa.hibernate.ddl-auto` | Hibernate DDL auto schema management (`none`, `validate`, `update`, `create-drop`). |
| `spring.jpa.show-sql` | Logs generated SQL statements to console (`true`/`false`). |
| `spring.jpa.properties.hibernate.format_sql` | Formats logged SQL queries for better readability (`true`/`false`). |
| `spring.r2dbc.url` | R2DBC reactive database URL (e.g. `r2dbc:h2:mem:///productdb`). |
| `spring.sql.init.mode` | Controls schema initialization scripts (`always`, `never`, `embedded`). |

---

### 🚀 2. Kafka Messaging Properties

| Property Key | Description & Typical Value |
|---|---|
| `spring.kafka.bootstrap-servers` | Comma-separated list of Kafka broker host:port addresses (e.g., `localhost:9092`). |
| `spring.kafka.consumer.group-id` | Default consumer group identifier (e.g. `learning-group`). |
| `spring.kafka.consumer.auto-offset-reset` | Where to start reading when no offset exists (`earliest`, `latest`). |
| `spring.kafka.consumer.key-deserializer` | Deserializer class for message keys (`StringDeserializer`). |
| `spring.kafka.consumer.value-deserializer` | Deserializer class for message values (`StringDeserializer`, `JsonDeserializer`). |
| `spring.kafka.producer.key-serializer` | Serializer class for message keys (`StringSerializer`). |
| `spring.kafka.producer.value-serializer` | Serializer class for message values (`StringSerializer`, `JsonSerializer`). |
| `spring.kafka.listener.concurrency` | Number of concurrent consumer listener threads (e.g. `5`). |

---

### 🏥 3. Spring Boot Actuator Properties

| Property Key | Description & Typical Value |
|---|---|
| `management.endpoints.web.exposure.include` | Endpoints to expose over HTTP (`*`, `health,info,metrics,circuitbreakers`). |
| `management.endpoints.web.base-path` | Custom base path for Actuator endpoints (default `/actuator`). |
| `management.endpoint.health.show-details` | Shows full health component details (`always`, `never`, `when-authorized`). |
| `management.info.env.enabled` | Enables environment properties in `/actuator/info` endpoint (`true`/`false`). |

---

### 📖 4. Swagger / OpenAPI Properties (`springdoc`)

| Property Key | Description & Typical Value |
|---|---|
| `springdoc.swagger-ui.path` | Custom path to access Swagger UI (e.g., `/swagger-ui.html`). |
| `springdoc.api-docs.path` | Custom path for OpenAPI JSON specification (e.g., `/v3/api-docs`). |
| `springdoc.swagger-ui.operationsSorter` | Sorting order for operations in Swagger UI (`method`, `alpha`). |
| `springdoc.swagger-ui.try-it-out-enabled` | Enables "Try it out" button by default (`true`/`false`). |

---

### 🤖 5. Spring AI (Anthropic Claude & VectorStore) Properties

| Property Key | Description & Typical Value |
|---|---|
| `spring.ai.anthropic.api-key` | API Key for Anthropic Claude (e.g. `${ANTHROPIC_API_KEY}`). |
| `spring.ai.anthropic.chat.options.model` | Target Claude model name (`claude-3-5-sonnet-20240620`). |
| `spring.ai.anthropic.chat.options.temperature` | Randomness / sampling temperature (e.g., `0.7`). |

---

### 🛡️ 6. Resilience4j Circuit Breaker Properties

| Property Key | Description & Typical Value |
|---|---|
| `resilience4j.circuitbreaker.configs.default.sliding-window-type` | Type of sliding window (`COUNT_BASED`, `TIME_BASED`). |
| `resilience4j.circuitbreaker.configs.default.sliding-window-size` | Number of calls recorded in sliding window (e.g. `10`). |
| `resilience4j.circuitbreaker.configs.default.failure-rate-threshold` | Failure percentage threshold to OPEN circuit (e.g. `50`). |
| `resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state` | Time circuit stays OPEN before probing HALF_OPEN (e.g. `10s`). |
| `resilience4j.circuitbreaker.configs.default.register-health-indicator` | Exposes circuit breaker state in `/actuator/health` (`true`). |

---

### 📜 7. Logging & Logging Level Properties

| Property Key | Description & Typical Value |
|---|---|
| `logging.level.root` | Root logger severity level (`INFO`, `DEBUG`, `WARN`, `ERROR`). |
| `logging.level.org.springframework` | Package-specific log level (e.g., `logging.level.org.springframework.web=DEBUG`). |
| `logging.file.name` | Log output file path (e.g. `logs/app.log`). |
| `logging.pattern.console` | Console log formatting pattern. |

---

### ☕ 8. JVM & System Properties (Environment / Spring)

| Property Key | Description & Typical Value |
|---|---|
| `spring.main.banner-mode` | Controls Spring Boot startup banner (`console`, `log`, `off`). |
| `spring.profiles.active` | Active Spring environment profiles (e.g. `dev`, `prod`, `staging`). |
| `server.port` | HTTP web server port (default `8080`). |

---

### ☸️ 9. Kubernetes & Docker Cloud Properties (`spring-cloud-kubernetes`)

| Property Key | Description & Typical Value |
|---|---|
| `spring.cloud.kubernetes.enabled` | Enables Kubernetes environment detection (`true`/`false`). |
| `spring.cloud.kubernetes.reload.enabled` | Enables auto-reloading configuration when K8s ConfigMap changes (`true`). |
| `spring.cloud.kubernetes.discovery.enabled` | Enables K8s service discovery for load balancing (`true`). |
