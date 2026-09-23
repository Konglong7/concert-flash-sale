# ============================================================
# 并发压测脚本：N 个用户同时抢 1 个票档，验证 0 超卖 + 统计 QPS
# 用法: .\load-test.ps1 -Users 100 -TierId 1 -Stock 20
# ============================================================
param(
    [int]$Users = 100,
    [int]$TierId = 1,
    [int]$Stock = 20,
    [int]$Concurrent = 50,
    [switch]$Direct
)
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$mysql = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'
$redisCli = 'E:\Redis\redis-cli.exe'
$ts = Get-Date -Format 'HHmmss'

function DbExec($sql) { & $mysql -uroot -p123456 -N -e "use flash_sale; $sql" 2>$null }

Write-Host "=== 重置数据: $Users 用户抢 票档$TierId 的 $Stock 张票 ===" -ForegroundColor Cyan
DbExec "truncate table orders; truncate table user_ticket_limit; update ticket_tier set stock = total_stock; update ticket_tier set stock = $Stock where id = $TierId;"
& $redisCli -a 123456 eval "for _,k in ipairs(redis.call('keys','seckill:*')) do redis.call('del',k) end for _,k in ipairs(redis.call('keys','rate:*')) do redis.call('del',k) end return 1" 0 2>$null | Out-Null
& $redisCli -a 123456 set "seckill:stock:$TierId" $Stock 2>$null | Out-Null
Write-Host '  数据已重置, Redis 库存已预载'

# --- 注册并登录用户 ---
Write-Host "=== 注册/登录 $Users 个用户 ===" -ForegroundColor Cyan
$tokens = 1..$Users | ForEach-Object -Parallel {
    $idx = $_
    $ts = $using:ts
    $base = $using:base
    $u = "lt$idx$ts"
    $b = @{ username = $u; password = '123456' } | ConvertTo-Json
    try { Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body $b | Out-Null } catch {}
    (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $b).data.token
} -ThrottleLimit 20
$tokens = @($tokens | Where-Object { $_ })
Write-Host "  已就绪用户: $($tokens.Count)"

# --- 并发抢票 ---
Write-Host "=== 开始抢票 (并发度 $Concurrent) ===" -ForegroundColor Cyan
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$url = if ($Direct) { "$base/api/order/direct" } else { "$base/api/order/seckill" }
$results = $tokens | ForEach-Object -Parallel {
    $token = $_
    $base = $using:base
    $tierId = $using:TierId
    $url = $using:url
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $hdr = @{ Authorization = "Bearer $token" }
    $b = @{ concertId = 1; tierId = $tierId; idCard = '110101199001011234' } | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Method Post -Uri $url -Headers $hdr -ContentType 'application/json' -Body $b
        [pscustomobject]@{ code = $r.code; ms = $sw.ElapsedMilliseconds }
    } catch {
        $c = 0
        try { $c = ($_.ErrorDetails.Message | ConvertFrom-Json).code } catch {}
        [pscustomobject]@{ code = $c; ms = $sw.ElapsedMilliseconds }
    }
} -ThrottleLimit $Concurrent
$sw.Stop()

# --- 统计 ---
$total = $results.Count
$success = ($results | Where-Object { $_.code -eq 200 }).Count
$soldOut = ($results | Where-Object { $_.code -eq 400 }).Count
$rateLimited = ($results | Where-Object { $_.code -eq 429 }).Count
$other = $total - $success - $soldOut - $rateLimited
$elapsedMs = $sw.ElapsedMilliseconds
$qps = if ($elapsedMs -gt 0) { [math]::Round($total / $elapsedMs * 1000, 1) } else { $total }
$avg = [math]::Round(($results | Measure-Object ms -Average).Average, 1)
$sorted = @($results | Sort-Object ms)
$p99 = $sorted[[math]::Min($total - 1, [int][math]::Floor($total * 0.99))].ms

Write-Host ''
Write-Host '========== 压测结果 ==========' -ForegroundColor Cyan
Write-Host "  总请求: $total | 成功(排队): $success | 售罄拒绝: $soldOut | 限流: $rateLimited | 其他: $other"
Write-Host "  接口耗时: 总时长 ${elapsedMs}ms | QPS ≈ $qps | 平均 ${avg}ms"
Write-Host "  P99: ${p99}ms"

# --- 轮询等待订单全部落库 ---
Write-Host '=== 等待异步下单完成 ===' -ForegroundColor Cyan
$expected = $Stock
$dbOrders = 0
foreach ($i in 1..20) {
    Start-Sleep 1
    $dbOrders = [int](DbExec "select count(*) from orders")
    if ($dbOrders -ge $expected) { break }
}
$dbStock = [int](DbExec "select stock from ticket_tier where id=$TierId")
$redisStock = [int](& $redisCli -a 123456 get "seckill:stock:$TierId" 2>$null)

Write-Host ''
Write-Host '========== 数据一致性验证 ==========' -ForegroundColor Cyan
$ok = $true
function Verify($name, $cond, $actual) {
    if ($cond) { Write-Host "  PASS - $name (实际: $actual)" -ForegroundColor Green }
    else { $script:ok = $false; Write-Host "  FAIL - $name (实际: $actual)" -ForegroundColor Red }
}
Verify "订单数 = 成功数 = $expected" ($dbOrders -eq $expected) $dbOrders
Verify "DB库存 = 0 (不多不少)" ($dbStock -eq 0) $dbStock
if (-not $Direct) { Verify "Redis库存 = 0 (不多不少)" ($redisStock -eq 0) $redisStock }
Verify "0 超卖 (DB库存 >= 0)" ($dbStock -ge 0) $dbStock

Write-Host ''
if ($ok) { Write-Host '========== 压测通过：0 超卖 ✓ ==========' -ForegroundColor Green }
else { Write-Host '========== 压测未通过 ==========' -ForegroundColor Red }
