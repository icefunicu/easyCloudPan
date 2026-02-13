<template>
    <div>登录中,请勿刷新页面</div>
</template>

<script setup>
import { getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
import { useUserInfoStore } from "@/stores/userInfoStore";
const { proxy } = getCurrentInstance();
const router = useRouter();
const userInfoStore = useUserInfoStore();
const api = {
    logincallback: "/qqlogin/callback",
};

const login = async () => {
    const result = await proxy.Request({
        url: api.logincallback,
        params: router.currentRoute.value.query,
        errorCallback: () => {
            router.push("/");
        },
    });
    if (!result) {
        return;
    }
    let redirectUrl = result.data.errorCallback || "/";
    if (redirectUrl == "/login") {
        redirectUrl = "/";
    }
    userInfoStore.setUserInfo(result.data.userInfo);
    router.push(redirectUrl);
};

login();
</script>

<style lang="scss" scoped>
</style>
