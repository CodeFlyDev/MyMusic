<template>
  <div class="upload-form">
    <div class="section-desc">
      支持批量导入：可一次选择多个文件拖入；支持 mp3 / flac / m4a / wav / ogg / ape / wma，单个文件最大 200MB。<br>
      优先使用内嵌封面，没有时自动联网按「歌手 + 歌名」匹配；歌词同样会在后台自动补全。
    </div>

    <el-upload
      class="uploader"
      drag
      multiple
      name="file"
      action="#"
      :headers="{ 'X-Upload-Code': code }"
      :http-request="enqueueUpload"
      :accept="accept"
      :show-file-list="false"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将音频文件拖到此处，或<em>点击选择（可多选）</em></div>
      <template #tip>
        <div class="el-upload__tip">
          支持批量导入：可一次选择多个文件拖入；支持 mp3 / flac / m4a / wav / ogg / ape / wma，单个文件最大 200MB。
          优先使用内嵌封面，没有时自动联网按「歌手 + 歌名」匹配；歌词同样会在后台自动补全
        </div>
      </template>
    </el-upload>

    <!-- 上传队列 -->
    <div v-if="queue.length" class="queue">
      <div class="queue-head">
        <h4>
          上传队列（{{ queue.length }}）
          <span class="queue-summary">
            成功 {{ doneCount.success }} · 失败 {{ doneCount.error }}
            <template v-if="pendingCount"> · 待传 {{ pendingCount }}</template>
          </span>
        </h4>
        <el-button size="small" text type="danger" :disabled="activeCount > 0" @click="clearQueue">
          清空记录
        </el-button>
      </div>
      <div v-for="(item, i) in queue" :key="i" class="queue-row">
        <div class="mini-cover placeholder" v-if="item.status !== 'success'">♪</div>
        <img v-else-if="item.song && item.song.coverName" :src="`/media/covers/${item.song.coverName}`" class="mini-cover" alt="" />
        <div v-else class="mini-cover placeholder">♪</div>

        <div class="queue-meta">
          <div class="queue-title">{{ item.song ? item.song.title : item.name }}</div>
          <el-progress
            v-if="item.status === 'uploading'"
            :percentage="item.percent"
            :stroke-width="6"
            :show-text="false"
            class="queue-progress"
          />
          <div class="queue-sub" v-else>
            {{ item.status === 'waiting' ? '排队中…' : item.status === 'success' ? (item.song && item.song.artist ? item.song.artist : '-') : item.msg }}
          </div>
        </div>

        <el-tag size="small" :type="tagType(item.status)">
          {{ statusText(item) }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listSongs } from '../api'

defineEmits(['upload-success'])

const accept = '.mp3,.flac,.m4a,.wav,.ogg,.ape,.wma'
const code = ref(sessionStorage.getItem('mymusic:upload-code') || '')

// 上传队列；限制并发为 2
const queue = ref([])
const pending = []
let active = 0
const MAX_CONCURRENT = 2

const activeCount = computed(() => queue.value.filter((q) => q.status === 'uploading').length)
const pendingCount = computed(() => queue.value.filter((q) => q.status === 'waiting').length)
const doneCount = computed(() => ({
  success: queue.value.filter((q) => q.status === 'success').length,
  error: queue.value.filter((q) => q.status === 'error').length
}))

function tagType(status) {
  if (status === 'success') return 'success'
  if (status === 'error') return 'danger'
  return 'info'
}
function statusText(item) {
  return { waiting: '等待', uploading: `${item.percent}%`, success: '成功', error: '失败' }[item.status]
}

// el-upload 每选一个文件就会调用一次；入队后按并发上限逐个上传
function enqueueUpload({ file }) {
  const item = reactive({ name: file.name, percent: 0, status: 'waiting', msg: '', song: null })
  queue.value.push(item)
  pending.push({ file, item })
  pump()
}

function pump() {
  while (active < MAX_CONCURRENT && pending.length) {
    const { file, item } = pending.shift()
    active++
    item.status = 'uploading'
    uploadOne(file, item).finally(() => {
      active--
      pump()
    })
  }
}

function uploadOne(file, item) {
  return new Promise((resolve) => {
    const xhr = new XMLHttpRequest()
    const fd = new FormData()
    fd.append('file', file, file.name)
    xhr.open('POST', '/api/songs/upload')
    xhr.setRequestHeader('X-Upload-Code', code.value)
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) item.percent = Math.round((e.loaded / e.total) * 100)
    }
    xhr.onload = () => {
      let body = {}
      try { body = JSON.parse(xhr.responseText) } catch { /* 非 JSON 响应 */ }
      if (body.code === 401) {
        item.status = 'error'
        item.msg = '口令被拒绝'
        code.value = ''
        sessionStorage.removeItem('mymusic:upload-code')
        pending.length = 0
        ElMessage.error('访问口令已被拒绝，请重新验证')
      } else if (xhr.status >= 200 && xhr.status < 300 && body.code === 0) {
        item.status = 'success'
        item.song = body.data
        item.percent = 100
        ElMessage.success(`${file.name} 上传成功`)
        emit('upload-success')
      } else {
        item.status = 'error'
        item.msg = body.msg || `请求失败 (${xhr.status})`
      }
      resolve()
    }
    xhr.onerror = () => {
      item.status = 'error'
      item.msg = '网络错误'
      resolve()
    }
    xhr.send(fd)
  })
}

function clearQueue() {
  queue.value = []
  pending.length = 0
  active = 0
}
</script>

<style scoped>
.upload-form { max-width: 720px; margin: 0 auto; }
.section-desc {
  font-size: 13px; color: var(--text-muted); line-height: 1.8;
  margin-bottom: 20px; padding: 12px 16px;
  background: var(--bg-page); border-radius: 8px; border: 1px solid var(--border-soft);
}
.uploader { width: 100%; }
.queue { margin-top: 20px; }
.queue-head {
  display: flex; align-items: center; justify-content: space-between;
}
.queue-head h4 { margin: 0 0 10px; color: var(--text-primary); font-size: 14px; }
.queue-summary { font-size: 12px; color: var(--text-muted); font-weight: normal; margin-left: 8px; }
.queue-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid var(--border-soft);
}
.queue-row:last-of-type { border-bottom: none; }
.mini-cover { width: 44px; height: 44px; border-radius: 6px; object-fit: cover; display: block; flex-shrink: 0; }
.placeholder {
  background: var(--accent); color: var(--accent-fg); font-size: 20px;
  display: flex; align-items: center; justify-content: center;
}
.queue-meta { flex: 1; min-width: 0; }
.queue-title { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.queue-progress { margin-top: 6px; max-width: 320px; }
.queue-sub { font-size: 12px; color: var(--text-muted); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
@media (max-width: 768px) {
  .section-desc { font-size: 12px; padding: 10px 12px; }
  .queue-head { flex-direction: column; align-items: flex-start; gap: 8px; }
}
</style>