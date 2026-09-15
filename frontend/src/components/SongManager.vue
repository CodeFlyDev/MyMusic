<template>
  <div class="song-manager">
    <div class="manager-header">
      <h3>歌曲管理（{{ songs.length }} 首<template v-if="totalSize"> · 共 {{ totalSize }}</template>）</h3>
      <el-button size="small" text type="primary" :loading="loadingSongs" @click="loadSongs">刷新</el-button>
    </div>

    <div v-loading="loadingSongs" class="song-list">
      <div v-for="song in songs" :key="song.id" class="song-row">
        <img v-if="song.coverName" :src="`/media/covers/${song.coverName}`" class="mini-cover" alt="" />
        <div v-else class="mini-cover placeholder">♪</div>

        <div class="song-meta">
          <div class="song-title">{{ song.title }}</div>
          <div class="song-sub">
            {{ song.artist || '未知歌手' }}<template v-if="song.album"> · {{ song.album }}</template>
            <span class="desktop-only"> · {{ formatSize(song.fileSize) }} · 播放 {{ song.playCount || 0 }} 次 · {{ formatDateTime(song.createdAt) }}</span>
          </div>
        </div>

        <el-button
          class="lyrics-btn"
          type="primary"
          size="small"
          text
          @click="openLyrics(song)"
        >
          歌词
        </el-button>
        <el-button
          class="del-btn"
          type="danger"
          size="small"
          circle
          plain
          :loading="deletingId === song.id"
          @click="confirmDelete(song)"
        >
          <el-icon v-if="deletingId !== song.id"><Delete /></el-icon>
        </el-button>
      </div>
      <div v-if="!loadingSongs && !songs.length" class="empty">还没有歌曲，先上传几首吧</div>
    </div>

    <!-- 歌词编辑弹窗 -->
    <el-dialog
      v-model="lyricsDialog"
      :title="`编辑歌词 - ${editingSong ? editingSong.title : ''}`"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="lyricsText"
        type="textarea"
        :rows="20"
        placeholder="粘贴 LRC 格式歌词，例如：&#10;[00:12.50]第一句歌词&#10;[00:17.30]第二句歌词&#10;留空表示清除歌词"
        class="lyrics-input"
      />
      <template #footer>
        <el-button @click="lyricsDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingLyrics" @click="saveLyrics">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { listSongs, deleteSong, getLyrics, updateLyrics, formatSize, formatDateTime } from '../api'
import { refreshLyrics } from '../store/player'

defineEmits(['refresh'])

const songs = ref([])
const loadingSongs = ref(false)
const deletingId = ref(null)

const lyricsDialog = ref(false)
const editingSong = ref(null)
const lyricsText = ref('')
const savingLyrics = ref(false)

const totalSize = computed(() => {
  const total = songs.value.reduce((sum, s) => sum + (s.fileSize || 0), 0)
  return total ? formatSize(total) : ''
})

async function loadSongs() {
  loadingSongs.value = true
  try {
    songs.value = await listSongs()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loadingSongs.value = false
  }
}

function confirmDelete(song) {
  ElMessageBox.confirm(`确定删除「${song.title}」吗？音频文件将一并删除。`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      deletingId.value = song.id
      try {
        const code = sessionStorage.getItem('mymusic:upload-code')
        await deleteSong(song.id, code)
        ElMessage.success('删除成功')
        loadSongs()
      } catch (e) {
        if (e.message && e.message.includes('口令')) {
          ElMessage.error('访问口令已被拒绝，请重新验证')
        } else {
          ElMessage.error(e.message)
        }
      } finally {
        deletingId.value = null
      }
    })
    .catch(() => {})
}

async function openLyrics(song) {
  editingSong.value = song
  lyricsText.value = ''
  lyricsDialog.value = true
  try {
    lyricsText.value = (await getLyrics(song.id)) || ''
  } catch (e) {
    lyricsText.value = ''
  }
}

async function saveLyrics() {
  if (!editingSong.value) return
  savingLyrics.value = true
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    await updateLyrics(editingSong.value.id, lyricsText.value, code)
    ElMessage.success('歌词已保存')
    lyricsDialog.value = false
    refreshLyrics()
  } catch (e) {
    if (e.message && e.message.includes('口令')) {
      ElMessage.error('访问口令已被拒绝，请重新验证')
    } else {
      ElMessage.error(e.message)
    }
  } finally {
    savingLyrics.value = false
  }
}

loadSongs()
</script>

<style scoped>
.song-manager { max-width: 900px; margin: 0 auto; }
.manager-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.manager-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.song-list { background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }
.song-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-bottom: 1px solid var(--border-soft);
}
.song-row:last-of-type { border-bottom: none; }
.mini-cover { width: 44px; height: 44px; border-radius: 6px; object-fit: cover; display: block; flex-shrink: 0; }
.placeholder {
  background: var(--accent); color: var(--accent-fg); font-size: 20px;
  display: flex; align-items: center; justify-content: center;
}
.song-meta { flex: 1; min-width: 0; }
.song-title { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.song-sub { font-size: 12px; color: var(--text-muted); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lyrics-btn { flex-shrink: 0; margin-right: 8px; }
.del-btn { flex-shrink: 0; }
.empty {
  text-align: center; color: var(--text-muted); padding: 40px 0; font-size: 14px;
}
.lyrics-input { resize: vertical; }
@media (max-width: 768px) {
  .desktop-only { display: none; }
  .song-row { gap: 8px; padding: 10px 12px; }
  .lyrics-btn { padding: 0 8px; font-size: 12px; }
}
</style>