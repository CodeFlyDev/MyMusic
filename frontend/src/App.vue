<template>
  <el-config-provider :locale="zhCn">
    <div class="app-shell">
      <header class="app-header">
        <div class="brand" @click="$router.push('/')">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 3v10.55A4 4 0 1 0 14 17V7h4V3h-6z"/></svg>
          <span>Music</span>
        </div>
        <nav class="nav">
          <router-link to="/" class="nav-item" exact-active-class="active">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22V12h6v10"/></svg>
            <span class="nav-label">首页</span>
          </router-link>
          <router-link to="/artists" class="nav-item" active-class="active">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10v1a7 7 0 0 0 14 0v-1"/><path d="M12 19v3"/></svg>
            <span class="nav-label">歌手</span>
          </router-link>
          <router-link to="/albums" class="nav-item" active-class="active">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3"/></svg>
            <span class="nav-label">专辑</span>
          </router-link>
          <router-link to="/community" class="nav-item" active-class="active">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
            <span class="nav-label">动态</span>
          </router-link>
          <button class="nav-item theme-toggle" @click="toggleTheme" :title="isDark ? '切换到亮色' : '切换到深色'">
            <svg v-if="isDark" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>
            <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            <span class="toggle-label">{{ isDark ? '浅色' : '深色' }}</span>
          </button>
        </nav>
        <input
          class="search-box"
          v-model="keyword"
          placeholder="搜索歌曲、歌手、专辑"
          @keyup.enter="doSearch"
        />
      </header>

      <main class="app-main">
        <router-view @play="onPlay" />
      </main>

      <PlayerBar />
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { zhCn } from 'element-plus/es/locale/index'
import { ElMessage } from 'element-plus'
import PlayerBar from './components/PlayerBar.vue'
import { togglePlay } from './store/player'
import { useTheme } from './store/theme'

const { isDark, toggleTheme } = useTheme()

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.q || '')

watch(() => route.query.q, (q) => {
  if (route.path !== '/search') keyword.value = q || ''
})

function doSearch() {
  const q = keyword.value.trim()
  router.push(q ? `/search?q=${encodeURIComponent(q)}` : '/search')
}

function onPlay(song) {
  togglePlay(song)
}
</script>

<style>
/* ===== 主题色变量：亮色（默认） ===== */
:root {
  --bg-page: #f7f8fa;
  --bg-card: #fff;
  --bg-hover: #f2f3f5;
  --bg-active: #eef1f5;
  --bg-cover: #f0f1f3;
  --bg-cover-hover: #e8eaed;
  --text-primary: #1f2329;
  --text-secondary: #4e5969;
  --text-muted: #86909c;
  --text-faint: #a9aeb8;
  --text-disabled: #c9cdd4;
  --border: #ebedf0;
  --border-soft: #f2f3f5;
  --border-input: #e5e6eb;
  --accent: #1f2329;
  --accent-fg: #fff;
  --shadow-fab: rgba(0,0,0,0.06);
}
/* ===== 主题色变量：深色 ===== */
html.dark {
  --bg-page: #14161c;
  --bg-card: #1c1f26;
  --bg-hover: #2a2e37;
  --bg-active: #32363f;
  --bg-cover: #2a2e37;
  --bg-cover-hover: #3a3f48;
  --text-primary: #e6e8ed;
  --text-secondary: #a9aeb8;
  --text-muted: #6b7280;
  --text-faint: #5b6068;
  --text-disabled: #3f444c;
  --border: #2a2e37;
  --border-soft: #23262e;
  --border-input: #2a2e37;
  --accent: #e6e8ed;
  --accent-fg: #1f2329;
  --shadow-fab: rgba(0,0,0,0.3);
}

* { box-sizing: border-box; }
html, body, #app { height: 100%; margin: 0; }
body {
  font-family: 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg-page);
  color: var(--text-primary);
  transition: background .2s, color .2s;
}
.app-shell { min-height: 100%; }
.app-header {
  position: sticky; top: 0; z-index: 50;
  display: flex; align-items: center; gap: 28px;
  background: var(--bg-card); border-bottom: 1px solid var(--border);
  padding: 0 32px; height: 56px;
  transition: background .2s, border-color .2s;
}
.brand {
  display: flex; align-items: center; gap: 7px;
  font-size: 16px; font-weight: 600; color: var(--text-primary);
  cursor: pointer; white-space: nowrap;
}
.nav { display: flex; gap: 4px; flex: 1; align-items: center; }
.nav-item {
  padding: 6px 14px; border-radius: 4px;
  font-size: 14px; color: var(--text-secondary); text-decoration: none;
  transition: background .15s, color .15s;
  display: inline-flex; align-items: center; gap: 5px;
  border: none; background: transparent; cursor: pointer;
  font-family: inherit;
}
.nav-item:hover { background: var(--bg-hover); color: var(--text-primary); }
.nav-item.active { color: var(--text-primary); font-weight: 500; background: var(--bg-hover); }
.theme-toggle .toggle-label { line-height: 1; }
/* 主题按钮与导航项图标视觉对齐 */
.theme-toggle svg { flex-shrink: 0; }
.search-box {
  width: 220px; padding: 7px 12px;
  border: 1px solid var(--border-input); border-radius: 4px;
  font-size: 13px; outline: none; background: var(--bg-page);
  font-family: inherit; color: var(--text-primary);
  transition: border-color .15s, background .15s;
}
.search-box:focus { border-color: var(--text-muted); background: var(--bg-card); }

/* 表单按钮与输入框聚焦色统一为深灰，与主 UI 风格一致 */
.el-button--primary {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-hover-bg-color: var(--text-secondary);
  --el-button-hover-border-color: var(--text-secondary);
  --el-button-active-bg-color: var(--accent);
  --el-button-active-border-color: var(--accent);
  --el-button-disabled-bg-color: var(--text-faint);
  --el-button-disabled-border-color: var(--text-faint);
  --el-button-text-color: var(--accent-fg);
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--text-primary) inset !important;
}
.el-textarea__inner:focus {
  box-shadow: 0 0 0 1px var(--text-primary) inset !important;
}
/* Element Plus 卡片、对话框跟随主题 */
.el-card {
  --el-card-bg-color: var(--bg-card);
  --el-card-border-color: var(--border);
}
.el-dialog {
  --el-dialog-bg-color: var(--bg-card);
  --el-dialog-title-font-color: var(--text-primary);
}
.el-input__wrapper {
  background-color: var(--bg-page);
  box-shadow: 0 0 0 1px var(--border-input) inset;
}
.el-textarea__inner {
  background-color: var(--bg-page);
  color: var(--text-primary);
  box-shadow: 0 0 0 1px var(--border-input) inset;
}
.el-loading-mask { background-color: rgba(0,0,0,0.5); }

.app-main {
  flex: 1; width: 100%; max-width: 1000px;
  margin: 0 auto; padding: 28px 32px 96px;
}

@media (max-width: 768px) {
  .app-header { gap: 12px; padding: 0 16px; height: 52px; }
  .brand span { display: none; }
  .nav { gap: 2px; }
  .nav-item { padding: 6px 10px; font-size: 13px; }
  /* 窄屏仅展示图标，隐藏文字 */
  .nav-label, .toggle-label { display: none; }
  .search-box { width: 130px; }
  .app-main { padding: 16px 16px 90px; }
}
</style>