<template>
    <div ref="docRef" class="doc-content"></div>
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
const initDoc = async () => {
    if (!props.url) return
    const result = await fetchBlob(props.url);
    if (!result || !docRef.value) {
        return;
    }
    docx.renderAsync(result, docRef.value);
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
</style>
