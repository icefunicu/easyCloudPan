<template>
  <div class="pdf">
    <div v-if="loading" class="pdf-state">正在加载 PDF 预览...</div>
    <div v-else-if="errorMsg" class="pdf-state error">{{ errorMsg }}</div>
    <iframe
      v-else
      :src="pdfBlobUrl"
      class="pdf-frame"
      title="PDF 预览"
    ></iframe>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";

const props = defineProps({
  url: {
    type: String,
    default: "",
  },
});

const pdfBlobUrl = ref("");
const loading = ref(false);
const errorMsg = ref("");
let fetchController: AbortController | null = null;

const initPdf = async () => {
  if (!props.url) {
    errorMsg.value = "文件地址无效";
    return;
  }
  if (fetchController) {
    fetchController.abort();
  }
  fetchController = new AbortController();
  loading.value = true;
  errorMsg.value = "";
  const response = await fetch(`/api${props.url}`, {
    method: "GET",
    credentials: "include",
    signal: fetchController.signal,
  }).catch(() => null);
  if (fetchController.signal.aborted) {
    return;
  }
  if (!response || !response.ok) {
    loading.value = false;
    errorMsg.value = "PDF 加载失败，请稍后重试";
    return;
  }
  const blob = await response.blob();
  loading.value = false;
  const objectUrl = URL.createObjectURL(blob);
  pdfBlobUrl.value = objectUrl;
};

onMounted(() => {
  initPdf();
});

onUnmounted(() => {
  if (fetchController) {
    fetchController.abort();
  }
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value);
  }
});
</script>

<style lang="scss" scoped>
.pdf {
  width: 100%;
  min-height: 480px;
  padding: 10px;
  border-radius: 14px;
  border: 1px solid rgba(195, 209, 219, 0.82);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
}

.pdf-frame {
  width: 100%;
  height: min(78vh, 960px);
  border: none;
  border-radius: 10px;
  background: #f8fafb;
}

.pdf-state {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.pdf-state.error {
  color: var(--danger);
}
</style>
