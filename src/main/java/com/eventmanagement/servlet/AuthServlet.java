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

@WebServlet(name = "AuthServlet", urlPatterns = {
        "/api/login",
        "/api/register",
        "/api/logout",
        "/api/login/status"
})
public class AuthServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/api/logout".equals(path)) {
            invalidateSession(request);
            JsonUtil.writeJson(response, Map.of("success", true, "message", "Logged out successfully."));
            return;
        }

        if ("/api/login/status".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User user = (User) session.getAttribute("user");
                JsonUtil.writeJson(response, Map.of(
                        "loggedIn", true,
                        "user", Map.of(
                                "id", user.getId(),
                                "name", user.getName(),
                                "email", user.getEmail(),
                                "role", user.getRole()
                        )
                ));
            } else {
                JsonUtil.writeJson(response, Map.of("loggedIn", false));
            }
            return;
        }

        JsonUtil.writeError(response, 405, "Method Not Allowed");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if ("/api/logout".equals(path)) {
            invalidateSession(request);
            JsonUtil.writeJson(response, Map.of("success", true, "message", "Logged out successfully."));
            return;
        }

        if ("/api/login".equals(path)) {
            handleLogin(request, response);
            return;
        }

        if ("/api/register".equals(path)) {
            handleRegister(request, response);
            return;
        }

        JsonUtil.writeError(response, 404, "Endpoint not found.");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (email != null) email = email.trim().toLowerCase();

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            JsonUtil.writeError(response, 400, "Please enter both email and password.");
            return;
        }

        try {
            User user = userDAO.findByEmail(email);
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
                JsonUtil.writeError(response, 401, "Invalid email or password.");
                return;
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);

            JsonUtil.writeJson(response, Map.of(
                    "success", true,
                    "message", "Login successful!",
                    "user", Map.of(
                            "id", user.getId(),
                            "name", user.getName(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    )
            ));

        } catch (SQLException e) {
            System.err.println("[AuthServlet] Login error: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Database error during login.");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (name != null) name = name.trim();
        if (email != null) email = email.trim().toLowerCase();

        if (name == null || name.length() < 2) {
            JsonUtil.writeError(response, 400, "Full name must be at least 2 characters.");
            return;
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JsonUtil.writeError(response, 400, "Please provide a valid email address.");
            return;
        }
        if (password == null || password.length() < 6) {
            JsonUtil.writeError(response, 400, "Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            JsonUtil.writeError(response, 400, "Passwords do not match.");
            return;
        }

        try {
            if (userDAO.emailExists(email)) {
                JsonUtil.writeError(response, 409, "This email is already registered. Please log in.");
                return;
            }

            String hashedPassword = PasswordUtil.hashPassword(password);
            User newUser = new User(name, email, hashedPassword, "STUDENT");
            int userId = userDAO.registerUser(newUser);

            if (userId > 0) {
                JsonUtil.writeJson(response, Map.of(
                        "success", true,
                        "message", "Registration successful! Please sign in with your credentials."
                ));
            } else {
                JsonUtil.writeError(response, 500, "Failed to create account.");
            }

        } catch (SQLException e) {
            System.err.println("[AuthServlet] Register error: " + e.getMessage());
            JsonUtil.writeError(response, 500, "Database error during registration.");
        }
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
