package com.classroom.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.config.TencentCosProperties;
import com.classroom.entity.File;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.FileMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class FileService extends ServiceImpl<FileMapper, File> {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private final TencentCosProperties cosProperties;

    /**
     * 可选注入：当启用 tencent.cos.enabled=true 且 COSClient Bean 存在时生效。
     */
    @Nullable
    private final CosService cosService;

    public File uploadFile(MultipartFile file,
                           Long courseId,
                           Long uploaderId,
                           Integer type,
                           String category,
                           Boolean persistRecord) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            originalFilename = "file";
        }
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 按日期组织目录（同时也作为 objectKey 的目录）
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFileName = UUID.randomUUID() + extension;
        String safeCategory = normalizeCategory(category);

        // 统一用 filePath 字段保存“相对 key/路径”（本地=相对路径，COS=objectKey）
        String relativePath = safeCategory + "/" + datePath + "/" + newFileName;

        // 启用 COS 时：上传到 COS；否则：落盘到本地 uploadDir
        if (isCosEnabled()) {
            if (cosService == null) {
                throw new IllegalStateException("已启用 tencent.cos.enabled=true，但 COSClient/CosService 未正确初始化");
            }
            try (var in = file.getInputStream()) {
                cosService.putObject(relativePath, in, file.getSize(), file.getContentType());
            }
        } else {
            // 创建上传目录
            java.io.File uploadPath = new java.io.File(uploadDir);
            if (!uploadPath.exists() && !uploadPath.mkdirs()) {
                throw new IllegalStateException("创建上传目录失败: " + uploadDir);
            }

            java.io.File dateDir = new java.io.File(uploadDir, datePath);
            if (!dateDir.exists() && !dateDir.mkdirs()) {
                throw new IllegalStateException("创建日期目录失败: " + dateDir.getAbsolutePath());
            }

            java.io.File destFile = new java.io.File(dateDir, newFileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(file.getBytes());
            }
        }

        // 保存记录
        File fileRecord = new File();
        fileRecord.setCourseId(courseId);
        fileRecord.setUploaderId(uploaderId);
        fileRecord.setType(type);
        fileRecord.setFileName(originalFilename);
        fileRecord.setFilePath(relativePath);
        fileRecord.setFileSize(file.getSize());
        fileRecord.setFileType(extension.startsWith(".") ? extension.substring(1) : extension);

        if (Boolean.TRUE.equals(persistRecord)) {
            this.save(fileRecord);
        }
        return fileRecord;
    }

    public String buildAccessibleUrl(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        if (isCosEnabled()) {
            String domain = StringUtils.trimToEmpty(cosProperties.getDomain());
            if (StringUtils.isNotBlank(domain) && cosService != null) {
                String key = cosService.normalizeKey(filePath);
                String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
                return normalizedDomain + "/" + key;
            }
            if (cosService != null) {
                URL presigned = cosService.generatePresignedPreviewUrl(filePath);
                return presigned == null ? null : presigned.toString();
            }
        }
        return null;
    }

    public java.io.File getFile(String filePath) {
        return new java.io.File(uploadDir, filePath);
    }

    public java.io.File getFileById(Long id) {
        File fileRecord = this.getById(id);
        if (fileRecord == null) {
            return null;
        }
        return getFile(fileRecord.getFilePath());
    }

    /**
     * 生成下载链接：
     * - 启用 COS：返回短期预签名 URL
     * - 未启用 COS：返回 null（由 Controller 走本地字节流下载）
     */
    public URL generateDownloadUrl(Long id) {
        if (!isCosEnabled()) {
            return null;
        }
        if (cosService == null) {
            throw new IllegalStateException("已启用 tencent.cos.enabled=true，但 CosService 未正确初始化");
        }
        File fileRecord = this.getById(id);
        if (fileRecord == null) {
            return null;
        }
        return cosService.generatePresignedDownloadUrl(fileRecord.getFilePath(), fileRecord.getFileName());
    }

    public URL generatePreviewUrl(Long id) {
        if (!isCosEnabled()) {
            return null;
        }
        if (cosService == null) {
            throw new IllegalStateException("已启用 tencent.cos.enabled=true，但 CosService 未正确初始化");
        }
        File fileRecord = this.getById(id);
        if (fileRecord == null) {
            return null;
        }
        return cosService.generatePresignedPreviewUrl(fileRecord.getFilePath());
    }

    public boolean isCosEnabled() {
        return cosProperties.isEnabled();
    }

    private String normalizeCategory(String category) {
        if (StringUtils.isBlank(category)) {
            return "materials";
        }
        String normalized = category.trim().toLowerCase();
        // 仅允许安全字符，避免路径穿越
        normalized = normalized.replaceAll("[^a-z0-9_-]", "-");
        return StringUtils.isBlank(normalized) ? "materials" : normalized;
    }

    public void deleteStoredObjectIfNeeded(File fileRecord) {
        if (fileRecord == null) {
            return;
        }
        if (isCosEnabled() && cosService != null && StringUtils.isNotBlank(fileRecord.getFilePath())) {
            cosService.deleteObjectQuietly(fileRecord.getFilePath());
        }
    }

    @SuppressWarnings("unused")
    public void deleteFileByActor(Long fileId, User actor) {
        File record = this.getById(fileId);
        if (record == null) {
            throw new BusinessException("文件不存在");
        }
        if (!canDelete(record, actor)) {
            throw new BusinessException("无权限删除该文件");
        }
        deleteStoredObjectIfNeeded(record);
        this.removeById(fileId);
    }

    private boolean canDelete(File record, User actor) {
        if (record == null || actor == null || actor.getRole() == null) {
            return false;
        }
        if (actor.getRole() == 3) {
            return true;
        }
        return actor.getId() != null && actor.getId().equals(record.getUploaderId());
    }
}
