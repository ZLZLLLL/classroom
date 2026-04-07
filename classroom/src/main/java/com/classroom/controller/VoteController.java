package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.VoteCreateRequest;
import com.classroom.dto.VoteSubmitRequest;
import com.classroom.entity.User;
import com.classroom.entity.Vote;
import com.classroom.service.VoteService;
import com.classroom.vo.VoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@Tag(name = "课程投票")
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师发起投票")
    public Result<VoteVO> createVote(@Valid @RequestBody VoteCreateRequest request,
                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Vote vote = voteService.createVote(request, user.getId());
        List<VoteVO> votes = voteService.getCourseVotes(vote.getCourseId(), user);
        VoteVO created = votes.stream().filter(v -> v.getId().equals(vote.getId())).findFirst().orElse(null);
        return Result.success(created);
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "获取课程投票列表")
    public Result<List<VoteVO>> getCourseVotes(@PathVariable Long courseId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(voteService.getCourseVotes(courseId, user));
    }

    @PostMapping("/{voteId}/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "学生参与投票")
    public Result<?> submitVote(@PathVariable Long voteId,
                                @Valid @RequestBody VoteSubmitRequest request,
                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        voteService.submitVote(voteId, request, user.getId());
        return Result.success();
    }

    @PutMapping("/{voteId}/close")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师结束投票")
    public Result<?> closeVote(@PathVariable Long voteId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        voteService.closeVote(voteId, user.getId());
        return Result.success();
    }
}

