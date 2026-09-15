# 歌词滚动展示功能 实施计划

## Context

当前 my-music 项目播放器（[FullPlayer.vue](file:///e:/Gitee/my-music/frontend/src/components/FullPlayer.vue)）只有黑胶唱片动画 + 控制面板，没有歌词同步展示。用户希望在播放歌曲时看到逐行滚动高亮的歌词，提升听歌体验。

本次需求基于现有架构扩展：**歌词来源采用「内嵌 ID3 + 联网兜底」策略**（与封面匹配流程一致），**联网匹配在上传后异步执行**避免拖慢上传响应，**全屏播放器展示歌词滚动**（底部播放条不动），并在 **Upload 页加歌词手动编辑入口**作为补充。

## 设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 歌词来源 | 内嵌 ID3 `FieldKey.LYRICS` + 联网网易云兜底 | 与封面同流程，复用 `searchNetEaseCover` 的网易云歌曲 id 搜索 |
| 存储格式 | 原始 LRC 文本（TEXT 列） | 紧凑（数十 KB），解析放前端，规则升级无需数据迁移 |
| 接口 | `GET /api/songs/{id}/lyrics` 按需获取；`PUT /api/songs/{id}/lyrics` 手动编辑 | 避免列表接口膨胀 |
| 联网时机 | **异步**：上传响应返回后由 `@Async` 方法补抓歌词入库 | 上传快，用户上传后短时间内可能歌词还没就绪，前端兜底"暂无歌词" |
| 展示位置 | 仅 FullPlayer 全屏播放器 | PlayerBar 空间紧张不动 |
| 滚动方案 | CSS `transform: translateY()` 平移 + `transition` | 不抢占原生滚动，移动端不与 touch 冲突 |
| 同步行计算 | `watch(activeLineIndex, flush: 'post')` 测量当前行 offsetTop 计算 translateY | 避免 computed 读 DOM 不响应 |

## 改动文件清单

### 后端（7 个文件，1 个新建）

| 文件 | 操作 | 关键改动 |
|---|---|---|
| [mysql/schema.sql](file:///e:/Gitee/my-music/mysql/schema.sql) | 修改 | songs 表加 `lyrics TEXT NULL` 列；末尾追加 ALTER 注释供已有库升级 |
| [backend/.../entity/Song.java](file:///e:/Gitee/my-music/backend/src/main/java/com/mymusic/entity/Song.java) | 修改 | 新增 `private String lyrics;` + getter/setter |
| [backend/.../mapper/SongMapper.java](file:///e:/Gitee/my-music/backend/src/main/java/com/mymusic/mapper/SongMapper.java) | 修改 | `insert` SQL 加 `lyrics` 列；新增 `findLyricsById(id)` 只查歌词列；新增 `updateLyrics(id, lyrics)` 用于异步补抓和手动编辑 |
| [backend/.../service/SongService.java](file:///e:/Gitee/my-music/backend/src/main/java/com/mymusic/service/SongService.java) | 修改 | 1) `parseTags` 内追加 `tag.getFirst(FieldKey.LYRICS)` 提取内嵌歌词；2) 抽取 `findNetEaseSongIds(query, limit)` 公共方法（封面与歌词共用）；3) `searchNetEaseCover` 改为接收 ids 列表参数；4) 新增 `getLyrics(id)` 和 `updateLyrics(id, text)` 公开方法 |
| **backend/.../service/LyricsAsyncService.java** | **新建** | `@Service` + `@Async` 方法 `fetchLyricsAndSave(Long songId, String query)`：内部调用网易云歌词 API `https://music.163.com/api/song/lyric?id={id}&lv=-1&kv=-1&tv=-1`，解析 `lrc.lyric` 字段，若拿到非空 LRC 则 `songMapper.updateLyrics`；含 `findNetEaseSongIds` 调用与 cover 共用一次搜索结果（也可独立搜索） |
| [backend/.../controller/SongController.java](file:///e:/Gitee/my-music/backend/src/main/java/com/mymusic/controller/SongController.java) | 修改 | 新增 `GET /{id}/lyrics` 返回歌词文本；新增 `PUT /{id}/lyrics`（需 `X-Upload-Code` 口令）手动编辑歌词 |
| [backend/.../MusicApplication.java](file:///e:/Gitee/my-music/backend/src/main/java/com/mymusic/MusicApplication.java) | 修改 | 加 `@EnableAsync` 启用异步方法 |

### 前端（5 个文件，1 个新建）

| 文件 | 操作 | 关键改动 |
|---|---|---|
| **frontend/src/utils/lrc.js** | **新建** | `parseLrc(text)` → `{lines: [{time, text}]}`，处理多时间戳行 `[00:12.50][00:30.00]歌词`、元数据行 `[ti:]/[ar:]/[offset:]`、空行；`findLineIndex(lines, t)` 二分查找返回最后一条 `time <= t` 的索引 |
| [frontend/src/api.js](file:///e:/Gitee/my-music/frontend/src/api.js) | 修改 | 新增 `getLyrics(id)` → 返回 LRC 原文；`updateLyrics(id, text, code)` → PUT 请求 |
| [frontend/src/store/player.js](file:///e:/Gitee/my-music/frontend/src/store/player.js) | 修改 | reactive 加 `lyrics: []` + `lyricsStatus: 'idle'`；导出 `activeLineIndex` computed；新增 `loadLyrics(song)` 含并发 token 防过期；watch `player.current` 触发 loadLyrics；同 id 跳过避免重复请求 |
| [frontend/src/components/FullPlayer.vue](file:///e:/Gitee/my-music/frontend/src/components/FullPlayer.vue) | 修改 | 把 `vinyl-area` 包到 `fp-body` Grid（桌面 1fr 1fr，移动单列 38vh + 1fr），右侧新增 `lyrics-pane`：transform 平移当前行到中线、当前行高亮加粗放大、点击行 `seek(line.time)`；loading/empty/error 三态降级文案 |
| [frontend/src/views/Upload.vue](file:///e:/Gitee/my-music/frontend/src/views/Upload.vue) | 修改 | 歌曲管理列表每行加「编辑歌词」按钮，点击弹出 `el-dialog` 含 `el-input type="textarea"`，提交时调 `updateLyrics` |

[frontend/src/components/PlayerBar.vue](file:///e:/Gitee/my-music/frontend/src/components/PlayerBar.vue) **不改**。

## 关键实现要点

### 后端：异步匹配流程

```java
// SongService.upload()
parseTags(...);              // 同步：解析内嵌 ID3 标签 + 内嵌歌词
songMapper.insert(song);     // 入库（lyrics 可能为 null）

// 异步：联网补抓封面和歌词（仅当缺失时）
if (isBlank(song.getCoverName()) || isBlank(song.getLyrics())) {
    String query = isBlank(song.getArtist()) || "未知歌手".equals(song.getArtist())
        ? song.getTitle() : song.getArtist() + " " + song.getTitle();
    // 异步执行，upload 立即返回
    lyricsAsyncService.fetchLyricsAndSave(song.getId(), query);
    // 注：封面匹配也可一并迁移到异步，但本次范围只动歌词；封面保持原同步逻辑
}
```

`LyricsAsyncService` 关键点：
- `@Async` 方法不能被同类内部直接调用（绕过代理），所以必须独立类
- 内部用 `findNetEaseSongIds`（与 `SongService.searchNetEaseCover` 同实现，可抽到公共 helper 或独立复制一份）
- 拿到第一个 id 后调网易云歌词 API，解析 `lrc.lyric` 字段
- 拿到非空 LRC → `songMapper.updateLyrics(songId, lrc)`
- 异常全 catch，避免后台线程崩

### 后端：网易云歌词 API 响应

```json
{
  "lrc": { "lyric": "[00:12.50]第一句\n[00:17.30]第二句\n...", "version": 6 },
  "tlyric": { "lyric": "...", "version": 6 },  // 翻译歌词（本期不用）
  "klyric": { ... },                           // 卡拉OK逐字（本期不用）
  "code": 200
}
```
只取 `lrc.lyric` 字段。

### 前端：LRC 解析边界

```js
// 多时间戳行展开为多条
"[00:12.50][00:30.00]歌词" → [{time: 12.5, text: "歌词"}, {time: 30, text: "歌词"}]
// 元数据行
"[ti:歌曲名]" → 跳过
"[offset:200]" → 应用 +0.2s 偏移到所有 time
// 空行跳过
// 最后按 time 升序排序
```

### 前端：滚动 transform 计算

```js
const translateY = ref(0)
watch([activeLineIndex, () => player.lyrics], async () => {
  await nextTick()
  const pane = lyricsPane.value
  if (!pane) return
  const el = pane.querySelector('.lyric-line.active')
  if (!el) { translateY.value = 0; return }
  translateY.value = pane.clientHeight / 2 - el.offsetTop - el.offsetHeight / 2
}, { flush: 'post' })
```
模板：`<div class="lyrics-list" :style="{ transform: 'translateY(' + translateY + 'px)' }">`

### 前端：Upload.vue 歌词编辑入口

- 管理列表每行加「编辑」按钮（el-button text small）
- 点击打开 `el-dialog`，title="编辑歌词 - {歌名}"
- 内部 `el-input type="textarea" :rows="20"` placeholder 提示 LRC 格式
- 确认按钮调 `updateLyrics(id, text, code)` → 成功后 `ElMessage.success` + 关闭弹窗 + 重新加载列表
- 失败时若口令失效则 lockPage

## 实施顺序

**阶段 1：后端**
1. `schema.sql` + 已有库执行 ALTER
2. `Song.java` 加字段
3. `SongMapper.java` 改 insert + 新增 findLyricsById/updateLyrics
4. `MusicApplication.java` 加 `@EnableAsync`
5. `LyricsAsyncService.java` 新建（含 fetchLyricsOnline + findNetEaseSongIds 复制或抽取）
6. `SongService.java` 改 parseTags + upload 调用异步 + getLyrics/updateLyrics
7. `SongController.java` 加 GET/PUT /{id}/lyrics

**阶段 2：前端**
1. `utils/lrc.js` 新建（可在浏览器 console 单元测试）
2. `api.js` 加 getLyrics/updateLyrics
3. `store/player.js` 加状态/计算属性/loadLyrics/watch
4. `FullPlayer.vue` Grid 改造 + 歌词滚动 + 降级文案
5. `Upload.vue` 加编辑入口

## 验证步骤

### 后端验证
1. 执行 `ALTER TABLE songs ADD COLUMN lyrics TEXT NULL COMMENT '原始LRC歌词文本' AFTER cover_name;`
2. 启动后端，看日志无报错
3. 上传带内嵌歌词的 MP3（用 Mp3tag 写 USLT 帧）：上传后立即 `SELECT lyrics FROM songs` 应有 LRC 文本
4. 上传无内嵌歌词的热门中文歌：上传响应立即返回；等待几秒后 `SELECT lyrics FROM songs` 应被异步填充（后端日志出现 `已联网匹配歌词`）
5. 接口：`curl http://localhost:8080/api/songs/{id}/lyrics` → `{code:0, data:{lyrics: "..."}}`
6. PUT 测试：`curl -X PUT -H "X-Upload-Code: 10010812" -d '{"lyrics":"[00:01.00]测试"}' http://localhost:8080/api/songs/{id}/lyrics` 应返回成功

### 前端验证
7. 启动 `npm run dev`，播放有歌词的歌曲：
   - FullPlayer 右侧展示完整歌词列表
   - 当前行白色加粗放大，整体平移让当前行位于中线
8. 拖动进度条到中间位置：松手后歌词立刻对齐到对应行
9. 点击任一非当前行：音频跳播到该行时间戳，立刻成为当前行
10. 上一首/下一首切歌：旧歌词清空 → "加载歌词中…" → 新歌词加载
11. 播放无歌词歌曲：显示"暂无歌词，尽情欣赏旋律"
12. 上传新歌后等几秒，重新进播放器：异步匹配的歌词应已就绪
13. Upload 页点编辑歌词：能加载现有 LRC，编辑保存后播放器重新加载生效
14. 移动端尺寸 375×812：vinyl 上半 38vh，歌词下半，正常滚动

## 风险与兜底

| 风险 | 兜底策略 |
|---|---|
| 网易云限流/封 IP | 异步方法异常全 catch，失败保持 lyrics 为 NULL，不影响上传与播放 |
| `FieldKey.LYRICS` 返回非 LRC 纯文本歌词 | `parseLrc` 匹配不到任何时间戳 → `lines.length === 0` → 前端展示"暂无歌词" |
| 异步任务在用户立即播放时尚未完成 | 前端 `loadLyrics` 时若拿到 null 显示"暂无歌词"；用户可稍后重新进入或手动编辑 |
| 切歌并发导致歌词串台 | `loadLyrics` 用自增 token，await 后对比 token 过期丢弃 |
| transform 计算时 DOM 未挂载 | `watch(flush: 'post')` + `nextTick` + 守卫 `if (!pane.value) return` |
| 多时间戳行 + offset 元数据导致顺序错乱 | `parseLrc` 先展开多时间戳、应用 offset、最后 sort |

## 范围外（后续迭代）

- 翻译歌词（网易云 `tlyric` 字段，原文+译文双行展示）
- 卡拉OK逐字高亮（网易云 `klyric` 字段）
- 异步封面匹配（当前封面仍同步，可后续一并迁移到 LyricsAsyncService）
