<template>
  <div class="table-info">
    <div v-if="loading" class="excel-state">正在加载表格预览...</div>
    <div v-else-if="errorMsg" class="excel-state error">
      {{ errorMsg }}
      <button class="retry-btn" @click="initExcel">重试</button>
    </div>
    <!-- eslint-disable-next-line vue/no-v-html -- Content sanitized with DOMPurify -->
    <div v-else v-html="excelContent"></div>
  </div>
</template>

<script setup lang="ts">
import * as XLSX from 'xlsx'
import DOMPurify from 'dompurify'
import { ref, onMounted } from 'vue'
import { fetchArrayBuffer } from '@/services'

const props = defineProps({
  url: {
    type: String,
  },
})

const excelContent = ref('')
const loading = ref(false)
const errorMsg = ref('')

// T25: 统一错误边界 + 重试按钮
const initExcel = async () => {
  if (!props.url) {
    errorMsg.value = '文件地址无效'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const result = await fetchArrayBuffer(props.url)
    if (!result) {
      errorMsg.value = '表格加载失败，请稍后重试'
      return
    }
    const workbook = XLSX.read(new Uint8Array(result), { type: 'array' })
    const worksheet = workbook.Sheets[workbook.SheetNames[0]]
    const rawHtml = XLSX.utils.sheet_to_html(worksheet)
    excelContent.value = DOMPurify.sanitize(rawHtml)
  } catch (e) {
    console.error('表格预览失败:', e)
    errorMsg.value = '表格预览失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  initExcel()
})
</script>

<style lang="scss" scoped>
.table-info {
  width: 100%;
  padding: 10px;

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid rgba(189, 208, 202, 0.74);

    td {
      border: 1px solid rgba(189, 208, 202, 0.76);
      border-collapse: collapse;
      padding: 6px 8px;
      height: 30px;
      min-width: 50px;
    }
  }
}

.excel-state {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 14px;
  gap: 12px;
}

.excel-state.error {
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
