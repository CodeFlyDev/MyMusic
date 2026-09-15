<template>
  <div class="song-row" :class="{ active: isActive }" @click="$emit('play', song)">
    <div v-if="index != null" class="idx">{{ index }}</div>
    <div class="cover">
      <img v-if="song.coverName" :src="`/media/covers/${song.coverName}`" alt="" />
      <span v-else>{{ song.title ? song.title.charAt(0) : '♪' }}</span>
    </div>
    <div class="info">
      <div class="t-row">
        <span class="t" v-html="highlight(song.title)"></span>
        <span class="dur">{{ formatDuration(song.durationSec) }}</span>
      </div>
      <div class="sub">
        <span class="link" @click.stop="goArtist(song.artist)">{{ song.artist || '未知歌手' }}</span>
        <template v-if="song.artist && song.album">
          <span class="sep">·</span>
          <span class="link" @click.stop="goAlbum(song.album)">{{ song.album }}</span>
        </template>
      </div>
    </div>
    <div v-if="$slots.default" class="meta-col">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { player } from '../store/player'
import { formatDuration } from '../api'

const props = defineProps({
  song: { type: Object, required: true },
  index: { type: Number, default: null },
  keyword: { type: String, default: '' }
})
defineEmits(['play'])

const router = useRouter()

const isActive = computed(() => player.current && player.current.id === props.song.id)

function highlight(text) {
  if (!text) return ''
  if (!props.keyword) return text
  const safe = props.keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(safe, 'gi'), m => `<span class="hl">${m}</span>`)
}

function goArtist(name) {
  if (name && name !== '未知歌手') router.push(`/artist/${encodeURIComponent(name)}`)
}
function goAlbum(name) {
  if (name) router.push(`/album/${encodeURIComponent(name)}`)
}
</script>

<style scoped>
.song-row {
  display: flex; align-items: center; gap: 14px;
  padding: 10px 12px; border-radius: 6px; cursor: pointer;
  transition: background .15s;
}
.song-row:hover { background: var(--bg-hover); }
.song-row.active { background: var(--bg-active); }
.idx {
  width: 22px; text-align: center; flex-shrink: 0;
  font-size: 13px; color: var(--text-faint); font-variant-numeric: tabular-nums;
}
.cover {
  width: 44px; height: 44px; border-radius: 4px; flex-shrink: 0;
  background: var(--bg-cover); color: var(--text-faint);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 500; overflow: hidden;
}
.cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.info { flex: 1; min-width: 0; }
.t-row { display: flex; align-items: baseline; gap: 10px; }
.t { font-size: 14px; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dur { font-size: 12px; color: var(--text-faint); flex-shrink: 0; font-variant-numeric: tabular-nums; }
.sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.link { cursor: pointer; transition: color .15s; }
.link:hover { color: var(--text-primary); }
.sep { margin: 0 4px; color: var(--text-disabled); }
.meta-col { font-size: 12px; color: var(--text-faint); flex-shrink: 0; text-align: right; line-height: 1.6; }
:deep(.hl) { color: var(--text-primary); font-weight: 600; }
</style>
