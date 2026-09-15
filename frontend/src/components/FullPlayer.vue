<template>
  <el-drawer
    v-model="visible"
    direction="btt"
    size="100%"
    :with-header="false"
    :append-to-body="true"
    class="full-player-drawer"
  >
    <div class="full-player">
      <div class="fp-header">
        <el-icon size="22" class="collapse" @click="visible = false"><ArrowDownBold /></el-icon>
        <span class="fp-title">正在播放</span>
        <span class="view-switch" @click="toggleView">
          {{ showLyrics ? '黑胶' : '歌词' }}
        </span>
      </div>

      <!-- 展示区：黑胶与歌词二选一，点击切换 -->
      <div class="fp-body" @click="toggleView">
        <!-- 黑胶 -->
        <div v-show="!showLyrics" class="vinyl-area">
          <div class="tone-arm" :class="{ on: player.playing }">
            <div class="arm-pivot"></div>
            <div class="arm-stick"></div>
            <div class="arm-head"></div>
          </div>
          <div class="vinyl" :class="{ spinning: player.playing }">
            <img v-if="cover" class="label" :src="cover" alt="" />
            <div v-else class="label"></div>
            <div class="hole"></div>
          </div>
        </div>

        <!-- 歌词 -->
        <div v-show="showLyrics" class="lyrics-pane" ref="lyricsPane">
          <div class="lyrics-list" ref="lyricsList" :style="{ transform: `translateY(${translateY}px)` }">
            <template v-if="player.lyricsStatus === 'loaded'">
              <div
                v-for="(line, i) in player.lyrics"
                :key="i"
                class="lyric-line"
                :class="{ active: i === activeLineIndex }"
                @click.stop="seek(line.time)"
              >{{ line.text || '♪' }}</div>
            </template>
            <div v-else class="lyrics-placeholder">
              <template v-if="player.lyricsStatus === 'loading'">正在加载歌词…</template>
              <template v-else-if="player.lyricsStatus === 'empty'">暂无歌词<br>尽情欣赏旋律</template>
              <template v-else-if="player.lyricsStatus === 'error'">歌词加载失败</template>
            </div>
          </div>
        </div>
      </div>

      <div class="view-hint-outer" @click="toggleView">点击切换{{ showLyrics ? '黑胶' : '歌词' }}</div>

      <div class="glass-panel" @click.stop>
        <div class="song-info">
          <div class="title">{{ player.current?.title }}</div>
          <div class="artist">{{ player.current?.artist || '未知歌手' }}</div>
        </div>

        <div class="progress">
          <span class="time">{{ fmt(sliderValue) }}</span>
          <el-slider
            class="slider"
            v-model="sliderValue"
            :max="sliderMax"
            :step="0.1"
            :show-tooltip="false"
            @input="onDrag"
            @change="onChange"
          />
          <span class="time">{{ fmt(sliderMax) }}</span>
        </div>

        <div class="controls">
          <!-- 播放模式：绝对定位在控制区最左 -->
          <span class="mode-wrap" @click="cyclePlayMode" :title="modeTitle">
            <!-- 顺序播放 -->
            <svg v-if="player.playMode === 'order'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
            <!-- 单曲循环 -->
            <svg v-else-if="player.playMode === 'one'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/><text x="12" y="15.5" text-anchor="middle" font-size="8" font-weight="700" fill="currentColor" stroke="none">1</text></svg>
            <!-- 随机播放 -->
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>
          </span>
          <el-icon size="30" class="ctrl" @click="prev"><PrevIcon /></el-icon>
          <el-icon size="48" class="ctrl main" @click="toggle">
            <VideoPause v-if="player.playing" />
            <VideoPlay v-else />
          </el-icon>
          <el-icon size="30" class="ctrl" @click="next"><NextIcon /></el-icon>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
export default { name: 'FullPlayer' }
</script>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowDownBold, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { h } from 'vue'
import {
  player, coverUrl, seek, togglePlay, next, prev, cyclePlayMode, activeLineIndex
} from '../store/player'

