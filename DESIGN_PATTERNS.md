# 🏗️ Design Patterns & Spring Boot Realizations Guide (`DESIGN_PATTERNS.md`)

A comprehensive guide covering key architectural and Gang of Four (GoF) design patterns: **API Gateway**, **Saga Pattern**, **Factory Pattern**, **Singleton Pattern**, and **Builder Pattern**, along with their descriptions, Mermaid flowcharts, and exact Spring Boot equivalents.

---

## 🏛️ 1. Architectural Patterns

### 🌐 1.1 API Gateway Pattern

#### 📌 Description & Purpose
The **API Gateway Pattern** acts as a single entry point for all client requests in a microservices architecture. It abstracts backend service complexities by handling cross-cutting concerns such as request routing, SSL termination, authentication/authorization, rate limiting, and response aggregation.

#### 🔄 Architecture Flowchart
```mermaid
flowchart TD
    Client["Client / Mobile / Web"]

    subgraph GatewayLayer["API Gateway Layer"]
        Gateway["API Gateway (Spring Cloud Gateway)"]
        Auth["Authentication & Security Filter"]
        RateLimiter["Rate Limiter & Circuit Breaker"]
        
        Gateway --> Auth --> RateLimiter
    end

    subgraph Microservices["Backend Microservices"]
        ProductService["Product Microservice (Port 8081)"]
        OrderService["Order Microservice (Port 8082)"]
        UserService["User Microservice (Port 8083)"]
    end

    Client -->|HTTP Request /api/v1/*| Gateway
    RateLimiter -->|Route /api/products| ProductService
    RateLimiter -->|Route /api/orders| OrderService
    RateLimiter -->|Route /api/users| UserService
```

#### 🍃 Spring Boot Equivalent & Features
- **Spring Cloud Gateway:** Provides non-blocking reactive routing built on Spring WebFlux (`RouteLocator`, `GatewayFilter`).
- **Spring Security + OAuth2 Resource Server:** Handled at Gateway level for JWT verification.
- **Resilience4j Gateway Filters:** Rate limiting (`RequestRateLimiter`) and Circuit Breaker routing (`CircuitBreaker`).

---

### 🔄 1.2 Saga Orchestration Pattern (Distributed Transactions)

#### 📌 Description & Purpose
In microservices, each service manages its own isolated database. Since traditional 2PC (Two-Phase Commit) transactions do not scale across service boundaries, the **Saga Orchestration Pattern** manages distributed transactions through a **Centralized Saga Orchestrator**. 

The orchestrator explicitly sends command messages to participant microservices, tracks the overall workflow state machine, and automatically triggers **compensating transactions** in reverse order if any step fails.

> [!NOTE]
> **Why Orchestration?** Unlike Choreography (where services react to events without central coordination), Orchestration centralizes control in a single orchestrator service. This prevents complex cyclic event dependencies, simplifies workflow auditing, and makes multi-step distributed rollbacks easy to trace.

#### 🔄 Architecture Flowchart (Saga Orchestration)
```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Orchestrator as Order Saga Orchestrator<br/>(Central Coordinator)
    participant OrderService as Order Service
    participant PaymentService as Payment Service
    participant InventoryService as Inventory Service

    Note over Client, InventoryService: Successful Saga Orchestration Flow
    Client->>Orchestrator: 1. Submit New Order Request
    Orchestrator->>OrderService: 2. Command: Create Order (PENDING)
    OrderService-->>Orchestrator: 3. Order Created (ID: 101)
    
    Orchestrator->>PaymentService: 4. Command: Process Payment
    
    alt Payment Succeeded
        PaymentService-->>Orchestrator: 5. Payment Success Response
        Orchestrator->>InventoryService: 6. Command: Reserve Stock
        InventoryService-->>Orchestrator: 7. Stock Reserved Response
        Orchestrator->>OrderService: 8. Command: Approve Order (COMPLETED)
        Orchestrator-->>Client: 9. HTTP 200 Order Confirmed
    else Payment Failed (Orchestrator Triggers Compensating Action)
        PaymentService-->>Orchestrator: 5b. Payment Failed Error
        Note over Orchestrator: Execute Compensating Command Flow
        Orchestrator->>OrderService: 6b. Compensating Command: Cancel Order (CANCELLED)
        OrderService-->>Orchestrator: 7b. Order Cancelled
        Orchestrator-->>Client: 8b. HTTP 400 Payment Failed / Order Cancelled
    end
```

