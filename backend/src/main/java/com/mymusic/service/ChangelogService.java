package com.mymusic.service;

import com.mymusic.entity.Changelog;
import com.mymusic.mapper.ChangelogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChangelogService {

    private final ChangelogMapper changelogMapper;

    public ChangelogService(ChangelogMapper changelogMapper) {
        this.changelogMapper = changelogMapper;
    }

    public List<Changelog> getAllChangelogs() {
        return changelogMapper.findAll();
    }

    public Changelog getChangelogById(Long id) {
        return changelogMapper.findById(id);
    }

    public Changelog createChangelog(Changelog changelog) {
        changelogMapper.insert(changelog);
        return changelog;
    }

    public Changelog updateChangelog(Changelog changelog) {
        changelogMapper.update(changelog);
        return changelogMapper.findById(changelog.getId());
    }

    public void deleteChangelog(Long id) {
        changelogMapper.deleteById(id);
    }
}