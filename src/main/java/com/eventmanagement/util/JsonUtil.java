package com.eventmanagement.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;


public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();

    /**
     * Writes a Java object as a JSON HTTP response.
     */
    public static void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(data));
    }

    /**
     * Writes a JSON HTTP response with a specific HTTP status code.
     */
    public static void writeJson(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        writeJson(response, data);
    }

    /**
     * Writes a simple JSON success message.
     */
    public static void writeSuccess(HttpServletResponse response, String message) throws IOException {
        writeJson(response, Map.of("success", true, "message", message));
    }

    /**
     * Writes a simple JSON error message with a given HTTP status code.
     */
    public static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, Map.of("success", false, "error", message));
    }
}
