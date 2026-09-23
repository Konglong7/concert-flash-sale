<template>
  <div class="page">
    <div class="page-head">
      <h1>我的订单</h1>
      <p class="sub">未支付订单 5 分钟后自动释放库存</p>
    </div>

    <div v-loading="loading" class="order-list">
      <article v-for="o in orders" :key="o.id" class="ticket order" :class="{ dimmed: o.status === 3 }">
        <div class="order-left">
          <div class="order-top">
            <span class="order-no display">NO.{{ o.orderNo }}</span>
            <span class="chip" :class="statusChip(o.status)">{{ statusText(o.status) }}</span>
          </div>
          <div class="order-info">
            下单 {{ fmt(o.createdAt) }} ｜ 支付截止 {{ fmt(o.expireAt) }}
          </div>
        </div>
        <div class="perf"></div>
        <div class="order-right">
          <span class="order-price display">¥{{ o.price }}</span>
          <button v-if="o.status === 0" class="grab-btn small" @click="pay(o)">立即支付</button>
        </div>
      </article>

      <div v-if="!loading && orders.length === 0" class="empty">
        <p>还没有订单</p>
        <router-link to="/concerts" class="empty-link">去抢一张 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const orders = ref([])
const loading = ref(false)

function fmt(s) { return s ? s.replace('T', ' ') : '-' }
function statusText(st) {
  return { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已超时' }[st] ?? '未知'
}
function statusChip(st) {
  return { 0: 'unpaid', 1: 'paid', 2: 'ended', 3: 'timeout' }[st] ?? 'ended'
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/order/list')
    orders.value = res.data
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function pay(o) {
  try {
    await ElMessageBox.confirm(`订单 ${o.orderNo}，金额 ¥${o.price}，确认支付？`, '模拟支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '再想想'
    })
  } catch { return }
  try {
    await api.post(`/order/${o.orderNo}/pay`)
    ElMessage.success('支付成功，ENJOY THE SHOW 🎉')
    load()
  } catch (e) {
    ElMessage.error(e.message)
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 32px; letter-spacing: 0.04em; }
.sub { color: var(--muted); margin: 6px 0 0; font-size: 14px; letter-spacing: 0.1em; }

.order-list {
  margin-top: 26px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.ticket.order { align-items: center; }
.ticket.order.dimmed { opacity: 0.55; }

.order-left { flex: 1; padding: 18px 26px; }
.order-top { display: flex; align-items: center; gap: 14px; }
.order-no { font-size: 20px; letter-spacing: 0.08em; }
.order-info { color: var(--muted); font-size: 13px; margin-top: 6px; letter-spacing: 0.03em; }

.order-right {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 18px 26px;
}
.order-price { font-size: 32px; color: var(--amber); }

.empty { text-align: center; padding: 60px 0; color: var(--muted); }
.empty p { font-size: 16px; margin: 0 0 10px; }
.empty-link { color: var(--magenta); font-weight: 700; text-decoration: none; }
.empty-link:hover { text-decoration: underline; }
</style>
