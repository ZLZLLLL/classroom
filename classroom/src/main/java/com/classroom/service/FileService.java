package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.File;
import com.classroom.repository.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileMapper, File> {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public File uploadFile(MultipartFile file, Long courseId, Long uploaderId, Integer type) throws IOException {
        // 创建上传目录
        java.io.File uploadPath = new java.io.File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        // 按日期组织目录
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        java.io.File dateDir = new java.io.File(uploadDir, datePath);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }

        // 保存文件
        java.io.File destFile = new java.io.File(dateDir, newFileName);
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            fos.write(file.getBytes());
        }

        // 保存记录
        File fileRecord = new File();
        fileRecord.setCourseId(courseId);
        fileRecord.setUploaderId(uploaderId);
        fileRecord.setType(type);
        fileRecord.setFileName(originalFilename);
        fileRecord.setFilePath(datePath + "/" + newFileName);
        fileRecord.setFileSize(file.getSize());
        fileRecord.setFileType(extension.substring(1));

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
}
