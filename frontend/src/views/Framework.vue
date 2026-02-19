<template>
    <div class="framework">
      <div class="header">
        <div class="header-left">
          <button
            class="mobile-menu-btn"
            type="button"
            aria-label="打开菜单"
            @click="mobileMenuOpen = true"
          >
            <span class="iconfont icon-more"></span>
          </button>
          <div class="logo" @click="goHome">
              <span class="iconfont icon-pan"></span>
              <div class="name">易云盘</div>
          </div>
        </div>
        <div class="right-panel">
            <div :class="['space-chip', `state-${useSpaceState}`, spaceChipUpdating ? 'updating' : '']">
              <div class="space-main">
                <span class="label">空间占用</span>
                <span class="value">{{ useSpacePercent }}%</span>
                <button
                  type="button"
                  class="refresh-space"
                  :aria-busy="refreshingUseSpace"
                  aria-label="刷新空间状态"
                  @click="getUseSpace"
                >
                  <span :class="['iconfont', 'icon-refresh', refreshingUseSpace ? 'spin' : '']"></span>
                </button>
              </div>
              <div class="space-sub">
                <span class="used-total">
                  已用 {{ proxy.Utils.size2Str(useSpaceInfo.useSpace) }} / 总量 {{
                    proxy.Utils.size2Str(useSpaceInfo.totalSpace)
                  }}
                </span>
                <span class="remain">剩余 {{ remainSpaceLabel }} · {{ remainSpacePercent }}%</span>
              </div>
              <div class="space-meter">
                <span :class="['fill', `fill-${useSpaceState}`]" :style="{ width: `${useSpacePercent}%` }"></span>
              </div>
            </div>
            <el-popover
              v-model:visible="showUploader"
              :width="uploaderPopoverWidth"
              trigger="click"
              :offset="isMobileDevice ? 10 : 20"
              transition="none"
              :hide-after="0"
              :popper-style="{ padding: '0px' }"
              :placement="isMobileDevice ? 'bottom-start' : 'bottom'"
              :teleported="true"
            >
            <template #reference>
              <div class="uploader-trigger">
                <span class="iconfont icon-transfer"></span>
                <span class="count-tag" v-if="activeTaskCount > 0">{{ activeTaskCount }}</span>
              </div>
            </template>
            <template #default>
              <Uploader
                ref="uploaderRef"
                @upload-callback="uploadCallbackHandler"
                @update:activeTaskCount="updateActiveTaskCount"
            ></Uploader>
            </template>
          </el-popover>

          <el-dropdown>
            <div class="user-info">
                <div class="avatar">
                    <Avatar
                      :user-id="userInfo.userId"
                      :avatar="userInfo.avatar"
                      :timestamp="timestamp"
                      :width="46"
                      ></Avatar>
                </div>
                <span class="nick-name">{{ userInfo.nickName }}</span>
            </div>
            <template #dropdown>
                <el-dropdown-menu>
                    <el-dropdown-item @click="updateAvatar"
                    >修改头像</el-dropdown-item
                    >
                    <el-dropdown-item @click="updateNickName"
                    >修改昵称</el-dropdown-item
                    >
                    <el-dropdown-item @click="updatePassword"
                    >修改密码</el-dropdown-item
                    >
                    <el-dropdown-item @click="logout">退出</el-dropdown-item>
                </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div
        v-if="mobileMenuOpen"
        class="mobile-mask"
        @click="mobileMenuOpen = false"
      ></div>
      <div class="body">
        <div 
            :class="['left-sider', mobileMenuOpen ? 'open' : '']"
            v-touch:swipe.left="() => mobileMenuOpen = false"
        >
            <div class="menu-list">
                <template v-for="item in menus" :key="item.menuCode">
                <div
                v-if="item.allShow || (!item.allShow && userInfo.admin)"
                :class="[
                    'menu-item',
                    item.menuCode == currentMenu.menuCode ? 'active' : '',
                    ]"
                @click="jump(item)"
                >
                    <div :class="['iconfont', 'icon-' + item.icon]"></div>
                    <div class="text">{{ item.name }}</div>
                </div>
            </template>
              </div>
            <div class="menu-sub-list">
                <div
                  v-for="sub in currentMenu.children"
                  :key="sub.path"
                  :class="['menu-item-sub', currentPath == sub.path ? 'active' : '']"
                  @click="jump(sub)"
                >
                  <span
                  v-if="sub.icon"
                  :class="['iconfont', 'icon-' + sub.icon]"
                  ></span>
                  <span class="text">{{ sub.name }}</span>
              </div>
              <div v-if="currentMenu && currentMenu.tips" class="tips">
                {{ currentMenu.tips }}
              </div>
            </div>
        </div>
        <div class="body-content">
            <router-view v-slot="{ Component }">
                <transition name="view-flow" mode="out-in">
                    <component
                      :is="Component"
                      ref="routerViewRef"
                      :key="route.fullPath"
                      @add-file="addFile"
                      @reload="getUseSpace"
                    ></component>
                </transition>
            </router-view>
        </div>
      </div>
      <!-- 修改头像 -->
      <UpdateAvatar
        ref="updateAvatarRef"
        @update-avatar="reloadAvatar"
      ></UpdateAvatar>
      <!-- 修改昵称 -->
      <UpdateNickName
        ref="updateNickNameRef"
        @update-nick-name="reloadNickName"
      ></UpdateNickName>
      <!-- 修改密码 -->
      <UpdatePassword ref="updatePasswordRef"></UpdatePassword>
  </div>
