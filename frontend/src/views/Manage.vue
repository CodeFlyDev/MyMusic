<template>
  <div class="manage-page">
    <div class="page-header">
      <h1 class="page-title">管理后台</h1>
      <p class="page-subtitle">歌曲上传、歌曲管理、公告管理、用户反馈处理</p>
    </div>

    <!-- 口令门禁：验证通过才显示管理功能 -->
    <div v-if="!verified" class="gate-card">
      <svg class="gate-icon" viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="11" width="18" height="11" rx="2"/>
        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
      </svg>
      <div class="gate-title">请输入管理口令</div>
      <div class="gate-sub">管理功能需要口令验证后才能使用</div>
      <el-input
        v-model="inputCode"
        class="gate-input"
        type="password"
        show-password
        placeholder="管理口令"
        @keyup.enter="doVerify"
      />
      <el-button class="gate-btn" type="primary" :loading="verifying" @click="doVerify">验证</el-button>
    </div>

    <el-card v-else class="manage-card" shadow="never">
      <div class="manage-head">
        <el-tabs v-model="activeTab" class="manage-tabs">
          <!-- 上传歌曲 -->
          <el-tab-pane label="上传歌曲" name="upload">
            <upload-form @upload-success="onUploadSuccess" />
          </el-tab-pane>

          <!-- 歌曲管理 -->
          <el-tab-pane label="歌曲管理" name="songs">
            <song-manager @refresh="loadSongs" />
          </el-tab-pane>

          <!-- 公告管理 -->
          <el-tab-pane label="公告管理" name="announcements">
            <AnnouncementManager @refresh="loadAnnouncements" />
          </el-tab-pane>

          <!-- 更新日志管理 -->
          <el-tab-pane label="更新日志" name="changelogs">
            <ChangelogManager />
          </el-tab-pane>

          <!-- 用户反馈 -->
          <el-tab-pane label="用户反馈" name="feedback">
            <FeedbackManager />
          </el-tab-pane>
        </el-tabs>
        <el-button class="exit-btn" text size="small" @click="exitManage">退出管理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import UploadForm from '../components/UploadForm.vue'
import SongManager from '../components/SongManager.vue'
import AnnouncementManager from '../components/AnnouncementManager.vue'
import ChangelogManager from '../components/ChangelogManager.vue'
import FeedbackManager from '../components/FeedbackManager.vue'
import { verifyPasscode } from '../api'

const activeTab = ref('upload')

// 口令门禁
const verified = ref(false)
const verifying = ref(false)
const inputCode = ref('')

const CODE_KEY = 'mymusic:upload-code'

onMounted(async () => {
  // 已缓存口令时静默自动校验
  const saved = sessionStorage.getItem(CODE_KEY)
  if (saved) {
    try {
      await verifyPasscode(saved)
      verified.value = true
    } catch {
      sessionStorage.removeItem(CODE_KEY)
    }
  }
})

async function doVerify() {
  const code = inputCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入口令')
    return
  }
  verifying.value = true
  try {
    await verifyPasscode(code)
    sessionStorage.setItem(CODE_KEY, code)
    verified.value = true
    ElMessage.success('验证通过')
  } catch (e) {
    ElMessage.error(e.message || '口令错误')
  } finally {
    verifying.value = false
  }
}

function exitManage() {
  sessionStorage.removeItem(CODE_KEY)
  verified.value = false
  inputCode.value = ''
}

function onUploadSuccess() {
  // 可选：上传成功后切换到歌曲管理页
  // activeTab.value = 'songs'
}
</script>

<style scoped>
.manage-page { max-width: 960px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin: 0 0 4px; }
.page-subtitle { font-size: 14px; color: var(--text-muted); margin: 0; }
.manage-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
.manage-head { position: relative; }
.exit-btn { position: absolute; top: 14px; right: 16px; z-index: 2; color: var(--text-muted); }
.exit-btn:hover { color: var(--text-primary); }
.manage-tabs :deep(.el-tabs__header) { border-bottom: 1px solid var(--border); background: var(--bg-page); padding: 0 20px; }
.manage-tabs :deep(.el-tabs__nav-wrap) { overflow: visible; }
.manage-tabs :deep(.el-tabs__item) { font-size: 14px; padding: 16px 20px; }
.manage-tabs :deep(.el-tabs__item.is-active) { color: var(--text-primary); font-weight: 500; }
.manage-tabs :deep(.el-tabs__active-bar) { height: 3px; background: var(--accent); }
.manage-tabs :deep(.el-tabs__content) { padding: 20px; }

/* 门禁卡片 */
.gate-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.gate-icon { color: var(--text-muted); margin-bottom: 4px; }
.gate-title { font-size: 17px; font-weight: 600; color: var(--text-primary); }
.gate-sub { font-size: 13px; color: var(--text-muted); margin-bottom: 16px; }
.gate-input { width: 280px; }
.gate-btn { width: 280px; margin-top: 12px; }

@media (max-width: 768px) {
  .manage-page { padding: 0 16px; }
  .page-title { font-size: 20px; }
  .manage-tabs :deep(.el-tabs__header) { padding: 0 12px; }
  .manage-tabs :deep(.el-tabs__item) { padding: 14px 12px; font-size: 13px; }
  .manage-tabs :deep(.el-tabs__content) { padding: 16px 12px; }
}
</style>
