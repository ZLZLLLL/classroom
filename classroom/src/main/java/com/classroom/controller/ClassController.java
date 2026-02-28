package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.Class;
import com.classroom.service.ClassService;
import com.classroom.vo.ClassVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理")
public class ClassController {

    private final ClassService classService;

    @PostMapping
    @Operation(summary = "创建班级")
    public Result<ClassVO> createClass(@RequestBody ClassVO request) {
        Class aClass = new Class();
        BeanUtils.copyProperties(request, aClass);
        classService.save(aClass);
        return Result.success(convertToVO(aClass));
    }

    @GetMapping
    @Operation(summary = "获取班级列表")
    public Result<List<ClassVO>> getClassList() {
        List<Class> classes = classService.list();
        return Result.success(classes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取班级详情")
    public Result<ClassVO> getClassById(@PathVariable Long id) {
        Class aClass = classService.getById(id);
        if (aClass == null) {
            return Result.notFound("班级不存在");
        }
        return Result.success(convertToVO(aClass));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新班级")
    public Result<ClassVO> updateClass(@PathVariable Long id, @RequestBody ClassVO request) {
        Class aClass = classService.getById(id);
        if (aClass == null) {
            return Result.notFound("班级不存在");
        }
        BeanUtils.copyProperties(request, aClass);
        classService.updateById(aClass);
        return Result.success(convertToVO(aClass));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    public Result<?> deleteClass(@PathVariable Long id) {
        classService.removeById(id);
        return Result.success();
    }

    private ClassVO convertToVO(Class aClass) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(aClass, vo);
        return vo;
    }
}
