package com.aios.platform.system.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.dto.SysUserVO;
import com.aios.platform.system.dto.UserSaveRequest;
import com.aios.platform.system.entity.SysRole;
import com.aios.platform.system.entity.SysUser;
import com.aios.platform.system.entity.SysUserRole;
import com.aios.platform.system.mapper.SysRoleMapper;
import com.aios.platform.system.mapper.SysUserMapper;
import com.aios.platform.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<SysUserVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(
                    w ->
                            w.like(SysUser::getUsername, keyword)
                                    .or()
                                    .like(SysUser::getNickname, keyword)
                                    .or()
                                    .like(SysUser::getEmail, keyword));
        }
        q.orderByDesc(SysUser::getCreatedTime);
        Page<SysUser> userPage = userMapper.selectPage(new Page<>(current, size), q);
        Set<Long> adminUserIds = loadAdminUserIds();

        Page<SysUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(
                userPage.getRecords().stream()
                        .map(u -> toVO(u, adminUserIds.contains(u.getUserId())))
                        .collect(Collectors.toList()));
        return voPage;
    }

    public SysUserVO getDetail(Long id) {
        SysUser u = getEntity(id);
        return toVO(u, isAdmin(u.getUserId()));
    }

    @Transactional
    public Long create(UserSaveRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        Long c =
                userMapper.selectCount(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (c != null && c > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setAvatar(req.getAvatar());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        userMapper.insert(user);

        setAdminRole(user.getUserId(), Boolean.TRUE.equals(req.getAdmin()));
        return user.getUserId();
    }

    @Transactional
    public void update(Long userId, UserSaveRequest req) {
        SysUser db = getEntity(userId);
        db.setNickname(req.getNickname());
        db.setEmail(req.getEmail());
        db.setPhone(req.getPhone());
        db.setAvatar(req.getAvatar());
        if (req.getStatus() != null) {
            db.setStatus(req.getStatus());
        }
        userMapper.updateById(db);

        if (req.getAdmin() != null) {
            setAdminRole(userId, req.getAdmin());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            resetPassword(userId, req.getPassword());
        }
    }

    @Transactional
    public void resetPassword(Long userId, String rawPassword) {
        SysUser db = getEntity(userId);
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        db.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(db);
    }

    @Transactional
    public void delete(Long userId) {
        userMapper.deleteById(userId);
        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    private SysUser getEntity(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        return u;
    }

    private Set<Long> loadAdminUserIds() {
        List<Long> ids = userRoleMapper.selectUserIdsByRoleCode(SystemConstants.ROLE_SUPER_ADMIN);
        return new HashSet<>(ids);
    }

    private boolean isAdmin(Long userId) {
        return loadAdminUserIds().contains(userId);
    }

    private SysRole requireSuperAdminRole() {
        SysRole role =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleCode, SystemConstants.ROLE_SUPER_ADMIN));
        if (role == null) {
            throw new BusinessException("系统未初始化 SUPER_ADMIN 角色，请重启后端完成数据初始化");
        }
        return role;
    }

    private void setAdminRole(Long userId, boolean admin) {
        SysRole superRole = requireSuperAdminRole();
        Long roleId = superRole.getRoleId();

        SysUserRole existing =
                userRoleMapper.selectOne(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, userId)
                                .eq(SysUserRole::getRoleId, roleId));

        if (admin) {
            if (existing == null) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        } else if (existing != null) {
            userRoleMapper.deleteById(existing.getId());
        }
    }

    private static SysUserVO toVO(SysUser u, boolean admin) {
        SysUserVO vo = new SysUserVO();
        vo.setUserId(u.getUserId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setEmail(u.getEmail());
        vo.setPhone(u.getPhone());
        vo.setAvatar(u.getAvatar());
        vo.setStatus(u.getStatus());
        vo.setAdmin(admin);
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setLastLoginIp(u.getLastLoginIp());
        return vo;
    }
}
