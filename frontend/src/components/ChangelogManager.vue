<template>
  <div class="changelog-manager">
    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingChangelog ? '编辑更新日志' : '新建更新日志'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="例如：v1.5.0" maxlength="32" />
        </el-form-item>
        <el-form-item label="发布日期" prop="releaseDate">
          <el-date-picker v-model="form.releaseDate" type="date" placeholder="选择日期" style="width: 100%;" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="更新内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="支持 Markdown 语法，建议使用列表格式：&#10;- 新增功能 A&#10;- 修复 Bug B&#10;- 优化体验 C"
            show-word-limit
            maxlength="10000"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 更新日志列表 -->
    <div v-loading="loading" class="manager-list">
      <div class="list-header">
        <h3>更新日志管理</h3>
        <el-button type="primary" @click="openCreateDialog">新建更新日志</el-button>
      </div>

      <div v-if="changelogs.length === 0" class="empty-state">
        暂无更新日志
      </div>

      <div v-else class="changelog-timeline">
        <div v-for="cl in changelogs" :key="cl.id" class="timeline-item">
          <div class="timeline-marker"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <span class="version-badge">{{ cl.version }}</span>
              <span class="timeline-date">{{ formatDate(cl.releaseDate) }}</span>
            </div>
            <div class="timeline-body" v-html="renderMarkdown(cl.content)"></div>
            <div class="timeline-actions">
              <el-button size="small" text @click="openEditDialog(cl)">编辑</el-button>
              <el-button size="small" text type="danger" @click="confirmDelete(cl)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listChangelogsAdmin,
  createChangelog,
  updateChangelog,
  deleteChangelog,
  formatDateTime
} from '../api'

const loading = ref(true)
const changelogs = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingChangelog = ref(null)

const form = reactive({
  version: '',
  releaseDate: '',
  content: ''
})

const rules = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  releaseDate: [{ required: true, message: '请选择发布日期', trigger: 'change' }],
  content: [{ required: true, message: '请输入更新内容', trigger: 'blur' }]
}

const formRef = ref(null)

async function loadChangelogs() {
  loading.value = true
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    changelogs.value = await listChangelogsAdmin(code)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
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

function openCreateDialog() {
  editingChangelog.value = null
  form.version = ''
  form.releaseDate = new Date().toISOString().split('T')[0]
  form.content = ''
  dialogVisible.value = true
}

function openEditDialog(cl) {
  editingChangelog.value = cl
  form.version = cl.version
  form.releaseDate = formatDate(cl.releaseDate)
  form.content = cl.content
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    const data = {
      version: form.version,
      releaseDate: form.releaseDate,
      content: form.content
    }
    if (editingChangelog.value) {
      await updateChangelog(editingChangelog.value.id, data, code)
      ElMessage.success('更新成功')
    } else {
      await createChangelog(data, code)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadChangelogs()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields?.()
}

function confirmDelete(cl) {
  ElMessageBox.confirm(`确定删除版本「${cl.version}」的更新日志吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const code = sessionStorage.getItem('mymusic:upload-code')
      await deleteChangelog(cl.id, code)
      ElMessage.success('已删除')
      loadChangelogs()
    } catch (e) {
      ElMessage.error(e.message)
    }
  }).catch(() => {})
}

loadChangelogs()
</script>

<style scoped>
.manager-list { background: var(--bg-card); border-radius: 8px; overflow: hidden; }
.list-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.list-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.changelog-timeline { padding: 16px 20px; position: relative; }
.changelog-timeline::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--border);
}
.timeline-item {
  position: relative;
  display: flex;
  gap: 16px;
  padding-bottom: 32px;
}
.timeline-item:last-child { padding-bottom: 0; }
.timeline-marker {
  position: relative;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--accent);
  border: 3px solid var(--bg-card);
  box-shadow: 0 0 0 1px var(--border);
  z-index: 1;
}
.timeline-content { flex: 1; min-width: 0; padding-top: 2px; }
.timeline-header {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 8px;
}
.version-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-fg);
  background: var(--accent);
  padding: 2px 8px;
  border-radius: 4px;
}
.timeline-date { font-size: 12px; color: var(--text-muted); }
.timeline-body {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  word-break: break-word;
  margin-bottom: 8px;
}
.timeline-body ul { margin: 0; padding-left: 20px; }
.timeline-body li { margin: 4px 0; }
.timeline-body code {
  background: var(--bg-hover);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 12px;
  font-family: monospace;
}
.timeline-actions { display: flex; gap: 8px; }
.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
@media (max-width: 768px) {
  .changelog-timeline::before { left: 8px; }
  .timeline-marker { width: 16px; height: 16px; }
}
</style>