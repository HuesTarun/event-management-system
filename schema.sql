-- ============================================================
-- EVENT AND WORKSHOP MANAGEMENT SYSTEM - DATABASE SCHEMA
-- ============================================================
-- Target Database: MySQL 8+
-- ============================================================

-- 1. Create and switch to the database
CREATE DATABASE IF NOT EXISTS event_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE event_management;

-- 2. Drop existing tables if restarting clean (in reverse foreign key order)
DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS users;

-- ============================================================
-- TABLE 1: users
-- Stores both students and administrators with hashed passwords.
-- ============================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 2: events
-- Stores campus events and workshops created by the administrator.
-- ============================================================
CREATE TABLE events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_date DATETIME NOT NULL,
    venue VARCHAR(150) NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 3: registrations
-- Links students to the events they registered for.
-- ticket_token holds a unique random identifier for the QR ticket.
-- UNIQUE (user_id, event_id) prevents duplicate registrations.
-- ============================================================
CREATE TABLE registrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    event_id INT NOT NULL,
    ticket_token VARCHAR(64) NOT NULL UNIQUE,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_event UNIQUE (user_id, event_id)
) ENGINE=InnoDB;


-- ============================================================
-- INITIAL SAMPLE DATA & ADMIN SETUP SCRIPT
-- ============================================================

-- Insert Admin and Student accounts
-- Passwords are encrypted with BCrypt (cost factor 12):
--   admin@college.edu   -> admin123
--   student@college.edu -> student123
INSERT INTO users (name, email, password, role) VALUES
('System Administrator', 'admin@college.edu', '$2a$12$huUkdHIU6eG2ki6SBZAlnu0WSthb033..rBenmpD/0lCiXtrKadCy', 'ADMIN'),
('Rahul Sharma', 'student@college.edu', '$2a$12$gyEHgsA1X3GvpSm.cXMjA.D.pUWH0dSxySMQjUb1y4jOq6Raf5kv2', 'STUDENT');

-- Insert Sample Upcoming Events
INSERT INTO events (title, description, event_date, venue, capacity) VALUES
('Full Stack Web Development Workshop', 
 'Hands-on practical session covering modern web application development with Servlets, JSP, and MySQL.', 
 '2026-11-15 10:00:00', 'Seminar Hall A', 50),

('AI & Machine Learning Seminar', 
 'Interactive seminar exploring recent advancements in Artificial Intelligence and Large Language Models.', 
 '2026-11-20 14:00:00', 'Auditorium 2', 100),

('Campus Coding Hackathon 2026', 
 'A 12-hour programming marathon testing algorithm skills and rapid software development.', 
 '2026-12-05 09:00:00', 'Computer Lab 3 & 4', 40);

-- Insert 1 Sample Registration for Student (Rahul) in Workshop #1
INSERT INTO registrations (user_id, event_id, ticket_token) VALUES
(2, 1, 'TICKET-A1B2C3D4E5F6G7H8');
