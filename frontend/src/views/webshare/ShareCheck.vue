<template>
    <div class="share">
        <div class="body-content">
            <div class="logo">
                <span class="iconfont icon-pan"></span>
                <span class="name">Easy云盘</span>
            </div>
            <div class="code-panel">
                <div class="file-info">
                    <div class="avatar">
                        <Avatar
                          :user-id="shareInfo.userId"
                          :avatar="shareInfo.avatar"
                          :width="50"
                        ></Avatar>
                    </div>
                    <div class="share-info">
                        <div class="user-info">
                            <span class="nick-name">{{ shareInfo.nickName }}</span>
                            <span class="share-time">分享于 {{ shareInfo.shareTime }}</span>
                        </div>
                        <div class="file-name">分享文件：{{ shareInfo.fileName }}</div>
                    </div>
                </div>
                <div class="code-body">
                    <div class="tips">请输入提取码: </div>
                    <div class="input-area">
                        <el-form
                          ref="formDataRef"
                          :model="formData"
                          :rules="rules"
                          @submit.prevent
                        >
                          <!-- 输入框 -->
                          <el-form-item prop="code">
                            <el-input
                              v-model.trim="formData.code"
                              class="input"
                              clearable
                              @keyup.enter="checkShare"
                            ></el-input>
                            <el-button class="get-btn" type="primary" @click="checkShare"
                              >提取文件</el-button
                            >
                          </el-form-item>
                        </el-form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import * as shareService from "@/services/shareService";
const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();


const shareId = route.params.shareId;
const shareInfo = ref({});
const getShareInfo = async () => {
    const result = await shareService.getShareInfo(shareId);
    if (!result) {
        return;
    }
    shareInfo.value = result;
};
getShareInfo();

const formData = ref({});
const formDataRef = ref();
const rules = {
    code: [
        { required: true, message: "请输入提取码" },
        { min: 5, message: "提取码为5位" },
        { max: 5, message: "提取码为5位" },
    ],
};

const checkShare = async () => {
    formDataRef.value.validate(async (valid) => {
        if (!valid) {
            return;
        }
        const params = {};
        Object.assign(params, formData.value);
        const result = await shareService.checkShareCode({
            shareId: shareId,
            code: formData.value.code,
        });
        if (!result) {
            return;
        }
        router.push(`/share/${shareId}`);
    });
};
</script>

<style lang="scss" scoped>
.share {
    min-height: 100vh;
    background-color: var(--bg-body);
    background-image:
      radial-gradient(1200px 300px at 50% 0%, rgba(37, 99, 235, 0.16), transparent 60%),
      url("../../assets/share_bg.png");
    background-repeat: no-repeat, repeat-x;
    background-position: 0 0, 0 bottom;
    display: flex;
    justify-content: center;
    .body-content {
        margin-top: 12vh;
        width: min(520px, calc(100% - 32px));
        .logo {
            display: flex;
            align-items: center;
            justify-content: center;
            user-select: none;
            .icon-pan {
                font-size: 60px;
                color: var(--primary);
            }
            .name {
                font-weight: bold;
                margin-left: 5px;
                font-size: 25px;
                color: var(--text-main);
            }
        }
        .code-panel {
            margin-top: 20px;
            background: var(--bg-card);
            border-radius: var(--border-radius-lg);
            overflow: hidden;
            border: 1px solid var(--border-color);
            box-shadow: var(--shadow-md);
            .file-info {
                padding: 10px 20px;
                background: linear-gradient(90deg, rgba(37, 99, 235, 0.12), rgba(37, 99, 235, 0.06));
                color: var(--text-main);
                display: flex;
                align-items: center;
                .avatar {
                    margin-right: 5px;
                }
                .share-info {
                    .user-info {
                        display: flex;
                        align-items: center;
                        flex-wrap: wrap;
                        .nick-name {
                            font-size: 15px;
                            color: var(--text-main);
                            font-weight: 600;
                        }
                        .share-time {
                            margin-left: 20px;
                            font-size: 12px;
                            color: var(--text-light);
                        }
                    }
                    .file-name {
                        color: var(--text-secondary);
                        margin-top: 10px;
                        font-size: 12px;
                    }
                }
            }
            .code-body {
                padding: 30px 20px 60px 20px;
                .tips {
                    font-weight: bold;
                    color: var(--text-main);
                }
                .input-area {
                    margin-top: 15px;
                    .input {
                        flex: 1;
                        margin-right: 10px;
                    }
                }
            }
        }
    }
}

@media screen and (max-width: 768px) {
    .share .body-content {
        margin-top: 10vh;
    }
    .share .body-content .code-panel .file-info .share-info .user-info .share-time {
        margin-left: 0;
        width: 100%;
        margin-top: 4px;
    }
}
</style>
