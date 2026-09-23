# ============================================================
# 功能测试脚本：抢票全链路（需后端已启动，建议 PAY_EXPIRE_MINUTES=0.05）
# 用法: .\functional-test.ps1
# ============================================================
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$mysql = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'
$redisCli = 'E:\Redis\redis-cli.exe'
$ts = Get-Date -Format 'HHmmss'
$script:pass = 0
$script:fail = 0

function Check($name, $cond) {
    if ($cond) { $script:pass++; Write-Host "  PASS - $name" -ForegroundColor Green }
    else { $script:fail++; Write-Host "  FAIL - $name" -ForegroundColor Red }
}

function NewUser($n) {
    $u = "ft$n$ts"
    $b = @{ username = $u; password = '123456' } | ConvertTo-Json
    try { Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body $b | Out-Null } catch {}
    $r = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $b
    return @{ user = $u; token = $r.data.token; hdr = @{ Authorization = "Bearer $($r.data.token)" } }
}

function DbQuery($sql) {
    (& $mysql -uroot -p123456 -N -e "use flash_sale; $sql" 2>$null)
}

function Seckill($u, $tierId) {
    $b = @{ concertId = 1; tierId = $tierId; idCard = '110101199001011234' } | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Method Post -Uri "$base/api/order/seckill" -Headers $u.hdr -ContentType 'application/json' -Body $b
        return $r.code
    } catch { return ($_.ErrorDetails.Message | ConvertFrom-Json).code }
}

function PollResult($u, $tierId) {
    foreach ($i in 1..12) {
        Start-Sleep 1
        try { $r = Invoke-RestMethod -Uri "$base/api/order/seckill/result/$tierId" -Headers $u.hdr } catch { continue }
        if ($r.data.status -ne 'processing') { return $r.data }
    }
    return @{ status = 'timeout' }
}

Write-Host '=== T1 抢票成功链路 ===' -ForegroundColor Cyan
$u1 = NewUser 1
$c1 = Seckill $u1 1
Check 'T1.1 抢票接口返回200' ($c1 -eq 200)
$r1 = PollResult $u1 1
Check "T1.2 轮询结果为success (实际: $($r1.status))" ($r1.status -eq 'success')
$orderNo = $r1.orderNo
$dbStock = [int](DbQuery "select stock from ticket_tier where id=1")
Check "T1.3 DB库存扣减为99 (实际: $dbStock)" ($dbStock -eq 99)
$rdStock = [int](& $redisCli -a 123456 get seckill:stock:1)
Check "T1.4 Redis库存扣减为99 (实际: $rdStock)" ($rdStock -eq 99)
$ordCnt = [int](DbQuery "select count(*) from orders where order_no='$orderNo' and status=0")
Check 'T1.5 订单落库且待支付' ($ordCnt -eq 1)
$limCnt = [int](DbQuery "select purchased_count from user_ticket_limit limit 1")
Check "T1.6 限购计数为1 (实际: $limCnt)" ($limCnt -eq 1)

Write-Host '=== T2 重复抢票拦截 ===' -ForegroundColor Cyan
$c2 = Seckill $u1 1
Check "T2.1 重复抢票被拒返回400 (实际: $c2)" ($c2 -eq 400)

Write-Host '=== T3 未登录拦截 ===' -ForegroundColor Cyan
try {
    $r = Invoke-RestMethod -Method Post -Uri "$base/api/order/seckill" -ContentType 'application/json' -Body '{"concertId":1,"tierId":1,"idCard":"x"}'
    Check "T3.1 无JWT返回401 (实际: $($r.code))" ($r.code -eq 401)
} catch {
    $code = ($_.ErrorDetails.Message | ConvertFrom-Json).code
    Check "T3.1 无JWT返回401 (实际: $code)" ($code -eq 401)
}

Write-Host '=== T4 超时释放（3秒支付有效期）===' -ForegroundColor Cyan
Write-Host '  等待10秒让延时消息触发...'
Start-Sleep 10
$oStatus = [int](DbQuery "select status from orders where order_no='$orderNo'")
Check "T4.1 订单转为超时状态3 (实际: $oStatus)" ($oStatus -eq 3)
$dbStock2 = [int](DbQuery "select stock from ticket_tier where id=1")
Check "T4.2 DB库存回补为100 (实际: $dbStock2)" ($dbStock2 -eq 100)
$rdStock2 = [int](& $redisCli -a 123456 get seckill:stock:1)
Check "T4.3 Redis库存回补为100 (实际: $rdStock2)" ($rdStock2 -eq 100)
$inSet = [int](& $redisCli -a 123456 sismember seckill:bought:1 $u1.user)
Check 'T4.4 去重标记已移除(可重新抢)' ($inSet -eq 0)
$limCnt2 = [int](DbQuery "select purchased_count from user_ticket_limit limit 1")
Check "T4.5 限购计数回退为0 (实际: $limCnt2)" ($limCnt2 -eq 0)

Write-Host '=== T5 重新抢票 + 支付 ===' -ForegroundColor Cyan
$c3 = Seckill $u1 1
Check 'T5.1 超时释放后可重新抢票' ($c3 -eq 200)
$r2 = PollResult $u1 1
Check "T5.2 再次抢票成功 (实际: $($r2.status))" ($r2.status -eq 'success')
$orderNo2 = $r2.orderNo
try {
    $payR = Invoke-RestMethod -Method Post -Uri "$base/api/order/$orderNo2/pay" -Headers $u1.hdr
    Check "T5.3 支付成功 (实际: $($payR.data.status))" ($payR.data.status -eq 1)
} catch {
    Check "T5.3 支付成功 (异常: $($_.ErrorDetails.Message))" $false
}
try {
    $payR2 = Invoke-RestMethod -Method Post -Uri "$base/api/order/$orderNo2/pay" -Headers $u1.hdr
    Check "T5.4 重复支付被拒 (实际: $($payR2.code))" ($payR2.code -eq 400)
} catch {
    $code = ($_.ErrorDetails.Message | ConvertFrom-Json).code
    Check "T5.4 重复支付被拒 (实际: $code)" ($code -eq 400)
}
$list = Invoke-RestMethod -Uri "$base/api/order/list" -Headers $u1.hdr
Check "T5.5 订单列表可见2笔订单 (实际: $($list.data.Count))" ($list.data.Count -eq 2)

Write-Host '=== T6 防刷限流（同账号 5次/10秒）===' -ForegroundColor Cyan
$u2 = NewUser 2
$codes = foreach ($i in 1..10) { Seckill $u2 3 }
$has429 = ($codes | Where-Object { $_ -eq 429 }).Count
Check "T6.1 第6次起触发限流429 (429次数: $has429)" ($has429 -ge 1)

Write-Host ''
Write-Host "========== 结果: $($script:pass) 通过 / $($script:fail) 失败 ==========" -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })

