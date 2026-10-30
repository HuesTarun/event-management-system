package com.eventmanagement.servlet;

import com.eventmanagement.dao.*;
import com.eventmanagement.model.*;
import com.eventmanagement.util.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet(name = "EventManagementServlet", urlPatterns = {
        "/api/admin/events",
        "/api/admin/events/detail",
        "/api/admin/events/create",
        "/api/admin/events/edit",
        "/api/admin/events/delete",
        "/api/admin/registrations"
})
public class EventManagementServlet extends HttpServlet {

    private EventDAO eventDAO;
    private RegistrationDAO registrationDAO;

    @Override
    public void init() throws ServletException {
        this.eventDAO = new EventDAO();
        this.registrationDAO = new RegistrationDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        try {
            if ("/api/admin/registrations".equals(path)) {
                String idStr = request.getParameter("eventId");
                if (idStr == null || idStr.trim().isEmpty()) {
                    JsonUtil.writeError(response, 400, "Event ID parameter is required.");
                    return;
                }
                int eventId = Integer.parseInt(idStr.trim());
                Event event = eventDAO.findById(eventId);
                if (event == null) {
                    JsonUtil.writeError(response, 404, "Event not found.");
                    return;
                }

                List<Registration> registrations = registrationDAO.getRegistrationsByEvent(eventId);
                var regList = registrations.stream().map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("userName", r.getUserName());
                    map.put("userEmail", r.getUserEmail());
                    map.put("ticketToken", r.getTicketToken());
                    map.put("registeredAt", r.getRegisteredAt() != null ? r.getRegisteredAt().toString() : "");
                    return map;
                }).toList();

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("id", event.getId());
                eventData.put("title", event.getTitle());
                eventData.put("eventDate", event.getEventDate() != null ? event.getEventDate().toString() : "");
                eventData.put("capacity", event.getCapacity());

                Map<String, Object> result = new HashMap<>();
                result.put("event", eventData);
                result.put("registrations", regList);
                result.put("totalRegistered", registrations.size());

                JsonUtil.writeJson(response, result);
                return;
            }

            if ("/api/admin/events/detail".equals(path)) {
                // Get single event for editing
                int id = Integer.parseInt(request.getParameter("id"));
                Event event = eventDAO.findById(id);
                if (event == null) {
                    JsonUtil.writeError(response, 404, "Event not found.");
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("id", event.getId());
                data.put("title", event.getTitle());
                data.put("description", event.getDescription() != null ? event.getDescription() : "");
                data.put("eventDate", event.getEventDate() != null ? event.getEventDate().toString() : "");
                data.put("venue", event.getVenue());
                data.put("capacity", event.getCapacity());
                data.put("registeredCount", event.getRegisteredCount());

                JsonUtil.writeJson(response, data);
                return;
            }

            // Default: /api/admin/events - List all events
            List<Event> events = eventDAO.getAllEvents();

            var eventList = events.stream().map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("title", e.getTitle());
                map.put("description", e.getDescription() != null ? e.getDescription() : "");
                map.put("venue", e.getVenue());
                map.put("eventDate", e.getEventDate() != null ? e.getEventDate().toString() : "");
                map.put("capacity", e.getCapacity());
                map.put("registeredCount", e.getRegisteredCount());
                map.put("availableSeats", e.getAvailableSeats());
                map.put("full", e.isFull());
                map.put("past", e.isPast());
                return map;
            }).toList();

            JsonUtil.writeJson(response, Map.of("events", eventList));

        } catch (Exception e) {
            System.err.println("[EventManagementServlet] Error in doGet: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Server error loading events.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        try {
            if ("/api/admin/events/delete".equals(path)) {
                int id = Integer.parseInt(request.getParameter("id"));
                eventDAO.deleteEvent(id);
                JsonUtil.writeSuccess(response, "Event deleted successfully!");
                return;
            }

            // For both create and edit: extract and validate fields
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String eventDateStr = request.getParameter("eventDate");
            String venue = request.getParameter("venue");
            String capacityStr = request.getParameter("capacity");

            if (title != null) title = title.trim();
            if (description != null) description = description.trim();
            if (venue != null) venue = venue.trim();

            int capacity = Integer.parseInt(capacityStr);
            Timestamp eventDate = parseTimestamp(eventDateStr);

            Event event = new Event(title, description, eventDate, venue, capacity);

            if ("/api/admin/events/create".equals(path)) {
                eventDAO.createEvent(event);
                JsonUtil.writeSuccess(response, "Event created successfully!");
            } else if ("/api/admin/events/edit".equals(path)) {
                int id = Integer.parseInt(request.getParameter("id"));
                event.setId(id);
                eventDAO.updateEvent(event);
                JsonUtil.writeSuccess(response, "Event updated successfully!");
            }

        } catch (Exception e) {
            System.err.println("[EventManagementServlet] Error in doPost: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Failed to save event.");
        }
    }

    // Helper to safely parse datetime-local HTML input string to java.sql.Timestamp
    private Timestamp parseTimestamp(String dtStr) {
        if (dtStr == null || dtStr.trim().isEmpty()) {
            return new Timestamp(System.currentTimeMillis());
        }
        // HTML datetime-local format: "2026-11-20T14:30"
        String formatted = dtStr.replace("T", " ");
        if (formatted.length() == 16) {
            formatted += ":00"; // Add seconds if missing
        }
        return Timestamp.valueOf(formatted);
    }
}
