<template>
    <div class="doc-content">
        <div v-if="loading" class="doc-state">正在加载文档预览...</div>
        <div v-else-if="errorMsg" class="doc-state error">
            {{ errorMsg }}
            <button class="retry-btn" @click="initDoc">重试</button>
        </div>
        <div v-else ref="docRef"></div>
    </div>
</template>

<script setup lang="ts">
import * as docx from "docx-preview";
import { ref, onMounted } from "vue";
import { fetchBlob } from "@/services";

const props = defineProps({
    url: {
        type: String,
    },
});

const docRef = ref<HTMLElement | null>(null);
const loading = ref(false);
const errorMsg = ref("");

// T25: 统一错误边界 + 重试按钮
const initDoc = async () => {
    if (!props.url) {
        errorMsg.value = "文件地址无效";
        return;
    }
    loading.value = true;
    errorMsg.value = "";
    try {
        const result = await fetchBlob(props.url);
        if (!result || !docRef.value) {
            errorMsg.value = "文档加载失败，请稍后重试";
            return;
        }
        await docx.renderAsync(result, docRef.value);
    } catch (e) {
        console.error("文档预览失败:", e);
        errorMsg.value = "文档预览失败，请稍后重试";
    } finally {
        loading.value = false;
    }
};
onMounted(() => {
    initDoc();
});
</script>

<style lang="scss" scoped>
.doc-content {
    margin: 0 auto;
    padding: 10px;

    :deep(.docx-wrapper) {
        background: rgba(255, 255, 255, 0.92);
        border: 1px solid rgba(189, 208, 202, 0.76);
        border-radius: 14px;
        box-shadow: var(--shadow-sm);
        padding: 12px 0;
    }

    :deep(.docx-wrapper > section.docx) {
        margin-bottom: 0;
    }
}

.doc-state {
    min-height: 200px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    font-size: 14px;
    gap: 12px;
}

.doc-state.error {
    color: var(--danger);
}

.retry-btn {
    padding: 6px 16px;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--bg-secondary, #f5f5f5);
    color: var(--text-primary);
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
}

.retry-btn:hover {
    background: var(--primary-light, #e3f2fd);
    border-color: var(--primary, #409eff);
}
</style>
