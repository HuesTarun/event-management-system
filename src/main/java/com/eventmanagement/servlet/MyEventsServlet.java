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


@WebServlet(name = "MyEventsServlet", urlPatterns = {"/api/my-events"})
public class MyEventsServlet extends HttpServlet {

    private RegistrationDAO registrationDAO;

    @Override
    public void init() throws ServletException {
        this.registrationDAO = new RegistrationDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            JsonUtil.writeError(response, 401, "Please log in to view your events.");
            return;
        }

        try {
            List<Registration> registrations = registrationDAO.getRegistrationsByStudent(user.getId());

            var regList = registrations.stream().map(r -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("eventId", r.getEventId());
                map.put("ticketToken", r.getTicketToken());
                map.put("registeredAt", r.getRegisteredAt() != null ? r.getRegisteredAt().toString() : "");
                map.put("eventTitle", r.getEventTitle());
                map.put("eventDate", r.getEventDate() != null ? r.getEventDate().toString() : "");
                map.put("eventVenue", r.getEventVenue());
                return map;
            }).toList();

            JsonUtil.writeJson(response, Map.of("registrations", regList));

        } catch (SQLException e) {
            System.err.println("[MyEventsServlet] Error: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Unable to load your registered events.");
        }
    }
}
