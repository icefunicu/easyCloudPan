<template>
  <PreviewImage v-if="fileInfo.fileCategory == 3" ref="imageViewRef" :image-list="[imageUrl]"></PreviewImage>
  <Window
    v-else
    :show="windowShow"
    :width="fileInfo.fileCategory == 1 ? 1000 : 900"
    :title="fileInfo.fileName"
    :align="fileInfo.fileCategory == 1 ? 'center' : 'top'"
    @close="closeWindow"
  >
    <div class="preview-shell">
      <div class="preview-toolbar">
        <div class="meta">
          <span class="type">{{ previewTypeLabel }}</span>
          <span v-if="fileInfo.fileSize">大小 {{ proxy.Utils.size2Str(fileInfo.fileSize) }}</span>
          <span v-if="fileInfo.lastUpdateTime">更新于 {{ fileInfo.lastUpdateTime }}</span>
        </div>
        <div class="actions">
          <el-button v-if="canOpenInNewTab" size="small" @click="openInNewTab">新窗口打开</el-button>
          <el-button v-if="canDownload" size="small" type="primary" @click="downloadCurrent">下载</el-button>
        </div>
      </div>
      <div v-if="previewLoading" class="preview-loading">
        <span class="loading-dot"></span>
        正在加载预览...
      </div>
      <template v-else>
        <PreviewVideo v-if="fileInfo.fileCategory == 1" :url="url"></PreviewVideo>
        <PreviewDoc v-if="fileInfo.fileType == 5" :url="url"></PreviewDoc>
        <PreviewExcel v-if="fileInfo.fileType == 6" :url="url"></PreviewExcel>
        <PreviewPdf v-if="fileInfo.fileType == 4" :url="url"></PreviewPdf>
        <PreviewTxt v-if="fileInfo.fileType == 7 || fileInfo.fileType == 8" :url="url"></PreviewTxt>
        <PreviewMusic v-if="fileInfo.fileCategory == 2" :url="url" :file-name="fileInfo.fileName"></PreviewMusic>

        <PreviewDownload
          v-if="fileInfo.fileCategory == 5 && fileInfo.fileType != 8"
          :create-download-url="createDownloadUrl"
          :download-url="downloadUrl"
          :file-info="fileInfo"
        ></PreviewDownload>
      </template>
    </div>
  </Window>
</template>

<script setup>
import { computed, defineAsyncComponent, getCurrentInstance, h, nextTick, onUnmounted, ref } from 'vue'
import { createDownloadCode } from '@/services'
import { resolveDownloadTarget } from '@/utils/url'
import PreviewImage from './PreviewImage.vue'

const AsyncLoading = {
  render() {
    return h(
      'div',
      {
        style: 'display:flex;align-items:center;justify-content:center;flex:1;color:var(--text-secondary);gap:8px;',
      },
      [
        h('span', {
          class: 'loading-dot',
          style:
            'width:8px;height:8px;border-radius:50%;background:var(--primary);animation:previewDot 0.9s ease-in-out infinite;',
        }),
        '加载组件中...',
      ]
    )
  },
}

const AsyncError = {
  render() {
    return h(
      'div',
      {
        style:
          'display:flex;flex-direction:column;align-items:center;justify-content:center;flex:1;color:var(--text-secondary);gap:12px;',
      },
      [h('span', { style: 'font-size:24px;' }, '!'), '组件加载失败，请刷新页面重试']
    )
  },
}

const asyncComponentOptions = {
  loadingComponent: AsyncLoading,
  errorComponent: AsyncError,
  delay: 200,
  timeout: 30000,
}

const PreviewVideo = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewVideo.vue'),
  ...asyncComponentOptions,
})
const PreviewDoc = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewDoc.vue'),
  ...asyncComponentOptions,
})
const PreviewExcel = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewExcel.vue'),
  ...asyncComponentOptions,
})
const PreviewPdf = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewPdf.vue'),
  ...asyncComponentOptions,
})
const PreviewTxt = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewTxt.vue'),
  ...asyncComponentOptions,
})
const PreviewMusic = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewMusic.vue'),
  ...asyncComponentOptions,
})
const PreviewDownload = defineAsyncComponent({
  loader: () => import('@/components/preview/PreviewDownload.vue'),
  ...asyncComponentOptions,
})

const { proxy } = getCurrentInstance()

const fileInfo = ref({})
const imageViewRef = ref()
const windowShow = ref(false)
const previewLoading = ref(false)
let previewLoadingTimer = null

const FILE_URL_MAP = {
  0: {
    fileUrl: '/file/getFile',
    videoUrl: '/file/ts/getVideoInfo',
    createDownloadUrl: '/file/createDownloadUrl',
    downloadUrl: '/api/file/download',
  },
  1: {
    fileUrl: '/admin/getFile',
    videoUrl: '/admin/ts/getVideoInfo',
    createDownloadUrl: '/admin/createDownloadUrl',
    downloadUrl: '/api/admin/download',
  },
  2: {
    fileUrl: '/showShare/getFile',
    videoUrl: '/showShare/ts/getVideoInfo',
    createDownloadUrl: '/showShare/createDownloadUrl',
    downloadUrl: '/api/showShare/download',
  },
}

