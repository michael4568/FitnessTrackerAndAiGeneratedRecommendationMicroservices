# AuraFit - AI-Powered Fitness Tracking Microservice

Welcome to **AuraFit**, an enterprise-grade fitness tracking platform built on a distributed microservices architecture. AuraFit empowers users to seamlessly track their physical activities, monitor dynamic 7-day rolling biometric statistics, manage private workout notes, and receive personalized, AI-driven biomechanical analyses utilizing Google Gemini's advanced Large Language Models (LLMs).

For detailed architectural diagrams and technical documentation, please refer to our deep-dive guides:
- [High-Level Design (HLD)](./HLD.md) - System architecture, component interaction, and design trade-offs.
- [Low-Level Design (LLD)](./LLD.md) - Database schemas, API specifications, and sequence diagrams.

---

## 🌟 Key Features

*   **AI-Powered Coaching:** Deep integration with Google Gemini provides tailored workout analyses, biomechanical feedback, and actionable recovery recommendations based on the user's logged metrics.
*   **Context-Aware Chatbots:** Intelligent, guard-railed fitness chatbots analyze a user's recent workout history (last 5 activities) to provide safe, strictly health-focused advice.
*   **Dynamic Rate Limiting (Anti-DDoS):** Utilizes Resilience4j to manage multiple rate-limiting buckets (e.g., Free vs. Custom API Key quotas) alongside UI-level DDoS protection.
*   **Event-Driven Asynchronous Processing:** Leverages Apache Kafka to decouple AI recommendation generation from the main application thread, ensuring high throughput and blazingly fast UI responses under heavy load.
*   **Strict Profile Isolation:** Bulletproof identity propagation using Keycloak JSON Web Tokens (JWT) and explicit ID mapping ensures absolute data privacy across user accounts.
*   **Interactive API Documentation:** Embedded Swagger UI (OpenAPI 3) across all microservices for seamless endpoint testing and developer onboarding.
*   **Modern UI/UX:** A sleek, dark-themed React/Vite dashboard featuring highly interactive components, smooth transitions, and dynamic statistical charts.

---

## 🏗️ Technical Stack & Architecture

AuraFit is engineered using industry-standard technologies to guarantee performance, security, and long-term maintainability.

### Backend Infrastructure
*   **Spring Boot 3 & Spring Cloud:** Forms the resilient backbone of the microservices ecosystem. Spring Cloud provides out-of-the-box solutions for distributed systems, significantly reducing the boilerplate required for routing, configuration, and service discovery.
*   **Netflix Eureka:** Acts as the central Service Registry. It enables microservices to dynamically discover and communicate with each other without relying on hardcoded IP addresses, facilitating seamless horizontal scaling.
*   **Spring Cloud Gateway:** Serves as the single entry point for all client traffic. It manages global CORS configurations, performs stateless JWT signature validation, and securely routes requests to the appropriate internal microservices.
*   **Spring Cloud Config:** Provides centralized external configuration management across all environments.

### Data Processing & Integration
*   **Apache Kafka:** A distributed event streaming platform used to decouple the `ActivityService` from the `aiService`. Upon logging a workout, Kafka reliably queues the event, enabling the AI engine to process recommendations asynchronously without blocking the user interface.
*   **Resilience4j:** Delivers robust fault tolerance. It is implemented for **Circuit Breaking** (ensuring graceful degradation if internal services experience downtime) and **Rate Limiting** (dynamically routing users between API quota buckets based on request headers).
*   **Google Gemini 2.5 Flash API:** The AI engine powering the application's coaching features. It is engineered with strict system prompts to reject off-topic queries and provide highly contextual, safe fitness advice.
*   **Polyglot Persistence:** Employs purpose-built databases for specific domain needs. **MySQL** and **PostgreSQL** manage strict relational data for user profiles and notes, while **MongoDB** handles highly flexible, schema-less documents for activity logs and AI recommendations (accommodating varied metrics like swimming vs. weightlifting).

### Security & Identity
*   **Keycloak (IAM):** An open-source Identity and Access Management solution that handles secure user registration, OAuth2/OIDC authentication flows, and JWT generation, ensuring enterprise-grade security.

### Frontend Presentation
*   **React 18, Vite, & Tailwind CSS:** Delivers a lightning-fast, highly responsive Single Page Application (SPA). Vite provides instant hot-module replacement during development, while Tailwind CSS enables rapid, consistent, and beautiful UI styling. **Lucide React** provides crisp, modern iconography.

---

## 📖 API Documentation (Swagger / OpenAPI)

To facilitate developer integration and testing, **Swagger UI** (via `springdoc-openapi`) is embedded directly into each business microservice. Once the application is running locally, you can access the interactive API documentation at the following endpoints:

- **User Service:** `http://localhost:8081/swagger-ui.html`
- **Activity Service:** `http://localhost:8082/swagger-ui.html`
- **AI Service:** `http://localhost:8083/swagger-ui.html`

---

## 🚀 Local Development Setup

This entire microservice ecosystem has been fully containerized. To make local testing as painless as possible, **all services have been pre-built and pushed to Docker Hub** under the `rashad2210` repository. You can choose to run the entire stack via Docker, or run the infrastructure via Docker and the services locally (non-dockerized).

### Prerequisites
- **Docker** and **Docker Compose** installed on your machine.
- **Java 17+** and **Maven** (If using the non-dockerized setup).
- **Node.js 18+** and **npm** (If using the non-dockerized setup).
- A free **Google Gemini API Key** (for the AI Service). *Note: The Gemini key can also be provided directly through the frontend UI if you want to see how the default key is working!*
- **Databases**: Ensure you have local instances of MySQL (`localhost:3306`) and MongoDB (`localhost:27017`) running.

