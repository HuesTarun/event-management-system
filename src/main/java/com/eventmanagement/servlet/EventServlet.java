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

@WebServlet(name = "EventServlet", urlPatterns = {
        "/api/events",
        "/api/event-details",
        "/api/events/register"
})
public class EventServlet extends HttpServlet {

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
            if ("/api/event-details".equals(path)) {
                String idParam = request.getParameter("id");
                if (idParam == null || idParam.trim().isEmpty()) {
                    JsonUtil.writeError(response, 400, "Event ID is required.");
                    return;
                }

                int eventId = Integer.parseInt(idParam.trim());
                Event event = eventDAO.findById(eventId);
                if (event == null) {
                    JsonUtil.writeError(response, 404, "Event not found.");
                    return;
                }

                boolean alreadyRegistered = false;
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("user") != null) {
                    User user = (User) session.getAttribute("user");
                    if (user.isStudent()) {
                        alreadyRegistered = registrationDAO.isUserRegistered(user.getId(), eventId);
                    }
                }

                Map<String, Object> data = new HashMap<>();
                data.put("id", event.getId());
                data.put("title", event.getTitle());
                data.put("description", event.getDescription() != null ? event.getDescription() : "");
                data.put("eventDate", event.getEventDate() != null ? event.getEventDate().toString() : "");
                data.put("venue", event.getVenue());
                data.put("capacity", event.getCapacity());
                data.put("registeredCount", event.getRegisteredCount());
                data.put("availableSeats", event.getAvailableSeats());
                data.put("full", event.isFull());
                data.put("past", event.isPast());
                data.put("alreadyRegistered", alreadyRegistered);

                JsonUtil.writeJson(response, data);
                return;
            }

            // Default: GET /api/events
            List<Event> events = eventDAO.getAllEvents();
            var eventList = events.stream().map(e -> Map.of(
                    "id", e.getId(),
                    "title", e.getTitle(),
                    "description", e.getDescription() != null ? e.getDescription() : "",
                    "eventDate", e.getEventDate() != null ? e.getEventDate().toString() : "",
                    "venue", e.getVenue(),
                    "capacity", e.getCapacity(),
                    "registeredCount", e.getRegisteredCount(),
                    "availableSeats", e.getAvailableSeats(),
                    "full", e.isFull(),
                    "past", e.isPast()
            )).toList();

            JsonUtil.writeJson(response, Map.of("events", eventList));

        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "Invalid event ID.");
        } catch (SQLException e) {
            System.err.println("[EventServlet] Database error: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Unable to load events.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            JsonUtil.writeError(response, 401, "Please log in to register for events.");
            return;
        }

        if (!user.isStudent()) {
            JsonUtil.writeError(response, 403, "Only students can register for events.");
            return;
        }

        String eventIdStr = request.getParameter("eventId");
        if (eventIdStr == null || eventIdStr.trim().isEmpty()) {
            JsonUtil.writeError(response, 400, "Event ID is required.");
            return;
        }

        try {
            int eventId = Integer.parseInt(eventIdStr.trim());
            String ticketToken = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                    + "-" + (System.currentTimeMillis() % 100000);

            registrationDAO.registerForEvent(user.getId(), eventId, ticketToken);

            JsonUtil.writeJson(response, Map.of(
                    "success", true,
                    "message", "Registration confirmed! Your ticket pass is ready.",
                    "ticketToken", ticketToken
            ));

        } catch (IllegalStateException e) {
            JsonUtil.writeError(response, 409, e.getMessage());
        } catch (Exception e) {
            System.err.println("[EventServlet] Registration error: " + e.getMessage());
            JsonUtil.writeError(response, 500, "An error occurred during registration.");
        }
    }
}
