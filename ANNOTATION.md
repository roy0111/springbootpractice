# 🏷️ Spring Boot Annotations Master Reference Guide

A complete, categorized cheat-sheet table of essential **Spring Boot**, **Spring Core**, **Spring WebFlux / MVC**, **Spring Data / R2DBC**, **Resilience4j**, and **Spring AI** annotations.

---

## 📑 1. Core Framework & Bean Management

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

## 🌐 2. Web, REST & Reactive Annotations (`WebFlux` / `MVC`)

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

## 🗄️ 3. Spring Data, R2DBC & Validation Annotations

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

## ⚡ 4. Messaging, Resiliency & Testing Annotations

| Annotation | Description & Usage |
|---|---|
| `@EnableKafka` | Enables detection of `@KafkaListener` annotations on Spring beans. |
| `@KafkaListener` | Marks a method as a listener for receiving Kafka topic messages. |
| `@Header` | Injects a specific message header (e.g. partition, offset, topic) in Kafka or WebSocket handlers. |
| `@CircuitBreaker` | Resilience4j annotation wrapping a method execution in a Circuit Breaker state machine with fallbacks. |
| `@Tag` | OpenAPI / Swagger annotation grouping endpoints under a specific tag category. |
| `@Operation` | OpenAPI / Swagger annotation documenting summary and description of an API operation. |
| `@Schema` | OpenAPI / Swagger annotation documenting model properties and example payloads. |
| `@SpringBootTest` | Spring Boot test annotation that bootstraps full application context for integration testing. |
