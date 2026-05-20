package com.aios.platform.kb.service;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.kb.config.KbStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class KbFileStorageService {

    private static final Set<String> ALLOWED_EXT =
            Set.of("txt", "md", "pdf", "doc", "docx", "html", "htm", "json", "csv");

    private final KbStorageProperties properties;

    public Path rootDir() {
        Path root = Path.of(properties.getKbUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException("无法创建上传目录: " + e.getMessage());
        }
        return root;
    }

    public StoredFile storeUpload(MultipartFile file, Long knowledgeBaseId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = extensionOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
        Path target = buildTargetPath(knowledgeBaseId, ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
        return new StoredFile(target, original, ext, file.getSize());
    }

    public StoredFile downloadFromUrl(String url, Long knowledgeBaseId, String suggestedName) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("请填写下载地址");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(URI.create(url.trim())).timeout(Duration.ofSeconds(60)).GET().build();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("URL 格式不正确");
        }
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            throw new BusinessException("下载失败: " + e.getMessage());
        }
        if (response.statusCode() >= 400) {
            throw new BusinessException("下载失败，HTTP " + response.statusCode());
        }
        String fileName = suggestedName != null && !suggestedName.isBlank() ? suggestedName : fileNameFromUrl(url);
        String ext = extensionOf(fileName);
        if (!ALLOWED_EXT.contains(ext)) {
            ext = "txt";
            if (!fileName.contains(".")) {
                fileName = fileName + ".txt";
            }
        }
        Path target = buildTargetPath(knowledgeBaseId, ext);
        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("保存下载文件失败: " + e.getMessage());
        }
        long size;
        try {
            size = Files.size(target);
        } catch (IOException e) {
            size = 0;
        }
        return new StoredFile(target, fileName, ext, size);
    }

    public String toPublicPath(Path stored) {
        return "/api/kb/documents/files/" + rootDir().relativize(stored.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    public Path resolvePublic(String relative) {
        Path resolved = rootDir().resolve(relative).normalize();
        if (!resolved.startsWith(rootDir())) {
            throw new BusinessException("非法文件路径");
        }
        if (!Files.exists(resolved)) {
            throw new BusinessException("文件不存在");
        }
        return resolved;
    }

    private Path buildTargetPath(Long knowledgeBaseId, String ext) {
        String kbPart = knowledgeBaseId != null ? String.valueOf(knowledgeBaseId) : "0";
        String name = UUID.randomUUID() + "." + ext;
        Path dir = rootDir().resolve(kbPart);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("无法创建知识库目录");
        }
        return dir.resolve(name);
    }

    private static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) {
            return "txt";
        }
        return name.substring(i + 1).toLowerCase();
    }

    private static String fileNameFromUrl(String url) {
        String path = URI.create(url.trim()).getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "download.txt";
        }
        int slash = path.lastIndexOf('/');
        return path.substring(slash + 1);
    }

    public record StoredFile(Path path, String originalName, String extension, long size) {}
}
