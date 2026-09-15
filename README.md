# MyMusic 音乐网站

无需登录的个人音乐站：查看歌曲库、点击播放（可拖进度条，播完即停）、上传歌曲（自动解析 ID3 标签与封面）、搜索、删除、播放次数统计。

## 技术栈

- 后端：Java 21 + Spring Boot 3.3 + MyBatis + MySQL 8（jaudiotagger 解析音频标签）
- 前端：Vue 3 + Element Plus + Vite + vue-router

## 本地开发

前置：JDK 21、Maven 3.9+、Node 18+、MySQL 8。

```bash
# 1. 初始化数据库
mysql -uroot -p < mysql/schema.sql

# 2. 启动后端（默认连接 localhost:3306，账号 music/mymusic123，可用环境变量覆盖）
cd backend
mvn spring-boot:run

# 3. 启动前端（已配置代理，/api 和 /media 转发到 8080）
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

## 生产部署

见 [docs/部署操作指南.md](docs/部署操作指南.md)（Docker Compose 主方案，含 1核1G 服务器内存调优）。

快速路径：

```bash
cd backend  && mvn package -DskipTests
cd frontend && npm run build
# 然后把 deploy/、backend/target/*.jar、frontend/dist/ 按指南上传服务器
cd ../deploy && docker compose up -d --build
```

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/songs?keyword= | 歌曲列表 / 按歌名歌手搜索 |
| POST | /api/songs/upload | 上传音频（multipart 字段名 file） |
| DELETE | /api/songs/{id} | 删除歌曲（记录 + 文件） |
| POST | /api/songs/{id}/play | 播放计数 +1 |
| GET | /media/songs/{fileName} | 音频流（支持 Range 断点） |
| GET | /media/covers/{coverName} | 封面图片 |
