# Campus Event and Workshop Management System

A clean Full Stack Development (FSD) college project built using **Java 24**, **Jakarta Servlets 6.0**, **JDBC**, **MySQL 8+**, **Apache Tomcat 10.1**, and modern **HTML5 / Vanilla CSS / Vanilla JS**.

---

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture & Design](#architecture--design)
4. [Database Design](#database-design)
5. [Prerequisites & Setup](#prerequisites--setup)
6. [Features by User Role](#features-by-user-role)
7. [Comprehensive Viva Questions & Answers](#comprehensive-viva-questions--answers)

---

## 1. Project Overview
The **Event and Workshop Management System** allows colleges to manage technical workshops, coding hackathons, seminars, and campus events.
- **Students** can browse upcoming events, view available seats, register with one click, receive an official **digital ticket token pass**, and view their registrations.
- **Administrators** have an executive dashboard, full CRUD operations over events, and an attendee roster for every event.

---

## 2. Technology Stack

| Component | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Java | 24 | Application logic & compilation |
| **Server** | Apache Tomcat | 10.1.x | Servlet container & web server |
| **Web API** | Jakarta Servlets | 6.0.0 | REST API routing & controller layer |
| **View Layer** | HTML5 & Vanilla JS | Modern ES5/6 | Clean, framework-free client interface |
| **Styling** | Vanilla CSS | Custom CSS | Clean, responsive dark mode design |
| **Database** | MySQL | 8.0+ | Relational data persistence |
| **Database Access** | JDBC | `mysql-connector-j` 8.3 | Direct SQL queries via `PreparedStatement` |
| **Security** | BCrypt (Favre) | 0.10.2 | Salted password hashing (cost 12) |
| **JSON** | Google Gson | 2.11.0 | JSON serialization for REST API |
| **QR Engine** | Google ZXing | 3.5.3 | Dynamic binary QR code generation |

---

## 3. Architecture & Design

The application follows a decoupled **RESTful MVC Architecture**:

```text
Browser Client (HTML5 + CSS + Vanilla JS)
   │
   │  AJAX Fetch Requests (JSON & URL-Encoded)
   ▼
[Jakarta Servlets & Filters] (REST API Layer)
   │  Handles routing, sessions, validation, and role security (AuthFilter)
   ▼
[DAO Layer] (Data Access Objects)
   │  Executes SQL queries via PreparedStatement & Transactions
   ▼
[MySQL Database] (InnoDB, utf8mb4)
```

### Directory Structure
```text
event-management-system/
├── pom.xml                                <-- Maven configuration (Java 24)
├── schema.sql                             <-- Database schema & seed data
├── README.md                              <-- Project documentation & viva guide
└── src/
    └── main/
        ├── java/
        │   └── com/eventmanagement/
        │       ├── dao/                   <-- Database operations (UserDAO, EventDAO, RegistrationDAO)
        │       ├── filter/                <-- Security filter (AuthFilter - RBAC)
        │       ├── model/                 <-- Model classes (User, Event, Registration)
        │       ├── servlet/               <-- REST Controllers (Login, Register, Event, Ticket)
        │       └── util/                  <-- Utilities (DBConnection, PasswordUtil, JsonUtil)
        ├── resources/
        │   └── db.properties              <-- MySQL database credentials
        └── webapp/
            ├── WEB-INF/
            │   └── web.xml                <-- Deployment descriptor
            ├── css/
            │   └── style.css              <-- Unified stylesheet
            ├── js/
            │   └── script.js              <-- Unified client JavaScript
            ├── index.html                 <-- Home page & metrics
            ├── events.html                <-- Event catalog
            ├── event-details.html         <-- Event details & registration
            ├── login.html                 <-- Sign in page
            ├── register.html              <-- Student sign up page
            ├── my-events.html             <-- Student registrations list
            ├── ticket.html                <-- Digital ticket pass view
            └── admin/
                ├── dashboard.html         <-- Admin metrics dashboard
                ├── events.html            <-- Admin event management table
                ├── event-form.html        <-- Create & edit event form
                └── registrations.html     <-- Attendee roster view
```

---

## 4. Database Design

Database: `event_management` (InnoDB, utf8mb4)

```text
  ┌──────────────┐         ┌──────────────────┐         ┌─────────────┐
  │    users     │         │  registrations   │         │   events    │
  ├──────────────┤         ├──────────────────┤         ├─────────────┤
  │ id (PK)      │1       *│ id (PK)          │*       1│ id (PK)     │
  │ name         │─────────│ user_id (FK)     │─────────│ title       │
  │ email (UQ)   │         │ event_id (FK)    │         │ description │
  │ password     │         │ ticket_token(UQ) │         │ event_date  │
  │ role         │         │ registered_at    │         │ venue       │
  │ created_at   │         └──────────────────┘         │ capacity    │
  └──────────────┘                                      │ created_at  │
                                                        └─────────────┘
```

---

## 5. Prerequisites & Setup

### Requirements:
- **Java Development Kit (JDK):** Version 24
- **Database:** MySQL 8.0 or later
- **Web Server:** Apache Tomcat 10.1.x

### Step 1: Initialize Database
Run the schema script in MySQL:
```bash
mysql -u root -p < schema.sql
```

### Step 2: Configure Database Credentials
Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/event_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=your_password
```

### Step 3: Build & Deploy
Build the project using Maven:
```bash
mvn clean package
```
Deploy the generated `target/event-management.war` into Tomcat's `webapps/` folder, then open:
`http://localhost:8080/event-management/`

---

## 6. Features by User Role

### Public Visitors:
- View campus events and workshops.
- View event date, venue, capacity, and remaining available seats.

### Registered Students:
- Secure login and registration with BCrypt password hashing.
- Register for upcoming open events with capacity validation.
- Prevent duplicate registrations for the same event.
- Access digital ticket passes with unique verification tokens.

### Administrators:
- Executive dashboard displaying total events, registrations, and student metrics.
- Complete CRUD management for campus events (create, edit, delete).
- View student registration and attendee roster for every event.

---

## 7. Comprehensive Viva Questions & Answers

**Q1: Why use Servlets with HTML/JavaScript instead of JSP?**  
*Answer:* Separating the view (HTML/CSS/JS) from the backend (Jakarta Servlets REST API) adheres to clean software architecture. The frontend handles UI rendering and user interactions, while Servlets focus purely on business logic, data validation, and JSON data exchange.

**Q2: How is SQL Injection prevented?**  
*Answer:* All SQL queries in the DAO layer use `java.sql.PreparedStatement` with parameterized placeholders (`?`). The database driver pre-compiles the SQL query structure and treats parameter values strictly as literal data, preventing malicious SQL code execution.

**Q3: How are passwords secured?**  
*Answer:* Passwords are never stored in plain text. They are hashed using BCrypt (`at.favre.lib:bcrypt`) with a workload cost factor of 12 and cryptographic per-user salting.

**Q4: How does Role-Based Access Control (RBAC) work?**  
*Answer:* `AuthFilter` intercepts incoming requests. If a request targets an admin path (`/admin/*`, `/api/admin/*`), the filter verifies that an active HTTP session exists and that the user's role equals `ADMIN`. Unauthorized requests receive a 401/403 status or redirect to the login page.
