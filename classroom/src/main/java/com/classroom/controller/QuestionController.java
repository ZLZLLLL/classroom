package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.QuestionCreateRequest;
import com.classroom.entity.Answer;
import com.classroom.entity.Course;
import com.classroom.entity.Question;
import com.classroom.entity.User;
import com.classroom.service.AnswerService;
import com.classroom.service.CourseService;
import com.classroom.service.QuestionService;
import com.classroom.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "提问管理")
public class QuestionController {

    private final QuestionService questionService;
    private final CourseService courseService;
    private final AnswerService answerService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师发起提问")
    public Result<QuestionVO> createQuestion(@Valid @RequestBody QuestionCreateRequest request,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Question question = questionService.createQuestion(request, user.getId());
        return Result.success(convertToVO(question, user));
    }

    @GetMapping("/active/{courseId}")
    @Operation(summary = "获取当前进行中的提问")
    public Result<QuestionVO> getActiveQuestion(@PathVariable Long courseId,
                                                Authentication authentication) {
        Question question = questionService.getActiveQuestion(courseId);
        if (question == null) {
            return Result.success(null);
        }
        User currentUser = authentication == null ? null : (User) authentication.getPrincipal();
        return Result.success(convertToVO(question, currentUser));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "获取课程提问历史")
    public Result<List<QuestionVO>> getCourseQuestions(@PathVariable Long courseId,
                                                       Authentication authentication) {
        List<Question> questions = questionService.getCourseQuestions(courseId);
        User currentUser = authentication == null ? null : (User) authentication.getPrincipal();
        return Result.success(questions.stream()
                .map(q -> convertToVO(q, currentUser))
                .collect(Collectors.toList()));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取教师所有提问")
    public Result<List<QuestionVO>> getTeacherQuestions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Question> questions = questionService.getTeacherQuestions(user.getId());
        return Result.success(questions.stream()
                .map(q -> convertToVO(q, user))
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取提问详情")
    public Result<QuestionVO> getQuestionById(@PathVariable Long id,
                                              Authentication authentication) {
        Question question = questionService.getQuestionById(id);
        if (question == null) {
            return Result.notFound("问题不存在");
        }
        User currentUser = authentication == null ? null : (User) authentication.getPrincipal();
        return Result.success(convertToVO(question, currentUser));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "结束提问")
    public Result<?> closeQuestion(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        questionService.closeQuestion(id, user.getId());
        return Result.success();
    }

    private QuestionVO convertToVO(Question question, User currentUser) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(question, vo);

        Course course = courseService.getById(question.getCourseId());
        vo.setCourseName(course == null ? null : course.getName());

        long answerCount = answerService.count(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, question.getId()));
        vo.setAnswerCount((int) answerCount);

        long correctCount = answerService.count(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, question.getId())
                .eq(Answer::getIsCorrect, 1));
        vo.setCorrectCount((int) correctCount);

        if (currentUser != null && currentUser.getRole() != null && currentUser.getRole() == 2) {
            Answer myAnswer = answerService.getMyAnswer(question.getId(), currentUser.getId());
            vo.setMyAnswer(myAnswer == null ? null : myAnswer.getContent());
        }
        return vo;
    }
}
