package com.mymusic.controller;

import com.mymusic.entity.Changelog;
import com.mymusic.service.ChangelogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/changelogs")
public class ChangelogController {

    private final ChangelogService changelogService;

    @Value("${app.upload-passcode}")
    private String uploadPasscode;

    public ChangelogController(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Changelog> list = changelogService.getAllChangelogs();
        return Map.of("code", 0, "data", list);
    }

    @GetMapping("/admin")
    public Map<String, Object> listAdmin(@RequestHeader(value = "X-Upload-Code", required = false) String code) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        List<Changelog> list = changelogService.getAllChangelogs();
        return Map.of("code", 0, "data", list);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        Changelog changelog = changelogService.getChangelogById(id);
        if (changelog == null) {
            return Map.of("code", 1, "msg", "更新日志不存在");
        }
        return Map.of("code", 0, "data", changelog);
    }

    @PostMapping
    public Map<String, Object> create(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @RequestBody Changelog changelog) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        if (changelog.getVersion() == null || changelog.getVersion().isBlank()) {
            return Map.of("code", 1, "msg", "版本号不能为空");
        }
        if (changelog.getContent() == null || changelog.getContent().isBlank()) {
            return Map.of("code", 1, "msg", "内容不能为空");
        }
        if (changelog.getReleaseDate() == null) {
            return Map.of("code", 1, "msg", "发布日期不能为空");
        }
        Changelog created = changelogService.createChangelog(changelog);
        return Map.of("code", 0, "data", created, "msg", "创建成功");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id,
            @RequestBody Changelog changelog) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Changelog existing = changelogService.getChangelogById(id);
        if (existing == null) {
            return Map.of("code", 1, "msg", "更新日志不存在");
        }
        changelog.setId(id);
        Changelog updated = changelogService.updateChangelog(changelog);
        return Map.of("code", 0, "data", updated, "msg", "更新成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Changelog existing = changelogService.getChangelogById(id);
        if (existing == null) {
            return Map.of("code", 1, "msg", "更新日志不存在");
        }
        changelogService.deleteChangelog(id);
        return Map.of("code", 0, "msg", "删除成功");
    }

    private boolean pass(String code) {
        return uploadPasscode != null && !uploadPasscode.isBlank() && uploadPasscode.equals(code);
    }
}