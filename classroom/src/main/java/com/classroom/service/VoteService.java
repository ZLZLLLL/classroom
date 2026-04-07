package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.VoteCreateRequest;
import com.classroom.dto.VoteSubmitRequest;
import com.classroom.entity.Course;
import com.classroom.entity.User;
import com.classroom.entity.Vote;
import com.classroom.entity.VoteRecord;
import com.classroom.exception.BusinessException;
import com.classroom.repository.UserMapper;
import com.classroom.repository.VoteMapper;
import com.classroom.repository.VoteRecordMapper;
import com.classroom.vo.VoteVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService extends ServiceImpl<VoteMapper, Vote> {

    private final VoteRecordMapper voteRecordMapper;
    private final CourseService courseService;
    private final UserMapper userMapper;

    @Transactional
    public Vote createVote(VoteCreateRequest request, Long teacherId) {
        courseService.assertTeacherOwnsCourse(request.getCourseId(), teacherId);
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new BusinessException("投票选项至少需要2项");
        }

        List<VoteOption> normalizedOptions = new ArrayList<>();
        int index = 0;
        for (VoteCreateRequest.Option option : request.getOptions()) {
            String content = option == null ? null : option.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            VoteOption voteOption = new VoteOption();
            String key = option.getKey();
            voteOption.setKey((key == null || key.isBlank()) ? defaultOptionKey(index) : key.trim().toUpperCase());
            voteOption.setContent(content.trim());
            normalizedOptions.add(voteOption);
            index++;
        }

        if (normalizedOptions.size() < 2) {
            throw new BusinessException("有效投票选项至少需要2项");
        }

        Vote vote = new Vote();
        vote.setCourseId(request.getCourseId());
        vote.setTeacherId(teacherId);
        vote.setTitle(request.getTitle().trim());
        vote.setOptions(JSONUtil.toJsonStr(normalizedOptions));
        vote.setStatus(1);
        vote.setType(request.getType() != null && request.getType() == 2 ? 2 : 1);
        vote.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()) ? 1 : 0);
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            vote.setClassIds(JSONUtil.toJsonStr(request.getClassIds()));
        }
        this.save(vote);
        return vote;
    }

    @Transactional
    public void submitVote(Long voteId, VoteSubmitRequest request, Long userId) {
        Vote vote = getVoteOrThrow(voteId);
        if (vote.getStatus() == null || vote.getStatus() != 1) {
            throw new BusinessException("投票已结束");
        }
        User user = userMapper.selectById(userId);
        assertCanStudentVote(vote, user);

        Set<String> selectedKeys = resolveSelectedOptionKeys(request);
        List<VoteOption> options = parseOptions(vote.getOptions());
        Set<String> validKeys = options.stream().map(VoteOption::getKey).collect(Collectors.toSet());
        if (!validKeys.containsAll(selectedKeys)) {
            throw new BusinessException("存在无效投票选项");
        }
        if ((vote.getType() == null || vote.getType() == 1) && selectedKeys.size() != 1) {
            throw new BusinessException("单选投票只能选择1个选项");
        }

        Long count = voteRecordMapper.selectCount(new LambdaQueryWrapper<VoteRecord>()
                .eq(VoteRecord::getVoteId, voteId)
                .eq(VoteRecord::getUserId, userId));
        if (count != null && count > 0) {
            throw new BusinessException("您已投票");
        }

        for (String optionKey : selectedKeys) {
            VoteRecord record = new VoteRecord();
            record.setVoteId(voteId);
            record.setCourseId(vote.getCourseId());
            record.setUserId(userId);
            record.setOptionKey(optionKey);
            voteRecordMapper.insert(record);
        }
    }

    @Transactional
    public void closeVote(Long voteId, Long teacherId) {
        Vote vote = getVoteOrThrow(voteId);
        if (!vote.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限结束该投票");
        }
        vote.setStatus(2);
        this.updateById(vote);
    }

    public List<VoteVO> getCourseVotes(Long courseId, User user) {
        assertCanAccessCourseVotes(courseId, user);

        List<Vote> votes = this.list(new LambdaQueryWrapper<Vote>()
                .eq(Vote::getCourseId, courseId)
                .orderByDesc(Vote::getCreateTime));
        if (votes.isEmpty()) {
            return List.of();
        }

        List<Long> voteIds = votes.stream().map(Vote::getId).toList();
        List<VoteRecord> records = voteRecordMapper.selectList(new LambdaQueryWrapper<VoteRecord>()
                .in(VoteRecord::getVoteId, voteIds));

        Map<Long, List<VoteRecord>> recordMap = records.stream()
                .collect(Collectors.groupingBy(VoteRecord::getVoteId, HashMap::new, Collectors.toList()));
        Map<Long, List<String>> myVoteMap = records.stream()
                .filter(r -> user.getId() != null && user.getId().equals(r.getUserId()))
                .collect(Collectors.groupingBy(VoteRecord::getVoteId,
                        Collectors.mapping(VoteRecord::getOptionKey, Collectors.toList())));

        return votes.stream()
                .filter(v -> canUserSeeVote(v, user))
                .map(v -> toVoteVO(v, recordMap.getOrDefault(v.getId(), List.of()), myVoteMap.get(v.getId())))
                .toList();
    }

    public Vote getVoteOrThrow(Long voteId) {
        Vote vote = this.getById(voteId);
        if (vote == null) {
            throw new BusinessException("投票不存在");
        }
        return vote;
    }

    private VoteVO toVoteVO(Vote vote, List<VoteRecord> records, List<String> myOptions) {
        VoteVO vo = new VoteVO();
        vo.setId(vote.getId());
        vo.setCourseId(vote.getCourseId());
        vo.setTeacherId(vote.getTeacherId());
        vo.setTitle(vote.getTitle());
        vo.setStatus(vote.getStatus());
        vo.setType(vote.getType() == null ? 1 : vote.getType());
        vo.setAnonymous(vote.getAnonymous() == null ? 1 : vote.getAnonymous());
        vo.setCreateTime(vote.getCreateTime());
        vo.setUpdateTime(vote.getUpdateTime());
        List<String> normalizedMyOptions = myOptions == null
                ? List.of()
                : myOptions.stream().filter(s -> s != null && !s.isBlank()).map(s -> s.trim().toUpperCase()).distinct().toList();
        vo.setMyOptions(normalizedMyOptions);
        vo.setMyOption(normalizedMyOptions.isEmpty() ? null : normalizedMyOptions.get(0));

        Course course = courseService.getById(vote.getCourseId());
        vo.setCourseName(course == null ? null : course.getName());
        User teacher = userMapper.selectById(vote.getTeacherId());
        vo.setTeacherName(teacher == null ? null : (teacher.getRealName() != null ? teacher.getRealName() : teacher.getUsername()));

        int totalVotes = records == null ? 0 : (int) records.stream().map(VoteRecord::getUserId).filter(id -> id != null).distinct().count();
        vo.setTotalVotes(totalVotes);

        Map<String, Integer> optionCount = new LinkedHashMap<>();
        if (records != null) {
            for (VoteRecord record : records) {
                String key = record.getOptionKey() == null ? "" : record.getOptionKey().trim().toUpperCase();
                if (!key.isEmpty()) {
                    optionCount.put(key, optionCount.getOrDefault(key, 0) + 1);
                }
            }
        }

        Map<String, List<String>> optionVoterNames = new HashMap<>();
        if (vote.getAnonymous() != null && vote.getAnonymous() == 0 && records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(VoteRecord::getUserId).filter(id -> id != null).distinct().toList();
            Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername(), (a, b) -> a));
            for (VoteRecord record : records) {
                if (record.getOptionKey() == null || record.getUserId() == null) {
                    continue;
                }
                String key = record.getOptionKey().trim().toUpperCase();
                String name = userNameMap.get(record.getUserId());
                if (name == null || name.isBlank()) {
                    continue;
                }
                optionVoterNames.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
            }
        }

        List<VoteVO.VoteOptionVO> optionVOs = parseOptions(vote.getOptions()).stream().map(option -> {
            VoteVO.VoteOptionVO optionVO = new VoteVO.VoteOptionVO();
            optionVO.setKey(option.getKey());
            optionVO.setContent(option.getContent());
            int count = optionCount.getOrDefault(option.getKey(), 0);
            optionVO.setVoteCount(count);
            int percentage = totalVotes == 0 ? 0 : (int) Math.round(count * 100.0 / totalVotes);
            optionVO.setPercentage(Math.max(0, Math.min(100, percentage)));
            if (vote.getAnonymous() != null && vote.getAnonymous() == 0) {
                optionVO.setVoterNames(optionVoterNames.getOrDefault(option.getKey(), List.of()));
            }
            return optionVO;
        }).toList();

        vo.setOptions(optionVOs);
        return vo;
    }

    private Set<String> resolveSelectedOptionKeys(VoteSubmitRequest request) {
        if (request == null) {
            throw new BusinessException("投票参数不能为空");
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (request.getOptionKey() != null && !request.getOptionKey().isBlank()) {
            keys.add(request.getOptionKey().trim().toUpperCase());
        }
        if (request.getOptionKeys() != null) {
            request.getOptionKeys().stream()
                    .filter(k -> k != null && !k.isBlank())
                    .map(k -> k.trim().toUpperCase())
                    .forEach(keys::add);
        }
        if (keys.isEmpty()) {
            throw new BusinessException("请选择投票选项");
        }
        return keys;
    }


    private List<VoteOption> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        return JSONUtil.toList(optionsJson, VoteOption.class).stream()
                .filter(o -> o.getKey() != null && !o.getKey().isBlank() && o.getContent() != null && !o.getContent().isBlank())
                .peek(o -> o.setKey(o.getKey().trim().toUpperCase()))
                .toList();
    }

    private boolean canUserSeeVote(Vote vote, User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        if (user.getRole() == 3) {
            return true;
        }
        if (user.getRole() == 1) {
            return vote.getTeacherId().equals(user.getId());
        }
        if (user.getRole() == 2) {
            if (vote.getClassIds() == null || vote.getClassIds().isBlank()) {
                return true;
            }
            if (user.getClassId() == null) {
                return false;
            }
            List<Long> classIds = JSONUtil.toList(vote.getClassIds(), Long.class);
            return classIds.contains(user.getClassId());
        }
        return false;
    }

    private void assertCanAccessCourseVotes(Long courseId, User user) {
        if (user == null || user.getRole() == null) {
            throw new BusinessException("无权限访问投票");
        }
        if (user.getRole() == 3) {
            return;
        }
        if (user.getRole() == 1 && courseService.isTeacherCourseOwner(courseId, user.getId())) {
            return;
        }
        if (user.getRole() == 2 && courseService.isStudentInCourse(courseId, user.getId())) {
            return;
        }
        throw new BusinessException("无权限访问该课程投票");
    }

    private void assertCanStudentVote(Vote vote, User user) {
        if (user == null || user.getRole() == null || user.getRole() != 2) {
            throw new BusinessException("仅学生可参与投票");
        }
        if (!courseService.isStudentInCourse(vote.getCourseId(), user.getId())) {
            throw new BusinessException("无权限参与该课程投票");
        }
        if (vote.getClassIds() != null && !vote.getClassIds().isBlank()) {
            List<Long> classIds = JSONUtil.toList(vote.getClassIds(), Long.class);
            if (user.getClassId() == null || !classIds.contains(user.getClassId())) {
                throw new BusinessException("当前投票不面向您所在班级");
            }
        }
    }

    private String defaultOptionKey(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "OPT" + (index + 1);
    }

    @Data
    private static class VoteOption {
        private String key;
        private String content;
    }
}



