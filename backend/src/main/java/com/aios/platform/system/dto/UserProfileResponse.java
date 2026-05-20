package com.aios.platform.system.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private Boolean admin;
    private List<String> permissions;
}
