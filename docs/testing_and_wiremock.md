# Testing with WireMock & Downstream Simulation

This document records the design decisions and architectural guidelines for simulating external downstream services using **WireMock** in both automated integration tests and local boot runs.

---

## 1. Mocking Downstream APIs
Our hexagonal application interacts with two downstream microservices:
1. **Cards Service**: Checked during session validation to load expiration dates, CVVs, and verification status.
2. **Payments Service Gateway**: Invoked to generate dynamic tokenized payment session references.

Rather than calling real APIs, these boundaries are simulated using WireMock in two different ways:

### A. Local Boot Run Stubs (`/wiremock`)
For local manual verification (e.g., when running `./gradlew bootRun`), a lightweight WireMock server can be spun up locally using Docker Compose to return static payloads.
* Stub configurations are saved as JSON mappings under `wiremock/mappings/`:
  - `get-card-200.json`: Stubs `/cards/{card_id}` to return standard valid JSON representations.
  - `post-session-201.json`: Stubs POST `/sessions` to generate mock gateway references.
* Use `docker-compose up -d` to launch the WireMock container on port `8085`. The volume maps `./wiremock:/home/wiremock` so mapping updates are picked up dynamically.
* Running `bootRun` directs Spring’s WebClient instances to point to the WireMock port `8085` instead of production endpoints.

### B. Automated Integration Tests (`testIntegration` SourceSet)
Automated integration tests run against a dynamic WireMock server lifecycle managed inside the JUnit 5 test cases.
* Uses `com.github.tomakehurst.wiremock.WireMockServer`.
* Stubs are defined programmatically using WireMock's fluent Java DSL.
* Asserts matching URI paths, HTTP request methods, header conditions, and request bodies.

---

## 2. Stateful Scenarios for Resilience Verification
To verify the WebClient exponential backoff resilience handler implemented in Phase 6, we utilize **WireMock Scenarios** to simulate transient downstream network failures:

1. **Transient Fault Recovery**:
   * We configure the WireMock stub to act as a state machine starting in `Scenario.STARTED`.
   * The first two requests return a `503 Service Unavailable` error and transition the scenario state (`.willSetStateTo("First Failure")` and `.willSetStateTo("Second Failure")`).
   * The third request matches only when the state is `"Second Failure"` and returns a successful `200 OK` JSON response.
   * This verifies that the adapter successfully retries on 503s and recovers on the 3rd attempt without throwing errors.

2. **Retry Exhaustion**:
   * We configure the stub to constantly return `503 Service Unavailable`.
   * We verify that the client retries exactly 3 times (making 4 total calls) and eventually bubbles up a `PaymentSessionFailedException`.

3. **No-Retry on Client Errors**:
   * We configure the stub to return `400 Bad Request` or `401 Unauthorized`.
   * We verify that the client immediately bubbles the error and makes exactly 1 call, proving it doesn't retry non-transient faults.

---

## 3. Resolved Jetty Dependency Conflict
* **Context**: Spring Boot 3.3.0 uses **Jetty 12** as its underlying dependency runtime. Standard `org.wiremock:wiremock` libraries pack transitive dependencies on **Jetty 11** classes.
* **Problem**: Placing standard WireMock on the classpath throws `IncompatibleClassChangeError` during integration test startup due to incompatible Jetty version overlaps.
* **Resolution**: We explicitly declare the dependency **`org.wiremock:wiremock-standalone:3.3.1`** in our integration test compilation block. The standalone JAR shades its own copies of Jetty and other utility libraries, keeping them isolated and preventing any library conflicts in Spring Boot 3.x runtimes.
