# Task List: Payment Session Implementation Plan

This checklist outlines the implementation phases for the payment session creation feature, following the Hexagonal Architecture guidelines.

---

## Phase 1: Domain Layer Foundation
Establish the core business rules and types with zero framework dependencies.

- [x] **1.1. Create Domain Models & Exceptions**
  * Create `CardType` enum (`NEW_CARD`, `SAVED_CARD`, `AUTOPAY_CARD`) under `domain/model`.
  * Create `CardDetails` record (Value Object) under `domain/model`.
  * Create `PaymentSession` record (Entity) under `domain/model`.
  * Create `CardNotFoundException` and `PaymentSessionFailedException` under `domain/exception`.
- [x] **1.2. Write Domain Unit Tests (in `src/test/java`)**
  * Test domain models comparison/equality checks.
  * Verify that domain exceptions hold appropriate messages and nested root cause properties.

---

## Phase 2: Application Layer (Ports & Orchestration)
Build the core orchestrator and the input/output boundaries.

- [x] **2.1. Define Inbound & Outbound Ports**
  * Create `CreatePaymentSessionUseCase` interface (Inbound Port) under `application/port/in`.
  * Create `CardClientPort` and `PaymentClientPort` interfaces (Outbound Ports) under `application/port/out`.
- [x] **2.2. Implement Inbound Command DTO**
  * Create `CreatePaymentSessionCommand` record under `application/port/in`.
  * Implement conditional self-validating rules inside the record's constructor (check `cardId` presence for saved cards and `inlineCardDetails` for new cards).
- [x] **2.3. Implement Core Use Case Service**
  * Create `PaymentSessionApplicationService` under `application/service` implementing `CreatePaymentSessionUseCase`.
  * Implement branching lookup logic based on `CardType`, then call the payment client port.
- [x] **2.4. Write Application Unit Tests (in `src/test/java`)**
  * Write Mockito unit tests for `PaymentSessionApplicationService`.
  * Verify that if `cardType` is `SAVED_CARD` or `AUTOPAY_CARD`, `CardClientPort` is invoked exactly once.
  * Verify that if `cardType` is `NEW_CARD`, `CardClientPort` is bypassed and inline details are passed directly.
  * Assert validation triggers when invalid commands are passed.

---

## Phase 3: Outbound Adapters (Driven Infrastructure)
Connect the application to external systems and map exceptions.

- [x] **3.1. Implement Cards API Client**
  * Create `CardsServiceAdapter` under `adapter/out/client` implementing `CardClientPort`.
  * Consume the external cards endpoints via Spring `WebClient`.
  * Catch `WebClientResponseException.NotFound` and map/rethrow it as `CardNotFoundException`.
- [x] **3.2. Implement Payments API Client**
  * Create `PaymentsServiceAdapter` under `adapter/out/client` implementing `PaymentClientPort`.
  * Catch network failures or gateway errors and map/rethrow them as `PaymentSessionFailedException`.
- [x] **3.3. Write Outbound Adapter Tests (in `src/testIntegration/java`)**
  * Implement integration tests utilizing **WireMock** to verify client request/response JSON mappings and correct exception translation behaviors.
- [x] **3.4. Create Local WireMock Mock Mappings (for bootRun)**
  * Create `wiremock/mappings/get-card-200.json` containing successful saved card json details.
  * Create `wiremock/mappings/post-session-201.json` returning dynamic payment session keys.
  * Configure local Spring Profile to point HTTP web clients to the WireMock port `8085` during boot.

---

## Phase 4: Inbound Adapters (Driving Entry Points)
Configure HTTP endpoints and response translators.

- [x] **4.1. Define OpenAPI Specifications**
  * Store `payment-session-api.yaml` under `src/main/resources/contracts/` representing the service contract.
  * Store `external-cards-api.yaml` and `external-payments-api.yaml` representing downstream mocks.
- [x] **4.2. Configure OpenAPI Generator Gradle Task & IDE Source Mapping**
  * Apply `org.openapi.generator` and the `idea` plugin in `build.gradle`.
  * Point task to `$projectDir/src/main/resources/contracts/payment-session-api.yaml`, setting `interfaceOnly: "true"`, and adding the generated directory path to both `sourceSets.main.java.srcDirs` and `idea.module.generatedSourceDirs`.
  * Run code generation (`./gradlew openApiGenerate`) to produce the `CreateSessionApi` interface and request/response DTOs.
- [x] **4.3. Implement REST Controller**
  * Create `PaymentSessionController` under `adapter/in/web` implementing the generated `CreateSessionApi` interface.
  * Implement mapping layer translating generated DTOs into core `CreatePaymentSessionCommand` entities.
- [x] **4.4. Implement Global REST Exception Advice**
  * Create/update `GlobalExceptionHandler` under `adapter/in/web`.
  * Add `@ExceptionHandler` mappings for `CardNotFoundException` ($\rightarrow$ `404 Not Found`) and `PaymentSessionFailedException` ($\rightarrow$ `502 Bad Gateway`).
- [x] **4.5. Write Driving Slice Tests (in `src/testIntegration/java`)**
  * Implement controller unit tests using `@WebMvcTest` with mocked `CreatePaymentSessionUseCase`.
  * Verify request validations, payload deserialization, and HTTP exception status code maps.

---

## Phase 5: Dependency Configuration & Wiring
Assemble the application and execute automated architectural checks.

- [x] **5.1. Implement Bean Configuration**
  * Create `PaymentSessionConfig` under `config` declaring `@Bean` instantiations for `CreatePaymentSessionUseCase` using pure constructor injections.
- [x] **5.2. ArchUnit Boundary Tests (in `src/testArchitecture/java`)**
  * Add `HexagonalArchitectureTest` to `src/testArchitecture/java`.
  * Execute standard `onionArchitecture()` rules to assert package dependency compliance.

---

## Phase 6: Resilience & Transient Fault Tolerance
Add protection against transient network spikes and downstream API hiccups.

- [x] **6.1. Configure Spring Retry or WebClient Retry**
  * Add `org.springframework.retry:spring-retry` or configure WebClient’s reactive `.retryWhen()` helper block.
  * Define backoff rules (exponential backoff with max 3 attempts) for transient errors (e.g. `503 Service Unavailable`, connect timeouts).
- [x] **6.2. Define Retryable vs. Unretryable Faults**
  * Verify that logic does *not* retry syntax validation errors or client-side issues (e.g., `400 Bad Request`, `404 Not Found`).
- [x] **6.3. Write Fault Integration Tests (in `src/testIntegration/java`)**
  * Write WireMock tests that return consecutive transient failures (e.g., two 503s followed by one 200 Success) to verify that the retry mechanism recovers correctly.
  * Verify that if max attempts are exceeded, `PaymentSessionFailedException` is thrown.
