// 全局播放器状态：应用内唯一的音频播放核心
// 规则：队列顺序连播，头尾循环；音量记忆在 localStorage
// 播放计数：累计连续播放 60 秒才 +1，避免刷量
import { reactive, computed, watch } from 'vue'
import { markRaw } from 'vue'
import { getLyrics } from '../api'
import { parseLrc, findLineIndex } from '../utils/lrc'

const VOLUME_KEY = 'mymusic:volume'
const PLAYMODE_KEY = 'mymusic:playmode'
const HEARTBEAT_INTERVAL = 60000 // 60秒

function loadVolume() {
  const raw = localStorage.getItem(VOLUME_KEY)
  if (raw == null) return 80
  const v = Number(raw)
  return v >= 0 && v <= 100 ? v : 80
}

// 播放模式：order 顺序播放 | one 单曲循环 | random 随机播放
function loadPlayMode() {
  const raw = localStorage.getItem(PLAYMODE_KEY)
  return raw === 'one' || raw === 'random' ? raw : 'order'
}

export const player = reactive({
  current: null,      // 当前歌曲对象（来自歌曲库接口）
  playing: false,
  currentTime: 0,
  duration: 0,
  ready: false,
  playlist: [],       // 播放队列（歌曲库列表）
  playMode: loadPlayMode(), // order | one | random
  volume: loadVolume(),
  lyrics: [],         // 解析后的歌词行 [{ time, text }]
  lyricsStatus: 'idle' // idle | loading | loaded | empty | error
})

let audio = null
// 歌词加载并发 token：切歌后丢弃过期请求的结果
let lyricsReqId = 0
let lyricsSongId = null
// 心跳相关
let heartbeatTimer = null
let heartbeatStartTime = 0 // 记录本次播放开始的时间戳
let lastHeartbeatTime = 0  // 上次心跳时间

function getAudio() {
  if (!audio) {
    audio = markRaw(new Audio())
    audio.volume = player.volume / 100
    audio.addEventListener('timeupdate', () => {
      player.currentTime = audio.currentTime
    })
    audio.addEventListener('durationchange', () => {
      player.duration = audio.duration || player.duration
    })
    audio.addEventListener('ended', () => {
      stopHeartbeat()
      if (player.playMode === 'one') {
        // 单曲循环：重新播放当前歌曲
        playSong(player.current)
      } else {
        next()
      }
    })
    audio.addEventListener('pause', () => {
      player.playing = false
      stopHeartbeat()
    })
    audio.addEventListener('play', () => {
      player.playing = true
      startHeartbeat()
    })
    audio.addEventListener('seeking', () => {
      // 拖动进度条时暂停心跳，避免非连续播放计数
      stopHeartbeat()
    })
    audio.addEventListener('seeked', () => {
      // 拖动结束后恢复心跳
      if (player.playing) startHeartbeat()
    })
  }
  return audio
}

export function coverUrl(song) {
  return song && song.coverName ? `/media/covers/${song.coverName}` : ''
}

export function audioUrl(song) {
  return `/media/songs/${song.fileName}`
}

function startHeartbeat() {
  if (heartbeatTimer) return
  const now = Date.now()
  heartbeatStartTime = now
  lastHeartbeatTime = now
  heartbeatTimer = setInterval(() => {
    if (!player.playing || !player.current) {
      stopHeartbeat()
      return
    }
    const elapsed = Date.now() - heartbeatStartTime
    // 每满 60 秒上报一次
    if (elapsed >= HEARTBEAT_INTERVAL) {
      const songId = player.current.id
      fetch(`/api/songs/${songId}/heartbeat`, { method: 'POST' }).catch(() => {})
      heartbeatStartTime = Date.now() // 重置计时
    }
  }, 5000) // 每 5 秒检查一次，更精确
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  heartbeatStartTime = 0
  lastHeartbeatTime = 0
}