// 播放模式提示文字
const MODE_TITLES = { order: '顺序播放', one: '单曲循环', random: '随机播放' }
const modeTitle = computed(() => MODE_TITLES[player.playMode] || '顺序播放')

// 上一首/下一首图标（Element Plus 没有对应的切歌图标）
const PrevIcon = () =>
  h('svg', { viewBox: '0 0 24 24', width: '1em', height: '1em', fill: 'currentColor' }, [
    h('path', { d: 'M7 5h2.2v14H7zM20 5.5v13L10 12z' })
  ])
const NextIcon = () =>
  h('svg', { viewBox: '0 0 24 24', width: '1em', height: '1em', fill: 'currentColor' }, [
    h('path', { d: 'M14.8 5H17v14h-2.2zM4 5.5v13L14 12z' })
  ])

const visible = ref(false)
const dragging = ref(false)
const sliderValue = ref(0)
const lyricsPane = ref(null)
const lyricsList = ref(null)
const translateY = ref(0)
const showLyrics = ref(true)

const placeholder =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120"><rect width="120" height="120" fill="#2c2f38"/><text x="60" y="82" font-size="56" text-anchor="middle" fill="#86909c">♪</text></svg>`
  )
const cover = computed(() =>
  player.current ? coverUrl(player.current) || placeholder : ''
)

// 音频元数据拿不到时长时，退回数据库里存的时长
const sliderMax = computed(() =>
  player.duration > 0 ? player.duration : player.current?.durationSec || 100
)

function fmt(s) {
  if (s == null || isNaN(s) || s <= 0) return '00:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function toggle() {
  togglePlay(player.current)
}

function toggleView() {
  showLyrics.value = !showLyrics.value
}

// 拖动中：只更新滑块显示，不真正 seek，避免与 timeupdate 冲突
function onDrag() {
  dragging.value = true
}

// 松手：seek 到目标位置
function onChange(v) {
  seek(v)
  dragging.value = false
}

// 歌词滚动：用「行索引 × 行高」计算位移，避免 offsetTop 在 flex 居中/变换下的不稳定。
// 给列表上下各加半屏-padding，使第 0 行在 translateY=0 时正好位于垂直中线。
async function updateLyricsScroll() {
  await nextTick()
  const pane = lyricsPane.value
  const list = lyricsList.value
  if (!pane || !list) return
  const firstLine = list.querySelector('.lyric-line')
  if (!firstLine) {
    translateY.value = 0
    return
  }
  const lineH = firstLine.offsetHeight
  const half = Math.max(0, pane.clientHeight / 2 - lineH / 2)
  list.style.paddingTop = half + 'px'
  list.style.paddingBottom = half + 'px'

  const idx = activeLineIndex.value
  if (idx < 0) {
    translateY.value = 0
    return
  }
  translateY.value = -idx * lineH
}

watch(
  [activeLineIndex, () => player.lyrics, () => player.lyricsStatus, showLyrics],
  updateLyricsScroll,
  { flush: 'post' }
)

function onResize() {
  updateLyricsScroll()
}

watch(
  () => player.currentTime,
  (v) => {
    if (!dragging.value) sliderValue.value = v
  }
)

// 切歌时重置
watch(
  () => player.current,
  () => {
    dragging.value = false
    sliderValue.value = 0
    translateY.value = 0
    // 当前歌被删除时自动收起
    if (!player.current) visible.value = false
  }
)

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

// 供 PlayerBar 通过模板 ref 打开
defineExpose({ visible })
</script>

