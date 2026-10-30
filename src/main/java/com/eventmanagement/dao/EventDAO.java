package com.eventmanagement.dao;

import com.eventmanagement.model.Event;
import com.eventmanagement.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public List<Event> getAllEvents() throws SQLException {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT e.*, " +
                     "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.id) AS reg_count " +
                     "FROM events e ORDER BY e.event_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToEvent(rs));
            }
        }
        return list;
    }

    /**
     * Finds a single event by ID with registration count.
     */
    public Event findById(int id) throws SQLException {
        String sql = "SELECT e.*, " +
                     "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.id) AS reg_count " +
                     "FROM events e WHERE e.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEvent(rs);
                }
            }
        }
        return null;
    }

    /**
     * Creates a new event (Admin operation).
     */
    public int createEvent(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, event_date, venue, capacity) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setTimestamp(3, event.getEventDate());
            ps.setString(4, event.getVenue());
            ps.setInt(5, event.getCapacity());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        }
    }

    /**
     * Updates an existing event (Admin operation).
     */
    public boolean updateEvent(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, event_date = ?, venue = ?, capacity = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setTimestamp(3, event.getEventDate());
            ps.setString(4, event.getVenue());
            ps.setInt(5, event.getCapacity());
            ps.setInt(6, event.getId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes an event by ID (Admin operation).
     * Foreign keys cascade deletion to registrations.
     */
    public boolean deleteEvent(int id) throws SQLException {
        String sql = "DELETE FROM events WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Counts total events for admin dashboard.
     */
    public int countEvents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Counts total registrations across all events for admin dashboard.
     */
    public int countRegistrations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    // Helper method to map ResultSet to Event object
    private Event mapRowToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setEventDate(rs.getTimestamp("event_date"));
        event.setVenue(rs.getString("venue"));
        event.setCapacity(rs.getInt("capacity"));
        event.setCreatedAt(rs.getTimestamp("created_at"));
        event.setRegisteredCount(rs.getInt("reg_count"));
        return event;
    }
}