function playSong(song) {
  const a = getAudio()
  player.current = song
  player.currentTime = 0
  player.duration = song.durationSec || 0
  a.src = audioUrl(song)
  a.play()
  // 播放开始不再直接计数，改为心跳机制
  fetch(`/api/songs/${song.id}/play`, { method: 'POST' }).catch(() => {})
}

// 同步播放队列（歌曲库刷新时调用），定位到当前播放的歌曲
export function setQueue(songs) {
  player.playlist = songs
}

// 点击歌曲库中的歌曲：同一首则切换播放/暂停，不同首则从头播放
export function togglePlay(song) {
  const a = getAudio()
  if (player.current && song.id === player.current.id) {
    if (player.playing) {
      a.pause()
    } else {
      a.play()
    }
    return
  }
  playSong(song)
}

// 按队列索引播放（头尾循环）
export function playByIndex(i) {
  if (!player.playlist.length) return
  const idx = ((i % player.playlist.length) + player.playlist.length) % player.playlist.length
  playSong(player.playlist[idx])
}

export function currentIndex() {
  if (!player.current) return -1
  return player.playlist.findIndex((s) => s.id === player.current.id)
}

// 随机挑选一首（避开当前歌曲）
function playRandom() {
  let i
  do {
    i = Math.floor(Math.random() * player.playlist.length)
  } while (player.playlist.length > 1 && i === currentIndex())
  playByIndex(i)
}

// 切换播放模式：顺序播放 → 单曲循环 → 随机播放
export function cyclePlayMode() {
  const modes = ['order', 'one', 'random']
  const i = modes.indexOf(player.playMode)
  player.playMode = modes[(i + 1) % modes.length]
  localStorage.setItem(PLAYMODE_KEY, player.playMode)
}

export function next() {
  if (!player.playlist.length) return
  stopHeartbeat()
  if (player.playMode === 'random' && player.playlist.length > 1) {
    playRandom()
    return
  }
  playByIndex(currentIndex() + 1)
}

export function prev() {
  if (!player.playlist.length) return
  stopHeartbeat()
  if (player.playMode === 'random' && player.playlist.length > 1) {
    playRandom()
    return
  }
  playByIndex(currentIndex() - 1)
}

export function seek(time) {
  if (audio && player.current) {
    audio.currentTime = time
    player.currentTime = time
  }
}

export function setVolume(v) {
  player.volume = v
  if (audio) audio.volume = v / 100
  localStorage.setItem(VOLUME_KEY, String(v))
}

// 歌词 --------------------------------------------------------------------

// 当前播放时间对应的歌词行索引
export const activeLineIndex = computed(() =>
  findLineIndex(player.lyrics, player.currentTime)
)

async function loadLyrics(song) {
  const reqId = ++lyricsReqId
  if (!song || !song.id) {
    player.lyrics = []
    player.lyricsStatus = 'idle'
    lyricsSongId = null
    return
  }
  // 同一首歌不重复请求
  if (lyricsSongId === song.id && player.lyricsStatus === 'loaded') return
  lyricsSongId = song.id
  player.lyrics = []
  player.lyricsStatus = 'loading'
  try {
    const text = await getLyrics(song.id)
    if (reqId !== lyricsReqId) return // 已切歌，丢弃过期结果
    if (!text) {
      player.lyricsStatus = 'empty'
      return
    }
    const lines = parseLrc(text)
    player.lyrics = lines
    player.lyricsStatus = lines.length ? 'loaded' : 'empty'
  } catch (e) {
    if (reqId !== lyricsReqId) return
    player.lyricsStatus = 'error'
  }
}

// 强制重新加载当前歌曲歌词（手动编辑歌词后调用）
export function refreshLyrics() {
  lyricsSongId = null
  loadLyrics(player.current)
}

// 切歌时自动加载歌词
watch(() => player.current, (song) => {
  stopHeartbeat()
  loadLyrics(song)
}, { immediate: true })