</template>

<script setup>
import Uploader from "@/views/main/Uploader.vue";
import UpdateAvatar from "./UpdateAvatar.vue";
import UpdateNickName from "./UpdateNickName.vue";
import UpdatePassword from "./UpdatePassword.vue";

import EventBus from "@/utils/EventBus";
import { ref, getCurrentInstance, nextTick, watch, onMounted, onUnmounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserInfoStore } from "@/stores/userInfoStore";
import * as accountService from "@/services/accountService";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const userInfoStore = useUserInfoStore();

const goHome = () => {
  router.push("/");
};

//显示上传窗口
const showUploader = ref(false);

// 添加文件
const uploaderRef = ref();
const addFile = (data) => {
  const { file, filePid } = data;
  showUploader.value = true;
  uploaderRef.value.addFile(file, filePid);
};

// 上传文件回调
const routerViewRef = ref();
const uploadCallbackHandler = () => {
    nextTick(() => {
      // routerViewRef.value.reload(); // Removed to avoid double refresh, handled by EventBus in Main.vue
      // getUseSpace(); // Handled by EventBus listener
    });
};

// EventBus Listener
const handleGlobalReload = () => {
    getUseSpace();
};

onMounted(() => {
    EventBus.on('reload_data', handleGlobalReload);
});

onUnmounted(() => {
    EventBus.off('reload_data', handleGlobalReload);
    stopUseSpaceAutoRefresh();
    if (spaceChipTimer) {
        clearTimeout(spaceChipTimer);
    }
});

const activeTaskCount = ref(0);
const updateActiveTaskCount = (count) => {
  activeTaskCount.value = count;
};
const timestamp = ref(0);
//获取用户信息
const userInfo = ref(userInfoStore.userInfo);

const menus = [
    {
        icon: "cloude",
        name: "首页",
        menuCode: "main",
        path: "/main/all",
        allShow: true,
        children: [
            {
                icon: "all",
                name: "全部",
                category: "all",
                path: "/main/all",
            },
            {
                icon: "video",
                name: "视频",
                category: "video",
                path: "/main/video",
            },
            {
                icon: "music",
                name: "音频",
                category: "music",
                path: "/main/music",
            },
            {
                icon: "image",
                name: "图片",
                category: "image",
                path: "/main/image",
            },
            {
                icon: "doc",
                name: "文档",
                category: "doc",
                path: "/main/doc",
            },
            {
                icon: "more",
                name: "其他",
                category: "others",
                path: "/main/others",
            },
        ],
    },
    {
        path: "/myshare",
        icon: "share",
        name: "分享",
        menuCode: "share",
        allShow: true,
        children: [
            {
                name: "分享记录",
                path: "/myshare",
            },
        ],
    },
    {
        path: "/recycle",
        icon: "del",
        name: "回收站",
        menuCode: "recycle",
        tips: "回收站为你保存10天内删除的文件",
        allShow: true,
        children: [
            {
                name: "删除的文件",
                path: "/recycle",
            },
        ],
    },
    {
        path: "/settings/fileList",
        icon: "settings",
        name: "设置",
        menuCode: "settings",
        allShow: false,
        children: [
            {
                name: "用户文件",
                path: "/settings/fileList",
            },
            {
                name: "用户管理",
                path: "/settings/userList",
            },
            {
                name: "系统设置",
                path: "/settings/sysSetting",
            },
        ],
    },
];

