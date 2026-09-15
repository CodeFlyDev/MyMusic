// 后端接口封装：统一解析 { code, data, msg } 响应格式
async function request(url, options = {}) {
  const res = await fetch(url, options)
  const body = await res.json().catch(() => ({}))
  if (!res.ok || body.code !== 0) {
    throw new Error(body.msg || `请求失败 (${res.status})`)
  }
  return body
}

export function listSongs(keyword) {
  const q = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  return request(`/api/songs${q}`).then(b => b.data)
}

// 按歌手/专辑精确过滤（详情页用）
export function listSongsByArtist(artist) {
  return request(`/api/songs?artist=${encodeURIComponent(artist)}`).then(b => b.data)
}
export function listSongsByAlbum(album) {
  return request(`/api/songs?album=${encodeURIComponent(album)}`).then(b => b.data)
}

export function listArtists() {
  return request('/api/artists').then(b => b.data)
}

export function listAlbums() {
  return request('/api/albums').then(b => b.data)
}

// 删除歌曲需要管理口令（与上传同一口令，服务端校验）
export function deleteSong(id, code) {
  return request(`/api/songs/${id}`, {
    method: 'DELETE',
    headers: code ? { 'X-Upload-Code': code } : {}
  })
}

// 管理后台门禁：仅校验口令，正确才放行
export function verifyPasscode(code) {
  return request('/api/songs/upload/check', {
    headers: code ? { 'X-Upload-Code': code } : {}
  })
}

// 歌词：按需获取原始 LRC 文本（无歌词时返回 null）
export function getLyrics(id) {
  return request(`/api/songs/${id}/lyrics`).then(b => b.data.lyrics ?? null)
}

// 手动编辑歌词，需要管理口令
export function updateLyrics(id, lyrics, code) {
  return request(`/api/songs/${id}/lyrics`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify({ lyrics })
  })
}

export function formatDuration(sec) {
  if (!sec || sec <= 0) return '--:--'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return Math.ceil(bytes / 1024) + ' KB'
}

export function formatDateTime(s) {
  if (!s) return ''
  return s.replace('T', ' ').slice(0, 19)
}

// 封面地址（无封面时返回空串，由调用方给占位）
export function coverUrlOf(item) {
  return item && item.coverName ? `/media/covers/${item.coverName}` : ''
}

// 用户反馈 -----------------------------------------------------------------

// 提交反馈（公开，无需口令）
export function submitFeedback(content, contact) {
  return request('/api/feedback', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content, contact: contact || '' })
  })
}

// 管理员查看反馈列表（需上传口令）
export function listFeedback(code) {
  return request('/api/feedback', {
    headers: code ? { 'X-Upload-Code': code } : {}
  }).then(b => b.data)
}

// 切换反馈的已解决状态
export function resolveFeedback(id, resolved, code) {
  return request(`/api/feedback/${id}/resolve`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify({ resolved })
  })
}

// 删除反馈
export function deleteFeedback(id, code) {
  return request(`/api/feedback/${id}`, {
    method: 'DELETE',
    headers: code ? { 'X-Upload-Code': code } : {}
  })
}

// 公告 -----------------------------------------------------------------

// 获取可见公告列表（公开）
export function listAnnouncements() {
  return request('/api/announcements').then(b => b.data)
}

// 管理员获取所有公告（需口令）
export function listAnnouncementsAdmin(code) {
  return request('/api/announcements/admin', {
    headers: code ? { 'X-Upload-Code': code } : {}
  }).then(b => b.data)
}

// 创建公告（需口令）
export function createAnnouncement(data, code) {
  return request('/api/announcements', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify(data)
  })
}

// 更新公告（需口令）
export function updateAnnouncement(id, data, code) {
  return request(`/api/announcements/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify(data)
  })
}

// 删除公告（需口令）
export function deleteAnnouncement(id, code) {
  return request(`/api/announcements/${id}`, {
    method: 'DELETE',
    headers: code ? { 'X-Upload-Code': code } : {}
  })
}

// 更新日志 -----------------------------------------------------------------

// 获取更新日志列表（公开）
export function listChangelogs() {
  return request('/api/changelogs').then(b => b.data)
}

// 管理员获取所有更新日志（需口令）
export function listChangelogsAdmin(code) {
  return request('/api/changelogs/admin', {
    headers: code ? { 'X-Upload-Code': code } : {}
  }).then(b => b.data)
}

// 创建更新日志（需口令）
export function createChangelog(data, code) {
  return request('/api/changelogs', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify(data)
  })
}

// 更新更新日志（需口令）
export function updateChangelog(id, data, code) {
  return request(`/api/changelogs/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(code ? { 'X-Upload-Code': code } : {})
    },
    body: JSON.stringify(data)
  })
}

// 删除更新日志（需口令）
export function deleteChangelog(id, code) {
  return request(`/api/changelogs/${id}`, {
    method: 'DELETE',
    headers: code ? { 'X-Upload-Code': code } : {}
  })
}
