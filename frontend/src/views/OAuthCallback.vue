<template>
  <div class="callback-container">
    <div class="loading-content">
      <div v-if="loading">正在登录中，请稍候...</div>
      <div v-else class="error-msg">
        {{ errorMsg }}
        <div class="op-btn">
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfoStore'
import { oauthCallback } from '@/services'
const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()

const loading = ref(true)
const errorMsg = ref('')

const goHome = () => {
  router.push('/login')
}

const login = async () => {
  const provider = route.params.provider
  if (!provider) {
    loading.value = false
    errorMsg.value = '登录参数错误'
    return
  }

  try {
    const data = await oauthCallback(String(provider), route.query)
    if (!data) {
      loading.value = false
      errorMsg.value = '登录请求失败'
      return
    }

    if (data.status === 'need_register') {
      // 新用户，跳转到注册页面设置密码
      router.push({
        path: '/oauth/register',
        query: {
          registerKey: data.registerKey,
          email: data.email || '',
          nickname: data.nickname || '',
          avatarUrl: data.avatarUrl || '',
          provider: data.provider || provider,
        },
      })
    } else {
      // 已有用户，直接登录
      userInfoStore.setUserInfo({
        ...data.userInfo,
        token: data.token,
        refreshToken: data.refreshToken,
      })
      let callbackUrl = data.callbackUrl || '/'
      if (callbackUrl.indexOf('/oauth/callback') !== -1 || callbackUrl.indexOf('/login') !== -1) {
        callbackUrl = '/'
      }
      router.push(callbackUrl)
    }
  } catch (error) {
    loading.value = false
    errorMsg.value = error.message || '登录发生异常'
  }
}

onMounted(() => {
  login()
})
</script>

<style lang="scss" scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  .loading-content {
    text-align: center;
    font-size: 18px;
    .error-msg {
      color: red;
      .op-btn {
        margin-top: 20px;
      }
    }
  }
}
</style>
