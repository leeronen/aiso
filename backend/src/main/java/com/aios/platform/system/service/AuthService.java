package com.aios.platform.system.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.security.JwtService;
import com.aios.platform.security.UserPrincipal;
import com.aios.platform.system.dto.LoginRequest;
import com.aios.platform.system.dto.RegisterRequest;
import com.aios.platform.system.dto.TokenResponse;
import com.aios.platform.system.dto.UserProfileResponse;
import com.aios.platform.system.entity.SysLoginLog;
import com.aios.platform.system.entity.SysUser;
import com.aios.platform.system.mapper.SysLoginLogMapper;
import com.aios.platform.system.mapper.SysPermissionMapper;
import com.aios.platform.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final AuthorizationService authorizationService;
    private final RbacSyncService rbacSyncService;
    private final SysLoginLogMapper loginLogMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse login(LoginRequest req, HttpServletRequest http) {
        try {
            Authentication auth =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
            UserPrincipal up = (UserPrincipal) auth.getPrincipal();
            writeLoginLog(up.getUserId(), up.getUsername(), http, 1, "OK");
            touchLogin(up.getUserId(), http);
            return tokensFor(up.getUserId(), up.getUsername());
        } catch (Exception ex) {
            SysUser u =
                    userMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
            writeLoginLog(u != null ? u.getUserId() : null, req.getUsername(), http, 0, ex.getMessage());
            throw ex;
        }
    }

    private void touchLogin(Long userId, HttpServletRequest http) {
        SysUser patch = new SysUser();
        patch.setUserId(userId);
        patch.setLastLoginTime(LocalDateTime.now());
        patch.setLastLoginIp(clientIp(http));
        userMapper.updateById(patch);
    }

    private void writeLoginLog(Long userId, String username, HttpServletRequest http, int ok, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(clientIp(http));
        log.setUserAgent(http.getHeader("User-Agent"));
        log.setSuccess(ok);
        log.setMessage(msg != null && msg.length() > 250 ? msg.substring(0, 250) : msg);
        log.setCreatedTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private static String clientIp(HttpServletRequest http) {
        String xf = http.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtService.parse(refreshToken);
        if (!jwtService.isRefreshToken(claims)) {
            throw new BusinessException(401, "无效的刷新令牌");
        }
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        SysUser u = userMapper.selectById(userId);
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            throw new BusinessException(401, "用户不可用");
        }
        return tokensFor(userId, username);
    }

    private TokenResponse tokensFor(Long userId, String username) {
        String access = jwtService.createAccessToken(userId, username);
        String refresh = jwtService.createRefreshToken(userId, username);
        long exp = jwtService.parse(access).getExpiration().getTime() - System.currentTimeMillis();
        return new TokenResponse(access, refresh, Math.max(0, exp / 1000));
    }

    @Transactional
    public void register(RegisterRequest req) {
        Long c =
                userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (c != null && c > 0) {
            throw new BusinessException("用户名已存在");
        }
        Long ce = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, req.getEmail()));
        if (ce != null && ce > 0) {
            throw new BusinessException("邮箱已被注册");
        }
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setEmail(req.getEmail());
        u.setNickname(req.getUsername());
        u.setStatus(1);
        userMapper.insert(u);
    }

    public UserProfileResponse me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            throw new BusinessException(401, "未登录");
        }
        SysUser u = userMapper.selectById(up.getUserId());
        boolean admin = authorizationService.isSuperAdmin(up.getUserId());
        List<String> perms =
                admin
                        ? rbacSyncService.allPermissionCodes()
                        : permissionMapper.selectPermissionCodesByUserId(up.getUserId());
        return UserProfileResponse.builder()
                .userId(up.getUserId())
                .username(up.getUsername())
                .nickname(u != null ? u.getNickname() : null)
                .email(u != null ? u.getEmail() : null)
                .admin(admin)
                .permissions(perms)
                .build();
    }
}
