<template>
  <div class="page">
    <div class="page-head">
      <h1>正在热售</h1>
      <p class="sub">开票瞬间，手慢无</p>
    </div>

    <div v-loading="loading" class="show-grid">
      <article v-for="c in concerts" :key="c.id" class="show-card" tabindex="0" @click="go(c)" @keyup.enter="go(c)">
        <div class="cover" :class="coverClass(c.id)">
          <span class="cover-icon">{{ icon(c.id) }}</span>
          <div class="cover-date display">
            <b>{{ day(c.showTime) }}</b>
            <small>{{ month(c.showTime) }}</small>
          </div>
        </div>
        <div class="show-body">
          <h3 class="show-name">{{ c.name }}</h3>
          <p class="show-venue">📍 {{ c.venue }}</p>
          <div class="show-foot">
            <span class="chip" :class="chipClass(c.status)">{{ statusText(c.status) }}</span>
            <span v-if="c.status !== 2" class="countdown display" :title="c.status === 1 ? '距停售' : '距开抢'">
              {{ cdText(c.countdownSeconds) }}
            </span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const concerts = ref([])
const loading = ref(false)
let tickTimer = null
let refreshTimer = null

const ICONS = ['🎤', '🎸', '🎹', '🎺']
const COVERS = ['g0', 'g1', 'g2', 'g3']

function coverClass(id) { return COVERS[id % COVERS.length] }
function icon(id) { return ICONS[id % ICONS.length] }

function day(s) { return s ? s.slice(8, 10) : '--' }
function month(s) {
  if (!s) return ''
  const m = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
  return m[Number(s.slice(5, 7)) - 1] || ''
}
function fmt(s) { return s ? s.replace('T', ' ') : '-' }

function statusText(st) {
  return st === 1 ? '🔥 销售中' : st === 0 ? '⏰ 未开售' : '已结束'
}
function chipClass(st) {
  return st === 1 ? 'live' : st === 0 ? 'soon' : 'ended'
}
function cdText(sec) {
  const s = Math.max(0, sec || 0)
  const d = Math.floor(s / 86400)
  const h = String(Math.floor((s % 86400) / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  const clock = `${h}:${m}:${ss}`
  return d > 0 ? `${d}天 ${clock}` : clock
}

function go(c) {
  router.push(`/concerts/${c.id}`)
}

async function load() {
  loading.value = concerts.value.length === 0
  try {
    const res = await api.get('/concert/list')
    concerts.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  // 本地秒级倒计时 + 每 10 秒与服务端校准一次
  tickTimer = setInterval(() => {
    concerts.value.forEach(c => {
      if (c.countdownSeconds > 0) c.countdownSeconds--
    })
  }, 1000)
  refreshTimer = setInterval(load, 10000)
})
onUnmounted(() => { clearInterval(tickTimer); clearInterval(refreshTimer) })
</script>

<style scoped>
.page-head h1 {
  margin: 0;
  font-size: 32px;
  letter-spacing: 0.04em;
}
.sub { color: var(--muted); margin: 6px 0 0; letter-spacing: 0.2em; font-size: 14px; }

.show-grid {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  min-height: 200px;
}

.show-card {
  border-radius: 18px;
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.3s ease;
}
.show-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 62, 143, 0.4);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
}

/* 舞台灯光海报封面 */
.cover {
  position: relative;
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cover.g0 { background: linear-gradient(135deg, #ff3e8f, #7c5cff); }
.cover.g1 { background: linear-gradient(135deg, #ff7a3e, #ff3e8f); }
.cover.g2 { background: linear-gradient(135deg, #3ec6ff, #7c5cff); }
.cover.g3 { background: linear-gradient(135deg, #ffb53e, #ff3e8f); }
.cover::after {
  /* 灯光扫过质感 */
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, transparent 30%, rgba(255, 255, 255, 0.18) 45%, transparent 60%);
}
.cover-icon { font-size: 54px; filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.35)); }
.cover-date {
  position: absolute;
  left: 16px;
  bottom: 10px;
  color: #fff;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}
.cover-date b { font-size: 40px; display: block; }
.cover-date small { font-size: 15px; letter-spacing: 0.2em; }

.show-body { padding: 16px 18px 18px; }
.show-name { margin: 0; font-size: 19px; }
.show-venue { color: var(--muted); margin: 8px 0 14px; font-size: 14px; }

.show-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.countdown {
  color: var(--amber);
  font-size: 18px;
  letter-spacing: 0.06em;
}
</style>
