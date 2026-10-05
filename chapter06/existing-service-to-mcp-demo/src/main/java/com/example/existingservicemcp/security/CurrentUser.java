package com.example.existingservicemcp.security;

import java.util.Set;

public record CurrentUser(
    String userId,
    String tenantId,
    Set<String> roles
) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
