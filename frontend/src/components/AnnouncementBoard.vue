<template>
  <div v-if="announcements.length" class="announcement-board">
    <div class="board-header">
      <span class="board-title">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
          <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/>
        </svg>
        公告栏
      </span>
      <router-link to="/community" class="board-more">查看全部</router-link>
    </div>
    <div class="board-list">
      <div
        v-for="ann in announcements.slice(0, 3)"
        :key="ann.id"
        class="announcement-item"
        :class="{ 'priority-top': ann.priority === 2, 'priority-high': ann.priority === 1 }"
      >
        <div class="ann-content">
          <div class="ann-header">
            <h4 class="ann-title" v-html="highlight(ann.title)"></h4>
            <span v-if="ann.priority === 2" class="priority-tag top">置顶</span>
            <span v-else-if="ann.priority === 1" class="priority-tag high">重要</span>
          </div>
          <div class="ann-body" v-html="renderMarkdown(ann.content)"></div>
          <div class="ann-meta">
            <span>{{ formatDate(ann.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { listAnnouncements, formatDateTime } from '../api'

const router = useRouter()
const announcements = ref([])

onMounted(async () => {
  try {
    announcements.value = await listAnnouncements()
  } catch (e) {
    console.error('加载公告失败', e)
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(' ', 'T'))
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function highlight(text) {
  return text
}

function renderMarkdown(text) {
  if (!text) return ''
  // 简单的 markdown 渲染：支持 **加粗**、`代码`、换行
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.announcement-board {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.board-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.board-more {
  font-size: 12px;
  color: var(--text-muted);
  text-decoration: none;
  transition: color .15s;
}
.board-more:hover { color: var(--text-primary); }
.board-list { display: flex; flex-direction: column; gap: 10px; }
.announcement-item {
  padding: 12px 14px;
  background: var(--bg-page);
  border-radius: 8px;
  border: 1px solid var(--border-soft);
  transition: border-color .15s, box-shadow .15s;
}
.announcement-item:hover {
  border-color: var(--border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.announcement-item.priority-top {
  border-left: 3px solid #f56c6c;
}
.announcement-item.priority-high {
  border-left: 3px solid #e6a23c;
}
.ann-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.ann-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.4;
}
.priority-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 600;
  white-space: nowrap;
}
.priority-tag.top { background: #fef0f0; color: #f56c6c; }
.priority-tag.high { background: #fdf6ec; color: #e6a23c; }
.ann-body {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-word;
}
.ann-body code {
  background: var(--bg-hover);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 12px;
  font-family: monospace;
}
.ann-meta {
  margin-top: 8px;
  font-size: 11px;
  color: var(--text-faint);
}
@media (max-width: 768px) {
  .announcement-board { padding: 12px 16px; }
  .announcement-item { padding: 10px 12px; }
}
</style>