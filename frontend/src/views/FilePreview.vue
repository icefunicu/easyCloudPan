<template>
  <div class="file-preview-page">
    <div v-if="loading" class="preview-loading">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
    <template v-else-if="fileInfo?.fileId">
      <div class="preview-header">
        <div class="header-left">
          <span class="file-name">{{ fileInfo.fileName }}</span>
          <span v-if="fileInfo.fileSize" class="file-meta">
            {{ proxy.Utils.size2Str(fileInfo.fileSize) }}
          </span>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="downloadFile">下载</el-button>
        </div>
      </div>
      <div class="preview-content">
        <PreviewVideo v-if="fileInfo.fileCategory == 1" :url="videoUrl"></PreviewVideo>
        <PreviewDoc v-else-if="fileInfo.fileType == 5" :url="fileUrl"></PreviewDoc>
        <PreviewExcel v-else-if="fileInfo.fileType == 6" :url="fileUrl"></PreviewExcel>
        <PreviewPdf v-else-if="fileInfo.fileType == 4" :url="fileUrl"></PreviewPdf>
        <PreviewTxt v-else-if="fileInfo.fileType == 7 || fileInfo.fileType == 8" :url="fileUrl"></PreviewTxt>
        <PreviewMusic v-else-if="fileInfo.fileCategory == 2" :url="fileUrl" :file-name="fileInfo.fileName"></PreviewMusic>
        <div v-else-if="fileInfo.fileCategory == 3" class="image-preview">
          <img :src="imageUrl" alt="preview" />
        </div>
        <div v-else class="unsupported-preview">
          <span class="iconfont icon-file"></span>
          <p>此文件类型不支持在线预览</p>
          <el-button type="primary" @click="downloadFile">下载文件</el-button>
        </div>
      </div>
    </template>
    <div v-else class="preview-error">
      <span class="iconfont icon-error"></span>
      <p>文件不存在或已被删除</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, getCurrentInstance, defineAsyncComponent } from 'vue';
import { useRoute } from 'vue-router';
import { getFileInfoById, getFileUrl, getVideoUrl } from '@/services/fileService';
import type { FileInfoVO } from '@/types/file';
import { createDownloadCode } from '@/services';

const PreviewVideo = defineAsyncComponent(() => import('@/components/preview/PreviewVideo.vue'));
const PreviewDoc = defineAsyncComponent(() => import('@/components/preview/PreviewDoc.vue'));
const PreviewExcel = defineAsyncComponent(() => import('@/components/preview/PreviewExcel.vue'));
const PreviewPdf = defineAsyncComponent(() => import('@/components/preview/PreviewPdf.vue'));
const PreviewTxt = defineAsyncComponent(() => import('@/components/preview/PreviewTxt.vue'));
const PreviewMusic = defineAsyncComponent(() => import('@/components/preview/PreviewMusic.vue'));

const instance = getCurrentInstance()
const proxy = instance!.proxy!
const route = useRoute();

const loading = ref(true);
const fileInfo = ref<FileInfoVO | null>(null);
const fileUrl = ref('');
const videoUrl = ref('');
const imageUrl = ref('');

const fileId = computed(() => route.params.fileId);

const loadFileInfo = async () => {
  if (!fileId.value) {
    loading.value = false;
    return;
  }
  
  try {
    const result = await getFileInfoById(fileId.value as string);
    if (result) {
      fileInfo.value = result;
      fileUrl.value = getFileUrl(fileId.value as string);
      videoUrl.value = getVideoUrl(fileId.value as string);
      imageUrl.value = proxy.globalInfo.imageUrl + (result.fileCover?.replaceAll('_.', '.') || '');
    }
  } catch (e) {
    console.error('Failed to load file info:', e);
  } finally {
    loading.value = false;
  }
};

const downloadFile = async () => {
  const code = await createDownloadCode(`/file/createDownloadUrl/${fileId.value}`);
  if (code) {
    window.location.href = `/api/file/download/${code}`;
  }
};

onMounted(() => {
  loadFileInfo();
});
</script>

<style lang="scss" scoped>
.file-preview-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  display: flex;
  flex-direction: column;
}

.preview-loading,
.preview-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-secondary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(31, 79, 104, 0.2);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.preview-error {
  .iconfont {
    font-size: 48px;
    color: var(--danger);
  }
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(194, 204, 220, 0.88);
  box-shadow: var(--shadow-xs);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .file-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-main);
    }

    .file-meta {
      font-size: 13px;
      color: var(--text-secondary);
    }
  }
}

.preview-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: auto;
}

.image-preview {
  max-width: 100%;
  max-height: calc(100vh - 100px);
  
  img {
    max-width: 100%;
    max-height: calc(100vh - 100px);
    object-fit: contain;
    border-radius: 8px;
    box-shadow: var(--shadow-md);
  }
}

.unsupported-preview {
  text-align: center;
  padding: 40px;

  .iconfont {
    font-size: 64px;
    color: var(--text-light);
  }

  p {
    margin: 16px 0;
    color: var(--text-secondary);
  }
}
</style>
