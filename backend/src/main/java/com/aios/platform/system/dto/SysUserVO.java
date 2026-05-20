package com.aios.platform.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    /** 是否超级管理员（拥有 SUPER_ADMIN 角色） */
    private Boolean admin;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}
