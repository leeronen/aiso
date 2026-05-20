package com.aios.platform.system.mapper;

import com.aios.platform.system.entity.SysUserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select(
            """
            SELECT DISTINCT ur.user_id
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.role_id AND r.deleted = 0
            WHERE ur.deleted = 0 AND r.role_code = #{roleCode}
            """)
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
