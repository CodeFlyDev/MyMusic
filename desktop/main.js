const { app, BrowserWindow, ipcMain, shell } = require('electron')
const path = require('path')
const fs = require('fs')

let mainWindow = null

function settingsPath() {
  return path.join(app.getPath('userData'), 'settings.json')
}

function loadServerUrl() {
  try {
    const url = JSON.parse(fs.readFileSync(settingsPath(), 'utf8')).serverUrl
    return typeof url === 'string' && url ? url : null
  } catch {
    return null
  }
}

function saveServerUrl(url) {
  fs.mkdirSync(path.dirname(settingsPath()), { recursive: true })
  fs.writeFileSync(settingsPath(), JSON.stringify({ serverUrl: url }))
}

/** 补全 scheme、去掉尾部斜杠；无效返回 null */
function normalizeUrl(raw) {
  if (!raw) return null
  let url = String(raw).trim()
  if (!url) return null
  if (!/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(url)) url = 'http://' + url
  while (url.endsWith('/')) url = url.slice(0, -1)
  const rest = url.replace(/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//, '')
  return rest ? url : null
}

/** 原生设置窗口：首次启动 / 连接失败时显示 */
function showSettings(errorUrl) {
  const win = new BrowserWindow({
    width: 620,
    height: 440,
    resizable: false,
    maximizable: false,
    backgroundColor: '#1d2431',
    show: false,
    title: 'MyMusic',
    webPreferences: { nodeIntegration: true, contextIsolation: false }
  })
  win.setMenuBarVisibility(false)
  win.once('ready-to-show', () => win.show())
  const query = errorUrl ? { error: encodeURIComponent(errorUrl) } : {}
  win.loadFile(path.join(__dirname, 'settings.html'), { query })
}

/** 主窗口：加载已部署的音乐站点 */
function createMainWindow(url) {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 840,
    backgroundColor: '#1d2431',
    autoHideMenuBar: true,
    title: 'MyMusic',
    show: false,
    webPreferences: {}
  })
  mainWindow.setMenuBarVisibility(false)
  mainWindow.once('ready-to-show', () => mainWindow.show())

  // 主帧加载失败（服务器不可达/地址错误）→ 回到设置页
  let failed = false
  mainWindow.webContents.on('did-fail-load', (_e, code, _desc, _url, isMainFrame) => {
    if (!isMainFrame || failed || code === -3) return
    failed = true
    const win = mainWindow
    mainWindow = null
    win.destroy()
    showSettings(url)
  })

  // 站点内的新窗口链接交给系统浏览器
  mainWindow.webContents.setWindowOpenHandler(({ url: u }) => {
    if (/^https?:/i.test(u)) shell.openExternal(u)
    return { action: 'deny' }
  })

  mainWindow.loadURL(url)
}

const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const [win] = BrowserWindow.getAllWindows()
    if (win) {
      if (win.isMinimized()) win.restore()
      win.focus()
    }
  })

  app.whenReady().then(() => {
    ipcMain.on('server-url', (_e, raw) => {
      const url = normalizeUrl(raw)
      if (!url) return
      saveServerUrl(url)
      createMainWindow(url)
    })

    const url = loadServerUrl()
    if (url) createMainWindow(url)
    else showSettings(null)
  })

  app.on('window-all-closed', () => app.quit())
}
