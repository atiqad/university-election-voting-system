# University Online Election Voting System

A secure web-based election platform developed using **Spring Boot**, **Thymeleaf**, and **MySQL**. This system enables universities to conduct elections with role-based access control, secure voting, candidate management, and automated result processing.

## Project Overview

This system provides a controlled and transparent environment for conducting university elections. It ensures that only authorized users can participate, enforces voting rules, and maintains data integrity throughout the process.

## Problem Statement

Traditional election processes in universities lack proper control, transparency, and security. This system addresses the following challenges:

- Ensuring only authenticated users can access the system
- Allowing students to vote only once per election
- Providing administrative control over elections and candidates
- Enforcing voting availability based on time and status
- Automatically calculating results and determining winners
- Maintaining logs of important system activities

## System Architecture

The application follows a layered architecture:

`Controller -> Service -> Repository -> Entity`

- Controllers handle HTTP requests and routing
- Services contain business logic and validation
- Repositories manage database operations using JPA
- Entities represent database tables

This design ensures separation of concerns and maintainability.

## Modules

### User Module

- Student registration
- Login using Spring Security
- Role-based access control (ADMIN, STUDENT)
- Password encryption using BCrypt

### Election Module

- Create and update elections
- Activate or deactivate elections
- Control voting using start and end dates

### Candidate Module

- Add, update, and remove candidates
- Manage candidate visibility
- Associate candidates with elections

### Voting Module

The system enforces strict voting rules:

- Only students are allowed to vote
- Election must be active
- Voting must be within the allowed time window
- Candidate must belong to the selected election
- Candidate must be active
- One student can vote only once per election

### Result Module

- Automatic vote counting
- Winner calculation
- Basic tie handling

### Activity Log Module

- Logs administrative actions
- Stores logs in database
- Supports system auditing

## Database Design

### Tables

- `users`
- `elections`
- `candidates`
- `votes`
- `activity_logs`

### Relationships

- Candidate -> Election (Many-to-One)
- Vote -> User (Many-to-One)
- Vote -> Candidate (Many-to-One)
- Vote -> Election (Many-to-One)

### Data Integrity

A unique constraint is applied:

`UNIQUE(user_id, election_id)`

This ensures that each student can vote only once per election and prevents duplicate voting.

## Security Implementation

The system uses Spring Security for authentication and authorization.

Key features:

- Role-based access control
- Secure password storage using BCrypt
- Restricted access to admin endpoints
- Students cannot access admin functionality

## REST APIs

Voting:

- `POST /votes/cast`

Results:

- `GET /votes/results/{electionId}`

Candidates:

- `GET /candidates/all`
- `GET /candidates/{id}`
- `POST /candidates/add`

Elections:

- `POST /elections/**`
- `PUT /elections/**`
- `DELETE /elections/**`

## Testing

The project includes test cases for:

- Duplicate vote prevention
- Election status validation
- Time-based voting restrictions
- Role-based access control

Run tests:

```bash
./mvnw test
```

## Design Patterns

- Model-View-Controller (MVC)
- Repository Pattern
- Service Layer Pattern
- Dependency Injection

## SOLID Principles

The system follows key SOLID principles:

- Single Responsibility Principle: Each class has a single responsibility
- Open/Closed Principle: System is extendable without modifying existing code
- Liskov Substitution Principle: Interfaces are properly implemented
- Interface Segregation Principle: Focused interfaces are used
- Dependency Inversion Principle: Dependencies are injected using Spring

## Additional Features

- Automatic vote counting
- Winner detection
- Activity logging
- Time-controlled voting
- Role-based dashboards

## How to Run the Project

Clone the repository:

```bash
git clone <repository-link>
```

Navigate to the project folder:

```bash
cd voting-system
```

Run the application:

```bash
./mvnw spring-boot:run
```

Open in browser:

`http://localhost:8080`

## Demonstration Flow

1. Admin logs in
2. Admin creates an election
3. Admin adds candidates
4. Admin activates the election
5. Student registers and logs in
6. Student casts a vote
7. System prevents duplicate voting
8. Results are displayed automatically

## Team Contribution
Atiqad hayat 
Bilal sami

- Backend Development: Spring Boot APIs, database design, security implementation
- Frontend Development: User interface, pages, and user interaction

## Technologies Used

- Java 17
- Spring Boot
- Spring Security
- Thymeleaf
- MySQL
- Maven

## License

This project is developed for academic purposes.

