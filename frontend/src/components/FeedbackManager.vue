<template>
  <div class="feedback-manager">
    <!-- 反馈详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="560px">
      <div class="detail-meta">
        <el-tag :type="detailRow?.resolved ? 'success' : 'warning'" size="small" effect="plain">
          {{ detailRow?.resolved ? '已解决' : '待处理' }}
        </el-tag>
        <span class="meta-time">提交于 {{ formatDateTime(detailRow?.createdAt) }}</span>
        <span v-if="detailRow?.resolvedAt" class="meta-time">处理于 {{ formatDateTime(detailRow.resolvedAt) }}</span>
      </div>
      <div class="detail-content">{{ detailRow?.content }}</div>
      <div v-if="detailRow?.contact" class="detail-contact">联系方式：{{ detailRow.contact }}</div>
      <template #footer>
        <el-button :type="detailRow?.resolved ? 'info' : 'primary'" @click="detailRow && toggleResolve(detailRow)">
          {{ detailRow?.resolved ? '取消解决' : '标记解决' }}
        </el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 反馈列表 -->
    <div v-loading="loading" class="manager-list">
      <div class="list-header">
        <h3>
          用户反馈
          <span v-if="pendingCount" class="pending-badge">{{ pendingCount }} 条待处理</span>
        </h3>
        <el-radio-group v-model="filter" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="pending">待处理</el-radio-button>
          <el-radio-button value="resolved">已解决</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="filteredList.length === 0" class="empty-state">
        {{ filter === 'all' ? '暂无用户反馈' : '当前筛选条件下暂无反馈' }}
      </div>

      <table v-else class="manager-table">
        <thead>
          <tr>
            <th width="40">#</th>
            <th>反馈内容</th>
            <th width="140">联系方式</th>
            <th width="80">状态</th>
            <th width="160">提交时间</th>
            <th width="180">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="fb in filteredList" :key="fb.id">
            <td>{{ fb.id }}</td>
            <td>
              <div class="content-cell" @click="openDetail(fb)">{{ fb.content }}</div>
            </td>
            <td>{{ fb.contact || '—' }}</td>
            <td>
              <el-tag :type="fb.resolved ? 'success' : 'warning'" size="small" effect="plain">
                {{ fb.resolved ? '已解决' : '待处理' }}
              </el-tag>
            </td>
            <td>{{ formatDateTime(fb.createdAt) }}</td>
            <td>
              <el-button size="small" text :type="fb.resolved ? 'info' : 'primary'" @click="toggleResolve(fb)">
                {{ fb.resolved ? '取消解决' : '标记解决' }}
              </el-button>
              <el-button size="small" text type="danger" @click="confirmDelete(fb)">删除</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
export default { name: 'FeedbackManager' }
</script>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listFeedback, resolveFeedback, deleteFeedback, formatDateTime } from '../api'

const loading = ref(true)
const list = ref([])
const filter = ref('all')
const detailVisible = ref(false)
const detailRow = ref(null)

const pendingCount = computed(() => list.value.filter((f) => !f.resolved).length)
const filteredList = computed(() => {
  if (filter.value === 'pending') return list.value.filter((f) => !f.resolved)
  if (filter.value === 'resolved') return list.value.filter((f) => f.resolved)
  return list.value
})

async function loadFeedback() {
  loading.value = true
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    list.value = await listFeedback(code)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function openDetail(fb) {
  detailRow.value = fb
  detailVisible.value = true
}

async function toggleResolve(fb) {
  try {
    const code = sessionStorage.getItem('mymusic:upload-code')
    await resolveFeedback(fb.id, !fb.resolved, code)
    ElMessage.success(!fb.resolved ? '已标记为解决' : '已取消标记')
    if (detailVisible.value) detailVisible.value = false
    loadFeedback()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function confirmDelete(fb) {
  ElMessageBox.confirm('确定删除这条反馈吗？删除后不可恢复。', '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const code = sessionStorage.getItem('mymusic:upload-code')
      await deleteFeedback(fb.id, code)
      ElMessage.success('已删除')
      if (detailVisible.value) detailVisible.value = false
      loadFeedback()
    } catch (e) {
      ElMessage.error(e.message)
    }
  }).catch(() => {})
}

loadFeedback()
</script>

<style scoped>
.manager-list { background: var(--bg-card); border-radius: 8px; overflow: hidden; }
.list-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.list-header h3 {
  margin: 0; font-size: 15px; font-weight: 600;
  color: var(--text-primary);
  display: flex; align-items: center; gap: 8px;
}
.pending-badge {
  font-size: 11px; font-weight: 500;
  color: var(--accent-fg); background: var(--accent);
  padding: 1px 8px; border-radius: 10px;
}
.manager-table { width: 100%; border-collapse: collapse; }
.manager-table th,
.manager-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid var(--border-soft);
  vertical-align: top;
}
.manager-table th {
  background: var(--bg-page);
  font-weight: 500;
  color: var(--text-secondary);
}
.manager-table tr:last-child td { border-bottom: none; }
.manager-table tr:hover td { background: var(--bg-hover); }
.content-cell {
  max-width: 320px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
  cursor: pointer;
  line-height: 1.6;
}
.content-cell:hover { color: var(--accent); }
.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
.detail-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.meta-time { font-size: 12px; color: var(--text-muted); }
.detail-content {
  font-size: 14px; line-height: 1.8; color: var(--text-primary);
  white-space: pre-wrap; word-break: break-word;
  background: var(--bg-page); border-radius: 8px; padding: 14px 16px;
}
.detail-contact { font-size: 13px; color: var(--text-muted); margin-top: 12px; }
@media (max-width: 768px) {
  .manager-table th:nth-child(3),
  .manager-table td:nth-child(3),
  .manager-table th:nth-child(5),
  .manager-table td:nth-child(5) { display: none; }
  .content-cell { max-width: 180px; }
  .list-header { flex-direction: column; align-items: flex-start; gap: 10px; }
}
</style>
