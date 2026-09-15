$bytes = [IO.File]::ReadAllBytes('E:\Gitee\my-music\frontend\dist.tar.gz')
$b64 = [Convert]::ToBase64String($bytes)
$b64 | Set-Content -Path 'E:\Gitee\my-music\frontend\dist.tar.gz.b64' -NoNewline
Write-Host "Done, length:" $b64.Length