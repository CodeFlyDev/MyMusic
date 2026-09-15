import { ref, watch } from 'vue'

const STORAGE_KEY = 'mymusic:theme'

// 亮色 / 深色模式；默认跟随系统偏好
const prefersDark =
  typeof window !== 'undefined' &&
  window.matchMedia &&
  window.matchMedia('(prefers-color-scheme: dark)').matches

const isDark = ref(
  localStorage.getItem(STORAGE_KEY)
    ? localStorage.getItem(STORAGE_KEY) === 'dark'
    : prefersDark
)

// 立即把 .dark 类同步到 <html>，Element Plus dark css-vars 据此生效
function apply() {
  document.documentElement.classList.toggle('dark', isDark.value)
}
apply()

watch(isDark, (v) => {
  apply()
  localStorage.setItem(STORAGE_KEY, v ? 'dark' : 'light')
})

export function toggleTheme() {
  isDark.value = !isDark.value
}

export function useTheme() {
  return { isDark, toggleTheme }
}
