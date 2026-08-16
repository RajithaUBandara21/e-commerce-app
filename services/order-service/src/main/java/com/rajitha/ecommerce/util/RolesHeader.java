package com.rajitha.ecommerce.util;

// Parses the comma-joined X-User-Roles header api-gateway forwards from the caller's
// JWT realm roles. Each service keeps its own copy of this tiny helper rather than a
// shared library — matches the existing per-service DTO duplication convention.
public final class RolesHeader {

    private RolesHeader() {
    }

    public static boolean isAdmin(String roles) {
        if (roles == null) {
            return false;
        }
        for (String role : roles.split(",")) {
            if (role.trim().equalsIgnoreCase("admin")) {
                return true;
            }
        }
        return false;
    }
}
