<template>
  <div>
    <div class="search-head">
      <span v-if="keyword">搜索 “{{ keyword }}” 的结果</span>
      <span v-else>输入关键词开始搜索</span>
    </div>

    <template v-if="keyword">
      <div v-if="loading" class="state">加载中…</div>
      <template v-else>
        <div class="tabs">
          <div class="tab" :class="{ active: tab === 'songs' }" @click="tab = 'songs'">
            歌曲 <span class="count">{{ songs.length }}</span>
          </div>
          <div class="tab" :class="{ active: tab === 'artists' }" @click="tab = 'artists'">
            歌手 <span class="count">{{ matchedArtists.length }}</span>
          </div>
          <div class="tab" :class="{ active: tab === 'albums' }" @click="tab = 'albums'">
            专辑 <span class="count">{{ matchedAlbums.length }}</span>
          </div>
        </div>

        <!-- 歌曲 -->
        <div v-show="tab === 'songs'">
          <template v-if="songs.length">
            <SongRow
              v-for="song in songs"
              :key="song.id"
              :song="song"
              :keyword="keyword"
              @play="$emit('play', $event)"
            />
          </template>
          <div v-else class="state">没有匹配的歌曲</div>
        </div>

        <!-- 歌手 -->
        <div v-show="tab === 'artists'">
          <div v-if="matchedArtists.length" class="grid" style="max-width: 460px;">
            <div
              v-for="a in matchedArtists"
              :key="a.name"
              class="grid-card"
              @click="openArtist(a.name)"
            >
              <div class="grid-cover circle">
                <img v-if="a.coverName" :src="`/media/covers/${a.coverName}`" alt="" />
                <span v-else>{{ a.name.charAt(0) }}</span>
              </div>
              <div class="grid-name" v-html="hl(a.name)"></div>
              <div class="grid-sub">{{ a.songCount }} 首 · {{ a.playCount }} 次播放</div>
            </div>
          </div>
          <div v-else class="state">没有匹配的歌手</div>
        </div>

        <!-- 专辑 -->
        <div v-show="tab === 'albums'">
          <div v-if="matchedAlbums.length" class="grid" style="max-width: 460px;">
            <div
              v-for="al in matchedAlbums"
              :key="al.name"
              class="grid-card"
              @click="openAlbum(al.name)"
            >
              <div class="grid-cover">
                <img v-if="al.coverName" :src="`/media/covers/${al.coverName}`" alt="" />
                <span v-else>{{ al.name.charAt(0) }}</span>
              </div>
              <div class="grid-name" v-html="hl(al.name)"></div>
              <div class="grid-sub">{{ al.artist }} · {{ al.songCount }} 首</div>
            </div>
          </div>
          <div v-else class="state">没有匹配的专辑</div>
        </div>
      </template>
    </template>
  </div>
</template>

<script>
export default { name: 'Search' }
</script>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSongs, listArtists, listAlbums } from '../api'
import { setQueue } from '../store/player'
import SongRow from '../components/SongRow.vue'

defineEmits(['play'])

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const songs = ref([])
const artists = ref([])
const albums = ref([])
const loading = ref(false)
const tab = ref('songs')

const matchedArtists = computed(() =>
  artists.value.filter(a => a.name.toLowerCase().includes(keyword.value.toLowerCase()))
)
const matchedAlbums = computed(() =>
  albums.value.filter(a =>
    a.name.toLowerCase().includes(keyword.value.toLowerCase()) ||
    (a.artist || '').toLowerCase().includes(keyword.value.toLowerCase())
  )
)

function hl(text) {
  if (!text) return ''
  const safe = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(safe, 'gi'), m => `<span class="hl">${m}</span>`)
}

function openArtist(name) {
  router.push(`/artist/${encodeURIComponent(name)}`)
}
function openAlbum(name) {
  router.push(`/album/${encodeURIComponent(name)}`)
}

async function loadAll() {
  // 歌手/专辑数据量小，一次性加载用于分组匹配
  const [songsData, artistsData, albumsData] = await Promise.all([
    listSongs(keyword.value).catch(() => []),
    listArtists().catch(() => []),
    listAlbums().catch(() => [])
  ])
  songs.value = songsData
  artists.value = artistsData
  albums.value = albumsData
  setQueue(songsData)
}

watch(() => route.query.q, async (q) => {
  keyword.value = q || ''
  tab.value = 'songs'
  if (!q) {
    songs.value = []
    return
  }
  loading.value = true
  try {
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}, { immediate: true })
</script>

<style scoped>
.search-head { font-size: 15px; color: var(--text-secondary); margin-bottom: 20px; }
.tabs {
  display: flex; gap: 24px; border-bottom: 1px solid var(--border); margin-bottom: 16px;
}
.tab {
  padding: 10px 2px; cursor: pointer; font-size: 14px;
  color: var(--text-secondary); border-bottom: 2px solid transparent;
  margin-bottom: -1px; transition: color .15s, border-color .15s;
}
.tab:hover { color: var(--text-primary); }
.tab.active { color: var(--text-primary); font-weight: 500; border-bottom-color: var(--accent); }
.tab .count { font-size: 12px; color: var(--text-faint); margin-left: 4px; }
.grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 24px 16px;
}
.grid-card { cursor: pointer; }
.grid-cover {
  width: 100%; aspect-ratio: 1; border-radius: 6px;
  background: var(--bg-cover); color: var(--text-muted); overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 500;
  transition: background .15s;
}
.grid-cover.circle { border-radius: 50%; }
.grid-card:hover .grid-cover { background: var(--bg-cover-hover); }
.grid-cover img { width: 100%; height: 100%; object-fit: cover; }
.grid-name {
  font-size: 13px; color: var(--text-primary); margin-top: 10px; text-align: center;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.grid-sub { font-size: 12px; color: var(--text-faint); margin-top: 2px; text-align: center; white-space: nowrap; }
:deep(.hl) { color: var(--text-primary); font-weight: 600; }
.state { text-align: center; color: var(--text-faint); padding: 80px 0; font-size: 14px; }
</style>
