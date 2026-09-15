<template>
  <div class="community-page">
    <div class="page-header">
      <h1 class="page-title">动态</h1>
      <p class="page-subtitle">公告、反馈与更新日志</p>
    </div>

    <el-tabs v-model="activeTab" class="community-tabs" @tab-click="onTabClick">
      <el-tab-pane label="公告" name="announcements">
        <AnnouncementBoard :announcements="announcements" />
        <div v-if="announcements.length === 0" class="empty-state">暂无公告</div>
        <div v-else class="load-more" @click="loadMoreAnnouncements">
          <el-button :loading="loadingAnnouncements" size="small" @click.stop>加载更多</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="意见反馈" name="feedback">
        <div class="feedback-section">
          <el-card class="feedback-form-card" shadow="never">
            <template #header>
              <span>提交反馈</span>
            </template>
            <el-form :model="feedbackForm" label-width="80px" :rules="feedbackRules" ref="feedbackFormRef">
              <el-form-item label="反馈内容" prop="content">
                <el-input
                  v-model="feedbackForm.content"
                  type="textarea"
                  :rows="4"
                  maxlength="2000"
                  show-word-limit
                  placeholder="描述你遇到的问题或功能建议…"
                />
              </el-form-item>
              <el-form-item label="联系方式" prop="contact">
                <el-input v-model="feedbackForm.contact" placeholder="可选，便于回复" maxlength="255" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="submittingFeedback" @click="doSubmitFeedback">提交反馈</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <div v-if="feedbackList.length === 0" class="empty-state">
            暂无反馈记录
          </div>
          <div v-else class="feedback-list">
            <div v-for="fb in feedbackList" :key="fb.id" class="feedback-item">
              <div class="feedback-content">{{ fb.content }}</div>
              <div class="feedback-meta">
                <span v-if="fb.contact">联系方式：{{ fb.contact }}</span>
                <span>{{ formatDateTime(fb.createdAt) }}</span>
                <el-tag :type="fb.resolved ? 'success' : 'info'" size="small" effect="plain">
                  {{ fb.resolved ? '已解决' : '待处理' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="更新日志" name="changelogs">
        <div v-if="changelogs.length === 0" class="empty-state">暂无更新日志</div>
        <div v-else class="changelog-list">
          <div v-for="cl in changelogs" :key="cl.id" class="changelog-card">
            <div class="changelog-header">
              <span class="version-badge">{{ cl.version }}</span>
              <span class="changelog-date">{{ formatDate(cl.releaseDate) }}</span>
            </div>
            <div class="changelog-body" v-html="renderMarkdown(cl.content)"></div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listAnnouncements,
  listChangelogs,
  submitFeedback as apiSubmitFeedback,
  formatDateTime
} from '../api'
import AnnouncementBoard from '../components/AnnouncementBoard.vue'

const activeTab = ref('announcements')
const announcements = ref([])
const changelogs = ref([])
const feedbackList = ref([])

const feedbackForm = ref({
  content: '',
  contact: ''
})
const submittingFeedback = ref(false)
const feedbackRules = {
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
}
const feedbackFormRef = ref(null)

const announcementPage = ref(1)
const announcementPageSize = 10
const loadingAnnouncements = ref(false)

async function loadAnnouncements() {
  try {
    const all = await listAnnouncements()
    announcements.value = all
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadChangelogs() {
  try {
    changelogs.value = await listChangelogs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadMoreAnnouncements() {
  loadingAnnouncements.value = true
  // Since we load all at once, just show a message
  ElMessage.info('已加载全部公告')
  loadingAnnouncements.value = false
}

async function doSubmitFeedback() {
  try {
    await feedbackFormRef.value.validate()
  } catch (e) {
    return
  }
  submittingFeedback.value = true
  try {
    await apiSubmitFeedback(feedbackForm.value.content, feedbackForm.value.contact)
    ElMessage.success('反馈已提交，感谢你的建议')
    feedbackForm.value.content = ''
    feedbackForm.value.contact = ''
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submittingFeedback.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

function onTabClick(tab) {
  if (tab.name === 'announcements' && announcements.value.length === 0) {
    loadAnnouncements()
  } else if (tab.name === 'changelogs' && changelogs.value.length === 0) {
    loadChangelogs()
  }
}

onMounted(() => {
  loadAnnouncements()
  loadChangelogs()
})
</script>

<style scoped>
.community-page { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin: 0 0 4px; }
.page-subtitle { font-size: 14px; color: var(--text-muted); margin: 0; }
.community-tabs :deep(.el-tabs__header) { margin-bottom: 16px; border-bottom: 1px solid var(--border); }
.community-tabs :deep(.el-tabs__nav-wrap) { overflow: visible; }
.community-tabs :deep(.el-tabs__item) { font-size: 14px; padding: 0 20px 12px; }
.community-tabs :deep(.el-tabs__item.is-active) { color: var(--text-primary); font-weight: 500; }
.community-tabs :deep(.el-tabs__active-bar) { height: 3px; background: var(--accent); }
.feedback-section { display: flex; flex-direction: column; gap: 20px; }
.feedback-form-card { margin-bottom: 8px; }
.feedback-list { display: flex; flex-direction: column; gap: 12px; }
.feedback-item {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
}
.feedback-content { font-size: 14px; color: var(--text-primary); line-height: 1.6; margin-bottom: 10px; white-space: pre-wrap; }
.feedback-meta {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  font-size: 12px; color: var(--text-muted);
}
.changelog-list { display: flex; flex-direction: column; gap: 16px; }
.changelog-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px;
  transition: box-shadow .15s, border-color .15s;
}
.changelog-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); border-color: var(--border-input); }
.changelog-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.version-badge {
  font-size: 12px; font-weight: 600; color: var(--accent-fg);
  background: var(--accent); padding: 4px 10px; border-radius: 4px;
}
.changelog-date { font-size: 13px; color: var(--text-muted); }
.changelog-body { font-size: 14px; color: var(--text-secondary); line-height: 1.7; }
.changelog-body ul { margin: 0; padding-left: 20px; }
.changelog-body li { margin: 4px 0; }
.changelog-body code { background: var(--bg-hover); padding: 1px 4px; border-radius: 3px; font-size: 12px; font-family: monospace; }
.empty-state { text-align: center; color: var(--text-muted); padding: 60px 20px; font-size: 14px; }
.load-more { text-align: center; padding: 16px 0; }
@media (max-width: 768px) {
  .community-page { padding: 0 16px; }
  .page-title { font-size: 20px; }
  .community-tabs :deep(.el-tabs__item) { padding: 0 12px 12px; font-size: 13px; }
  .changelog-card { padding: 16px; }
}
</style>