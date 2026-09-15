<template>
  <div>
    <div v-if="loading" class="state">加载中…</div>
    <template v-else-if="songs.length">
      <div class="detail-header">
        <div class="detail-cover">
          <img v-if="coverName" :src="`/media/covers/${coverName}`" alt="" />
          <span v-else>{{ name.charAt(0) }}</span>
        </div>
        <div class="detail-info">
          <h2>{{ name }}</h2>
          <div class="stats">{{ songs[0].artist || '未知歌手' }} · {{ songs.length }} 首歌曲</div>
        </div>
      </div>

      <SongRow
        v-for="(song, i) in sortedSongs"
        :key="song.id"
        :song="song"
        :index="i + 1"
        @play="$emit('play', $event)"
      >
        {{ song.playCount || 0 }} 次播放
      </SongRow>
    </template>
    <div v-else class="state">没有该专辑的歌曲</div>
  </div>
</template>

<script>
export default { name: 'AlbumDetail' }
</script>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listSongsByAlbum } from '../api'
import { setQueue } from '../store/player'
import SongRow from '../components/SongRow.vue'

defineEmits(['play'])

const route = useRoute()
const songs = ref([])
const loading = ref(true)

const name = computed(() => route.params.name || '')
const coverName = computed(() => {
  const withCover = songs.value.find(s => s.coverName)
  return withCover ? withCover.coverName : ''
})
// 按播放次数降序排列（热度优先）
const sortedSongs = computed(() =>
  [...songs.value].sort((a, b) => (b.playCount || 0) - (a.playCount || 0))
)

async function load() {
  loading.value = true
  try {
    songs.value = await listSongsByAlbum(name.value)
    setQueue(sortedSongs.value)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

watch(name, load, { immediate: true })
</script>

<style scoped>
.detail-header {
  display: flex; align-items: center; gap: 20px;
  padding-bottom: 24px; margin-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.detail-cover {
  width: 96px; height: 96px; border-radius: 6px;
  background: var(--bg-cover); color: var(--text-muted); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; font-weight: 500; overflow: hidden;
}
.detail-cover img { width: 100%; height: 100%; object-fit: cover; }
.detail-info h2 { font-size: 22px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px; }
.stats { font-size: 13px; color: var(--text-muted); }
.state { text-align: center; color: var(--text-faint); padding: 80px 0; font-size: 14px; }
</style>
