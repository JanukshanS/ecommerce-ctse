
# CTSE Assignment 1 – 2026
**Module:** Current Trends in Software Engineering (SE4010)  
**Faculty:** SLIIT – Faculty of Computing  
**Department:** Computer Science & Software Engineering  
**Semester:** 2026 | Semester 1  

---

# Cloud Computing Assignment

## Objective
Design and implement a prototype of a **secure, microservice-based application component** using fundamental **DevOps practices and cloud capabilities**.

---

# Assignment Overview

Your group (4 students) will design and build a **single cohesive application** broken down into **four microservices**, one per student.

Each microservice must:

- Be **independently deployable** on a **public cloud provider**
- **Integrate and communicate** with other services
- Contribute to a **working end-to-end application**

Each microservice **must communicate with at least one other service in the group**.

The final deliverable should demonstrate:

- DevOps practices
- Security considerations
- DevSecOps practices
- Cloud capabilities

---

# Shared Architecture Diagram

The group must produce **one high-level architecture diagram** showing:

- All four microservices
- Cloud deployment architecture
- Communication paths between services
- Shared infrastructure components

Examples:

- API Gateway
- Message Broker
- Shared Database
- Other infrastructure

This diagram **must be included in the report**.

---

# Specific Tasks

## 1. Design a Simple Microservice (LO1, LO3)

Choose a **core component of a larger application**.

Examples:

- User authentication service
- Product catalog service
- Order management service

Tasks:

- Define the **functionality of the microservice**
- Define **API endpoints**
- Choose any **programming language or framework**

---

## 2. Implement Basic DevOps Practices (LO1, LO2)

Requirements:

- Host the code in a **public version control repository** (e.g., GitHub).
- Implement **CI/CD pipelines** to automate:
  - Build
  - Testing
  - Deployment

---

## 3. Containerize the Microservice using Docker (LO3, LO4)

Requirements:

- The microservice **must be containerized**
- Use **Docker**
- Push the image to a **container registry**

Examples:

- Docker Hub
- AWS ECR
- Azure Container Registry

Deployment must **pull the container image from the registry**.

---

## 4. Deploy the Microservice (LO2, LO4)

Deploy the service using **managed container orchestration services**.

Examples:

- AWS ECS (Elastic Container Service)
- Azure Container Apps
- Managed Kubernetes services

Requirements:

- The microservice must be **accessible via the internet**
- Demonstrate **application security implementation**

---

## 5. Integrate Basic Security Measures (LO2, LO4)

Implement security best practices such as:

- IAM roles
- Security groups
- Least privilege access

Additionally:

- Secure data handling
- Implement DevSecOps practices

Use **managed SAST tools**, for example:

- SonarCloud
- Snyk

Free versions are acceptable.

---

# Deliverables

## Project Report

The report should contain the following sections:

### 1. Architecture Diagram

- Shared architecture diagram
- Communication flows
- Cloud infrastructure

### 2. Microservice Description

Explain:

- The purpose of your assigned microservice
- Its role in the full system

### 3. Service Communication

Explain how your service communicates with other services.

Include:

- Integration examples
- API interactions

### 4. DevOps and Security

Describe:

- DevOps practices implemented
- Security practices implemented

### 5. Challenges

Discuss:

- Individual development challenges
- Integration challenges
- Solutions used

---

# Code Repository

You must provide a **public repository** containing:

- Full source code
- API contract (OpenAPI / Swagger)
- CI/CD pipeline configuration
- Dockerfile
- Container configuration files

---

# Demonstration (10 minutes)

During the demo you must show:

1. A working **microservice deployed in the cloud**
2. **Inter-service communication**
3. **CI/CD pipeline execution**
4. **Security implementation explanation**

---

# Assessment Criteria

Most marks (**90%**) will be evaluated during the **viva demonstration**.

| Criteria | Marks |
|--------|--------|
| Practicality and functionality | 10% |
| DevOps and cloud implementation | 30% |
| Inter-service communication | 10% |
| Security and DevSecOps | 20% |
| Code quality | 20% |
| Report and demonstration clarity | 10% |

---

# Notes

- Focus on **depth rather than breadth**
- A **well implemented single microservice** is better than multiple weak services
- Document **design decisions and challenges**
- Ensure you **stay within the cloud provider free tier**
