<template>
  <div v-loading="loading" class="page">
    <button class="back-link" @click="$router.push('/concerts')">← 返回演出列表</button>

    <!-- 演出主视觉 -->
    <div class="hero" :class="coverClass(concert.id)">
      <span class="hero-icon">{{ icon(concert.id) }}</span>
      <div class="hero-info">
        <h1>{{ concert.name }}</h1>
        <p>📍 {{ concert.venue }} ｜ {{ fmt(concert.showTime) }} 开演</p>
      </div>
    </div>

    <!-- LED 倒计时牌 -->
    <div v-if="concert.status !== 2" class="led">
      <span class="led-label">{{ concert.status === 1 ? '距停售' : '距开抢' }}</span>
      <div class="led-cells display">
        <div class="cell"><b>{{ pad(d) }}</b><i>天</i></div>
        <div class="cell"><b>{{ pad(h) }}</b><i>时</i></div>
        <div class="cell"><b>{{ pad(m) }}</b><i>分</i></div>
        <div class="cell"><b>{{ pad(s) }}</b><i>秒</i></div>
      </div>
    </div>
    <div v-else class="led ended-strip">本场售票已结束</div>

    <!-- 票档：演唱会门票票根 -->
    <h3 class="tier-title">选择票档</h3>
    <div class="tier-list">
      <article v-for="t in concert.tiers || []" :key="t.id" class="ticket tier">
        <div class="tier-left">
          <span class="tier-name">{{ t.name }}</span>
          <span class="tier-price display">¥{{ t.price }}</span>
        </div>
        <div class="perf"></div>
        <div class="tier-right">
          <div class="tier-meta">
            <span>剩余 <b class="display stock-num">{{ t.stock }}</b> / {{ t.totalStock }} 张</span>
            <span class="dim">限购 {{ t.limitPerUser }} 张/人</span>
          </div>
          <button
            class="grab-btn"
            :disabled="concert.status !== 1 || t.stock <= 0 || grabbing"
            @click="seckill(t)"
          >
            {{ t.stock <= 0 ? '已售罄' : '立即抢票' }}
          </button>
        </div>
      </article>
    </div>

    <!-- 抢票结果 -->
    <el-dialog v-model="dialogVisible" width="420px" align-center :close-on-click-modal="false">
      <template #header><b>抢票结果</b></template>
      <div class="result-box">
        <template v-if="result.status === 'processing'">
          <div class="spinner"></div>
          <p class="result-msg">排队处理中，请稍候…</p>
        </template>
        <template v-else-if="result.status === 'success'">
          <div class="result-icon ok">✓</div>
          <p class="result-title">抢票成功！</p>
          <p class="result-sub">订单号 <b class="display">{{ result.orderNo }}</b></p>
          <button class="grab-btn" @click="goPay">去支付</button>
        </template>
        <template v-else>
          <div class="result-icon no">✕</div>
          <p class="result-title">抢票失败</p>
          <p class="result-sub">{{ result.message }}</p>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const concert = ref({})
const loading = ref(false)
const grabbing = ref(false)
const dialogVisible = ref(false)
const result = reactive({ status: 'processing', orderNo: null, message: null })
let pollTimer = null
let failTimer = null
let countdownSeconds = 0

const ICONS = ['🎤', '🎸', '🎹', '🎺']
const COVERS = ['cg0', 'cg1', 'cg2', 'cg3']
function coverClass(id) { return COVERS[id % COVERS.length] }
function icon(id) { return ICONS[id % ICONS.length] }

const d = computed(() => Math.floor(countdownSeconds / 86400))
const h = computed(() => Math.floor((countdownSeconds % 86400) / 3600))
const m = computed(() => Math.floor((countdownSeconds % 3600) / 60))
const s = computed(() => countdownSeconds % 60)
function pad(n) { return String(n).padStart(2, '0') }

function fmt(t) { return t ? t.replace('T', ' ') : '-' }

