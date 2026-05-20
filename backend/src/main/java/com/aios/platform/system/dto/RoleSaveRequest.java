package com.aios.platform.system.dto;

import java.util.List;
import lombok.Data;

@Data
public class RoleSaveRequest {

    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
    private List<Long> permissionIds;
}