const currentMenu = ref({});
const currentPath = ref();
const mobileMenuOpen = ref(false);
const jump = (data) => {
    if (!data.path) {
        return;
    }
    if (data.menuCode && data.menuCode == currentMenu.value.menuCode) {
        mobileMenuOpen.value = false;
        return;
    }
    router.push(data.path);
    mobileMenuOpen.value = false;
};

const setMenu = (menuCode, path) => {
    const menu = menus.find((item) => {
        return item.menuCode === menuCode;
    });
    currentMenu.value = menu;
    currentPath.value = path;
};
watch (
    () => route,
    (newVal, oldVal) => {
        if (newVal.meta.menuCode) {
            setMenu(newVal.meta.menuCode, newVal.path);
        }
        mobileMenuOpen.value = false;
    },
    { immediate: true, deep: true }
);

// 修改头像
const updateAvatarRef = ref();
const updateAvatar = () => {
    updateAvatarRef.value.show(userInfo.value);
};

const reloadAvatar = () => {
    userInfo.value = userInfoStore.userInfo;
    timestamp.value = new Date().getTime();
};

// 修改昵称
const updateNickNameRef = ref();
const updateNickName = () => {
    updateNickNameRef.value.show(userInfo.value);
};

const reloadNickName = () => {
    userInfo.value = userInfoStore.userInfo;
};

// 修改密码
const updatePasswordRef = ref();
const updatePassword = () => {
    updatePasswordRef.value.show();
};

// 退出
const logout = async () => {
    proxy.Confirm(`你确定要退出吗`, async () => {
        const result = await accountService.logout();
        if (!result){
            return;
        }
        userInfoStore.clearUserInfo();
        router.push("/login");
    });
};

// 使用空间
const useSpaceInfo = ref({ useSpace: 0, totalSpace: 1 });
const spaceChipUpdating = ref(false);
const useSpacePercent = computed(() => {
    const total = Number(useSpaceInfo.value.totalSpace || 1);
    const used = Number(useSpaceInfo.value.useSpace || 0);
    return Math.min(100, Math.max(0, Math.round((used / total) * 10000) / 100));
});
const remainSpaceLabel = computed(() => {
    const total = Number(useSpaceInfo.value.totalSpace || 0);
    const used = Number(useSpaceInfo.value.useSpace || 0);
    return proxy.Utils.size2Str(Math.max(0, total - used));
});
const remainSpacePercent = computed(() => {
    const total = Number(useSpaceInfo.value.totalSpace || 0);
    const used = Number(useSpaceInfo.value.useSpace || 0);
    if (total <= 0) {
        return 0;
    }
    const remain = Math.max(0, total - used);
    return Math.max(0, Math.min(100, Math.round((remain / total) * 10000) / 100));
});
const useSpaceState = computed(() => {
    const percent = useSpacePercent.value;
    if (percent >= 90) {
        return "danger";
    }
    if (percent >= 75) {
        return "warning";
    }
    return "safe";
});
const refreshingUseSpace = ref(false);
let spaceChipTimer = null;
const markSpaceUpdated = () => {
    spaceChipUpdating.value = true;
    if (spaceChipTimer) {
        clearTimeout(spaceChipTimer);
    }
    spaceChipTimer = setTimeout(() => {
        spaceChipUpdating.value = false;
    }, 420);
};
const getUseSpace = async () => {
    if (refreshingUseSpace.value) {
        return;
    }
    refreshingUseSpace.value = true;
    const result = await accountService.getUseSpace();
    refreshingUseSpace.value = false;
    if (!result) {
        return;
    }
    if (result.useSpace !== useSpaceInfo.value.useSpace || result.totalSpace !== useSpaceInfo.value.totalSpace) {
        markSpaceUpdated();
    }
    useSpaceInfo.value = result;
};

