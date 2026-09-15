<template>
  <div class="library-page">
    <!-- Hero 横幅区 -->
    <section class="hero">
      <!-- 唱片墙背景：热门单曲 + 最近上传封面拼接，整体倾斜 -->
      <div v-if="wallTiles.length" class="hero-wall" aria-hidden="true">
        <div class="wall-grid">
          <div
            v-for="(url, i) in wallTiles"
            :key="i"
            class="wall-tile"
            :style="tileStyle(i)"
          ></div>
        </div>
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">Music音乐·更简单</h1>
        <p class="hero-subtitle">你的私人音乐空间，随时随地享受纯净聆听</p>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalSongs }}</span>
            <span class="stat-label">首歌曲</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalArtists }}</span>
            <span class="stat-label">位歌手</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalAlbums }}</span>
            <span class="stat-label">张专辑</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalPlays }}</span>
            <span class="stat-label">次播放</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 主要内容区：PC 双栏并排，手机端上下堆叠 -->
    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="songs.length" class="module-row">
      <section class="module">
        <div class="module-title">
          <span>热门单曲</span>
          <router-link to="/search" class="more">查看全部</router-link>
        </div>
        <SongRow
          v-for="song in hotSongs"
          :key="song.id"
          :song="song"
          @play="$emit('play', $event)"
        >
          {{ song.playCount || 0 }} 次播放
        </SongRow>
      </section>

      <section class="module">
        <div class="module-title">
          <span>最近上传</span>
        </div>
        <SongRow
          v-for="song in recentSongs"
          :key="'r-' + song.id"
          :song="song"
          @play="$emit('play', $event)"
        >
          {{ formatDateTime(song.createdAt) }}
        </SongRow>
      </section>
    </div>
    <div v-else class="state">还没有歌曲，<router-link to="/admin" class="link">去管理后台上传</router-link></div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listSongs, listArtists, listAlbums, formatDateTime } from '../api'
import { setQueue } from '../store/player'
import SongRow from '../components/SongRow.vue'

defineEmits(['play'])

const songs = ref([])
const loading = ref(true)
const stats = ref({
  totalSongs: 0,
  totalArtists: 0,
  totalAlbums: 0,
  totalPlays: 0
})

// Hero 唱片墙背景：热门单曲 5 首 + 最近上传 5 首的封面拼接（参考网易云"唱片墙·角度"启动图）
const hotSongs = computed(() =>
  [...songs.value].sort((a, b) => (b.playCount || 0) - (a.playCount || 0)).slice(0, 5)
)
// 接口默认按 id 倒序返回，即最近上传在前
const recentSongs = computed(() => songs.value.slice(0, 5))

// 封面池洗牌后循环填充至 15 格（3行×5列），相对位置每次刷新随机
const wallTiles = computed(() => {
  const pool = [...hotSongs.value, ...recentSongs.value]
    .map(s => s.coverName ? `/media/covers/${s.coverName}` : '')
    .filter(Boolean)
  if (!pool.length) return []
  const shuffled = [...pool]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return Array.from({ length: 15 }, (_, i) => shuffled[i % shuffled.length])
})

// 每块封面固定伪随机小角度，形成错落的"角度"质感
const TILE_ANGLES = [3, -2, 4, -3, 2, -4, 3, -2, 4, -3, 2, -4, 3, -2, 4]
function tileStyle(i) {
  return {
    backgroundImage: `url(${wallTiles.value[i]})`,
    transform: `rotate(${TILE_ANGLES[i % TILE_ANGLES.length]}deg)`
  }
}

async function loadData() {
  try {
    const [songsData, artistsData, albumsData] = await Promise.all([
      listSongs().catch(() => []),
      listArtists().catch(() => []),
      listAlbums().catch(() => [])
    ])
    songs.value = songsData
    stats.value = {
      totalSongs: songsData.length,
      totalArtists: artistsData.length,
      totalAlbums: albumsData.length,
      totalPlays: songsData.reduce((sum, s) => sum + (s.playCount || 0), 0)
    }
    setQueue(songsData)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.library-page { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

/* Hero 区 */
.hero {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  min-height: 280px;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, var(--accent) 0%, #2d3a4f 100%);
}
/* 唱片墙：整体倾斜铺满，超出部分裁剪 */
.hero-wall {
  position: absolute;
  inset: -18%;
  z-index: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(8deg);
}
.wall-grid {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-auto-rows: 1fr;
  gap: 12px;
}
.wall-tile {
  background-size: cover;
  background-position: center;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}
.hero-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(135deg, rgba(0,0,0,0.62) 0%, rgba(0,0,0,0.35) 100%);
}
.hero-content {
  position: relative;
  z-index: 2;
  padding: 48px 40px;
  color: #fff;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}
.hero-title {
  margin: 0 0 8px;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.hero-subtitle {
  margin: 0 0 28px;
  font-size: 16px;
  opacity: 0.9;
  font-weight: 400;
}
.hero-stats {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}
.stat-label {
  font-size: 13px;
  opacity: 0.8;
}

/* 公告栏下方间距已在组件内处理 */

/* 模块区：PC 端两栏并排，窄屏（手机）单列上下堆叠 */
.module-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  align-items: start;
  margin-bottom: 32px;
}
.module-row .module { margin-bottom: 0; }
.module { margin-bottom: 32px; }
.module-title {
  font-size: 15px; font-weight: 600; color: var(--text-primary);
  margin-bottom: 12px; display: flex; align-items: baseline; justify-content: space-between;
}
.more {
  font-size: 13px; color: var(--text-muted); text-decoration: none;
  transition: color .15s;
}
.more:hover { color: var(--text-primary); }
.state {
  text-align: center; color: var(--text-faint); padding: 60px 0; font-size: 14px;
}
.link { color: var(--accent); text-decoration: none; font-weight: 500; }
.link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  .hero { min-height: 220px; border-radius: 12px; margin-bottom: 16px; }
  .hero-wall { inset: -22%; }
  .wall-grid { gap: 8px; grid-template-columns: repeat(4, 1fr); }
  .wall-tile { border-radius: 8px; }
  .module-row { grid-template-columns: 1fr; gap: 24px; }
  .hero-content { padding: 32px 20px; }
  .hero-title { font-size: 26px; }
  .hero-subtitle { font-size: 14px; }
  .hero-stats { gap: 20px; }
  .stat-value { font-size: 20px; }
  .stat-label { font-size: 12px; }
}
</style>