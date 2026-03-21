package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.AnswerCreateRequest;
import com.classroom.dto.AnswerReviewRequest;
import com.classroom.dto.AiGradeSuggestionRequest;
import com.classroom.dto.AiGradeSuggestionResponse;
import com.classroom.entity.Answer;
import com.classroom.entity.User;
import com.classroom.repository.UserMapper;
import com.classroom.service.AnswerService;
import com.classroom.vo.AnswerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
@Tag(name = "回答管理")
public class AnswerController {

    private final AnswerService answerService;
    private final UserMapper userMapper;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "学生回答")
    public Result<AnswerVO> createAnswer(@Valid @RequestBody AnswerCreateRequest request,
                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Answer answer = answerService.createAnswer(request, user.getId());
        return Result.success(convertToVO(answer));
    }

    @GetMapping("/question/{questionId}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取问题回答列表")
    public Result<List<AnswerVO>> getQuestionAnswers(@PathVariable Long questionId) {
        List<Answer> answers = answerService.getQuestionAnswers(questionId);
        return Result.success(answers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/my/{questionId}")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "获取我的回答")
    public Result<AnswerVO> getMyAnswer(@PathVariable Long questionId,
                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Answer answer = answerService.getMyAnswer(questionId, user.getId());
        if (answer == null) {
            return Result.success(null);
        }
        return Result.success(convertToVO(answer));
    }

    @PostMapping("/review")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师阅卷/打分")
    public Result<AnswerVO> reviewAnswer(@Valid @RequestBody AnswerReviewRequest request,
                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Answer answer = answerService.reviewAnswer(request, user.getId());
        return Result.success(convertToVO(answer));
    }

    @PostMapping("/review-suggestion")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "简答题AI评分建议")
    public Result<AiGradeSuggestionResponse> suggestGrade(@Valid @RequestBody AiGradeSuggestionRequest request,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(answerService.suggestSubjectiveGrade(request.getAnswerId(), user.getId()));
    }

    private AnswerVO convertToVO(Answer answer) {
        AnswerVO vo = new AnswerVO();
        BeanUtils.copyProperties(answer, vo);
        User user = userMapper.selectById(answer.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        return vo;
    }
}
