package com.aios.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedTime", LocalDateTime.class, now);
        Long uid = currentUserIdOrZero();
        strictInsertFill(metaObject, "createdUserId", Long.class, uid);
        strictInsertFill(metaObject, "updatedUserId", Long.class, uid);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedUserId", Long.class, currentUserIdOrZero());
    }

    private static Long currentUserIdOrZero() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return 0L;
        }
        Object p = auth.getPrincipal();
        if (p instanceof com.aios.platform.security.UserPrincipal up) {
            return up.getUserId();
        }
        return 0L;
    }
}
