<template>
  <div class="framework">
    <FrameworkHeader
        ref="headerRef"
        v-model:mobile-menu-open="mobileMenuOpen"
    />
    <div
      v-if="mobileMenuOpen"
      class="mobile-mask"
      @click="mobileMenuOpen = false"
    ></div>
    <div class="body">
      <FrameworkSider
          v-model:mobile-menu-open="mobileMenuOpen"
      />
      <div class="body-content">
          <router-view v-slot="{ Component }">
              <transition name="view-flow" mode="out-in">
                  <keep-alive>
                      <component
                        :is="Component"
                        ref="routerViewRef"
                        :key="route.fullPath"
                        @add-file="addFile"
                        @reload="getUseSpace"
                      ></component>
                  </keep-alive>
              </transition>
          </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import FrameworkHeader from "@/components/Framework/FrameworkHeader.vue";
import FrameworkSider from "@/components/Framework/FrameworkSider.vue";

const route = useRoute();
const mobileMenuOpen = ref(false);

const headerRef = ref();
const routerViewRef = ref();

// 添加文件 (供 router-view 主页面抛出事件时调用)
const addFile = (data) => {
  headerRef.value?.addFile(data.file, data.filePid);
};

// 触发空间刷新 (供 router-view 局部刷新时联动上方饼图/容量条)
const getUseSpace = () => {
  headerRef.value?.getUseSpace();
};

</script>

<style lang="scss" scoped>
.framework {
    height: 100vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .mobile-mask {
        display: none;
    }

    .body {
        flex: 1;
        display: flex;
        height: calc(100vh - 56px); /* Header height assumed 56px */
        width: 100vw;
        overflow: hidden;

        .body-content {
            flex: 1;
            padding: 20px;
            background: var(--bg-body);
            overflow: auto;
        }
    }

    @media screen and (max-width: 768px) {
        .mobile-mask {
            display: block;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.4);
            z-index: 300;
        }

        .body {
            .body-content {
                padding: 12px;
            }
        }
    }
}

.view-flow-enter-active,
.view-flow-leave-active {
    transition: all 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.view-flow-enter-from {
    opacity: 0;
    transform: translateY(4px);
}

.view-flow-leave-to {
    opacity: 0;
    transform: translateY(-4px);
}
</style>
