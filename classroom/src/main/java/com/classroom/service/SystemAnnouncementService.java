package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.AnnouncementCreateRequest;
import com.classroom.entity.SystemAnnouncement;
import com.classroom.entity.User;
import com.classroom.repository.SystemAnnouncementMapper;
import com.classroom.repository.UserMapper;
import com.classroom.vo.SystemAnnouncementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemAnnouncementService extends ServiceImpl<SystemAnnouncementMapper, SystemAnnouncement> {

    private final UserMapper userMapper;

    public SystemAnnouncement publish(AnnouncementCreateRequest request, Long publisherId) {
        SystemAnnouncement announcement = new SystemAnnouncement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPublisherId(publisherId);
        this.save(announcement);
        return announcement;
    }

    public List<SystemAnnouncementVO> getRecent(Integer size) {
        int limit = size == null || size <= 0 ? 10 : Math.min(size, 50);
        List<SystemAnnouncement> list = this.list(new LambdaQueryWrapper<SystemAnnouncement>()
                .orderByDesc(SystemAnnouncement::getCreateTime)
                .last("LIMIT " + limit));

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    public SystemAnnouncementVO toVO(SystemAnnouncement entity) {
        SystemAnnouncementVO vo = new SystemAnnouncementVO();
        BeanUtils.copyProperties(entity, vo);
        User publisher = userMapper.selectById(entity.getPublisherId());
        if (publisher != null) {
            vo.setPublisherName(publisher.getRealName() != null ? publisher.getRealName() : publisher.getUsername());
        }
        return vo;
    }
}

