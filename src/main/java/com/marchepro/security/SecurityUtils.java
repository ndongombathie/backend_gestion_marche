package com.marchepro.security;

import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.utilisateur.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static Role currentRole() {
        return currentUser().getRole();
    }

    public static boolean hasRole(Role role) {
        return currentRole() == role;
    }

    public static boolean hasAnyRole(Role... roles) {
        Role current = currentRole();
        for (Role role : roles) {
            if (role == current) {
                return true;
            }
        }
        return false;
    }
}
