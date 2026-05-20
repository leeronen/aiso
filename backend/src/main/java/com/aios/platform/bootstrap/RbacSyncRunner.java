package com.aios.platform.bootstrap;

import com.aios.platform.system.service.RbacSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
public class RbacSyncRunner implements ApplicationRunner {

    private final RbacSyncService rbacSyncService;

    @Override
    public void run(ApplicationArguments args) {
        rbacSyncService.syncPermissionsToCatalog();
        rbacSyncService.syncSystemMenus();
    }
}
