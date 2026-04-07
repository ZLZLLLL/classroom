package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.File;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.UserMapper;
import com.classroom.service.CourseService;
import com.classroom.service.FileService;
import com.classroom.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final FileService fileService;
    private final UserMapper userMapper;
    private final CourseService courseService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "3") Integer type,
            @RequestParam(defaultValue = "materials") String category,
            @RequestParam(defaultValue = "true") Boolean persist,
            Authentication authentication) throws IOException {

        User user = (User) authentication.getPrincipal();
        validateUploadPermission(user, courseId, category);
        File fileRecord = fileService.uploadFile(file, courseId, user.getId(), type, category, persist);
        return Result.success(convertToVO(fileRecord));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "获取课程文件列表")
    public Result<List<FileVO>> getCourseFiles(@PathVariable Long courseId,
                                                @RequestParam(required = false) Integer type) {
        List<File> files = fileService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<File>()
                .eq(File::getCourseId, courseId)
                .eq(type != null, File::getType, type)
                .orderByDesc(File::getCreateTime));

        return Result.success(files.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文件信息")
    public Result<FileVO> getFileById(@PathVariable Long id) {
        File file = fileService.getById(id);
        if (file == null) {
            return Result.notFound("文件不存在");
        }
        return Result.success(convertToVO(file));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载文件")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) throws IOException {
        // COS 模式：返回 302 重定向到短期签名 URL（对象数据走 COS，鉴权仍走后端）
        URL presignedUrl = fileService.generateDownloadUrl(id);
        if (presignedUrl != null) {
            // 302 不需要返回 body，这里复用 ResponseEntity<byte[]> 的泛型签名即可。
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presignedUrl.toString()))
                    .build();
        }

        // 本地模式：读取文件并回传
        java.io.File file = fileService.getFileById(id);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        File fileRecord = fileService.getById(id);
        byte[] content;
        try (FileInputStream fis = new FileInputStream(file)) {
            content = fis.readAllBytes();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileRecord.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取文件下载直链")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        URL presignedUrl = fileService.generateDownloadUrl(id);
        return Result.success(presignedUrl == null ? null : presignedUrl.toString());
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "预览文件")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long id) throws IOException {
        URL presignedUrl = fileService.generatePreviewUrl(id);
        if (presignedUrl != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presignedUrl.toString()))
                    .build();
        }

        java.io.File file = fileService.getFileById(id);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        File fileRecord = fileService.getById(id);
        byte[] content;
        try (FileInputStream fis = new FileInputStream(file)) {
            content = fis.readAllBytes();
        }

        String contentType = Files.probeContentType(file.toPath());
        MediaType mediaType;
        try {
            mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileRecord.getFileName() + "\"")
                .contentType(mediaType)
                .body(content);
    }

    @GetMapping("/{id}/preview-url")
    @Operation(summary = "获取文件预览直链")
    public Result<String> getPreviewUrl(@PathVariable Long id) {
        URL presignedUrl = fileService.generatePreviewUrl(id);
        return Result.success(presignedUrl == null ? null : presignedUrl.toString());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    public Result<?> deleteFile(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        fileService.deleteFileByActor(id, user);
        return Result.success();
    }

    private FileVO convertToVO(File file) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);
        vo.setFileUrl(fileService.buildAccessibleUrl(file.getFilePath()));
        User uploader = userMapper.selectById(file.getUploaderId());
        if (uploader != null) {
            vo.setUploaderName(uploader.getRealName() != null ? uploader.getRealName() : uploader.getUsername());
        }
        return vo;
    }

    private void validateUploadPermission(User user, Long courseId, String category) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            throw new BusinessException("用户身份无效");
        }

        String normalizedCategory = normalizeCategory(category);
        Integer role = user.getRole();

        if ("avatar".equals(normalizedCategory)) {
            return;
        }

        if ("course-cover".equals(normalizedCategory)) {
            if (role != 1 && role != 3) {
                throw new BusinessException("仅教师或管理员可上传课程封面");
            }
            if (courseId != null && role == 1 && !courseService.isTeacherCourseOwner(courseId, user.getId())) {
                throw new BusinessException("无权限为该课程上传封面");
            }
            return;
        }

        if ("materials".equals(normalizedCategory) || "homework-submit".equals(normalizedCategory)) {
            if (courseId == null) {
                throw new BusinessException("课程文件上传必须提供 courseId");
            }
            if (role == 3) {
                return;
            }
            if (role == 1 && courseService.isTeacherCourseOwner(courseId, user.getId())) {
                return;
            }
            if (role == 2 && courseService.isStudentInCourse(courseId, user.getId())) {
                return;
            }
            throw new BusinessException("无权限上传该课程文件");
        }

        throw new BusinessException("不支持的上传分类");
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "materials";
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "materials";
        }
        normalized = normalized.replaceAll("[^a-z0-9_-]", "-");
        return normalized.isEmpty() ? "materials" : normalized;
    }
}
