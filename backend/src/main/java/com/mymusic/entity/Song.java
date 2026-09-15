package com.mymusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public class Song {

    private Long id;
    private String title;
    private String artist;
    private String album;
    private Integer durationSec;
    private String fileName;    // 磁盘上的音频文件名（uuid.ext）
    private String coverName;   // 封面文件名，可能为 null
    private Long fileSize;
    private Integer playCount;
    private String lyrics;      // 原始 LRC 歌词文本，可能为 null
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCoverName() { return coverName; }
    public void setCoverName(String coverName) { this.coverName = coverName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getPlayCount() { return playCount; }
    public void setPlayCount(Integer playCount) { this.playCount = playCount; }

    // 歌词文本较大，列表/上传等接口不返回，仅由 /lyrics 接口按需获取
    @JsonIgnore
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
