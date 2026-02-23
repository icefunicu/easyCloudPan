<template>
  <div class="code">
    <div class="top-op">
      <div class="encode-select">
        <!-- 下拉框 -->
        <el-select v-model="encode" clearable placeholder="选择编码" @change="changeEncode">
          <el-option value="utf8" label="utf8编码"></el-option>
          <el-option value="gbk" label="gbk编码"></el-option>
        </el-select>
        <div class="tips">乱码了? 切换编码</div>
      </div>
      <div class="copy-btn">
        <el-button type="primary" @click="copy">复制</el-button>
      </div>
    </div>
    <!-- eslint-disable-next-line vue/no-v-html -- Content is escaped or highlighted, safe -->
    <pre class="hljs code-pre"><code v-html="highlightedHtml"></code></pre>
  </div>
</template>

<script setup lang="ts">
import useClipboard from 'vue-clipboard3'
const { toClipboard } = useClipboard()

import { ref, getCurrentInstance, onMounted } from 'vue'
const instance = getCurrentInstance()
const proxy = instance!.proxy!
import { fetchBlob } from '@/services'

const props = defineProps({
  url: {
    type: String,
  },
})

const txtContent = ref('')
const highlightedHtml = ref('')
const blobResult = ref<Blob | null>(null)
const encode = ref('utf8')

interface HljsType {
  highlightAuto(text: string): { value: string }
}

let hljs: HljsType | null = null
const ensureHljs = async (): Promise<HljsType> => {
  if (hljs) {
    return hljs
  }
  const mod = await import('highlight.js/lib/common')
  hljs = mod.default as HljsType
  // 仅在打开预览时加载 CSS.
  await import('highlight.js/styles/atom-one-light.css')
  return hljs
}

const escapeHtml = (text: string): string => {
  return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const renderHighlight = async () => {
  const text = txtContent.value || ''
  // 超大文件跳过高亮，避免主线程阻塞.
  if (text.length > 200000) {
    highlightedHtml.value = escapeHtml(text)
    return
  }

  try {
    const hl = await ensureHljs()
    highlightedHtml.value = hl.highlightAuto(text).value
  } catch {
    highlightedHtml.value = escapeHtml(text)
  }
}
const readTxt = async () => {
  if (!props.url) return
  const result = await fetchBlob(props.url)
  if (!result) {
    return
  }
  blobResult.value = result
  showTxt()
}

const changeEncode = (e: string) => {
  encode.value = e
  showTxt()
}

const showTxt = () => {
  if (!blobResult.value) return
  const reader = new FileReader()
  reader.onload = () => {
    const txt = reader.result as string
    txtContent.value = txt
    renderHighlight()
  }
  reader.readAsText(blobResult.value, encode.value)
}

onMounted(() => {
  readTxt()
})

const copy = async () => {
  await toClipboard(txtContent.value)
  proxy.Message.success('复制成功')
}
</script>

<style lang="scss" scoped>
.code {
  width: 100%;

  .top-op {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
  }

  .encode-select {
    flex: 1;
    display: flex;
    align-items: center;
    margin: 0;

    .tips {
      margin-left: 10px;
      color: var(--text-light);
      font-size: 12px;
    }
  }

  .copy-btn {
    margin-right: 0;
  }

  pre {
    margin: 0;
  }

  .code-pre {
    background: rgba(255, 255, 255, 0.9);
    padding: 12px;
    border-radius: var(--border-radius-md);
    border: 1px solid rgba(189, 208, 202, 0.78);
    box-shadow: var(--shadow-xs);
    overflow: auto;
  }
}
</style>
