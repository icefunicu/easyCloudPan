import { defineStore } from 'pinia'
import type { SessionWebUserDto } from '@/types'

export type StoredUserInfo = (SessionWebUserDto & { token?: string; refreshToken?: string }) | null

export const useUserInfoStore = defineStore('userInfo', {
  state: () => ({
    userInfo: null as StoredUserInfo,
  }),
  actions: {
    setUserInfo(info: Exclude<StoredUserInfo, null>) {
      this.userInfo = info
    },
    clearUserInfo() {
      this.userInfo = null
    },
    getToken() {
      return this.userInfo?.token ?? null
    },
    getTenantId() {
      return this.userInfo?.tenantId ?? null
    },
  },
  persist: {
    storage: sessionStorage,
    pick: ['userInfo'],
  },
})
