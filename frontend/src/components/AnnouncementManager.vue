<template>
  <div class="announcement-manager">
    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingAnnouncement ? '编辑公告' : '新建公告'"
      width="640px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="255" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="支持 Markdown 语法（加粗、代码、换行）"
            show-word-limit
            maxlength="5000"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority" style="display: flex; gap: 20px;">
            <el-radio :label="0">普通</el-radio>
            <el-radio :label="1">重要</el-radio>
            <el-radio :label="2">置顶</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="显示状态" prop="visible">
          <el-switch v-model="form.visible" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 公告列表 -->
    <div v-loading="loading" class="manager-list">
      <div class="list-header">
        <h3>公告管理</h3>
        <el-button type="primary" @click="openCreateDialog">新建公告</el-button>
      </div>

      <div v-if="announcements.length === 0" class="empty-state">
        暂无公告，点击「新建公告」创建第一条
      </div>

      <table v-else class="manager-table">
        <thead>
          <tr>
            <th width="40">#</th>
            <th>标题</th>
            <th width="100">优先级</th>
            <th width="80">状态</th>
            <th width="160">创建时间</th>
            <th width="180">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ann in announcements" :key="ann.id">
            <td>{{ ann.id }}</td>
            <td>
              <div class="title-cell" v-html="highlight(ann.title)"></div>
            </td>
            <td>
              <el-tag :type="priorityType(ann.priority)" size="small" effect="plain">
                {{ priorityText(ann.priority) }}
              </el-tag>
            </td>
            <td>
              <el-tag :type="ann.visible ? 'success' : 'info'" size="small" effect="plain">
                {{ ann.visible ? '显示' : '隐藏' }}
              </el-tag>
            </td>
            <td>{{ formatDateTime(ann.createdAt) }}</td>
            <td>
              <el-button size="small" text @click="openEditDialog(ann)">编辑</el-button>
              <el-button size="small" text type="danger" @click="confirmDelete(ann)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAnnouncementsAdmin,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  formatDateTime
} from '../api'

const emit = defineEmits(['refresh'])

const loading = ref(true)
const announcements = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingAnnouncement = ref(null)

const form = reactive({
  title: '',
  content: '',
  priority: 0,
  visible: 1
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const formRef = ref(null)

async function loadAnnouncements() {
  loading.value = true
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    announcements.value = await listAnnouncementsAdmin(code)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function priorityText(p) {
  return { 0: '普通', 1: '重要', 2: '置顶' }[p] || '普通'
}
function priorityType(p) {
  return { 0: 'info', 1: 'warning', 2: 'danger' }[p] || 'info'
}
function highlight(text) {
  return text
}

function openCreateDialog() {
  editingAnnouncement.value = null
  form.title = ''
  form.content = ''
  form.priority = 0
  form.visible = 1
  dialogVisible.value = true
}

function openEditDialog(ann) {
  editingAnnouncement.value = ann
  form.title = ann.title
  form.content = ann.content
  form.priority = ann.priority
  form.visible = ann.visible
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
    if (editingAnnouncement.value) {
      await updateAnnouncement(editingAnnouncement.value.id, { ...form }, code)
      ElMessage.success('更新成功')
    } else {
      await createAnnouncement({ ...form }, code)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadAnnouncements()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields?.()
}

function confirmDelete(ann) {
  ElMessageBox.confirm(`确定删除公告「${ann.title}」吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const code = sessionStorage.getItem('mymusic:upload-code')
      await deleteAnnouncement(ann.id, code)
      ElMessage.success('已删除')
      loadAnnouncements()
    } catch (e) {
      ElMessage.error(e.message)
    }
  }).catch(() => {})
}

loadAnnouncements()
</script>

<style scoped>
.manager-list { background: var(--bg-card); border-radius: 8px; overflow: hidden; }
.list-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.list-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.manager-table { width: 100%; border-collapse: collapse; }
.manager-table th,
.manager-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid var(--border-soft);
}
.manager-table th {
  background: var(--bg-page);
  font-weight: 500;
  color: var(--text-secondary);
}
.manager-table tr:last-child td { border-bottom: none; }
.manager-table tr:hover td { background: var(--bg-hover); }
.title-cell { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
@media (max-width: 768px) {
  .manager-table th:nth-child(3),
  .manager-table td:nth-child(3),
  .manager-table th:nth-child(4),
  .manager-table td:nth-child(4) { display: none; }
  .title-cell { max-width: 200px; }
}
</style>