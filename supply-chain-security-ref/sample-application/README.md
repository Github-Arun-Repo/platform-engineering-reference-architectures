# TODO Application

A simple Spring Boot REST API demonstrating core microservice patterns.

## Overview

This application provides CRUD operations for managing TODO items. It serves as a reference implementation for understanding containerization and CI/CD pipeline patterns — **the application logic itself is intentionally minimal** to keep focus on the CI/CD and infrastructure concerns.

## Quick Start (Local)

### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run

```bash
mvn clean package
java -jar target/todo-app-0.0.1-SNAPSHOT.jar
```

Access: `http://localhost:8080/api/todos`

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/todos` | List all TODOs |
| GET | `/api/todos/{id}` | Get TODO by ID |
| POST | `/api/todos` | Create new TODO |
| PUT | `/api/todos/{id}` | Update TODO |
| DELETE | `/api/todos/{id}` | Delete TODO |
| GET | `/api/todos/health` | Health check |

## Docker

Build the Docker image:

```bash
docker build -t todo-app:latest .
```

Run the container:

```bash
docker run -p 8080:8080 todo-app:latest
```

The Dockerfile uses 2026 best practices:
- **Multi-stage builds** for optimized image size
- **Non-root user** for security
- **Health checks** for container orchestration
- **Alpine base** for minimal attack surface
- **Layer caching** optimization
