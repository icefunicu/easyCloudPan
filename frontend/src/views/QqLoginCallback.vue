<template>
  <div class="qq-callback">
    <div class="status-card">
      <span class="iconfont icon-pan"></span>
      <span class="loading-spinner"></span>
      <div class="text">QQ 登录处理中，请勿刷新页面...</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfoStore'
import { qqLoginCallback } from '@/services'

const router = useRouter()
const userInfoStore = useUserInfoStore()

const login = async () => {
  const data = await qqLoginCallback(router.currentRoute.value.query)
  if (!data) {
    router.push('/')
    return
  }

  let redirectUrl = data.redirectUrl || '/'
  if (redirectUrl === '/login') {
    redirectUrl = '/'
  }
  userInfoStore.setUserInfo(data.userInfo)
  router.push(redirectUrl)
}

onMounted(() => {
  login()
})
</script>

<style lang="scss" scoped>
.qq-callback {
  min-height: 100vh;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(560px 360px at 0% 0%, rgba(47, 109, 140, 0.2), rgba(47, 109, 140, 0)),
    linear-gradient(180deg, #f5f8fa 0%, #eef3f6 100%);
}

.status-card {
  width: min(420px, 100%);
  padding: 26px;
  border-radius: 18px;
  border: 1px solid rgba(194, 204, 220, 0.82);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(8px);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.icon-pan {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: #fff;
  background: var(--primary);
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(31, 79, 104, 0.2);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.text {
  color: var(--text-secondary);
}

@keyframes spin {
  from {
    transform: rotate(0);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

