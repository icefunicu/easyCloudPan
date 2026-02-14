import { defineStore } from 'pinia'
import VueCookies from 'vue-cookies'

export const useUserInfoStore = defineStore('userInfo', {
    state: () => ({
        // Provide a safe default shape to align with plan expectations
        userInfo: (VueCookies as any).get('userInfo') || ({ admin: false } as any),
    }),
    actions: {
        setUserInfo(info: any) {
            this.userInfo = info;
            (VueCookies as any).set('userInfo', info, 0);
        },
        clearUserInfo() {
            this.userInfo = null;
            (VueCookies as any).remove('userInfo');
        },
        getToken() {
            return this.userInfo ? this.userInfo.token : null;
        }
    },
})
