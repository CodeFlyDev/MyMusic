$bytes = [IO.File]::ReadAllBytes('E:\Gitee\my-music\backend\target\mymusic-backend-1.0.0.jar')
$b64 = [Convert]::ToBase64String($bytes)
$b64 | Set-Content -Path 'E:\Gitee\my-music\backend\target\mymusic-backend-1.0.0.jar.b64' -NoNewline
Write-Host "Done, length:" $b64.Length