### Step 1: Start the Core Infrastructure (Kafka & Keycloak)

Instead of installing heavy infrastructure manually, you can run Kafka and an in-memory Keycloak (which requires no database setup) via Docker. 

Run the following commands in your terminal to pull the images and start them:

**1. Pull the images:**
```bash
docker pull quay.io/keycloak/keycloak:latest
docker pull bitnami/kafka:latest
```

**2. Start an In-Memory Keycloak:**
```bash
docker run -d --name keycloak-memory -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin123 quay.io/keycloak/keycloak:latest start-dev
```
*(Once booted, go to `http://localhost:8180` and login with `admin/admin123`. The `fitness-realm` gets created automatically by our code—you just need to switch to it from the `master` realm dropdown in the UI!)*

**3. Start Apache Kafka (KRaft mode):**
```bash
docker run -d --name kafka -p 9092:9092 -e KAFKA_ENABLE_KRAFT=yes -e KAFKA_CFG_PROCESS_ROLES=broker,controller -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://127.0.0.1:9092 -e KAFKA_BROKER_ID=1 -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093 -e ALLOW_PLAINTEXT_LISTENER=yes bitnami/kafka:latest
```

### Step 2: Set your Gemini API Key

The application requires a Gemini API key. The key provided via the terminal acts as the **default key**. However, if you provide a key directly through the frontend UI, that key will become **active** and override the default one! 

Before starting the microservices, export your default Gemini API key:

**Windows (PowerShell):**
```powershell
$env:GEMINI_API_KEY="your_api_key_here"
```
**Mac/Linux:**
```bash
export GEMINI_API_KEY="your_api_key_here"
```

### Step 3: Spin up the Services

You have two choices for running the services:

#### Option A: Fully Dockerized Setup (Recommended)
Since all services are already published to Docker Hub, you can launch the entire ecosystem with a single command!

1. Pull the pre-built microservice images (optional, docker-compose will do this automatically):
   ```bash
   docker pull rashad2210/config-service:latest
   docker pull rashad2210/eureka-server:latest
   docker pull rashad2210/gateway-service:latest
   docker pull rashad2210/user-service:latest
   docker pull rashad2210/activity-service:latest
   docker pull rashad2210/ai-service:latest
   docker pull rashad2210/frontend:latest
   ```
2. Navigate to the root directory where the `docker-compose.yml` file is located, and run:
   ```bash
   docker-compose up -d
   ```
   *Docker will automatically boot the Discovery and Config servers first, wait 15-30 seconds, and then successfully launch all business microservices and the React frontend.*

#### Option B: Non-Dockerized Setup (For Active Development)
If you want to run the Java Spring Boot services and the React frontend locally (while keeping Kafka and Keycloak in Docker):

1. **Config Server** (Must be started first! Wait ~10 seconds for it to boot)
   ```bash
   cd configService
   mvn spring-boot:run
   ```
2. **Discovery Server** (Netflix Eureka. Wait ~10 seconds for it to boot)
   ```bash
   cd eureka
   mvn spring-boot:run
   ```
3. **Gateway & Business Services** (Can be started concurrently once Eureka is up)
   - Start `gateway` (Port 8080): `cd gateway && mvn spring-boot:run`
   - Start `UserService` (Port 8081): `cd UserService && mvn spring-boot:run`
   - Start `ActivityService` (Port 8082): `cd ActivityService && mvn spring-boot:run`
   - Start `aiService` (Port 8083): `cd aiService && mvn spring-boot:run`

4. **Start the Frontend**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

*Note: You can verify all services are registered by visiting the Eureka dashboard at `http://localhost:8761`. You can access the frontend at `http://localhost:5173` (for non-dockerized) or `http://localhost:80` (for dockerized).*

---

## 📂 Project Structure

```text
FitnessTrackerAndSuggestionsMicroservice/
├── frontend/                     # React / Vite Application
│   ├── src/
│   │   ├── components/           # Reusable UI components
│   │   ├── App.jsx               # Main dashboard and routing logic
│   │   └── index.css             # Tailwind configuration and global styles
│   └── package.json
├── gateway/                      # Spring Cloud Gateway
│   └── src/main/java/com/fitness/gateway/
│       └── config/SecurityConfig.java  # Global CORS and JWT validation
├── config-server/                # Spring Cloud Config Server
│   └── src/main/resources/       # Centralized application properties
├── discovery-server/             # Netflix Eureka Registry
├── UserService/                  # User Profile & Notes Microservice (MySQL)
│   └── src/main/java/com/FitnessApp/UserService/
│       ├── controller/           # REST Endpoints (/api/user)
│       └── service/              # Keycloak sync & Notes logic
├── ActivityService/              # Workout Tracking Microservice (MongoDB)
│   └── src/main/java/com/fitness/ActivityService/
│       ├── controller/           # REST Endpoints (/api/activities)
│       └── service/              # Rolling 7-day stats & Kafka publishing
├── aiService/                    # AI & Recommendation Microservice (MongoDB)
│   └── src/main/java/com/fitness/aiService/
│       ├── controller/           # REST Endpoints (/api/recommendations)
│       └── service/              # Kafka Consumer, Gemini Integration, Resilience4j
├── HLD.md                        # High-Level Design Documentation
├── LLD.md                        # Low-Level Design Documentation
├── README.md                     # This file
└── pom.xml                       # Root Maven Aggregator
```
