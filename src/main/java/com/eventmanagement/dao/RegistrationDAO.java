package com.eventmanagement.dao;

import com.eventmanagement.model.Registration;
import com.eventmanagement.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDAO {

    /**
     * Checks if a student is already registered for an event.
     */
    public boolean isUserRegistered(int userId, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE user_id = ? AND event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int registerForEvent(int userId, int eventId, String ticketToken) 
            throws SQLException, IllegalStateException {

        String checkSql = "SELECT e.capacity, e.event_date, " +
                          "(SELECT COUNT(*) FROM registrations WHERE event_id = e.id) AS reg_count, " +
                          "(SELECT COUNT(*) FROM registrations WHERE user_id = ? AND event_id = e.id) AS user_registered " +
                          "FROM events e WHERE e.id = ? FOR UPDATE";

        String insertSql = "INSERT INTO registrations (user_id, event_id, ticket_token) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // 1. Check capacity, date, and duplicate in an atomic query
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setInt(1, userId);
                    checkPs.setInt(2, eventId);

                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Event not found.");
                        }

                        int capacity = rs.getInt("capacity");
                        Timestamp eventDate = rs.getTimestamp("event_date");
                        int regCount = rs.getInt("reg_count");
                        int userRegistered = rs.getInt("user_registered");

                        // Check if event has already passed
                        if (eventDate != null && eventDate.before(new Timestamp(System.currentTimeMillis()))) {
                            throw new IllegalStateException("Registration closed: This event has already taken place.");
                        }

                        // Check if student already registered
                        if (userRegistered > 0) {
                            throw new IllegalStateException("You are already registered for this event.");
                        }

                        // Check capacity
                        if (regCount >= capacity) {
                            throw new IllegalStateException("Registration full: No seats remaining for this event.");
                        }
                    }
                }

                // 2. Insert new registration record
                int registrationId = -1;
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertPs.setInt(1, userId);
                    insertPs.setInt(2, eventId);
                    insertPs.setString(3, ticketToken);

                    int rows = insertPs.executeUpdate();
                    if (rows > 0) {
                        try (ResultSet rs = insertPs.getGeneratedKeys()) {
                            if (rs.next()) {
                                registrationId = rs.getInt(1);
                            }
                        }
                    }
                }

                conn.commit(); // Commit transaction
                return registrationId;

            } catch (Exception e) {
                conn.rollback(); // Rollback if any check fails
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new SQLException("Failed to register: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves all registrations for a specific student, joined with event details.
     */
    public List<Registration> getRegistrationsByStudent(int userId) throws SQLException {
        List<Registration> list = new ArrayList<>();
        String sql = "SELECT r.id, r.user_id, r.event_id, r.ticket_token, r.registered_at, " +
                     "e.title AS event_title, e.event_date, e.venue AS event_venue " +
                     "FROM registrations r " +
                     "JOIN events e ON r.event_id = e.id " +
                     "WHERE r.user_id = ? " +
                     "ORDER BY e.event_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Registration reg = new Registration();
                    reg.setId(rs.getInt("id"));
                    reg.setUserId(rs.getInt("user_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setTicketToken(rs.getString("ticket_token"));
                    reg.setRegisteredAt(rs.getTimestamp("registered_at"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setEventVenue(rs.getString("event_venue"));
                    list.add(reg);
                }
            }
        }
        return list;
    }

    /**
     * Finds a registration by its unique ticket token.
     */
    public Registration findByTicketToken(String token) throws SQLException {
        String sql = "SELECT r.id, r.user_id, r.event_id, r.ticket_token, r.registered_at, " +
                     "u.name AS user_name, u.email AS user_email, " +
                     "e.title AS event_title, e.event_date, e.venue AS event_venue " +
                     "FROM registrations r " +
                     "JOIN users u ON r.user_id = u.id " +
                     "JOIN events e ON r.event_id = e.id " +
                     "WHERE r.ticket_token = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Registration reg = new Registration();
                    reg.setId(rs.getInt("id"));
                    reg.setUserId(rs.getInt("user_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setTicketToken(rs.getString("ticket_token"));
                    reg.setRegisteredAt(rs.getTimestamp("registered_at"));
                    reg.setUserName(rs.getString("user_name"));
                    reg.setUserEmail(rs.getString("user_email"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setEventVenue(rs.getString("event_venue"));
                    return reg;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all registrations for an event (used by Admin to view attendee roster).
     */
    public List<Registration> getRegistrationsByEvent(int eventId) throws SQLException {
        List<Registration> list = new ArrayList<>();
        String sql = "SELECT r.id, r.user_id, r.event_id, r.ticket_token, r.registered_at, " +
                     "u.name AS user_name, u.email AS user_email, " +
                     "e.title AS event_title, e.event_date, e.venue AS event_venue " +
                     "FROM registrations r " +
                     "JOIN users u ON r.user_id = u.id " +
                     "JOIN events e ON r.event_id = e.id " +
                     "WHERE r.event_id = ? " +
                     "ORDER BY u.name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Registration reg = new Registration();
                    reg.setId(rs.getInt("id"));
                    reg.setUserId(rs.getInt("user_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setTicketToken(rs.getString("ticket_token"));
                    reg.setRegisteredAt(rs.getTimestamp("registered_at"));
                    reg.setUserName(rs.getString("user_name"));
                    reg.setUserEmail(rs.getString("user_email"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setEventVenue(rs.getString("event_venue"));
                    list.add(reg);
                }
            }
        }
        return list;
    }
}
