## CodeSync: A Microservices-Based Collaborative Development Platform

**CodeSync** is a high-performance, real-time collaborative IDE platform designed for modern developers. It enables multiple users to co-edit code, manage projects via a virtual file system, execute code in isolated sandboxes, and maintain full version control through cryptographic snapshots. The architecture is built on a distributed microservices model, ensuring high availability, scalability, and fault tolerance.



---

### Core Architecture & Features

The system is composed of 11 specialized microservices, each handling a specific domain of the platform:

1.  **Identity & Access Management:** Secure authentication and authorization using JWT (JSON Web Tokens).
2.  **Real-Time Collaboration:** WebSocket-driven co-editing with live cursor tracking and rotating participant markers.
3.  **Virtual File System (VFS):** Hierarchical file and folder management with recursive project forking and cloning capabilities.
4.  **Isolated Execution Engine:** Sandboxed code execution for Java, Python, and JavaScript using Docker containers with strict CPU/RAM quotas.
5.  **Version Control System:** Git-like snapshots using SHA-256 hashing for data integrity and Myers Diff Algorithm for line-by-line comparisons.
6.  **Omnichannel Notifications:** Instant system alerts via WebSockets and professional HTML email dispatches for project collaboration requests.
7.  **Financial Infrastructure:** Razorpay integration for subscription management and automated billing cycles.



---

### Technical Stack

#### Backend Frameworks & Tools
* **Java 17:** Primary programming language for core business logic.
* **Spring Boot 3.5.x:** Main framework for microservice development.
* **Spring Cloud (2025.x):** Implementation of Netflix Eureka for Service Discovery and OpenFeign for inter-service communication.
* **Spring Security:** Stateless authentication and role-based access control.

#### Data & Messaging
* **MySQL:** Relational database for persistent storage of user, project, and session metadata.
* **RabbitMQ:** Message broker for asynchronous task distribution (Execution jobs and Email dispatch).
* **Spring Data JPA / Hibernate:** Object-Relational Mapping for database interactions.

#### DevOps & Infrastructure
* **Docker:** Containerization of code execution environments to ensure sandbox isolation.
* **Spring Boot Admin:** Centralized monitoring dashboard for real-time health checks and log management.
* **Maven:** Project management and build tool.

#### Third-Party Integrations
* **Razorpay:** Payment gateway for processing subscriptions.
* **Java Diff Utils:** Implementation of the Myers diff algorithm for version comparisons.
* **Monaco Editor (Frontend):** High-fidelity code editing experience.

---

### System Requirements

* **Java:** JDK 17 or higher
* **Database:** MySQL 8.0+
* **Message Broker:** RabbitMQ
* **Containerization:** Docker Desktop (Required for Execution-Service)
* **Build Tool:** Maven 3.8+

---

### Setup and Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/abhays07/codesync-backend.git
    cd codesync-backend
    ```

2.  **Environment Configuration:**
    Use `codesync-backend-main/.env` as the single shared backend environment file for all microservices.
    Keep backend URLs, OAuth credentials, SMTP settings, JWT settings, and database names in that one file.
    Configure the following environment variables in the shared `.env` file:
    * `DB_HOST`, `DB_USER`, `DB_PASSWORD`
    * `AUTH_DB_NAME`, `PROJECT_DB_NAME`, `FILE_DB_NAME`, `COLLAB_DB_NAME`, `EXECUTION_DB_NAME`, `COMMENT_DB_NAME`, `NOTIFICATION_DB_NAME`, `VERSION_DB_NAME`, `PAYMENT_DB_NAME`
    * `FRONTEND_URL`, `BACKEND_GATEWAY_URL`, `AUTH_BASE_URL`
    * `JWT_SECRET`, `TOKEN_EXPIRY`
    * `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`
    * `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
    * `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
    * `EMAIL_USERNAME`, `EMAIL_APP_PASSWORD` (Google App Password)

    Backend services now import `../.env`, so separate per-service `.env` files are no longer needed.

3.  **Start Infrastructure Services:**
    Ensure MySQL, RabbitMQ, and the Docker Daemon are running.

4.  **Service Startup Sequence:**
    Follow this order to ensure proper registration and discovery:
    1.  `Eureka-Server` (Port 8761)
    2.  `Config-Server` (If applicable)
    3.  `API-Gateway` (Port 8080)
    4.  All business microservices (Auth, Project, File, etc.)
    5.  `Admin-Server` (Port 9090)

---

### API Documentation

The platform uses **SpringDoc / OpenAPI 3** for interactive documentation. Once the services are running, you can access the Swagger UI through the API Gateway:
* `http://localhost:8080/swagger-ui.html`

---

### Docker Deployment

The backend is now set up to build and run through a single Compose stack from the `code-sync-backend` directory.

1. Copy `.env.example` to `.env` and fill in the required secrets.
   Set `FRONTEND_URL` to the public URL of your frontend for notification-service CORS.
2. Start Docker Desktop.
3. Run:
   ```bash
   docker compose up --build
   ```

The stack includes:
* `mysql`
* `rabbitmq`
* `eureka-server`
* `admin-server`
* all backend microservices
* `api-gateway`

Useful URLs after startup:
* API Gateway: `http://localhost:9010`
* Swagger UI: `http://localhost:9010/swagger-ui.html`
* Eureka Dashboard: `http://localhost:8761`
* RabbitMQ Dashboard: `http://localhost:15672`
* Admin Server: `http://localhost:9090/admin`

Notes:
* `execution-service` mounts `/var/run/docker.sock` so it can create sandbox containers.
* Compose sets each service's `EUREKA_INSTANCE_HOSTNAME` to its Docker service name so service discovery works correctly inside the Docker network.
* The previous Swagger URL in this README used port `8080`; this project is currently configured to run the gateway on port `9010`.