let useSpaceAutoRefreshTimer = null;
const stopUseSpaceAutoRefresh = () => {
    if (!useSpaceAutoRefreshTimer) {
        return;
    }
    clearInterval(useSpaceAutoRefreshTimer);
    useSpaceAutoRefreshTimer = null;
};
const startUseSpaceAutoRefresh = () => {
    if (useSpaceAutoRefreshTimer) {
        return;
    }
    useSpaceAutoRefreshTimer = setInterval(() => {
        getUseSpace();
    }, 2000);
};

watch(activeTaskCount, (count) => {
    if (count > 0) {
        startUseSpaceAutoRefresh();
        getUseSpace();
        return;
    }
    stopUseSpaceAutoRefresh();
    getUseSpace();
});

// Mobile detection
const windowWidth = ref(window.innerWidth);
const isMobileDevice = computed(() => windowWidth.value < 768);
const uploaderPopoverWidth = computed(() => {
    if (isMobileDevice.value) {
        return Math.min(windowWidth.value - 24, 360);
    }
    return Math.min(800, windowWidth.value - 100);
});

const updateWindowWidth = () => {
    windowWidth.value = window.innerWidth;
};

onMounted(() => {
    window.addEventListener('resize', updateWindowWidth);
});

onUnmounted(() => {
    window.removeEventListener('resize', updateWindowWidth);
});

getUseSpace();
</script>


