## 优化方案：播放器界面 + 黑胶唱片动画 + 移动端歌曲库 + 音量调节

技术栈：Vue 3 + Element Plus（Vite），无 Pinia，播放器状态在 `frontend/src/store/player.js`（reactive 单例）。所有改动仅限前端 4 个文件 + 1 个新组件，后端不动。

### 1. 播放器状态扩展 — `frontend/src/store/player.js`
- 新增 `player.playlist`（播放队列，Library 加载的歌曲列表）、`player.volume`（0-100，持久化到 localStorage）
- 新增 `setQueue(songs, index)`、`playByIndex(i)`、`next()`、`prev()`（头尾循环）
- `ended` 事件改为自动调用 `next()` 顺序连播（替换现有"播完即停"）
- 新增 `setVolume(v)`：设 `audio.volume = v/100`，切歌后保留音量

### 2. 新组件：全屏播放器界面 — `frontend/src/components/FullPlayer.vue`
- 从底部播放条点击封面/信息区唤起，`el-drawer`（direction="btt"，全屏抽屉）+ slide-up 过渡实现移动端风格的上滑展开
- 界面内容（竖屏布局）：
  - **黑胶唱片动画**：纯 CSS 绘制黑胶唱片（径向渐变唱片纹理 + 封面图居中作为唱片标），`@keyframes spin` 匀速旋转，`animation-play-state: paused` 跟随暂停；播放时唱片旁"唱臂"摆上（CSS transform 旋转过渡），暂停时摆开
  - 歌名/歌手、进度条（可拖动 seek）+ 时间
  - 控制区：上一首 ⏮ / 播放暂停 ⏯（大按钮）/ 下一首 ⏭
  - 音量条：音量图标（静音切换）+ el-slider 调节，双向绑定 `player.volume`
  - 下滑收起按钮

### 3. 底部播放条升级 — `frontend/src/components/PlayerBar.vue`
- 新增上一首/下一首按钮、音量图标 + 音量滑块（桌面端横向显示）
- 点击封面/信息区打开 FullPlayer
- 移动端（`@media (max-width: 768px)`）：隐藏桌面音量滑块和时间文本，布局改为「封面 + 歌名 + 播放键」，信息区可点开全屏播放器

### 4. 歌曲库改版 — `frontend/src/views/Library.vue`
- 移除 `el-table`，改为歌曲列表卡片（每行：封面 56px + 歌名/歌手·专辑·时长 + 播放按钮 + 删除按钮），点击整行播放
- 当前播放中的歌曲高亮显示（播放图标替代封面角标）
- 搜索/上传工具栏保持；桌面端 `max-width` 容器内单列列表，间距优化
- 移动端竖屏：单列铺满、封面缩小、操作按钮精简为图标
- `load()` 成功后调用 `setQueue()` 同步播放队列

### 5. 全局移动端适配 — `frontend/src/App.vue`
- `@media (max-width: 768px)`：header 内边距收紧、`app-main` padding 缩小、隐藏品牌文字只留 🎵

### 验证方式
- `npm run dev` 启动前端 + 后端，浏览器桌面窗口验证完整播放/切歌/音量/黑胶动画
- 用 browser-use 工具把视口调成 390×844（iPhone 竖屏）验证歌曲库单列、播放条布局、全屏播放器上滑交互
- 运行 `npm run build` 确认构建通过