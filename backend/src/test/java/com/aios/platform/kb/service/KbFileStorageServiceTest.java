package com.aios.platform.kb.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.config.KbStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KbFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private KbFileStorageService service;

    @BeforeEach
    void setUp() {
        KbStorageProperties props = new KbStorageProperties();
        props.setKbUploadDir(tempDir.toString());
        service = new KbFileStorageService(props);
    }

    @Test
    void resolvePublic_rejectsPathTraversal() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.resolvePublic("../../../etc/passwd"));
        assertTrue(ex.getMessage().contains("非法"));
    }

    @Test
    void resolvePublic_returnsExistingFile() throws Exception {
        Path kbDir = tempDir.resolve("1");
        Files.createDirectories(kbDir);
        Path file = kbDir.resolve("test.txt");
        Files.writeString(file, "content");
        Path resolved = service.resolvePublic("1/test.txt");
        assertTrue(Files.exists(resolved));
        assertTrue(resolved.startsWith(service.rootDir()));
    }
}
