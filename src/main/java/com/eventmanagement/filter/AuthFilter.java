package com.eventmanagement.filter;

import com.eventmanagement.model.*;
import com.eventmanagement.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;

@WebFilter(filterName = "AuthFilter", urlPatterns = {
        "/admin/*",
        "/my-events",
        "/my-events.html",
        "/api/admin/*",
        "/api/my-events",
        "/api/events/register"
})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("[AuthFilter] RBAC filter initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());
        boolean isApiRequest = path.startsWith("/api/");

        // 1. Check if user is authenticated
        if (user == null) {
            if (isApiRequest) {
                JsonUtil.writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
            } else {
                res.sendRedirect(contextPath + "/login.html?error=auth_required");
            }
            return;
        }

        // 2. Protect Admin URLs: Only users with role 'ADMIN' can access
        if (path.startsWith("/admin/") || path.startsWith("/api/admin/")) {
            if (!user.isAdmin()) {
                if (isApiRequest) {
                    JsonUtil.writeError(res, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Administrator role required.");
                } else {
                    res.sendRedirect(contextPath + "/login.html?error=access_denied");
                }
                return;
            }
        }

        // 3. User is authenticated and authorized -> proceed
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
