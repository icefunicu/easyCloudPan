<template>
  <!-- eslint-disable-next-line vue/no-v-html -- Content sanitized with DOMPurify -->
  <div class="table-info" v-html="excelContent"></div>
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
const initExcel = async () => {
  if (!props.url) return
  const result = await fetchArrayBuffer(props.url)
  if (!result) {
    return
  }
  const workbook = XLSX.read(new Uint8Array(result), { type: 'array' })
  const worksheet = workbook.Sheets[workbook.SheetNames[0]]
  const rawHtml = XLSX.utils.sheet_to_html(worksheet)
  excelContent.value = DOMPurify.sanitize(rawHtml)
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
</style>
