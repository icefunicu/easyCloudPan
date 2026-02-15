<template>
  <div class="table-info" v-html="excelContent"></div>
</template>

<script setup>
import * as XLSX from 'xlsx'
import DOMPurify from 'dompurify'
import { ref, onMounted } from 'vue'
import { fetchArrayBuffer } from '@/services'

const props = defineProps({
  url: {
    type: String,
  },
})

const excelContent = ref()
const initExcel = async () => {
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
    td {
      border: 1px solid #ddd;
      border-collapse: collapse;
      padding: 5px;
      height: 30px;
      min-width: 50px;
    }
  }
}
</style>
