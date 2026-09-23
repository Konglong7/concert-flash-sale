# ============================================================
# 生成 JMeter 压测所需的用户数据 CSV（username,password,token）
# 用法: .\gen-users.ps1 -Count 500   -> 生成 users.csv
# ============================================================
param([int]$Count = 500)

$base = 'http://localhost:8080'
$ts = Get-Date -Format 'HHmmss'
$out = Join-Path $PSScriptRoot 'users.csv'

$lines = New-Object System.Collections.Generic.List[string]
$done = 0
1..$Count | ForEach-Object -Parallel {
    $idx = $_
    $ts = $using:ts
    $base = $using:base
    $u = "jm$idx$ts"
    $p = '123456'
    $b = @{ username = $u; password = $p } | ConvertTo-Json
    try { Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body $b | Out-Null } catch {}
    $r = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $b
    "$u,$p,$($r.data.token)"
} -ThrottleLimit 20 | ForEach-Object {
    $lines.Add($_)
    $done++
    if ($done % 50 -eq 0) { Write-Host "  $done / $Count" }
}

$lines | Set-Content $out -Encoding UTF8
Write-Host "已生成 $out（共 $($lines.Count) 个用户）"
Write-Host "JMeter 运行示例:"
Write-Host "  jmeter -n -t seckill-test.jmx -Jcsv=$out -Jthreads=$Count -JtierId=1 -l seckill-report.jtl"
