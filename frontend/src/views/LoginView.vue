<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-head">
        <span class="display auth-logo">ENCORE</span>
        <p class="auth-slogan">{{ isRegister ? '创建账号，开票瞬间快人一步' : '欢迎回来，去看一场现场' }}</p>
      </div>

      <div class="tab-switch" role="tablist">
        <button :class="{ on: !isRegister }" @click="isRegister = false">登录</button>
        <button :class="{ on: isRegister }" @click="isRegister = true">注册</button>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="3-20 位字母 / 数字 / 下划线" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" size="large" />
        </el-form-item>
        <template v-if="isRegister">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="选填" size="large" />
          </el-form-item>
          <el-form-item label="身份证">
            <el-input v-model="form.idCard" placeholder="选填" size="large" />
          </el-form-item>
        </template>
      </el-form>

      <button class="grab-btn auth-submit" :disabled="loading" @click="submit">
        {{ loading ? '请稍候…' : (isRegister ? '创建账号' : '登录') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const isRegister = ref(false)
const loading = ref(false)
const form = reactive({ username: '', password: '', phone: '', idCard: '' })

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    if (isRegister.value) {
      await api.post('/auth/register', form)
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
    } else {
      const res = await api.post('/auth/login', { username: form.username, password: form.password })
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.username)
      ElMessage.success('登录成功')
      router.push('/concerts')
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 36px 36px;
  border-radius: 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.45), 0 0 60px rgba(124, 92, 255, 0.08);
}

.auth-head { text-align: center; margin-bottom: 24px; }
.auth-logo {
  font-size: 44px;
  line-height: 1;
  background: linear-gradient(135deg, var(--magenta), var(--violet));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  display: block;
}
.auth-slogan { color: var(--muted); margin: 10px 0 0; font-size: 14px; letter-spacing: 0.08em; }

.tab-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 5px;
  border-radius: 12px;
  background: var(--surface-2);
  margin-bottom: 22px;
}
.tab-switch button {
  border: none;
  background: transparent;
  color: var(--muted);
  font-family: inherit;
  font-weight: 700;
  font-size: 15px;
  padding: 9px;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.2s;
}
.tab-switch button.on {
  color: #fff;
  background: linear-gradient(135deg, var(--magenta), var(--violet));
}

.auth-submit { width: 100%; margin-top: 6px; }
</style>
