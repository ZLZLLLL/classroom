package com.classroom.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.config.TencentCosProperties;
import com.classroom.entity.File;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileMapper, File> {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private final TencentCosProperties cosProperties;

    /**
     * 可选注入：当启用 tencent.cos.enabled=true 且 COSClient Bean 存在时生效。
     */
    @Nullable
    private final CosService cosService;

    public File uploadFile(MultipartFile file, Long courseId, Long uploaderId, Integer type) throws IOException {
        String originalFilename = StringUtils.defaultString(file.getOriginalFilename(), "file");
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 按日期组织目录（同时也作为 objectKey 的目录）
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String newFileName = UUID.randomUUID() + extension;

        // 统一用 filePath 字段保存“相对 key/路径”（本地=相对路径，COS=objectKey）
        String relativePath = datePath + "/" + newFileName;

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
            if (!uploadPath.exists()) {
                uploadPath.mkdirs();
            }

            java.io.File dateDir = new java.io.File(uploadDir, datePath);
            if (!dateDir.exists()) {
                dateDir.mkdirs();
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

        this.save(fileRecord);
        return fileRecord;
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

    public void deleteStoredObjectIfNeeded(File fileRecord) {
        if (fileRecord == null) {
            return;
        }
        if (isCosEnabled() && cosService != null && StringUtils.isNotBlank(fileRecord.getFilePath())) {
            cosService.deleteObjectQuietly(fileRecord.getFilePath());
        }
    }
}
