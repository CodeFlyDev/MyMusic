<template>
  <div>
    <h2 class="page-title">全部专辑</h2>

    <div v-if="loading" class="state">加载中…</div>
    <template v-else-if="albums.length">
      <div class="grid">
        <div
          v-for="al in pagedAlbums"
          :key="al.name"
          class="grid-card"
          @click="openAlbum(al.name)"
        >
          <div class="grid-cover">
            <img v-if="al.coverName" :src="`/media/covers/${al.coverName}`" alt="" />
            <span v-else>{{ al.name.charAt(0) }}</span>
          </div>
          <div class="grid-name">{{ al.name }}</div>
          <div class="grid-sub">{{ al.artist }} · {{ al.songCount }} 首</div>
        </div>
      </div>

      <div v-if="albums.length > pageSize" class="pagination">
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
    <div v-else class="state">还没有专辑数据</div>
  </div>
</template>

<script>
export default { name: 'Albums' }
</script>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAlbums } from '../api'

const router = useRouter()
const albums = ref([])
const loading = ref(true)
const page = ref(1)
const pageSize = 24

const totalPages = computed(() => Math.max(1, Math.ceil(albums.value.length / pageSize)))
const pagedAlbums = computed(() =>
  albums.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

function openAlbum(name) {
  router.push(`/album/${encodeURIComponent(name)}`)
}

onMounted(async () => {
  try {
    albums.value = await listAlbums()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-title { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 0 0 20px; }
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
.grid-card:hover .grid-cover { background: var(--bg-cover-hover); }
.grid-cover img { width: 100%; height: 100%; object-fit: cover; }
.grid-name {
  font-size: 13px; color: var(--text-primary); margin-top: 10px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; text-align: center;
}
.grid-sub { font-size: 12px; color: var(--text-faint); margin-top: 2px; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
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
