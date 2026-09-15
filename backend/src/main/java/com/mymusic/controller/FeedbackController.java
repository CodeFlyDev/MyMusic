package com.mymusic.controller;

import com.mymusic.entity.Feedback;
import com.mymusic.mapper.FeedbackMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户建议与反馈：
 *  - POST /api/feedback：公开提交（无需口令）
 *  - GET /api/feedback：需上传口令，管理员查看全部反馈
 *  - PUT /api/feedback/{id}/resolve：需上传口令，切换已解决状态
 *  - DELETE /api/feedback/{id}：需上传口令，删除反馈
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackMapper feedbackMapper;

    @Value("${app.upload-passcode}")
    private String uploadPasscode;

    public FeedbackController(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @PostMapping
    public Map<String, Object> submit(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return Map.of("code", 1, "msg", "反馈内容不能为空");
        }
        if (content.length() > 2000) {
            return Map.of("code", 1, "msg", "反馈内容过长（最多 2000 字）");
        }
        String contact = body.getOrDefault("contact", "").trim();
        if (contact.length() > 255) contact = contact.substring(0, 255);

        Feedback fb = new Feedback();
        fb.setContent(content);
        fb.setContact(contact.isEmpty() ? null : contact);
        feedbackMapper.insert(fb);
        return Map.of("code", 0, "msg", "反馈已提交，感谢你的建议");
    }

    @GetMapping
    public Map<String, Object> list(@RequestHeader(value = "X-Upload-Code", required = false) String code) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        List<Feedback> list = feedbackMapper.findAll();
        return Map.of("code", 0, "data", list);
    }

    @PutMapping("/{id}/resolve")
    public Map<String, Object> resolve(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        Feedback fb = feedbackMapper.findById(id);
        if (fb == null) {
            return Map.of("code", 1, "msg", "反馈不存在");
        }
        boolean resolved = Boolean.TRUE.equals(body.get("resolved"));
        feedbackMapper.updateResolved(id, resolved ? 1 : 0, resolved ? LocalDateTime.now() : null);
        return Map.of("code", 0, "msg", resolved ? "已标记为解决" : "已取消标记");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(
            @RequestHeader(value = "X-Upload-Code", required = false) String code,
            @PathVariable Long id) {
        if (!pass(code)) {
            return Map.of("code", 401, "msg", "访问口令错误");
        }
        feedbackMapper.deleteById(id);
        return Map.of("code", 0, "msg", "已删除");
    }

    private boolean pass(String code) {
        return uploadPasscode != null && !uploadPasscode.isBlank() && uploadPasscode.equals(code);
    }
}
