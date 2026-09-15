// LRC 歌词解析与时间定位工具
import { pinyin } from 'pinyin-pro'

// 时间标签：[mm:ss.xx]，xx 为 2 位百分秒或 3 位毫秒
const TIME_TAG = /\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]/g
// 元数据行：[ti:歌名] [ar:歌手] [al:专辑] [by:] [re:] [ve:]
const META_TAG = /^\[(ti|ar|al|by|re|ve|length|hash|sign|qq):.*\]\s*$/i

/**
 * 解析 LRC 文本为时间有序的歌词行数组
 * @param {string} text 原始 LRC 文本
 * @returns {{ time: number, text: string }[]}
 */
export function parseLrc(text) {
  if (!text || !text.trim()) return []

  let offsetMs = 0
  const lines = []

  for (const raw of text.split(/\r?\n/)) {
    const line = raw.replace(/\s+$/, '')
    if (!line.trim()) continue

    // [offset:200] 全局时间偏移（毫秒，正数提前/负数延后，各播放器约定不一；按常见 LRC 规范处理）
    const offsetMatch = line.match(/^\[offset:(-?\d+)\]/i)
    if (offsetMatch) {
      offsetMs = parseInt(offsetMatch[1], 10) || 0
      continue
    }
    if (META_TAG.test(line)) continue

    // 提取该行所有时间戳（支持 [00:12.50][00:30.00]歌词 多时间戳行）
    const times = []
    let lastIndex = 0
    let m
    TIME_TAG.lastIndex = 0
    while ((m = TIME_TAG.exec(line)) !== null) {
      const mm = parseInt(m[1], 10)
      const ss = parseInt(m[2], 10)
      const frac = m[3]
        ? m[3].length === 3 ? parseInt(m[3], 10) : parseInt(m[3], 10) * 10
        : 0
      times.push(mm * 60 + ss + frac / 1000)
      lastIndex = TIME_TAG.lastIndex
    }
    const content = line.slice(lastIndex).trim()
    if (!times.length) continue
    for (const t of times) {
      lines.push({ time: t - offsetMs / 1000, text: content })
    }
  }

  lines.sort((a, b) => a.time - b.time)
  return lines
}

/**
 * 二分查找当前时间对应的歌词行索引（最后一条 time <= t）
 * @param {{time:number}[]} lines
 * @param {number} t 当前播放时间（秒）
 * @returns {number} 索引；无匹配返回 -1
 */
export function findLineIndex(lines, t) {
  if (!lines || !lines.length) return -1
  let lo = 0
  let hi = lines.length - 1
  let ans = -1
  while (lo <= hi) {
    const mid = (lo + hi) >> 1
    if (lines[mid].time <= t) {
      ans = mid
      lo = mid + 1
    } else {
      hi = mid - 1
    }
  }
  return ans
}

/**
 * 取歌手名首字母（用于字母索引）；中文按拼音首字母，非字母归到 '#'
 */
export function initialOf(name) {
  if (!name) return '#'
  const first = name.trim().charAt(0)
  // 先取拼音首字母（中文），再转大写；英文直接取首字符
  const ch = pinyin(first, { pattern: 'first', toneType: 'none' }).charAt(0).toUpperCase()
  return /[A-Z]/.test(ch) ? ch : '#'
}
