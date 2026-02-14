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
          <div class="logo">
              <span class="iconfont icon-pan"></span>
              <div class="name">Easy云盘</div>
          </div>
        </div>
        <div class="right-panel">
            <el-popover
              v-model:visible="showUploader"
              :width="800"
              trigger="click"
              :offset="20"
              transition="none"
              :hide-after="0"
              :popper-style="{ padding: '0px' }"
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
        <div :class="['left-sider', mobileMenuOpen ? 'open' : '']">
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
              <div class="space-info">
                <div>空间使用</div>
                <div class="percent">
                    <el-progress
                      :percentage="
                        Math.floor(
                            (useSpaceInfo.useSpace / useSpaceInfo.totalSpace) * 10000
                        ) / 100
                      "
                      color="#2563EB"
                    ></el-progress>
                </div>
                <div class="space-use">
                    <div class="use">
                        {{ proxy.Utils.size2Str(useSpaceInfo.useSpace) }} / {{
                           proxy.Utils.size2Str(useSpaceInfo.totalSpace)
                        }}
                    </div>
                    <div class="iconfont icon-refresh" @click="getUseSpace"></div>
                </div>
              </div>
            </div>
        </div>
        <div class="body-content">
            <router-view v-slot="{ Component }">
                <component
                  :is="Component"
                  ref="routerViewRef"
                  @add-file="addFile"
                  @reload="getUseSpace"
                ></component>
            </router-view>
        </div>
      </div>
      <!-- 修改头像 -->
      <UpdateAvatar
        ref="updateAvatarRef"
        @update-avatar="reloadAvatar"
      ></UpdateAvatar>
      <!-- 修改密码 -->
      <UpdatePassword ref="updatePasswordRef"></UpdatePassword>
  </div>
</template>


<script setup>
import Uploader from "@/views/main/Uploader.vue";
import UpdateAvatar from "./UpdateAvatar.vue";
import UpdatePassword from "./UpdatePassword.vue";

import { ref, getCurrentInstance, nextTick, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserInfoStore } from "@/stores/userInfoStore";
import * as accountService from "@/services/accountService";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();
const userInfoStore = useUserInfoStore();

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
      routerViewRef.value.reload();
      getUseSpace();
    });
};

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
const getUseSpace = async () => {
    const result = await accountService.getUseSpace();
    if (!result) {
        return;
    }
    useSpaceInfo.value = result;
};

getUseSpace();
</script>


