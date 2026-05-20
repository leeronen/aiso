package com.aios.platform.system.mapper;

import com.aios.platform.system.entity.SysPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select(
            """
            SELECT DISTINCT p.permission_code
            FROM sys_user_role ur
            INNER JOIN sys_role_permission rp ON ur.role_id = rp.role_id AND rp.deleted = 0
            INNER JOIN sys_permission p ON rp.permission_id = p.permission_id AND p.deleted = 0
            WHERE ur.user_id = #{userId} AND ur.deleted = 0 AND p.permission_code IS NOT NULL
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT permission_code FROM sys_permission WHERE deleted = 0 AND permission_code IS NOT NULL")
    List<String> selectAllCodes();

    /** Bypasses logic-delete filter so sync can revive or skip soft-deleted rows. */
    @Select(
            """
            SELECT permission_id, permission_name, permission_code, permission_type, deleted,
                   created_time, updated_time, created_user_id, updated_user_id
            FROM sys_permission
            WHERE permission_code = #{code}
            ORDER BY permission_id DESC
            LIMIT 1
            """)
    SysPermission selectByCodeIncludeDeleted(@Param("code") String code);

    @Select(
            """
            SELECT COUNT(1) FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.role_id AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND ur.deleted = 0 AND r.role_code = #{roleCode}
            """)
    int countUserRoleCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