#### 🍃 Spring Boot Equivalent & Features
- **Central State Machine (Spring StateMachine / Temporal / Axon):** Manages saga execution states (`PENDING`, `PAYMENT_COMPLETED`, `STOCK_RESERVED`, `FAILED`, `COMPENSATED`).
- **Kafka Command Topics:** Orchestrator sends targeted command messages to dedicated request topics (e.g. `payment-commands`, `inventory-commands`) via `KafkaTemplate`.
- **Spring `@KafkaListener` Command Handlers:** Participant services listen for specific command topics and return response/status events back to the Orchestrator reply topic.

---

## 🧩 2. Gang of Four (GoF) Creational Design Patterns

### 🏭 2.1 Factory Pattern (Factory Method / Abstract Factory)

#### 📌 Description & Purpose
The **Factory Pattern** provides an interface for creating objects in a superclass, but allows subclasses to alter the type of objects that will be created. It encapsulates object instantiation logic away from client code.

#### 🔄 Architecture Flowchart
```mermaid
classDiagram
    class NotificationFactory {
        +createNotification(type String) Notification
    }
    class Notification {
        <<interface>>
        +send(message String)
    }
    class EmailNotification {
        +send(message String)
    }
    class SMSNotification {
        +send(message String)
    }

    NotificationFactory ..> Notification : Instantiates
    Notification <|.. EmailNotification : Implements
    Notification <|.. SMSNotification : Implements
```

#### 🍃 Spring Boot Equivalent & Features
- **Spring `BeanFactory` / `ApplicationContext`:** Spring's IoC Container is a massive Implementation of the Abstract Factory Pattern.
- **Service Locator via Map Injection:** Spring automatically populates a `Map<String, ServiceInterface>` bean factory map:
  ```java
  @Autowired
  private Map<String, NotificationService> notificationServices;
  ```

---

### 🔒 2.2 Singleton Pattern

#### 📌 Description & Purpose
The **Singleton Pattern** ensures that a class has only one instance in the application lifetime and provides a global point of access to that instance.

#### 🔄 Architecture Flowchart
```mermaid
classDiagram
    class SingletonBean {
        -static SingletonBean instance
        -SingletonBean()
        +static getInstance() SingletonBean
    }
```

#### 🍃 Spring Boot Equivalent & Features
- **Spring Bean Default Scope (`@Scope("singleton")`)**: By default, every bean managed by the Spring IoC Container (`@Component`, `@Service`, `@Bean`) is created once per Spring `ApplicationContext`.
- **Note:** A Spring Singleton is *one instance per Spring Container context*, whereas a classic GoF Singleton is *one instance per JVM ClassLoader*.

---

### 🏗️ 2.3 Builder Pattern

#### 📌 Description & Purpose
The **Builder Pattern** separates the construction of a complex object from its representation, allowing the same construction process to create different representations step-by-step.

#### 🔄 Architecture Flowchart
```mermaid
classDiagram
    class Product {
        -String name
        -BigDecimal price
        -String category
    }
    class ProductBuilder {
        -String name
        -BigDecimal price
        -String category
        +name(String) ProductBuilder
        +price(BigDecimal) ProductBuilder
        +category(String) ProductBuilder
        +build() Product
    }
    ProductBuilder ..> Product : Builds
```

#### 🍃 Spring Boot Equivalent & Features
- **Lombok `@Builder`:** Annotates DTOs or Entities to automatically generate fluent builder methods.
- **Spring Framework Builders:**
  - `UriComponentsBuilder.fromHttpUrl(...)`
  - `TopicBuilder.name("my-topic").partitions(5).build()`
  - `ChatClient.builder().build()` (Spring AI)
  - `WebClient.builder().build()`

---

## 📊 Summary Comparison Table

| Design Pattern | Category | Core Purpose | Spring Boot Realization / Equivalent |
|---|---|---|---|
| **API Gateway** | Architectural | Unified routing, security, and rate-limiting entry point | **Spring Cloud Gateway** (`RouteLocator`) |
| **Saga Orchestration** | Architectural | Centralized command coordinator managing distributed transactions & compensations | **Spring StateMachine / Temporal / Axon** + **Spring Kafka** (`KafkaTemplate`, `@KafkaListener`) |
| **Factory Pattern** | Creational (GoF) | Encapsulates object instantiation logic | **`ApplicationContext` / `BeanFactory`** & `Map<String, Bean>` injection |
| **Singleton Pattern** | Creational (GoF) | Guarantees single instance creation | **Default Spring Bean Scope** (`@Scope("singleton")`) |
| **Builder Pattern** | Creational (GoF) | Step-by-step fluent construction of complex objects | **Lombok `@Builder`**, `UriComponentsBuilder`, `TopicBuilder` |
