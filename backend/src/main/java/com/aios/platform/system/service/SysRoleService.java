package com.aios.platform.system.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.dto.RoleSaveRequest;
import com.aios.platform.system.dto.SysRoleVO;
import com.aios.platform.system.entity.SysRole;
import com.aios.platform.system.entity.SysRolePermission;
import com.aios.platform.system.mapper.SysRoleMapper;
import com.aios.platform.system.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public Page<SysRole> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysRole> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(
                    w ->
                            w.like(SysRole::getRoleName, keyword)
                                    .or()
                                    .like(SysRole::getRoleCode, keyword));
        }
        q.orderByDesc(SysRole::getCreatedTime);
        return roleMapper.selectPage(new Page<>(current, size), q);
    }

    public SysRoleVO getDetail(Long roleId) {
        SysRole role = get(roleId);
        SysRoleVO vo = toVO(role);
        List<SysRolePermission> rps =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        vo.setPermissionIds(rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList()));
        return vo;
    }

    @Transactional
    public Long create(RoleSaveRequest req) {
        Long c =
                roleMapper.selectCount(
                        new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, req.getRoleCode()));
        if (c != null && c > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleName(req.getRoleName());
        role.setRoleCode(req.getRoleCode());
        role.setDescription(req.getDescription());
        role.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        roleMapper.insert(role);
        assignPermissions(role.getRoleId(), req.getPermissionIds());
        return role.getRoleId();
    }

    @Transactional
    public void update(Long roleId, RoleSaveRequest req) {
        SysRole role = get(roleId);
        if (SystemConstants.ROLE_SUPER_ADMIN.equals(role.getRoleCode())) {
            throw new BusinessException("不能修改超级管理员角色编码");
        }
        role.setRoleName(req.getRoleName());
        role.setDescription(req.getDescription());
        if (req.getStatus() != null) {
            role.setStatus(req.getStatus());
        }
        roleMapper.updateById(role);
        if (req.getPermissionIds() != null) {
            assignPermissions(roleId, req.getPermissionIds());
        }
    }

    @Transactional
    public void delete(Long roleId) {
        SysRole role = get(roleId);
        if (SystemConstants.ROLE_SUPER_ADMIN.equals(role.getRoleCode())) {
            throw new BusinessException("不能删除超级管理员角色");
        }
        roleMapper.deleteById(roleId);
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
    }

    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds == null) {
            return;
        }
        for (Long pid : permissionIds) {
            if (pid == null) {
                continue;
            }
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }

    private SysRole get(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private static SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        return vo;
    }
}