<style lang="scss" scoped>
.framework {
    height: 100vh;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 14px 16px 16px;
    overflow: hidden;

    .mobile-mask {
        display: none;
    }

    .header {
        min-height: 68px;
        padding: 0 20px;
        position: relative;
        z-index: 220;
        display: flex;
        align-items: center;
        justify-content: space-between;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(192, 200, 208, 0.85);
        box-shadow: var(--shadow-sm);
        backdrop-filter: blur(10px);
        animation: riseIn 0.32s cubic-bezier(0.22, 1, 0.36, 1);

        .header-left {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .mobile-menu-btn {
            display: none;
            width: 42px;
            height: 42px;
            border-radius: 14px;
            background: rgba(255, 255, 255, 0.76);
            border: 1px solid var(--border-color);
            cursor: pointer;
            color: var(--text-secondary);
            transition: var(--transition-fast);

            &:hover {
                color: var(--text-main);
                border-color: var(--primary-light);
                box-shadow: 0 8px 16px rgba(31, 79, 104, 0.16);
            }

            .iconfont {
                font-size: 20px;
            }
        }

        .logo {
            display: flex;
            align-items: center;
            cursor: pointer;
            gap: 10px;

            .icon-pan {
                font-size: 24px;
                width: 38px;
                height: 38px;
                border-radius: 12px;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #fff;
                background: var(--primary);
                box-shadow: 0 8px 16px rgba(31, 79, 104, 0.26);
                transition: var(--transition-normal);
            }

            .name {
                font-size: 19px;
                font-weight: 700;
                letter-spacing: 0.03em;
                color: var(--text-main);
                font-family: var(--font-heading);
            }

            &:hover .icon-pan {
                transform: translateY(-0.5px);
            }
        }

        .right-panel {
            display: flex;
            align-items: center;
            gap: 10px;

            .space-chip {
                width: clamp(260px, 34vw, 460px);
                min-height: 58px;
                padding: 8px 12px 7px;
                display: inline-flex;
                flex-direction: column;
                justify-content: center;
                gap: 4px;
                border-radius: 14px;
                background: rgba(36, 95, 124, 0.08);
                border: 1px solid rgba(31, 79, 104, 0.24);
                color: var(--text-secondary);
                transition: box-shadow 0.24s ease, border-color 0.24s ease, transform 0.24s ease, background 0.24s ease;

                &.updating {
                    border-color: rgba(31, 79, 104, 0.5);
                    box-shadow: 0 0 0 2px rgba(31, 79, 104, 0.12);
                    transform: translateY(-0.5px);
                }

                &.state-safe {
                    background: rgba(36, 95, 124, 0.08);
                    border-color: rgba(31, 79, 104, 0.24);
                }

                &.state-warning {
                    background: rgba(154, 118, 83, 0.12);
                    border-color: rgba(154, 118, 83, 0.3);
                }

                &.state-danger {
                    background: rgba(178, 81, 81, 0.12);
                    border-color: rgba(178, 81, 81, 0.32);
                }

                .space-main {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }

                .label {
                    font-size: 11px;
                    letter-spacing: 0.03em;
                    opacity: 0.8;
                }

                .value {
                    font-size: 14px;
                    font-weight: 700;
                    color: var(--text-main);
                    margin-right: auto;
                }

                .refresh-space {
                    width: 24px;
                    height: 24px;
                    border-radius: 8px;
                    background: rgba(255, 255, 255, 0.72);
                    border: 1px solid rgba(31, 79, 104, 0.2);
                    color: var(--primary);
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    transition: var(--transition-fast);

                    .iconfont {
                        font-size: 13px;
                    }

                    &:hover {
                        border-color: rgba(31, 79, 104, 0.48);
                        color: var(--primary-dark);
                        background: rgba(255, 255, 255, 0.95);
                    }

                    .spin {
                        animation: rotateSpaceRefresh 0.8s linear infinite;
                    }
                }

                .space-sub {
                    display: flex;
                    flex-direction: column;
                    align-items: flex-start;
                    gap: 2px;
                    font-size: 11px;
                    color: var(--text-light);

                    .used-total,
                    .remain {
                        line-height: 1.25;
                        max-width: 100%;
                        white-space: normal;
                        word-break: break-word;
                    }
                }

                .space-meter {
                    width: 100%;
                    height: 5px;
                    border-radius: 999px;
                    background: rgba(122, 147, 163, 0.28);
                    overflow: hidden;

                    .fill {
                        display: block;
                        height: 100%;
                        border-radius: 999px;
                        transition: width 0.34s cubic-bezier(0.22, 1, 0.36, 1), background 0.2s ease;
                    }

                    .fill-safe {
                        background: linear-gradient(90deg, #2f6d8c 0%, #1f4f68 100%);
                    }

                    .fill-warning {
                        background: linear-gradient(90deg, #2f6d8c 0%, #9a7653 100%);
                    }

                    .fill-danger {
                        background: linear-gradient(90deg, #bf6666 0%, #b25151 100%);
                    }
                }
            }

            .uploader-trigger {
                position: relative;
                cursor: pointer;
                width: 42px;
                height: 42px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 14px;
                background: rgba(255, 255, 255, 0.8);
                border: 1px solid var(--border-color);
                transition: var(--transition-fast);

                &:hover {
                    border-color: var(--primary-light);
                    box-shadow: 0 8px 16px rgba(31, 79, 104, 0.16);
                    transform: translateY(-0.5px);
                }
            }

            .icon-transfer {
                font-size: 20px;
                color: var(--text-secondary);
            }

            .count-tag {
                position: absolute;
                top: -3px;
                right: -3px;
                min-width: 18px;
                height: 18px;
                line-height: 18px;
                padding: 0 5px;
                border-radius: 9px;
                text-align: center;
                font-size: 11px;
                font-weight: 700;
                color: #fff;
                background: var(--primary);
                box-shadow: 0 8px 14px rgba(31, 79, 104, 0.24);
            }

            .user-info {
                display: flex;
                align-items: center;
                gap: 8px;
                cursor: pointer;
                padding: 4px 8px 4px 4px;
                border-radius: 999px;
                border: 1px solid transparent;
                transition: var(--transition-fast);

                &:hover {
                    border-color: rgba(31, 79, 104, 0.26);
                    background: rgba(255, 255, 255, 0.7);
                }

                .avatar {
                    display: flex;
                }

                .nick-name {
                    max-width: 120px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    font-size: 13px;
                    font-weight: 600;
                    color: var(--text-main);
                }
            }
        }
    }

    .body {
        flex: 1;
        min-height: 0;
        display: flex;
        gap: 12px;
        overflow: hidden;

        .left-sider {
            width: 320px;
            display: flex;
            height: 100%;
            border-radius: 18px;
            border: 1px solid rgba(194, 204, 220, 0.88);
            background: rgba(255, 255, 255, 0.92);
            box-shadow: var(--shadow-sm);
            overflow: hidden;
            animation: riseIn 0.34s cubic-bezier(0.22, 1, 0.36, 1);

            .menu-list {
                width: 92px;
                height: 100%;
                padding: 10px 8px;
                border-right: 1px solid rgba(194, 204, 220, 0.86);
                background: rgba(36, 95, 124, 0.06);

                .menu-item {
                    text-align: center;
                    font-size: 11px;
                    font-weight: 600;
                    padding: 14px 0 12px;
                    margin-bottom: 6px;
                    border-radius: 14px;
                    cursor: pointer;
                    color: var(--text-secondary);
                    transition: var(--transition-fast);
                    position: relative;

                    .iconfont {
                        display: block;
                        font-size: 22px;
                        margin-bottom: 5px;
                        transition: var(--transition-fast);
                    }

                    &:hover {
                        color: var(--text-main);
                        background: rgba(255, 255, 255, 0.62);
                    }
                }

                .active {
                    color: #fff;
                    background: var(--primary);
                    box-shadow: 0 10px 20px rgba(31, 79, 104, 0.24);

                    .iconfont {
                        color: #fff;
                        transform: translateY(-0.5px);
                    }

                    &::after {
                        content: "";
                        position: absolute;
                        left: -5px;
                        top: 50%;
                        transform: translateY(-50%);
                        width: 4px;
                        height: 24px;
                        border-radius: 999px;
                        background: var(--primary-light);
                    }
                }
            }

            .menu-sub-list {
                width: 228px;
                height: 100%;
                padding: 16px 10px 12px;
                position: relative;

                .menu-item-sub {
                    height: 42px;
                    padding: 0 14px;
                    display: flex;
                    align-items: center;
                    border-radius: 12px;
                    margin-bottom: 6px;
                    cursor: pointer;
                    color: var(--text-main);
                    transition: var(--transition-fast);

                    .iconfont {
                        font-size: 16px;
                        margin-right: 10px;
                        color: var(--text-secondary);
                    }

                    .text {
                        font-size: 13px;
                        font-weight: 500;
                    }

                    &:hover {
                        background: rgba(36, 95, 124, 0.1);
                    }
                }

                .active {
                    background: rgba(36, 95, 124, 0.14);
                    color: var(--primary-dark);
                    font-weight: 600;

                    .iconfont {
                        color: var(--primary);
                    }
                }

                .tips {
                    margin-top: 8px;
                    padding: 0 8px;
                    font-size: 12px;
                    line-height: 1.45;
                    color: var(--text-light);
                }
            }
        }

        .body-content {
            flex: 1;
            min-width: 0;
            height: 100%;
            padding: 18px;
            border-radius: 18px;
            border: 1px solid rgba(194, 204, 220, 0.7);
            background: rgba(255, 255, 255, 0.8);
            box-shadow: var(--shadow-sm);
            backdrop-filter: blur(6px);
            overflow: auto;
            animation: riseIn 0.34s cubic-bezier(0.22, 1, 0.36, 1);
        }
    }

    @media screen and (max-width: 768px) {
        padding: 0;
        gap: 0;

        .mobile-mask {
            display: block;
            position: fixed;
            inset: 0;
            background: rgba(16, 29, 40, 0.34);
            backdrop-filter: blur(3px);
            z-index: 300;
        }

        .header {
            min-height: 62px;
            border-radius: 0;
            border-left: none;
            border-right: none;
            padding: 0 12px;

            .mobile-menu-btn {
                display: inline-flex;
                align-items: center;
                justify-content: center;
            }

            .space-chip {
                width: min(56vw, 220px);
                min-height: 52px;
                padding: 6px 8px;

                .label {
                    display: none;
                }

                .value {
                    font-size: 13px;
                }

                .refresh-space {
                    width: 22px;
                    height: 22px;
                }

                .space-sub {
                    font-size: 10px;
                    gap: 1px;
                }
            }

            .logo {
                .icon-pan {
                    width: 34px;
                    height: 34px;
                    font-size: 22px;
                }

                .name {
                    font-size: 16px;
                }
            }

            .right-panel {
                gap: 8px;

                .user-info .nick-name {
                    display: none;
                }
            }
        }

        .body {
            gap: 0;

            .left-sider {
                position: fixed;
                top: 62px;
                left: 0;
                bottom: 0;
                width: min(88vw, 320px);
                z-index: 320;
                border-radius: 0 18px 18px 0;
                border-left: none;
                transform: translateX(-100%);
                transition: transform 0.24s ease;

                &.open {
                    transform: translateX(0);
                }
            }

            .body-content {
                border-radius: 0;
                border-left: none;
                border-right: none;
                padding: 12px;
            }
        }
    }
}

@keyframes rotateSpaceRefresh {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
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