<style scoped>
/* 播放器局部主题变量：浅色 / 深色分别覆盖 */
.full-player {
  --fp-bg: linear-gradient(180deg, #f7f8fa 0%, #eef1f5 70%, #e6e8ed 100%);
  --fp-fg: #1f2329;
  --fp-fg-soft: rgba(31,35,41,0.6);
  --fp-fg-muted: rgba(31,35,41,0.45);
  --fp-fg-faint: rgba(31,35,41,0.25);
  --fp-glass-bg: rgba(31,35,41,0.04);
  --fp-glass-border: rgba(31,35,41,0.1);
  --fp-lyric-inactive: rgba(31,35,41,0.35);
  --fp-lyric-hover: rgba(31,35,41,0.7);
  --fp-slider-runway: rgba(31,35,41,0.15);
}

.full-player {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 24px 24px;
  background: var(--fp-bg);
  color: var(--fp-fg);
  position: relative;
  overflow: hidden;
  transition: background .2s, color .2s;
}

.fp-header {
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}
.collapse { cursor: pointer; color: var(--fp-fg-soft); }
.collapse:hover { color: var(--fp-fg); }
.fp-title {
  flex: 1;
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--fp-fg-muted);
}
.view-switch {
  font-size: 13px;
  color: var(--fp-fg-soft);
  cursor: pointer;
  padding: 4px 10px;
  border: 1px solid var(--fp-glass-border);
  border-radius: 4px;
  transition: color .15s, border-color .15s;
}
.view-switch:hover { color: var(--fp-fg); border-color: var(--fp-fg-soft); }

.fp-body {
  flex: 1;
  width: 100%;
  max-width: 880px;
  margin: 8px auto 0;
  min-height: 0;
  overflow: hidden;
  position: relative;
  z-index: 1;
  cursor: pointer;
}
.view-hint-outer {
  font-size: 11px;
  color: var(--fp-fg-faint);
  text-align: center;
  cursor: pointer;
  padding: 6px 0 2px;
  user-select: none;
  transition: color .15s;
}
.view-hint-outer:hover { color: var(--fp-fg-soft); }

