package com.rajitha.ecommerce.util;

// Parses the comma-joined X-User-Roles header api-gateway forwards from the caller's
// JWT realm roles. Shared by ProductController and CategoryController — both live in
// this service, so this is ordinary intra-service reuse, not the cross-service shared
// library the project deliberately avoids (see CLAUDE.md).
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
