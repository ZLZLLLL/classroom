package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.ExamAnswerGradeRequest;
import com.classroom.dto.ExamAnswerRequest;
import com.classroom.dto.ExamGradeRequest;
import com.classroom.dto.ExamSubmitRequest;
import com.classroom.dto.AiGradeSuggestionResponse;
import com.classroom.entity.Exam;
import com.classroom.entity.ExamAnswer;
import com.classroom.entity.ExamQuestion;
import com.classroom.entity.ExamSubmit;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.ExamAnswerMapper;
import com.classroom.repository.ExamQuestionMapper;
import com.classroom.repository.ExamSubmitMapper;
import com.classroom.repository.UserMapper;
import com.classroom.vo.ExamAnswerVO;
import com.classroom.vo.ExamSubmitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSubmitService extends ServiceImpl<ExamSubmitMapper, ExamSubmit> {

    private final ExamService examService;
    private final CourseService courseService;
    private final UserMapper userMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ClassroomAiService classroomAiService;

    @Transactional
    public ExamSubmit startExam(Long examId, Long userId) {
        Exam exam = examService.getExamOrThrow(examId);
        validateStudentExamAccess(exam, userId);
        if (!examService.isExamActive(exam)) {
            throw new BusinessException("考试未开始或已结束");
        }

        ExamSubmit submit = getUserSubmit(examId, userId);
        if (submit != null) {
            if (submit.getStatus() != null && submit.getStatus() >= 2) {
                throw new BusinessException("已提交考试");
            }
            return submit;
        }

        submit = new ExamSubmit();
        submit.setExamId(examId);
        submit.setUserId(userId);
        submit.setStatus(1);
        submit.setAutoSubmit(0);
        submit.setObjectiveScore(0);
        submit.setSubjectiveScore(0);
        submit.setTotalScore(0);
        this.save(submit);
        return submit;
    }

    @Transactional
    public ExamSubmit saveProgress(Long examId, ExamSubmitRequest request, Long userId) {
        ExamSubmit submit = startExam(examId, userId);
        saveAnswers(submit, request.getAnswers());
        return submit;
    }

    @Transactional
    public ExamSubmit submitExam(Long examId, ExamSubmitRequest request, Long userId, boolean autoSubmit) {
        Exam exam = examService.getExamOrThrow(examId);
        if (!autoSubmit && (exam.getEndTime() != null && LocalDateTime.now().isAfter(exam.getEndTime()))) {
            throw new BusinessException("已超过考试截止时间");
        }
        ExamSubmit submit = startExam(examId, userId);
        if (submit.getStatus() != null && submit.getStatus() >= 2) {
            throw new BusinessException("已提交考试");
        }

        saveAnswers(submit, request.getAnswers());
        finalizeSubmit(submit, exam, autoSubmit);
        return submit;
    }

    @Transactional
    public void autoSubmitExpired(Exam exam) {
        List<ExamSubmit> submits = this.list(new LambdaQueryWrapper<ExamSubmit>()
                .eq(ExamSubmit::getExamId, exam.getId())
                .eq(ExamSubmit::getStatus, 1));

        for (ExamSubmit submit : submits) {
            finalizeSubmit(submit, exam, true);
        }
    }

    public ExamSubmit getUserSubmit(Long examId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<ExamSubmit>()
                .eq(ExamSubmit::getExamId, examId)
                .eq(ExamSubmit::getUserId, userId));
    }

    public ExamSubmitVO getMySubmitDetail(Long examId, Long userId) {
        Exam exam = examService.getExamOrThrow(examId);
        validateStudentExamAccess(exam, userId);

        ExamSubmit submit = getUserSubmit(examId, userId);
        if (submit == null) {
            return null;
        }

        Map<Long, ExamQuestion> questionMap = getQuestionMapByExamId(examId);
        List<ExamAnswer> answers = getSubmitAnswers(submit.getId());
        return toSubmitVO(submit, answers, questionMap);
    }

    public List<ExamSubmitVO> getSubmitList(Long examId, Long teacherId) {
        List<ExamSubmit> submits = getExamSubmits(examId, teacherId);
        return submits.stream()
                .map(submit -> toSubmitVO(submit, null, null))
                .collect(Collectors.toList());
    }

    public ExamSubmitVO getSubmitDetail(Long submitId, Long teacherId) {
        ExamSubmit submit = this.getById(submitId);
        if (submit == null) {
            throw new BusinessException("提交记录不存在");
        }

        Exam exam = examService.getExamOrThrow(submit.getExamId());
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限查看");
        }

        Map<Long, ExamQuestion> questionMap = getQuestionMapByExamId(submit.getExamId());
        List<ExamAnswer> answers = getSubmitAnswers(submit.getId());
        return toSubmitVO(submit, answers, questionMap);
    }

    public List<ExamSubmit> getExamSubmits(Long examId, Long teacherId) {
        Exam exam = examService.getExamOrThrow(examId);
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限查看");
        }
        return this.list(new LambdaQueryWrapper<ExamSubmit>()
                .eq(ExamSubmit::getExamId, examId)
                .orderByDesc(ExamSubmit::getSubmitTime));
    }

    @Transactional
    public void gradeExamAnswers(Long examId, ExamGradeRequest request, Long teacherId) {
        Exam exam = examService.getExamOrThrow(examId);
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限批改");
        }

        Map<Long, ExamAnswer> answerMap = new HashMap<>();
        List<Long> answerIds = request.getAnswers().stream()
                .map(ExamAnswerGradeRequest::getAnswerId)
                .collect(Collectors.toList());
        if (!answerIds.isEmpty()) {
            answerMap = examAnswerMapper.selectBatchIds(answerIds).stream()
                    .collect(Collectors.toMap(ExamAnswer::getId, a -> a));
        }

        for (ExamAnswerGradeRequest gradeRequest : request.getAnswers()) {
            ExamAnswer answer = answerMap.get(gradeRequest.getAnswerId());
            if (answer == null) {
                continue;
            }
            ExamQuestion question = examQuestionMapper.selectById(answer.getQuestionId());
            if (question == null || question.getExamId() == null || !question.getExamId().equals(examId)) {
                continue;
            }
            int score = gradeRequest.getScore() == null ? 0 : gradeRequest.getScore();
            if (question.getPoints() != null && score > question.getPoints()) {
                throw new BusinessException("得分不能超过题目分值");
            }
            answer.setScore(score);
            if (gradeRequest.getCorrect() != null) {
                answer.setIsCorrect(gradeRequest.getCorrect() ? 1 : 2);
            } else if (score > 0) {
                answer.setIsCorrect(1);
            } else {
                answer.setIsCorrect(2);
            }
            answer.setFeedback(gradeRequest.getFeedback());
            examAnswerMapper.updateById(answer);
        }

        List<Long> submitIds = answerMap.values().stream()
                .map(ExamAnswer::getSubmitId)
                .distinct()
                .toList();
        for (Long submitId : submitIds) {
            refreshSubmitScore(submitId);
        }
    }

    public AiGradeSuggestionResponse suggestSubjectiveGrade(Long answerId, Long teacherId) {
        ExamAnswer answer = examAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new BusinessException("答题记录不存在");
        }
        ExamQuestion question = examQuestionMapper.selectById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        Exam exam = examService.getExamOrThrow(question.getExamId());
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限阅卷");
        }
        if (question.getType() == null || question.getType() != 4) {
            throw new BusinessException("仅支持简答题评分建议");
        }
        return classroomAiService.suggestExamSubjectiveGrade(question, answer);
    }

    public ExamSubmitVO toSubmitVO(ExamSubmit submit, List<ExamAnswer> answers, Map<Long, ExamQuestion> questionMap) {
        ExamSubmitVO vo = new ExamSubmitVO();
        BeanUtils.copyProperties(submit, vo);
        User user = userMapper.selectById(submit.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }

        if (answers != null) {
            List<ExamAnswerVO> answerVOs = answers.stream()
                    .map(answer -> toAnswerVO(answer, questionMap.get(answer.getQuestionId())))
                    .collect(Collectors.toList());
            vo.setAnswers(answerVOs);
        }
        return vo;
    }

    public ExamAnswerVO toAnswerVO(ExamAnswer answer, ExamQuestion question) {
        ExamAnswerVO vo = new ExamAnswerVO();
        BeanUtils.copyProperties(answer, vo);
        if (question != null) {
            vo.setQuestionContent(question.getContent());
            vo.setQuestionType(question.getType());
            vo.setQuestionPoints(question.getPoints());
        }
        return vo;
    }

    private void saveAnswers(ExamSubmit submit, List<ExamAnswerRequest> answerRequests) {
        Map<Long, ExamQuestion> questionMap = getQuestionMapByExamId(submit.getExamId());
        Set<Long> validQuestionIds = questionMap.keySet();

        examAnswerMapper.delete(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSubmitId, submit.getId()));

        if (answerRequests == null || answerRequests.isEmpty()) {
            return;
        }

        for (ExamAnswerRequest request : answerRequests) {
            if (request.getQuestionId() == null || !validQuestionIds.contains(request.getQuestionId())) {
                throw new BusinessException("存在不属于当前考试的题目");
            }
            ExamAnswer answer = new ExamAnswer();
            answer.setSubmitId(submit.getId());
            answer.setExamId(submit.getExamId());
            answer.setQuestionId(request.getQuestionId());
            answer.setUserId(submit.getUserId());
            answer.setContent(request.getContent());
            answer.setIsCorrect(0);
            answer.setScore(0);
            examAnswerMapper.insert(answer);
        }
    }

    private Map<Long, ExamQuestion> getQuestionMapByExamId(Long examId) {
        return examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId))
                .stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));
    }

    private List<ExamAnswer> getSubmitAnswers(Long submitId) {
        return examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSubmitId, submitId)
                .orderByAsc(ExamAnswer::getId));
    }

    private void finalizeSubmit(ExamSubmit submit, Exam exam, boolean autoSubmit) {
        List<ExamQuestion> questions = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, exam.getId()));
        Map<Long, ExamQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSubmitId, submit.getId()));

        int objectiveScore = 0;
        boolean hasSubjective = false;
        for (ExamAnswer answer : answers) {
            ExamQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            if (question.getType() != null && question.getType() == 4) {
                hasSubjective = true;
                continue;
            }
            int score = gradeObjective(question, answer.getContent());
            answer.setScore(score);
            answer.setIsCorrect(score > 0 ? 1 : 2);
            examAnswerMapper.updateById(answer);
            objectiveScore += score;
        }

        submit.setObjectiveScore(objectiveScore);
        submit.setSubjectiveScore(0);
        submit.setTotalScore(objectiveScore);
        submit.setAutoSubmit(autoSubmit ? 1 : 0);
        submit.setSubmitTime(LocalDateTime.now());
        submit.setStatus(hasSubjective ? 2 : 3);
        this.updateById(submit);
    }

    private void refreshSubmitScore(Long submitId) {
        ExamSubmit submit = this.getById(submitId);
        if (submit == null) {
            return;
        }

        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getSubmitId, submitId));
        if (answers.isEmpty()) {
            return;
        }

        Map<Long, ExamQuestion> questionMap = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, submit.getExamId()))
                .stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        int objectiveScore = 0;
        int subjectiveScore = 0;
        boolean pending = false;
        for (ExamAnswer answer : answers) {
            ExamQuestion question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            if (question.getType() != null && question.getType() == 4) {
                if (answer.getIsCorrect() == null || answer.getIsCorrect() == 0) {
                    pending = true;
                }
                subjectiveScore += answer.getScore() == null ? 0 : answer.getScore();
            } else {
                objectiveScore += answer.getScore() == null ? 0 : answer.getScore();
            }
        }

        submit.setObjectiveScore(objectiveScore);
        submit.setSubjectiveScore(subjectiveScore);
        submit.setTotalScore(objectiveScore + subjectiveScore);
        submit.setStatus(pending ? 2 : 3);
        this.updateById(submit);
    }

    private int gradeObjective(ExamQuestion question, String answerContent) {
        if (question.getCorrectAnswer() == null) {
            return 0;
        }
        String correct = normalizeAnswer(question.getCorrectAnswer());
        String student = normalizeAnswer(answerContent);
        if (question.getType() != null && question.getType() == 2) {
            if (normalizeMulti(correct).equals(normalizeMulti(student))) {
                return question.getPoints() == null ? 0 : question.getPoints();
            }
            return 0;
        }
        if (correct.equalsIgnoreCase(student)) {
            return question.getPoints() == null ? 0 : question.getPoints();
        }
        return 0;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().replaceAll("\\s+", "");
    }

    private String normalizeMulti(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }
        String[] parts = answer.split(",");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed.toUpperCase());
            }
        }
        normalized.sort(String::compareTo);
        return String.join(",", normalized);
    }

    private void validateStudentExamAccess(Exam exam, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 2) {
            throw new BusinessException("仅学生可参加考试");
        }
        if (user.getClassId() == null) {
            throw new BusinessException("学生未绑定班级，无法参加考试");
        }

        List<Long> courseClassIds = courseService.getCourseClasses(exam.getCourseId()).stream()
                .map(com.classroom.entity.Class::getId)
                .toList();
        if (!courseClassIds.contains(user.getClassId())) {
            throw new BusinessException("您不在该课程的班级范围内");
        }

        if (exam.getClassIds() != null && !exam.getClassIds().isBlank()) {
            List<Long> classIds = JSONUtil.toList(exam.getClassIds(), Long.class);
            if (!classIds.contains(user.getClassId())) {
                throw new BusinessException("当前考试不面向您所在班级");
            }
        }
    }

}