/* 黑胶 */
.vinyl-area {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 唱片尺寸统一用变量，指针位置/长度据此计算，保证贴合 */
  --vinyl-size: min(46vh, 320px);
  --vinyl-r: calc(var(--vinyl-size) / 2);
}
.tone-arm {
  position: absolute;
  /* pivot 固定在唱片右上方边缘外：水平刚出右边缘、垂直在顶部稍上 */
  top: calc(50% - var(--vinyl-r) - 14px);
  left: calc(50% + var(--vinyl-r) - 30px);
  width: 12px;
  /* 指针长度略大于半径，旋转落下时头部刚好落在唱片右边缘内的沟槽区 */
  height: calc(var(--vinyl-r) * 1.05);
  transform-origin: 50% 8px;
  transform: rotate(-38deg);
  transition: transform 0.6s ease;
  z-index: 2;
}
.tone-arm.on { transform: rotate(8deg); }
.arm-pivot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #7a7f8c, #3a3d46);
  margin: 0 auto;
  box-shadow: 0 2px 4px rgba(0,0,0,0.4);
}
.arm-stick {
  width: 3px;
  height: calc(100% - 30px);
  margin: 0 auto;
  background: linear-gradient(#b9bec9, #8d929e);
  border-radius: 2px;
}
.arm-head {
  width: 12px;
  height: 16px;
  margin: 0 auto;
  background: #cfd4de;
  border-radius: 2px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.3);
}
.vinyl {
  position: relative;
  width: var(--vinyl-size);
  aspect-ratio: 1;
  border-radius: 50%;
  background: repeating-radial-gradient(circle at 50% 50%, #141414 0 2px, #222226 2px 4px);
  box-shadow: 0 14px 40px rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: vinyl-spin 18s linear infinite;
  animation-play-state: paused;
}
.vinyl.spinning { animation-play-state: running; }
.label {
  position: absolute;
  width: 38%;
  height: 38%;
  border-radius: 50%;
  object-fit: cover;
  background: #2c2f38;
  box-shadow: 0 0 0 4px rgba(0,0,0,0.55);
}
.hole {
  position: absolute;
  width: 3.5%;
  height: 3.5%;
  border-radius: 50%;
  background: #0c0c0e;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
@keyframes vinyl-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 歌词 */
.lyrics-pane {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.lyrics-list {
  transition: transform 0.45s ease;
  will-change: transform;
}
.lyric-line {
  padding: 10px 4px;
  font-size: 15px;
  color: var(--fp-lyric-inactive);
  cursor: pointer;
  transition: color 0.25s;
  text-align: center;
  user-select: none;
  line-height: 1.5;
}
.lyric-line.active {
  color: var(--fp-fg);
  font-weight: 500;
}
.lyric-line:hover { color: var(--fp-lyric-hover); }
.lyrics-placeholder {
  text-align: center;
  color: var(--fp-fg-muted);
  font-size: 14px;
  padding: 40px 12px;
  line-height: 2;
}

/* 控制面板 */
.glass-panel {
  width: 100%;
  max-width: 540px;
  margin-top: 10px;
  padding: 14px 22px 16px;
  border-radius: 14px;
  background: var(--fp-glass-bg);
  border: 1px solid var(--fp-glass-border);
  position: relative;
  z-index: 1;
}
.song-info { text-align: center; }
.title { font-size: 17px; font-weight: 600; }
.artist { font-size: 13px; color: var(--fp-fg-muted); margin-top: 3px; }
.progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}
.progress .slider { flex: 1; }
.time {
  font-size: 12px;
  color: var(--fp-fg-muted);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.controls {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 44px;
  margin-top: 10px;
}
/* 播放模式按钮：绝对定位在控制区左边缘，垂直居中 */
.mode-wrap {
  position: absolute;
  left: 4px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  cursor: pointer;
  color: var(--fp-fg-soft);
  transition: color .15s;
}
.mode-wrap:hover { color: var(--fp-fg); }
.mode-wrap svg { width: 20px; height: 20px; }
.ctrl { cursor: pointer; color: var(--fp-fg-soft); }
.ctrl:hover { color: var(--fp-fg); }
.ctrl.main { color: var(--fp-fg); }

@media (max-width: 768px) {
  .full-player { padding: 10px 16px 20px; }
  .vinyl { width: min(56vw, 200px); }
  .lyric-line { font-size: 14px; padding: 9px 4px; }
  .controls { gap: 36px; }
  .mode-wrap svg { width: 18px; height: 18px; }
}

/* Element Plus 滑块各部分颜色跟随主题 */
.full-player :deep(.el-slider__runway) { background: var(--fp-slider-runway); height: 4px; }
.full-player :deep(.el-slider__bar) { background: var(--fp-fg); }
.full-player :deep(.el-slider__button) {
  border-color: var(--fp-fg);
  background: var(--fp-fg);
  width: 12px; height: 12px;
  border-width: 2px;
}
.full-player :deep(.el-slider__button:hover) { transform: scale(1.2); }
</style>

<style>
/* el-drawer 挂在 body 下，需用全局选择器覆盖 */
.full-player-drawer {
  background: transparent !important;
}
.full-player-drawer .el-drawer__body {
  padding: 0;
}

/* 深色模式变量覆盖（全局样式，不受 scoped 限制） */
html.dark .full-player {
  --fp-bg: linear-gradient(180deg, #23262e 0%, #1c1e26 70%, #14161c 100%);
  --fp-fg: #fff;
  --fp-fg-soft: rgba(255,255,255,0.6);
  --fp-fg-muted: rgba(255,255,255,0.55);
  --fp-fg-faint: rgba(255,255,255,0.25);
  --fp-glass-bg: rgba(255,255,255,0.06);
  --fp-glass-border: rgba(255,255,255,0.1);
  --fp-lyric-inactive: rgba(255,255,255,0.38);
  --fp-lyric-hover: rgba(255,255,255,0.75);
  --fp-slider-runway: rgba(255,255,255,0.18);
}
</style>
