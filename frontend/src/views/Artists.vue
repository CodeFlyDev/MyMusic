<template>
  <div>
    <h2 class="page-title">全部歌手</h2>

    <div v-if="loading" class="state">加载中…</div>
    <template v-else-if="artists.length">
      <!-- 字母索引：中文等非字母归入 # -->
      <div class="alpha-index">
        <span
          v-for="letter in letters"
          :key="letter"
          class="alpha-item"
          :class="{ active: letter === activeLetter }"
          @click="activeLetter = letter"
        >{{ letter }}</span>
      </div>

      <div v-if="pagedArtists.length" class="grid">
        <div
          v-for="a in pagedArtists"
          :key="a.name"
          class="grid-card"
          @click="openArtist(a.name)"
        >
          <div class="grid-cover circle">
            <img v-if="a.coverName" :src="`/media/covers/${a.coverName}`" alt="" />
            <span v-else>{{ a.name.charAt(0) }}</span>
          </div>
          <div class="grid-name">{{ a.name }}</div>
          <div class="grid-sub">{{ a.songCount }} 首 · {{ a.playCount }} 次播放</div>
        </div>
      </div>
      <div v-else class="state">该字母下暂无歌手</div>

      <div v-if="filteredArtists.length > pageSize" class="pagination">
        <span
          class="page-btn"
          :class="{ disabled: page === 1 }"
          @click="page > 1 && page--"
        >上一页</span>
        <span
          v-for="p in totalPages"
          :key="p"
          class="page-btn"
          :class="{ active: p === page }"
          @click="page = p"
        >{{ p }}</span>
        <span
          class="page-btn"
          :class="{ disabled: page === totalPages }"
          @click="page < totalPages && page++"
        >下一页</span>
      </div>
    </template>
    <div v-else class="state">还没有歌手数据</div>
  </div>
</template>

<script>
export default { name: 'Artists' }
</script>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listArtists } from '../api'
import { initialOf } from '../utils/lrc'

const router = useRouter()
const artists = ref([])
const loading = ref(true)
const activeLetter = ref('全部')
const page = ref(1)
const pageSize = 24

const letters = ['全部', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#']

const filteredArtists = computed(() =>
  activeLetter.value === '全部'
    ? artists.value
    : artists.value.filter(a => initialOf(a.name) === activeLetter.value)
)
const totalPages = computed(() => Math.max(1, Math.ceil(filteredArtists.value.length / pageSize)))
const pagedArtists = computed(() =>
  filteredArtists.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

watch(activeLetter, () => { page.value = 1 })

function openArtist(name) {
  router.push(`/artist/${encodeURIComponent(name)}`)
}

onMounted(async () => {
  try {
    artists.value = await listArtists()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-title { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0 0 20px; }
.alpha-index { display: flex; flex-wrap: wrap; gap: 2px; margin-bottom: 24px; }
.alpha-item {
  padding: 4px 10px; font-size: 13px; cursor: pointer;
  border-radius: 4px; color: var(--text-secondary); transition: background .15s, color .15s;
  min-width: 30px; text-align: center;
}
.alpha-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.alpha-item.active { background: var(--accent); color: var(--accent-fg); }
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
  font-size: 13px; color: var(--text-primary); margin-top: 10px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-align: center;
}
.grid-sub { font-size: 12px; color: var(--text-faint); margin-top: 2px; text-align: center; white-space: nowrap; }
.pagination { display: flex; justify-content: center; gap: 4px; margin-top: 32px; }
.page-btn {
  padding: 6px 12px; border: 1px solid var(--border-input); background: var(--bg-card);
  border-radius: 4px; cursor: pointer; font-size: 13px; color: var(--text-secondary);
  transition: all .15s; min-width: 32px; text-align: center;
}
.page-btn:hover { border-color: var(--text-muted); color: var(--text-primary); }
.page-btn.active { background: var(--accent); color: var(--accent-fg); border-color: var(--accent); }
.page-btn.disabled { color: var(--text-disabled); cursor: default; border-color: var(--border); }
.page-btn.disabled:hover { border-color: var(--border); color: var(--text-disabled); }
.state { text-align: center; color: var(--text-faint); padding: 80px 0; font-size: 14px; }
</style>
