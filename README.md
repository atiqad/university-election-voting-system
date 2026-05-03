# University Online Election Voting System

Spring Boot + Thymeleaf + MySQL project for conducting a university election with secure login, role-based access, candidate management, voting, and real-time results.

## 1) Problem Understanding (What Problem This Solves)

Universities need a controlled way to run elections where:
- Only authenticated users can access the system.
- Admin can create/manage elections and candidates.
- Students can cast **exactly one vote per election**.
- Voting should be controlled by admin enable/disable and by start/end time.
- Results and winner are calculated automatically.
- Important actions can be audited (activity logs).

## 2) System Design (Layered Architecture)

This project follows a beginner-friendly layered design:

`Controller → Service → Repository → Entity`

- **Controllers**: handle HTTP requests/routes and view navigation.
- **Services**: contain business rules (voting rules, validation, winner calculation).
- **Repositories**: database queries via Spring Data JPA.
- **Entities**: database table mappings (JPA).

Design goal: **No business logic inside controllers** (controllers stay thin).

## 3) Modules

### User Module
- Registration (students)
- Login (Spring Security)
- Roles: `ADMIN`, `STUDENT`
- Password hashing: BCrypt

### Election Module
- Admin can create/update elections
- Active/inactive toggle
- Time window: startDate/endDate used to open/close voting

### Candidate Module
- Admin can add/update/delete candidates
- Candidate visibility: active/inactive (hide from students)
- Optional profile image upload

### Voting Module
- Students cast a vote for a candidate in a specific election
- Business rules:
  - Only STUDENT can vote (ADMIN cannot vote)
  - Election must be enabled (active=true)
  - Election must be within time window (start/end)
  - Candidate must belong to the selected election
  - Candidate must be active/visible
  - One-student-one-vote per election (enforced in service + DB)

### Result Module
- Automatic vote counting
- Winner calculation (and tie detection for demo output)

### Activity Log Module
- Logs important actions (create/update/delete election/candidate, cast vote)
- Stored in DB table `activity_logs` and also printed in app logs

## 4) Database Handling (Tables + Relationships)

### Tables
- `users`
- `elections`
- `candidates`
- `votes`
- `activity_logs`

### Relationships
- `Candidate` → Many-to-One → `Election` (`candidates.election_id`)
- `Vote` → Many-to-One → `User` (`votes.user_id`)
- `Vote` → Many-to-One → `Candidate` (`votes.candidate_id`)
- `Vote` → Many-to-One → `Election` (`votes.election_id`)
- `ActivityLog` → Many-to-One (optional) → `User` (`activity_logs.user_id`)

### Reliability Rules (Important for Viva)
- **One-student-one-vote (DB enforced)**:
  - Unique constraint: `votes(user_id, election_id)`
  - This prevents race conditions (two requests at the same time).

## 5) Security (Spring Security)

Public routes:
- `GET /login`
- `GET /register`
- `POST /register`

Role-based routes:
- Admin pages and admin APIs require `ADMIN`
- Student pages (vote dashboard/results) require `STUDENT`
- Students cannot access admin pages

Password security:
- Passwords are stored as **BCrypt hash** (never plain text).

## 6) REST APIs (for rubric)

Main voting endpoint:
- `POST /votes/cast` (STUDENT only)

Results:
- `GET /votes/results/{electionId}` (ADMIN and STUDENT)

Candidate APIs (admin-only by security config; also used by pages):
- `GET /candidates/all`
- `GET /candidates/{id}`
- `GET /candidates/election/{electionId}`
- `POST /candidates/add` (JSON API version)

Election APIs (admin-only by security config):
- `POST /elections/**`, `PUT /elections/**`, `DELETE /elections/**`

Note: This project intentionally keeps both **Thymeleaf pages** and some **JSON endpoints** for evaluation/demo purposes.

## 7) Testing

Location: `src/test/java/...`

Included tests (examples):
- Duplicate vote restriction (student can vote only once per election)
- Winner/result counting
- Election rules: inactive election blocks voting
- Election rules: not-started election blocks voting
- Election rules: ended election blocks voting
- Role-based access: student cannot open admin dashboard
- Security rule: admin cannot vote (service-level)

Run tests:
```bash
./mvnw test
```

## 8) Documentation + Diagrams (Rubric Requirement)

Diagrams you should include in your final report/slides:
- Use Case Diagram
- ER Diagram
- Class Diagram
- Sequence Diagram (casting vote flow)
- Activity Diagram (voting lifecycle)

Suggested sequence diagram flow (vote):
1. Student submits vote
2. Controller calls VoteService.castVote
3. VoteService checks election open, candidate valid, already voted
4. VoteRepository saves vote
5. Results calculated from vote counts

## 9) Design Patterns Used (Easy Viva Points)

- **Dependency Injection** (Spring): controllers/services/repositories injected via constructors.
- **Repository Pattern**: Spring Data JPA repositories.
- **Service Layer Pattern**: all business rules implemented in services.
- **MVC Pattern**: Thymeleaf views + controllers.

## 10) Innovation / Extra Features

Implemented features aligned with rubric:
- Automatic vote counting
- Winner calculation (+ tie detection)
- One-student-one-vote restriction (service + DB constraint)
- Active/inactive election toggle
- Time-controlled voting (start/end)
- Activity logging (DB + application logs)

## Presentation & Demo Flow (Recommended)

1. Admin login
2. Create election (set start/end time)
3. Activate election
4. Add candidates (with optional images)
5. Student registration → student login
6. Student opens voting page → casts vote
7. Student tries to vote again → blocked with error
8. Open results page → show counts + winner

