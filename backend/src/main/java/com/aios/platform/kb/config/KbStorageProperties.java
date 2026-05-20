package com.aios.platform.kb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aios.storage")
public class KbStorageProperties {

    /** 知识库文档本地上传目录 */
    private String kbUploadDir = "./data/kb-uploads";
}
