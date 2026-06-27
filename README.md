# Hexagonal Architecture Java Spring Boot Sample

This repository implements a production-grade, highly resilient Spring Boot microservice utilizing **Hexagonal Architecture** (also known as Ports & Adapters).

The service provides a REST API to dynamically initialize order payment sessions by checking saved card details against a downstream card service, creating a session key with an external payment gateway, and handling network transient resilience.

---

## 🏗️ Architectural Layout

To preserve the separation of concerns, the core business domain is 100% free of framework dependencies (no Spring, Jackson, or Jakarta Persistence imports). Dependency injection annotations are kept strictly inside the `config` bootstrap package.

```
src/
├── main/
│   ├── java/com/example/myapp/order/
│   │   ├── adapter/                   # Driving & Driven Infrastructure
│   │   │   ├── in/rest/               # REST Driving Adapters (Controllers & Advice)
│   │   │   └── out/client/            # WebClient downstream API clients
│   │   ├── application/               # Core Application Orchestration
│   │   │   ├── port/                  # Inbound and Outbound Port Interfaces & commands
│   │   │   └── service/               # Application Service Use Case implementations
│   │   ├── config/                    # Spring Configuration and Manual Bean Wiring
│   │   ├── domain/                    # Pure Domain Model (Entities, Value Objects, Domain Exceptions)
│   │   └── OrderApplication.java      # Application bootstrap entrypoint
│   └── resources/
│       ├── contracts/                 # OpenAPI Spec contracts defining API interfaces
│       └── application.properties     # Spring properties & base URLs configuration
├── test/                              # Fast Core Unit Tests (Domain & Application logic)
├── testIntegration/                   # HTTP integration slices (WireMock downstream & WebMvc tests)
└── testArchitecture/                  # ArchUnit boundary validation check suites
```

---

## 🛠️ Key Implementation Standards

1. **Pure Domain & Core**: The domain entities and application services are compiled without third-party frameworks. They define their own rules, inputs, and outbound needs (via Ports).
2. **Driving REST Adapters**: Implemented via controller stubs inside `/adapter/in/rest` generated directly from [payment-session-api.yaml](src/main/resources/contracts/payment-session-api.yaml).
3. **RFC 9457 Problem Details**: System errors are translated by the global REST advice handler into standardized Problem Details JSON payloads.
4. **Transient Resilience**: Outbound WebClient HTTP adapters protect against network hiccups using reactive exponential backoff retries targeting only transient faults (HTTP 5xx, socket timeouts), leaving client errors (HTTP 4xx) un-retried.
5. **Architectural Guardrails**: ArchUnit checks automatically enforce package boundaries during compile verification to prevent leakage of infrastructure packages into the domain core.

---

## 🚀 CLI Commands Reference

Execute these commands from the project root directory:

### Building & Code Generation
| Command | Purpose |
| :--- | :--- |
| `./gradlew openApiGenerate` | Compiles OpenAPI YAML contracts and generates controller stubs & request/response DTOs. |
| `./gradlew compileJava` | Compiles main application source code (depends on `openApiGenerate` automatically). |
| `./gradlew build -x test` | Compiles and builds the production-ready runnable jar file without running tests. |

### Running Tests
All testing sourceSets are prefixed with `test` for easy discovery:

| Command | Purpose |
| :--- | :--- |
| `./gradlew test` | Runs fast **Unit Tests** verifying application services, commands, and domain record rules. |
| `./gradlew testIntegration` | Runs slow **Integration Tests** testing controller HTTP stubs and WebClient mock exchanges using WireMock. |
| `./gradlew testArchitecture` | Runs **ArchUnit compliance tests** verifying package boundary dependencies. |
| `./gradlew check` | Runs **ALL tests** (Unit, Integration, and Architecture compliance) and generates reports. |

For detailed information regarding integration mocking strategy, stateful scenarios, and how Jetty classpath conflicts were resolved, see [testing_and_wiremock.md](docs/testing_and_wiremock.md).

### Running the Application
To run and test the application manually:

1. **Start the Downstream Mock Services**:
   Spin up the standalone WireMock container containing the stub mapping files on port `8085`:
   ```bash
   docker compose up -d
   ```
2. **Launch the Spring Boot Application**:
   Start the application locally on port `8080` (pre-configured to talk to the WireMock container on port `8085`):
   ```bash
   ./gradlew bootRun
   ```

| Command | Purpose |
| :--- | :--- |
| `docker compose up -d` | Boots the downstream mock APIs container using WireMock on port `8085`. |
| `docker compose down` | Stops the downstream mock APIs container. |
| `./gradlew bootRun` | Launches the Spring Boot application locally on port `8080`. |