<style lang="scss" scoped>
.framework {
    .mobile-mask {
        display: none;
    }

    .header {
        box-shadow: var(--shadow-sm);
        height: 56px;
        padding-left: 24px;
        padding-right: 24px;
        position: relative;
        z-index: 200;
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: #fff;

        .header-left {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .mobile-menu-btn {
            display: none;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: transparent;
            cursor: pointer;
            color: var(--text-secondary);
            transition: var(--transition-fast);

            &:hover {
                background: var(--bg-hover);
                color: var(--text-main);
            }

            .iconfont {
                font-size: 20px;
            }
        }

        .logo {
            display: flex;
            align-items: center;
            cursor: pointer;
            
            .icon-pan {
                font-size: 32px;
                color: var(--primary);
                transition: var(--transition-fast);
            }
            
            .name {
                font-weight: 700;
                margin-left: 8px;
                font-size: 20px;
                color: var(--text-main);
                letter-spacing: 0.5px;
                font-family: var(--font-heading);
            }
            
            &:hover {
                .icon-pan {
                    transform: scale(1.1);
                }
            }
        }
        
        .right-panel {
            display: flex;
            align-items: center;
            
            .uploader-trigger {
                position: relative;
                cursor: pointer;
                width: 40px;
                height: 40px;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 50%;
                transition: var(--transition-fast);
                
                &:hover {
                    background: var(--bg-hover);
                }
            }
            
            .icon-transfer {
                font-size: 20px;
                color: var(--text-secondary);
            }
            
            .count-tag {
                position: absolute;
                top: 0;
                right: 0;
                background: var(--danger);
                color: #fff;
                height: 16px;
                line-height: 16px;
                min-width: 16px;
                border-radius: 8px;
                text-align: center;
                font-size: 10px;
                padding: 0 4px;
                border: 2px solid #fff;
            }
            
            .user-info {
                margin-left: 15px;
                display: flex;
                align-items: center;
                cursor: pointer;
                padding: 4px 8px;
                border-radius: var(--border-radius-md);
                transition: var(--transition-fast);
                
                &:hover {
                    background: var(--bg-hover);
                }
                
                .avatar {
                    margin-right: 8px;
                }
                
                .nick-name {
                    color: var(--text-main);
                    font-size: 14px;
                    font-weight: 500;
                }
            }
        }
    }
    
    .body {
        display: flex;
        
        .left-sider {
            border-right: 1px solid var(--border-color);
            display: flex;
            background: #fff;
            
            .menu-list {
                height: calc(100vh - 56px);
                width: 80px;
                border-right: 1px solid var(--border-color);
                
                .menu-item {
                    text-align: center;
                    font-size: 12px;
                    font-weight: 500;
                    padding: 16px 0;
                    cursor: pointer;
                    color: var(--text-secondary);
                    transition: var(--transition-fast);
                    position: relative;
                    
                    &:hover {
                        background: var(--bg-hover);
                        color: var(--primary);
                    }
                    
                    .iconfont {
                        display: block;
                        font-weight: normal;
                        font-size: 24px;
                        margin-bottom: 4px;
                        transition: var(--transition-fast);
                    }
                }
                
                .active {
                    color: var(--primary);
                    background: #f0f5ff; // Very light primary bg
                    
                    .iconfont {
                        color: var(--primary);
                    }
                    
                    &::after {
                        content: '';
                        position: absolute;
                        left: 0;
                        top: 50%;
                        transform: translateY(-50%);
                        width: 3px;
                        height: 24px;
                        background: var(--primary);
                        border-top-right-radius: 3px;
                        border-bottom-right-radius: 3px;
                    }
                }
            }
            
            .menu-sub-list {
                width: 200px;
                padding: 16px 8px 0;
                position: relative;
                
                .menu-item-sub {
                    display: flex;
                    align-items: center;
                    padding: 0 16px;
                    height: 40px;
                    border-radius: var(--border-radius-md);
                    cursor: pointer;
                    color: var(--text-main);
                    transition: var(--transition-fast);
                    margin-bottom: 4px;
                    
                    &:hover {
                        background: var(--bg-hover);
                    }
                    
                    .iconfont {
                        font-size: 16px;
                        margin-right: 12px;
                        color: var(--text-secondary);
                    }
                    
                    .text {
                        font-size: 14px;
                    }
                }
                
                .active {
                    background: #eef9fe; // Light primary bg
                    color: var(--primary);
                    font-weight: 500;
                    
                    .iconfont {
                        color: var(--primary);
                    }
                }
                
                .tips {
                    margin-top: 10px;
                    color: var(--text-light);
                    font-size: 12px;
                    padding: 0 10px;
                    line-height: 1.5;
                }
                
                .space-info {
                    position: absolute;
                    bottom: 20px;
                    width: calc(100% - 16px);
                    padding: 12px;
                    background: var(--bg-body);
                    border-radius: var(--border-radius-md);
                    
                    .percent {
                        margin-top: 8px;
                    }
                    
                    .space-use {
                        margin-top: 8px;
                        color: var(--text-secondary);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        font-size: 12px;
                        
                        .iconfont {
                            cursor: pointer;
                            color: var(--primary);
                            font-size: 14px;
                            transition: var(--transition-fast);
                            
                            &:hover {
                                transform: rotate(180deg);
                            }
                        }
                    }
                }
            }
        }
        
        .body-content {
            flex: 1;
            width: 0;
            padding: 20px;
            background: var(--bg-body);
        }
    }

    @media screen and (max-width: 768px) {
        .mobile-mask {
            display: block;
            position: fixed;
            top: 56px;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.45);
            backdrop-filter: blur(2px);
            z-index: 300;
        }

        .header {
            padding-left: 12px;
            padding-right: 12px;

            .mobile-menu-btn {
                display: inline-flex;
                align-items: center;
                justify-content: center;
            }

            .logo {
                .name {
                    font-size: 18px;
                }
            }

            .right-panel {
                .user-info {
                    .nick-name {
                        display: none;
                    }
                }
            }
        }

        .body {
            .left-sider {
                position: fixed;
                top: 56px;
                left: 0;
                bottom: 0;
                z-index: 320;
                transform: translateX(-100%);
                transition: transform 0.2s ease;
                box-shadow: var(--shadow-lg);
                border-right: none;

                &.open {
                    transform: translateX(0);
                }
            }

            .body-content {
                padding: 12px;
            }
        }
    }
}
</style>
