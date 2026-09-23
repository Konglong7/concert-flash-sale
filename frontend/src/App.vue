<template>
  <div class="app-shell">
    <header class="topbar">
      <router-link to="/concerts" class="logo">
        <span class="logo-en display">ENCORE</span>
        <span class="logo-cn">演唱会抢票</span>
      </router-link>
      <nav class="nav">
        <router-link to="/concerts" class="nav-link">演出</router-link>
        <router-link to="/orders" class="nav-link">我的订单</router-link>
      </nav>
      <div class="user-area">
        <template v-if="username">
          <span class="username">👤 {{ username }}</span>
          <button class="link-btn" @click="logout">退出</button>
        </template>
        <router-link v-else to="/login" class="login-link">登录 / 注册</router-link>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref(localStorage.getItem('username'))

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  username.value = null
  router.push('/concerts')
}
</script>

<style scoped>
.app-shell { min-height: 100vh; display: flex; flex-direction: column; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 0 40px;
  height: 64px;
  border-bottom: 1px solid var(--line);
  background: rgba(13, 11, 26, 0.75);
  backdrop-filter: blur(12px);
}

.logo { display: flex; align-items: baseline; gap: 10px; text-decoration: none; }
.logo-en {
  font-size: 28px;
  line-height: 1;
  background: linear-gradient(135deg, var(--magenta), var(--violet));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.logo-cn { font-size: 13px; color: var(--muted); letter-spacing: 0.3em; }

.nav { display: flex; gap: 8px; flex: 1; }
.nav-link {
  color: var(--muted);
  text-decoration: none;
  font-weight: 600;
  font-size: 15px;
  padding: 6px 16px;
  border-radius: 999px;
  transition: color 0.2s, background 0.2s;
}
.nav-link:hover { color: var(--text); }
.nav-link.router-link-active { color: var(--text); background: rgba(255, 62, 143, 0.14); }

.user-area { display: flex; align-items: center; gap: 12px; white-space: nowrap; }
.username { color: var(--text); font-weight: 600; }
.link-btn {
  background: none;
  border: none;
  color: var(--red);
  font-family: inherit;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}
.login-link {
  color: var(--text);
  text-decoration: none;
  font-weight: 700;
  padding: 8px 20px;
  border-radius: 999px;
  border: 1px solid rgba(255, 62, 143, 0.5);
  transition: background 0.2s;
}
.login-link:hover { background: rgba(255, 62, 143, 0.14); }

.main { flex: 1; width: 100%; max-width: 1100px; margin: 0 auto; padding: 32px 40px 80px; }

@media (max-width: 640px) {
  .topbar { padding: 0 20px; gap: 16px; }
  .logo-cn { display: none; }
  .main { padding: 20px 20px 60px; }
}
</style>
