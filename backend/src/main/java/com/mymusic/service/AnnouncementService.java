package com.mymusic.service;

import com.mymusic.entity.Announcement;
import com.mymusic.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    public List<Announcement> getVisibleAnnouncements() {
        return announcementMapper.findVisible();
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementMapper.findAll();
    }

    public Announcement getAnnouncementById(Long id) {
        return announcementMapper.findById(id);
    }

    public Announcement createAnnouncement(Announcement announcement) {
        announcementMapper.insert(announcement);
        return announcement;
    }

    public Announcement updateAnnouncement(Announcement announcement) {
        announcementMapper.update(announcement);
        return announcementMapper.findById(announcement.getId());
    }

    public void deleteAnnouncement(Long id) {
        announcementMapper.deleteById(id);
    }
}