package com.aios.platform.system.service;

import com.aios.platform.security.UserPrincipal;
import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.mapper.SysPermissionMapper;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final SysPermissionMapper permissionMapper;
    private final RbacSyncService rbacSyncService;

    public boolean has(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return true;
        }
        UserPrincipal user = currentUser();
        if (user == null) {
            return false;
        }
        if (isSuperAdmin(user.getUserId())) {
            return true;
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permissionCode::equals);
    }

    public boolean hasAny(String... codes) {
        for (String c : codes) {
            if (has(c)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> currentPermissionCodes() {
        UserPrincipal user = currentUser();
        if (user == null) {
            return Set.of();
        }
        if (isSuperAdmin(user.getUserId())) {
            return new HashSet<>(rbacSyncService.allPermissionCodes());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
    }

    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return permissionMapper.countUserRoleCode(userId, SystemConstants.ROLE_SUPER_ADMIN) > 0;
    }

    public boolean currentIsSuperAdmin() {
        UserPrincipal user = currentUser();
        return user != null && isSuperAdmin(user.getUserId());
    }

    private static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }
}
