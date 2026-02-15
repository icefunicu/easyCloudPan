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
    <pre class="hljs code-pre"><code v-html="highlightedHtml"></code></pre>
  </div>
</template>

<script setup>
import useClipboard from 'vue-clipboard3'
const { toClipboard } = useClipboard()

import { ref, getCurrentInstance, onMounted } from 'vue'
const { proxy } = getCurrentInstance()
import { fetchBlob } from '@/services'

const props = defineProps({
  url: {
    type: String,
  },
})

const txtContent = ref('')
const highlightedHtml = ref('')
const blobResult = ref()
const encode = ref('utf8')

let hljs = null
const ensureHljs = async () => {
  if (hljs) {
    return hljs
  }
  const mod = await import('highlight.js/lib/common')
  hljs = mod.default
  // Load CSS only when preview is opened.
  await import('highlight.js/styles/atom-one-light.css')
  return hljs
}

const escapeHtml = text => {
  return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const renderHighlight = async () => {
  const text = txtContent.value || ''
  // Avoid heavy highlight work for very large files.
  if (text.length > 200000) {
    highlightedHtml.value = escapeHtml(text)
    return
  }

  try {
    const hl = await ensureHljs()
    highlightedHtml.value = hl.highlightAuto(text).value
  } catch (e) {
    highlightedHtml.value = escapeHtml(text)
  }
}
const readTxt = async () => {
  const result = await fetchBlob(props.url)
  if (!result) {
    return
  }
  blobResult.value = result
  showTxt()
}

const changeEncode = e => {
  encode.value = e
  showTxt()
}

const showTxt = () => {
  const reader = new FileReader()
  reader.onload = () => {
    const txt = reader.result
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
    justify-content: space-around;
  }
  .encode-select {
    flex: 1;
    display: flex;
    align-items: center;
    margin: 5px 10px;
    .tips {
      margin-left: 10px;
      color: #828282;
    }
  }
  .copy-btn {
    margin-right: 10px;
  }
  pre {
    margin: 0px;
  }
  .code-pre {
    background: #fff;
    padding: 10px;
    border-radius: var(--border-radius-md);
    border: 1px solid var(--border-color);
    overflow: auto;
  }
}
</style>
