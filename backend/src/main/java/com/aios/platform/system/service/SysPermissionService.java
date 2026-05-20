package com.aios.platform.system.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.system.entity.SysPermission;
import com.aios.platform.system.mapper.SysPermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    public Page<SysPermission> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysPermission> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            q.and(
                    w ->
                            w.like(SysPermission::getPermissionCode, keyword)
                                    .or()
                                    .like(SysPermission::getPermissionName, keyword));
        }
        q.orderByAsc(SysPermission::getPermissionCode);
        return permissionMapper.selectPage(new Page<>(current, size), q);
    }

    public SysPermission get(Long id) {
        SysPermission p = permissionMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("权限不存在");
        }
        return p;
    }

    @Transactional
    public Long save(SysPermission body) {
        if (body.getPermissionId() == null) {
            Long c =
                    permissionMapper.selectCount(
                            new LambdaQueryWrapper<SysPermission>()
                                    .eq(SysPermission::getPermissionCode, body.getPermissionCode()));
            if (c != null && c > 0) {
                throw new BusinessException("权限编码已存在");
            }
            if (body.getPermissionType() == null) {
                body.setPermissionType("api");
            }
            permissionMapper.insert(body);
            return body.getPermissionId();
        }
        SysPermission db = get(body.getPermissionId());
        db.setPermissionName(body.getPermissionName());
        db.setPermissionType(body.getPermissionType());
        permissionMapper.updateById(db);
        return db.getPermissionId();
    }

    @Transactional
    public void delete(Long id) {
        permissionMapper.deleteById(id);
    }
}