const url = ref(null)
const createDownloadUrl = ref(null)
const downloadUrl = ref(null)

const imageUrl = computed(() => {
  const cover = fileInfo.value.fileCover
  if (!cover) {
    return ''
  }
  return proxy.globalInfo.imageUrl + cover.replaceAll('_.', '.')
})

const canOpenInNewTab = computed(() => {
  return !!url.value && fileInfo.value.fileCategory !== 2
})

const canDownload = computed(() => {
  return !!createDownloadUrl.value && !!downloadUrl.value && fileInfo.value.folderType === 0
})

const previewTypeLabel = computed(() => {
  if (fileInfo.value.folderType === 1) {
    return '文件夹'
  }
  if (fileInfo.value.fileCategory == 1) {
    return '视频预览'
  }
  if (fileInfo.value.fileCategory == 2) {
    return '音频预览'
  }
  if (fileInfo.value.fileCategory == 3) {
    return '图片预览'
  }
  if (fileInfo.value.fileType == 4) {
    return 'PDF 文档'
  }
  if (fileInfo.value.fileType == 5) {
    return 'Word 文档'
  }
  if (fileInfo.value.fileType == 6) {
    return 'Excel 表格'
  }
  if (fileInfo.value.fileType == 7 || fileInfo.value.fileType == 8) {
    return '文本预览'
  }
  return '文件预览'
})

const closeWindow = () => {
  windowShow.value = false
  previewLoading.value = false
  if (previewLoadingTimer) {
    clearTimeout(previewLoadingTimer)
  }
}

const openInNewTab = () => {
  if (!fileInfo.value.fileId) {
    return
  }
  window.open(`/preview/${fileInfo.value.fileId}`, '_blank')
}

const downloadCurrent = async () => {
  if (!canDownload.value) {
    return
  }
  const codeOrUrl = await createDownloadCode(createDownloadUrl.value)
  if (!codeOrUrl) {
    return
  }
  const target = resolveDownloadTarget(downloadUrl.value, codeOrUrl)
  if (!target) {
    return
  }
  window.location.href = target
}

const showPreview = (data, showPart) => {
  previewLoading.value = true
  if (previewLoadingTimer) {
    clearTimeout(previewLoadingTimer)
  }
  previewLoadingTimer = setTimeout(() => {
    previewLoading.value = false
  }, 260)

  fileInfo.value = data
  if (data.fileCategory == 3) {
    nextTick(() => {
      imageViewRef.value.show(0)
    })
    return
  }

  windowShow.value = true
  let currentUrl = FILE_URL_MAP[showPart].fileUrl
  let currentCreateDownloadUrl = FILE_URL_MAP[showPart].createDownloadUrl
  const currentDownloadUrl = FILE_URL_MAP[showPart].downloadUrl

  if (data.fileCategory == 1) {
    currentUrl = FILE_URL_MAP[showPart].videoUrl
  }

  if (showPart == 0) {
    currentUrl = `${currentUrl}/${data.fileId}`
    currentCreateDownloadUrl = `${currentCreateDownloadUrl}/${data.fileId}`
  } else if (showPart == 1) {
    currentUrl = `${currentUrl}/${data.userId}/${data.fileId}`
    currentCreateDownloadUrl = `${currentCreateDownloadUrl}/${data.userId}/${data.fileId}`
  } else if (showPart == 2) {
    currentUrl = `${currentUrl}/${data.shareId}/${data.fileId}`
    currentCreateDownloadUrl = `${currentCreateDownloadUrl}/${data.shareId}/${data.fileId}`
  }

  url.value = currentUrl
  createDownloadUrl.value = currentCreateDownloadUrl
  downloadUrl.value = currentDownloadUrl
}

defineExpose({ showPreview })

onUnmounted(() => {
  if (previewLoadingTimer) {
    clearTimeout(previewLoadingTimer)
  }
})
</script>

<style lang="scss" scoped>
.preview-shell {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  height: calc(100vh - 120px);
  max-height: calc(100vh - 120px);
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid rgba(189, 208, 202, 0.78);
  background: rgba(255, 255, 255, 0.78);
  flex-shrink: 0;

  .meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
    color: var(--text-secondary);

    .type {
      font-weight: 600;
      color: var(--text-main);
    }
  }

  .actions {
    display: flex;
    gap: 6px;
  }
}

.preview-loading {
  flex: 1;
  border-radius: 14px;
  border: 1px solid rgba(189, 208, 202, 0.72);
  background: rgba(255, 255, 255, 0.86);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);

  .loading-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--primary);
    animation: previewDot 0.9s ease-in-out infinite;
  }
}

:deep(.el-image-viewer__wrapper) {
  backdrop-filter: blur(4px);
}

@keyframes previewDot {
  0%,
  100% {
    transform: scale(0.75);
    opacity: 0.65;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

@media screen and (max-width: 768px) {
  .preview-shell {
    height: calc(100vh - 100px);
    max-height: calc(100vh - 100px);
    padding: 6px;
    gap: 6px;
  }

  .preview-toolbar {
    flex-wrap: wrap;
    padding: 6px 8px;

    .meta {
      font-size: 11px;
      gap: 6px;
    }
  }
}
</style>
