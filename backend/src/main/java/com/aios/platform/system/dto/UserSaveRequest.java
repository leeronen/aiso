package com.aios.platform.system.dto;

import lombok.Data;

@Data
public class UserSaveRequest {

    private String username;
    /** 新建必填；编辑时留空表示不修改密码 */
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 是否设为超级管理员 */
    private Boolean admin;
}
