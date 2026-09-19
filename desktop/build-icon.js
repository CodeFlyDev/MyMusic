// 从内嵌 SVG 生成 Windows 图标（CI 中运行，避免仓库提交二进制图标文件）
const fs = require('fs')
const sharp = require('sharp')
const pngToIco = require('png-to-ico')

const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
  <rect width="108" height="108" rx="18" fill="#1d2431"/>
  <g transform="translate(21.6 21.6) scale(2.8)">
    <path fill="#409eff" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
  </g>
</svg>`

;(async () => {
  await sharp(Buffer.from(svg), { density: 300 })
    .resize(256, 256)
    .png()
    .toFile('icon-256.png')
  const buf = await pngToIco(['icon-256.png'])
  fs.writeFileSync('icon.ico', buf)
  console.log('icon.ico generated')
})().catch(e => {
  console.error(e)
  process.exit(1)
})