async function load() {
  loading.value = true
  try {
    const res = await api.get(`/concert/${route.params.id}`)
    concert.value = res.data
    countdownSeconds = res.data.countdownSeconds || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function seckill(tier) {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  grabbing.value = true
  result.status = 'processing'
  dialogVisible.value = true
  try {
    await api.post('/order/seckill', {
      concertId: concert.value.id,
      tierId: tier.id,
      idCard: '110101199001011234'
    })
    // 轮询抢票结果
    pollTimer = setInterval(async () => {
      try {
        const r = await api.get(`/order/seckill/result/${tier.id}`)
        if (r.data.status !== 'processing') {
          clearInterval(pollTimer)
          Object.assign(result, r.data)
          if (r.data.status === 'success') load()
        }
      } catch { /* 轮询失败忽略，下轮重试 */ }
    }, 800)
    failTimer = setTimeout(() => {
      if (result.status === 'processing') {
        clearInterval(pollTimer)
        result.status = 'fail'
        result.message = '处理超时，请稍后在订单页查看'
      }
    }, 15000)
  } catch (e) {
    dialogVisible.value = false
    ElMessage.error(e.message)
  } finally {
    grabbing.value = false
  }
}

function goPay() {
  dialogVisible.value = false
  router.push('/orders')
}

onMounted(() => {
  load()
  setInterval(() => { if (countdownSeconds > 0) countdownSeconds-- }, 1000)
})
onUnmounted(() => { clearInterval(pollTimer); clearTimeout(failTimer) })
</script>

<style scoped>
.back-link {
  background: none;
  border: none;
  color: var(--muted);
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
  margin-bottom: 16px;
}
.back-link:hover { color: var(--text); }

/* 演出主视觉 */
.hero {
  position: relative;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 34px 36px;
  border-radius: 20px;
  overflow: hidden;
}
.hero.cg0 { background: linear-gradient(120deg, #ff3e8f, #7c5cff); }
.hero.cg1 { background: linear-gradient(120deg, #ff7a3e, #ff3e8f); }
.hero.cg2 { background: linear-gradient(120deg, #3ec6ff, #7c5cff); }
.hero.cg3 { background: linear-gradient(120deg, #ffb53e, #ff3e8f); }
.hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, transparent 35%, rgba(255, 255, 255, 0.16) 50%, transparent 65%);
}
.hero-icon { font-size: 64px; filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.35)); }
.hero-info { position: relative; z-index: 1; }
.hero-info h1 { margin: 0; font-size: 34px; color: #fff; text-shadow: 0 3px 12px rgba(0, 0, 0, 0.3); }
.hero-info p { margin: 10px 0 0; color: rgba(255, 255, 255, 0.92); font-weight: 600; letter-spacing: 0.06em; }

/* LED 倒计时牌 */
.led {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 20px;
  padding: 18px 26px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
}
.led-label {
  color: var(--amber);
  font-weight: 700;
  letter-spacing: 0.3em;
  font-size: 15px;
}
.led-cells { display: flex; gap: 10px; }
.cell {
  min-width: 62px;
  text-align: center;
  padding: 8px 6px 6px;
  border-radius: 10px;
  background: #0a0817;
  border: 1px solid rgba(255, 181, 62, 0.25);
}
.cell b {
  display: block;
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 400;
  line-height: 1;
  color: var(--amber);
  text-shadow: 0 0 14px rgba(255, 181, 62, 0.55);
}
.cell i { font-style: normal; font-size: 12px; color: var(--muted); }
.ended-strip {
  margin-top: 20px;
  padding: 16px 26px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--muted);
  text-align: center;
  letter-spacing: 0.3em;
}

/* 票档票根 */
.tier-title { margin: 32px 0 14px; font-size: 20px; }
.tier-list { display: flex; flex-direction: column; gap: 14px; }

.ticket.tier { align-items: center; }
.tier-left {
  flex: 1;
  padding: 20px 26px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tier-name { color: var(--muted); font-weight: 600; letter-spacing: 0.1em; font-size: 14px; }
.tier-price { font-size: 38px; line-height: 1; color: var(--text); }

.tier-right {
  flex: 1.2;
  padding: 20px 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}
.tier-meta { display: flex; flex-direction: column; gap: 4px; font-size: 14px; color: var(--text); }
.stock-num { color: var(--amber); font-size: 18px; }
.dim { color: var(--muted); font-size: 13px; }

/* 结果弹窗 */
.result-box { text-align: center; padding: 8px 0 4px; }
.result-msg { color: var(--muted); margin-top: 14px; }
.result-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
}
.result-icon.ok { color: var(--green); border: 2px solid rgba(62, 232, 165, 0.5); background: rgba(62, 232, 165, 0.08); }
.result-icon.no { color: var(--red); border: 2px solid rgba(255, 107, 107, 0.5); background: rgba(255, 107, 107, 0.08); }
.result-title { font-size: 20px; font-weight: 700; margin: 16px 0 4px; }
.result-sub { color: var(--muted); margin: 0 0 18px; }
.result-sub .display { font-size: 18px; color: var(--text); letter-spacing: 0.05em; }
</style>
