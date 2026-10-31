package com.eventmanagement.servlet;

import com.eventmanagement.dao.RegistrationDAO;
import com.eventmanagement.model.Registration;
import com.eventmanagement.util.JsonUtil;
import com.eventmanagement.util.QRCodeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeServlet - Streams dynamically generated ZXing QR code images and ticket pass data.
 *
 * GET /api/ticket/qr?token=TKT-XXXX
 * GET /ticket/qr?token=TKT-XXXX
 *   Returns: image/png binary stream of the QR Code
 *
 * GET /api/ticket/details?token=TKT-XXXX
 *   Returns: JSON ticket pass information
 */
@WebServlet(name = "QRCodeServlet", urlPatterns = {
        "/api/ticket/qr",
        "/ticket/qr",
        "/api/ticket/details"
})
public class QRCodeServlet extends HttpServlet {

    private RegistrationDAO registrationDAO;

    @Override
    public void init() throws ServletException {
        this.registrationDAO = new RegistrationDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String token = request.getParameter("token");

        if (token == null || token.trim().isEmpty()) {
            if ("/api/ticket/details".equals(path)) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing ticket token parameter.");
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing ticket token");
            }
            return;
        }

        token = token.trim();

        // 1. JSON details endpoint for digital passes
        if ("/api/ticket/details".equals(path)) {
            try {
                Registration reg = registrationDAO.findByTicketToken(token);
                if (reg == null) {
                    JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Ticket not found or invalid token.");
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("id", reg.getId());
                data.put("ticketToken", reg.getTicketToken());
                data.put("userName", reg.getUserName());
                data.put("userEmail", reg.getUserEmail());
                data.put("eventTitle", reg.getEventTitle());
                data.put("eventDate", reg.getEventDate() != null ? reg.getEventDate().toString() : "");
                data.put("eventVenue", reg.getEventVenue());
                data.put("registeredAt", reg.getRegisteredAt() != null ? reg.getRegisteredAt().toString() : "");

                JsonUtil.writeJson(response, data);

            } catch (SQLException e) {
                System.err.println("[QRCodeServlet] Database error: " + e.getMessage());
                JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving ticket.");
            }
            return;
        }

        // 2. Stream QR code binary image
        try {
            int size = 250;
            String sizeParam = request.getParameter("size");
            if (sizeParam != null && !sizeParam.trim().isEmpty()) {
                try {
                    int parsed = Integer.parseInt(sizeParam.trim());
                    if (parsed >= 100 && parsed <= 800) {
                        size = parsed;
                    }
                } catch (NumberFormatException ignored) {}
            }

            response.setContentType("image/png");
            response.setHeader("Cache-Control", "public, max-age=86400"); // Cache image for performance

            QRCodeUtil.generateQRCodeStream(token, size, size, response.getOutputStream());
            response.getOutputStream().flush();

        } catch (Exception e) {
            System.err.println("[QRCodeServlet] Error streaming QR code: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating QR code");
        }
    }
}

