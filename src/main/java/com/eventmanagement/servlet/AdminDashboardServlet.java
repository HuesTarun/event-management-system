package com.eventmanagement.servlet;

import com.eventmanagement.dao.*;
import com.eventmanagement.model.*;
import com.eventmanagement.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/api/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private EventDAO eventDAO;
    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int totalEvents = eventDAO.countEvents();
            int totalRegistrations = eventDAO.countRegistrations();
            int totalStudents = userDAO.countStudents();
            List<Event> recentEvents = eventDAO.getAllEvents();

            var eventList = recentEvents.stream().map(e -> Map.of(
                    "id", e.getId(),
                    "title", e.getTitle(),
                    "venue", e.getVenue(),
                    "eventDate", e.getEventDate() != null ? e.getEventDate().toString() : "",
                    "capacity", e.getCapacity(),
                    "registeredCount", e.getRegisteredCount()
            )).toList();

            JsonUtil.writeJson(response, Map.of(
                    "totalEvents", totalEvents,
                    "totalRegistrations", totalRegistrations,
                    "totalStudents", totalStudents,
                    "recentEvents", eventList
            ));

        } catch (SQLException e) {
            System.err.println("[AdminDashboardServlet] Error loading dashboard: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Error loading dashboard metrics.");
        }
    }
}
