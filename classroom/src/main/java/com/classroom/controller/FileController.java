package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.File;
import com.classroom.entity.User;
import com.classroom.service.FileService;
import com.classroom.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "文件管理")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "3") Integer type,
            Authentication authentication) throws IOException {

        User user = (User) authentication.getPrincipal();
        File fileRecord = fileService.uploadFile(file, courseId, user.getId(), type);
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
        java.io.File file = fileService.getFileById(id);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }

        File fileRecord = fileService.getById(id);
        byte[] content = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(content);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileRecord.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件")
    public Result<?> deleteFile(@PathVariable Long id) {
        fileService.removeById(id);
        return Result.success();
    }

    private FileVO convertToVO(File file) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);
        return vo;
    }
}
