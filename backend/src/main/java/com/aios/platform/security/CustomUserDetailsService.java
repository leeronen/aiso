package com.aios.platform.security;

import com.aios.platform.system.SystemConstants;
import com.aios.platform.system.entity.SysUser;
import com.aios.platform.system.mapper.SysPermissionMapper;
import com.aios.platform.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser u =
                userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (u == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<String> codes;
        if (permissionMapper.countUserRoleCode(u.getUserId(), SystemConstants.ROLE_SUPER_ADMIN) > 0) {
            codes = permissionMapper.selectAllCodes();
        } else {
            codes = permissionMapper.selectPermissionCodesByUserId(u.getUserId());
        }
        var auths = codes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        boolean enabled = u.getStatus() != null && u.getStatus() == 1;
        return new UserPrincipal(u.getUserId(), u.getUsername(), u.getPassword(), auths, enabled);
    }
}
