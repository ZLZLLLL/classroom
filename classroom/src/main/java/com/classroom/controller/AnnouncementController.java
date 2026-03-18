package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.service.SystemAnnouncementService;
import com.classroom.vo.SystemAnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "系统公告")
public class AnnouncementController {

	private final SystemAnnouncementService announcementService;

	@GetMapping("/recent")
	@Operation(summary = "获取最近系统公告")
	public Result<List<SystemAnnouncementVO>> getRecent(@RequestParam(defaultValue = "10") Integer size) {
		return Result.success(announcementService.getRecent(size));
	}
}

