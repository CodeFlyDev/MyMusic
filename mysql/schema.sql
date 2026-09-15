-- 音乐网站数据库初始化脚本
-- 执行: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE music_db;

CREATE TABLE IF NOT EXISTS songs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL COMMENT '歌名',
    artist       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '歌手',
    album        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '专辑',
    duration_sec INT          NOT NULL DEFAULT 0  COMMENT '时长(秒)',
    file_name    VARCHAR(128) NOT NULL              COMMENT '音频文件名(存储目录内)',
    cover_name   VARCHAR(128) NULL                  COMMENT '封面文件名',
    file_size    BIGINT       NOT NULL DEFAULT 0    COMMENT '文件大小(字节)',
    play_count   INT          NOT NULL DEFAULT 0    COMMENT '播放次数',
    lyrics       TEXT         NULL                  COMMENT '原始LRC歌词文本(空表示无歌词)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 已有库升级用（幂等：列已存在时会报错，忽略即可）：
-- ALTER TABLE songs ADD COLUMN lyrics TEXT NULL COMMENT '原始LRC歌词文本(空表示无歌词)' AFTER cover_name;

-- 用户建议与反馈
CREATE TABLE IF NOT EXISTS feedback (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    content     TEXT         NOT NULL              COMMENT '反馈内容',
    contact     VARCHAR(255) NULL                  COMMENT '联系方式（可选）',
    resolved    TINYINT      NOT NULL DEFAULT 0    COMMENT '是否已解决：0 否 1 是',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    resolved_at DATETIME     NULL                  COMMENT '标记解决时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 已有库升级用：
-- CREATE TABLE IF NOT EXISTS feedback (...);  -- 同上建表语句，IF NOT EXISTS 可直接执行

-- 公告栏
CREATE TABLE IF NOT EXISTS announcements (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(255) NOT NULL COMMENT '标题',
    content      TEXT         NOT NULL COMMENT '内容(Markdown)',
    priority     TINYINT      NOT NULL DEFAULT 0 COMMENT '优先级：0普通 1重要 2置顶',
    visible      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏 1显示',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 更新日志
CREATE TABLE IF NOT EXISTS changelogs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    version       VARCHAR(32)  NOT NULL COMMENT '版本号',
    content       TEXT         NOT NULL COMMENT '更新内容(Markdown/列表)',
    release_date  DATE         NOT NULL COMMENT '发布日期',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
