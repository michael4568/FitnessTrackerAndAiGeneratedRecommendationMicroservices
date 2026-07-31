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
