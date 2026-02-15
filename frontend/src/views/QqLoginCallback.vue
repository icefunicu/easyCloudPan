<template>
  <div>登录中,请勿刷新页面</div>
</template>

<script setup>
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
  if (redirectUrl == '/login') {
    redirectUrl = '/'
  }
  userInfoStore.setUserInfo(data.userInfo)
  router.push(redirectUrl)
}

login()
</script>

<style lang="scss" scoped></style>
