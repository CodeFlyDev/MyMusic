<template>
  <div class="player-bar" v-if="player.current">
    <!-- 进度条：贴顶边，贯穿整条 -->
    <div class="top-progress">
      <div class="bar-fill" :style="{ width: progressPct + '%' }"></div>
    </div>

    <div class="bar-inner" @touchstart.passive="onTouchStart" @touchend.passive="onTouchEnd">
      <!-- 左：封面 + 歌曲信息 -->
      <div class="lead" @click="openFull">
        <img class="cover" :class="{ spinning: player.playing }" :src="cover" alt="" />
        <div class="info">
          <div class="title">{{ player.current.title }}</div>
          <div class="artist">{{ player.current.artist }}</div>
        </div>
      </div>

      <!-- 中：播放控制（播放模式/上一首/播放/下一首） -->
      <div class="controls">
        <button class="ctrl-btn mode-btn" @click="cyclePlayMode" :title="modeTitle">
          <!-- 顺序播放 -->
          <svg v-if="player.playMode === 'order'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
          <!-- 单曲循环 -->
          <svg v-else-if="player.playMode === 'one'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/><text x="12" y="15.5" text-anchor="middle" font-size="8" font-weight="700" fill="currentColor" stroke="none">1</text></svg>
          <!-- 随机播放 -->
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>
        </button>
        <button class="ctrl-btn" @click="goPrev" title="上一首"><PrevIcon /></button>
        <button class="ctrl-btn play" @click="toggle" :title="player.playing ? '暂停' : '播放'">
          <VideoPause v-if="player.playing" />
          <VideoPlay v-else />
        </button>
        <button class="ctrl-btn" @click="goNext" title="下一首"><NextIcon /></button>
      </div>

      <!-- 右：时间显示（桌面端） -->
      <div class="right-area desktop-only">
        <span class="time">{{ fmt(sliderValue) }}</span>
        <span class="time-sep">/</span>
        <span class="time">{{ fmt(sliderMax) }}</span>
      </div>
    </div>

    <FullPlayer ref="fullPlayer" />
  </div>
</template>

<script>
export default { name: 'PlayerBar' }
</script>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { VideoPlay, VideoPause } from '@element-plus/icons-vue'
import FullPlayer from './FullPlayer.vue'
import {
  player, togglePlay, seek, coverUrl, next, prev, cyclePlayMode
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

const dragging = ref(false)
const sliderValue = ref(0)
const fullPlayer = ref(null)

const placeholder =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="56" height="56"><rect width="56" height="56" rx="8" fill="#409eff"/><text x="28" y="36" font-size="26" text-anchor="middle" fill="#fff">♪</text></svg>`
  )
const cover = computed(() =>
  player.current ? coverUrl(player.current) || placeholder : placeholder
)

// 音频元数据拿不到时长时，退回数据库里存的时长
const sliderMax = computed(() =>
  player.duration > 0 ? player.duration : player.current?.durationSec || 100
)
// 顶部进度条百分比（0-100）
const progressPct = computed(() => {
  const max = sliderMax.value || 1
  return Math.min(100, (sliderValue.value / max) * 100)
})

function fmt(s) {
  if (s == null || isNaN(s) || s <= 0) return '00:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function toggle() {
  togglePlay(player.current)
}

function openFull() {
  if (fullPlayer.value) fullPlayer.value.visible = true
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

function goNext() { next() }
function goPrev() { prev() }

// 触摸滑动切歌（移动端）：左滑下一首，右滑上一首
// 以横向位移为主（|dx| > |dy| * 1.5）且超过 50px 阈值才触发；
// 500ms 防抖避免与点击打开全屏播放器的手势冲突
let touchStartX = 0
let touchStartY = 0
let lastSwipeTime = 0

function onTouchStart(e) {
  touchStartX = e.touches[0].clientX
  touchStartY = e.touches[0].clientY
}

function onTouchEnd(e) {
  const dx = e.changedTouches[0].clientX - touchStartX
  const dy = e.changedTouches[0].clientY - touchStartY
  if (Math.abs(dx) < 50 || Math.abs(dx) <= Math.abs(dy) * 1.5) return
  const now = Date.now()
  if (now - lastSwipeTime < 500) return
  lastSwipeTime = now
  if (dx < 0) next()
  else prev()
}

watch(
  () => player.currentTime,
  (v) => {
    if (!dragging.value) sliderValue.value = v
  }
)

// 切歌时重置滑块
watch(
  () => player.current,
  () => {
    dragging.value = false
    sliderValue.value = 0
  }
)
</script>

<style scoped>
.player-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  z-index: 100;
  transition: background .2s, border-color .2s;
}

/* 顶部进度条（贯穿整条，移动端唯一进度指示） */
.top-progress {
  height: 4px;
  background: var(--border);
  position: relative;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: var(--accent);
  transition: width .15s linear;
}

.bar-inner {
  display: flex; align-items: center; gap: 16px;
  padding: 0 24px;
  height: 64px;
}

/* 左：封面 + 歌曲信息 */
.lead {
  display: flex; align-items: center; gap: 12px;
  flex: 1; min-width: 0;
  cursor: pointer; overflow: hidden;
}
.cover {
  width: 44px; height: 44px; border-radius: 50%;
  object-fit: cover; display: block; flex-shrink: 0;
  background: var(--bg-cover);
  /* 始终圆形 + 始终应用旋转动画，通过 play-state 控制暂停/恢复，
     暂停时保持当前角度，避免形状切换导致的黑边 */
  animation: cover-spin 18s linear infinite;
  animation-play-state: paused;
}
@keyframes cover-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.info { overflow: hidden; flex: 1; }
.title { font-weight: 500; font-size: 13px; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.artist { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }

/* 中：播放控制 */
.controls {
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0;
}
.ctrl-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: none; background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: color .15s, background .15s;
  padding: 0;
}
.ctrl-btn svg { width: 18px; height: 18px; }
.ctrl-btn:hover { color: var(--text-primary); background: var(--bg-hover); }
.ctrl-btn.play {
  width: 38px; height: 38px;
  color: var(--accent-fg);
  background: var(--accent);
}
.ctrl-btn.play svg { width: 20px; height: 20px; }
.ctrl-btn.play:hover { opacity: .85; background: var(--accent); }

/* 右：时间显示（桌面端） */
.right-area {
  flex-shrink: 0;
  display: flex; align-items: center; gap: 6px;
  justify-content: flex-end;
}
.time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; font-variant-numeric: tabular-nums; }
.time-sep { font-size: 11px; color: var(--text-faint); }

/* 移动端 */
@media (max-width: 768px) {
  .bar-inner { gap: 10px; padding: 0 12px; height: 56px; }
  .lead { flex: 1; width: auto; min-width: 0; }
  .desktop-only { display: none; }
  .ctrl-btn { width: 28px; height: 28px; }
  .ctrl-btn svg { width: 16px; height: 16px; }
  .ctrl-btn.play { width: 34px; height: 34px; }
  .ctrl-btn.play svg { width: 18px; height: 18px; }
}
</style>
