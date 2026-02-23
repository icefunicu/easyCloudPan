<template>
  <el-config-provider :locale="locale" :message="config">
    <div class="app-shell">
      <router-view v-slot="{ Component, route }">
        <transition name="route-sculpt" mode="out-in">
          <component :is="Component" class="route-page" />
        </transition>
      </router-view>
    </div>
  </el-config-provider>
</template>

<script setup>
import { reactive } from "vue";
import zhCn from "element-plus/es/locale/lang/zh-cn";

const locale = zhCn;
const config = reactive({
  max: 1,
});
</script>

<style lang="scss" scoped>
.app-shell {
  position: relative;
  height: 100%;
  overflow: hidden;
  isolation: isolate;
}

.ambient {
  position: fixed;
  border-radius: 999px;
  filter: blur(80px);
  pointer-events: none;
  z-index: -1;
}

.ambient-a {
  width: 44vw;
  height: 44vw;
  top: -18vw;
  left: -10vw;
  background: rgba(43, 137, 169, 0.2);
  animation: drift-a 10s ease-in-out infinite;
}

.ambient-b {
  width: 38vw;
  height: 38vw;
  right: -8vw;
  bottom: -16vw;
  background: rgba(58, 115, 145, 0.16);
  animation: drift-b 12s ease-in-out infinite;
}

.route-page {
  height: 100%;
}

.route-sculpt-enter-active,
.route-sculpt-leave-active {
  transition: all 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.route-sculpt-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.995);
  filter: blur(2px);
}

.route-sculpt-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(1.003);
  filter: blur(1px);
}

@keyframes drift-a {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -10px, 0);
  }
}

@keyframes drift-b {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 10px, 0);
  }
}

@media screen and (max-width: 768px) {
  .ambient-a {
    width: 62vw;
    height: 62vw;
    top: -30vw;
    left: -22vw;
  }

  .ambient-b {
    width: 56vw;
    height: 56vw;
    right: -18vw;
    bottom: -24vw;
  }
}
</style>

