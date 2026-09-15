package com.mymusic.controller;

import com.mymusic.entity.Announcement;
import com.mymusic.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Value("${app.upload-passcode}")
    private String uploadPasscode;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Announcement> list = announcementService.getVisibleAnnouncements();
        return Map.of("code", 0, "data", list);
    }

    @GetMapping("/admin")
    public Map<String, Object> listAdmin(@RequestHeader(value = "X-Upload-Code", required = false) String code) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        List<Announcement> list = announcementService.getAllAnnouncements();
        return Map.of("code", 0, "data", list);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementById(id);
        if (announcement == null) {
            return Map.of("code", 1, "msg", "公告不存在");
        }
        return Map.of("code", 0, "data", announcement);
    }

    @PostMapping
    public Map<String, Object> create(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @RequestBody Announcement announcement) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            return Map.of("code", 1, "msg", "标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            return Map.of("code", 1, "msg", "内容不能为空");
        }
        if (announcement.getPriority() == null) announcement.setPriority(0);
        if (announcement.getVisible() == null) announcement.setVisible(1);
        Announcement created = announcementService.createAnnouncement(announcement);
        return Map.of("code", 0, "data", created, "msg", "创建成功");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id,
            @RequestBody Announcement announcement) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Announcement existing = announcementService.getAnnouncementById(id);
        if (existing == null) {
            return Map.of("code", 1, "msg", "公告不存在");
        }
        announcement.setId(id);
        if (announcement.getPriority() == null) announcement.setPriority(0);
        if (announcement.getVisible() == null) announcement.setVisible(1);
        Announcement updated = announcementService.updateAnnouncement(announcement);
        return Map.of("code", 0, "data", updated, "msg", "更新成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Announcement existing = announcementService.getAnnouncementById(id);
        if (existing == null) {
            return Map.of("code", 1, "msg", "公告不存在");
        }
        announcementService.deleteAnnouncement(id);
        return Map.of("code", 0, "msg", "删除成功");
    }

    private boolean pass(String code) {
        return uploadPasscode != null && !uploadPasscode.isBlank() && uploadPasscode.equals(code);
    }
}