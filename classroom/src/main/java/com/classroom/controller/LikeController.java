package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.LikeCreateRequest;
import com.classroom.entity.Like;
import com.classroom.entity.User;
import com.classroom.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
@Tag(name = "点赞管理")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    @Operation(summary = "点赞")
    public Result<Like> addLike(@Valid @RequestBody LikeCreateRequest request,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Like like = likeService.addLike(request, user.getId());
        return Result.success(like);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "取消点赞")
    public Result<?> removeLike(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        likeService.removeLike(id, user.getId());
        return Result.success();
    }

    @GetMapping("/answer/{answerId}/count")
    @Operation(summary = "获取回答点赞数")
    public Result<Long> getAnswerLikeCount(@PathVariable Long answerId) {
        Long count = likeService.getAnswerLikeCount(answerId);
        return Result.success(count);
    }
}
