# 音乐 Web 项目开发计划（Mymusic）

## 项目结构
```
D:\AI_project\Mymusic\
├── backend\          # Spring Boot 3 + MyBatis（JDK 21）
│   └── src\main\java\com\mymusic\...
├── frontend\         # Vue 3 + Vite + Element Plus（Element UI 的 Vue3 版）
├── mysql\schema.sql  # 建库建表脚本
├── deploy\           # docker-compose.yml、nginx.conf、调优说明
└── docs\部署操作指南.md
```

## 技术选型
- 后端：Spring Boot 3.3.x、MyBatis spring-boot-starter、MySQL 8、jaudiotagger（解析 MP3/FLAC/M4A/WAV 的 ID3 标签与内嵌封面）
- 前端：Vue 3、Vite、Element Plus、vue-router（两个页面：歌曲库 / 上传）
- 无登录，无歌单；单曲播放，播完即停

## 功能明细
1. **歌曲库页**：表格/卡片列出歌曲（封面、歌名、歌手、专辑、时长、播放次数、上传时间），顶部搜索框按歌名/歌手实时过滤，每行有播放和删除（带确认）按钮
2. **底部播放器**：点击歌曲开始播放，固定底栏显示歌名/封面，el-slider 进度条可任意拖动，显示当前时间/总时长，歌曲播完自动停止（不连播），仅 播放/暂停/拖进度 三种控制
3. **上传页**：Element Plus 拖拽上传，支持 mp3/flac/m4a/wav，单个限 100MB；上传后后端自动解析 ID3 标签（歌名、歌手、专辑、时长、内嵌封面），解析失败回退用文件名（按"歌手-歌名"格式尝试解析），页面上传成功后显示识别结果
4. **接口**：GET /api/songs（含 keyword 搜索）、POST /api/songs/upload、DELETE /api/songs/{id}（删记录+删文件）、POST /api/songs/{id}/play（播放计数+1）、GET /api/songs/{id}/file（音频流，支持 HTTP Range 断点/拖动）、封面静态访问

## 数据库（songs 表）
id, title, artist, album, duration_sec, file_name, cover_name(可空), file_size, play_count, created_at；音频文件存磁盘目录（application.yml 可配置），不入库

## 实施步骤
1. **安装本地开发环境**（你已确认）：winget 安装 JDK 21（Temurin）、Maven、Node.js LTS、MySQL 8（portable zip 方式，避免污染系统），初始化 music_db
2. **后端开发**：Maven 工程骨架 → schema.sql → 实体/Mapper/Service/Controller → 上传与标签解析 → Range 音频流 → 联调通过
3. **前端开发**：Vite 脚手架 → 路由两页 → 歌曲库页（搜索/删除）+ 上传页 → 底部播放器组件 → vite.config 代理 /api 到后端
4. **本地自测**：前后端同时启动，用浏览器实际验证：上传一首测试音频（生成或找现有文件）、标签识别、播放、拖进度条、搜索、删除、播放计数
5. **生产构建**：后端 mvn package 出 jar；前端 npm run build 出 dist
6. **部署物**：deploy/ 下 docker-compose.yml（mysql + backend + nginx 三个服务，含内存调优：MySQL innodb_buffer_pool_size=64M、JVM -Xmx384m、建 2G swap）、nginx.conf（托管前端静态资源 + 反代 /api 与音频流）
7. **部署指南** docs/部署操作指南.md：
   - 主方案：云服务器装 Docker → 建 swap → 上传 jar/dist/compose 文件 → docker compose up → 安全组放行 80 端口
   - 备选方案：jar + systemd + Nginx + 服务器直装 MySQL（Docker 方案内存不足时用）
   - 包含：1核1G 内存调优参数、数据备份、常见问题（上传 413、音频拖动、FLAC 缓冲）排错

## 验收标准
本地浏览器实测通过全部核心功能后交付；部署指南按 Docker Compose 为主、含完整命令可直接复制执行