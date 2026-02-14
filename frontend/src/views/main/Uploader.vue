<template>
  <div class="uploader-panel">
    <div class="uploader-title">
      <span>上传任务</span>
      <span class="tips">（仅展示本次上传任务）</span>
      <div class="title-actions">
        <el-button 
          v-if="hasFailedTasks" 
          size="small" 
          type="warning" 
          @click="retryAllFailed"
        >
          重试全部失败
        </el-button>
        <el-button 
          v-if="hasCompletedTasks" 
          size="small" 
          @click="clearCompleted"
        >
          清除已完成
        </el-button>
      </div>
    </div>
    <div class="file-list">
      <div v-for="(item, index) in fileList" :key="item.uid" class="file-item">
        <div class="upload-panel">
          <div class="file-name">{{ item.fileName }}</div>
          <div class="progress">
            <el-progress
              v-if="item.status == STATUS.uploading.value || item.status == STATUS.upload_seconds.value || item.status == STATUS.upload_finish.value"
              :percentage="item.uploadProgress"
            />
          </div>
          <div class="upload-status">
            <span
              :class="['iconfont', 'icon-' + STATUS[item.status].icon]"
              :style="{ color: STATUS[item.status].color }"
            ></span>
            <span class="status" :style="{ color: STATUS[item.status].color }">
              <span v-if="item.status === 'transferring'" class="loading-spinner"></span>
              {{ item.status == 'fail' ? item.errorMsg : STATUS[item.status].desc }}
            </span>
            <span v-if="item.status == STATUS.uploading.value && item.isResume" class="resume-badge">
              续传
            </span>
            <span v-if="item.status == STATUS.uploading.value" class="upload-info">
              {{ proxy.Utils.size2Str(item.uploadSize) }} / {{ proxy.Utils.size2Str(item.totalSize) }}
            </span>
          </div>
        </div>
        <div class="op">
          <el-progress
            v-if="item.status == STATUS.init.value"
            type="circle"
            :width="50"
            :percentage="item.md5Progress"
          />
          <div class="op-btn">
            <span v-if="item.status === STATUS.uploading.value">
              <Icon
                v-if="item.pause"
                :width="28"
                class="btn-item"
                icon-name="upload"
                title="上传"
                @click="startUpload(item.uid)"
              ></Icon>
              <Icon
                v-else
                :width="28"
                class="btn-item"
                icon-name="pause"
                title="暂停"
                @click="pauseUpload(item.uid)"
              ></Icon>
            </span>
            <Icon
              v-if="item.status != STATUS.init.value && item.status != STATUS.upload_finish.value && item.status != STATUS.upload_seconds.value"
              :width="28"
              class="del btn-item"
              icon-name="del"
              title="删除"
              @click="delUpload(item.uid, index)"
            ></Icon>
            <Icon
              v-if="item.status == STATUS.upload_finish.value || item.status == STATUS.upload_seconds.value"
              :width="28"
              class="clean btn-item"
              icon-name="clean"
              title="清除"
              @click="delUpload(item.uid, index)"
            ></Icon>
          </div>
        </div>
      </div>
      <div v-if="fileList.length == 0">
        <NoData msg="暂无上传任务"></NoData>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onUnmounted, computed, watch } from "vue";

const { proxy } = getCurrentInstance();

const api = {
  upload: "/file/uploadFile",
  uploadedChunks: "/file/uploadedChunks",
  transferStatus: "/file/transferStatus",
};

const STATUS = {
  emptyfile: {
    value: "emptyfile",
    desc: "文件为空",
    color: "#F75000",
    icon: "close",
  },
  fail: {
    value: "fail",
    desc: "上传失败",
    color: "#F75000",
    icon: "close",
  },
  init: {
    value: "init",
    desc: "解析中",
    color: "#e6a23c",
    icon: "clock",
  },
  uploading: {
    value: "uploading",
    desc: "上传中",
    color: "#409eff",
    icon: "upload",
  },
  upload_finish: {
    value: "upload_finish",
    desc: "上传完成",
    color: "#67c23a",
    icon: "ok",
  },
  upload_seconds: {
    value: "upload_seconds",
    desc: "秒传",
    color: "#67c23a",
    icon: "ok",
  },
  retrying: {
    value: "retrying",
    desc: "重试中",
    color: "#409eff",
    icon: "upload",
  },
  network_error: {
    value: "network_error",
    desc: "网络错误",
    color: "#F75000",
    icon: "close",
  },
  auth_error: {
    value: "auth_error",
    desc: "鉴权失败",
    color: "#F75000",
    icon: "close",
  },
  server_error: {
    value: "server_error",
    desc: "服务器错误",
    color: "#F75000",
    icon: "close",
  },
  transferring: {
    value: "transferring",
    desc: "转码中",
    color: "#e6a23c",
    icon: "clock",
  },
  transfer_done: {
    value: "transfer_done",
    desc: "转码完成",
    color: "#67c23a",
    icon: "ok",
  },
  transfer_fail: {
    value: "transfer_fail",
    desc: "转码失败",
    color: "#F75000",
    icon: "close",
  },
};

const chunkSize = 1024 * 1024 * 5;
const uploadWindowSize = 3;
const fileList = ref([]);
const delList = ref([]);

// 计算属性：是否有失败的任务
const hasFailedTasks = computed(() => {
  return fileList.value.some(item => 
    item.status === STATUS.fail.value || 
    item.status === STATUS.network_error.value ||
    item.status === STATUS.auth_error.value ||
    item.status === STATUS.server_error.value
  );
});

// 计算属性：是否有已完成的任务
const hasCompletedTasks = computed(() => {
  return fileList.value.some(item => 
    item.status === STATUS.upload_finish.value || 
    item.status === STATUS.upload_seconds.value ||
    item.status === STATUS.transfer_done.value
  );
});

// 计算属性：活跃任务数（上传中+转码中）
const activeTaskCount = computed(() => {
  return fileList.value.filter(item => 
    item.status === STATUS.uploading.value || 
    item.status === STATUS.init.value ||
    item.status === STATUS.transferring.value
  ).length;
});

// 重试所有失败的任务
const retryAllFailed = () => {
  fileList.value.forEach(item => {
    if (item.status === STATUS.fail.value || 
        item.status === STATUS.network_error.value ||
        item.status === STATUS.auth_error.value ||
        item.status === STATUS.server_error.value) {
      item.status = STATUS.uploading.value;
      item.pause = false;
      item.errorMsg = null;
      uploadFile(item.uid, item.chunkIndex);
    }
  });
};

// 清除已完成的任务
const clearCompleted = () => {
  for (let i = fileList.value.length - 1; i >= 0; i--) {
    const item = fileList.value[i];
    if (item.status === STATUS.upload_finish.value || 
        item.status === STATUS.upload_seconds.value) {
      clearMd5Worker(item);
      fileList.value.splice(i, 1);
    }
  }
};

const createMd5Worker = () => {
  return new Worker(new URL("../../workers/md5.worker.js", import.meta.url), {
    type: "module",
  });
};

const createUploadFileId = () => {
  const chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  let id = "";
  for (let i = 0; i < 10; i++) {
    id += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return id;
};

const getFileByUid = (uid) => {
  return fileList.value.find((item) => item.file.uid === uid);
};

const clearMd5Worker = (fileItem) => {
  if (!fileItem || !fileItem.md5Worker) {
    return;
  }
  fileItem.md5Worker.terminate();
  fileItem.md5Worker = null;
};

const clearDeletedMark = (uid) => {
  const delIndex = delList.value.indexOf(uid);
  if (delIndex !== -1) {
    delList.value.splice(delIndex, 1);
  }
};

const isFileDeleted = (uid) => {
  return delList.value.includes(uid) || !getFileByUid(uid);
};

const syncUploadProgress = (currentFile) => {
  if (!currentFile || !currentFile.chunkLoadedMap) {
    return;
  }
  const loaded = Object.values(currentFile.chunkLoadedMap).reduce((sum, value) => sum + Number(value || 0), 0);
  currentFile.uploadSize = Math.min(loaded, currentFile.totalSize);
  currentFile.uploadProgress = Math.floor((currentFile.uploadSize / currentFile.totalSize) * 100);
};

const isTerminalUploadStatus = (statusCode) => {
  return statusCode === STATUS.upload_seconds.value || statusCode === STATUS.upload_finish.value;
};

const emit = defineEmits(["uploadCallback", "update:activeTaskCount"]);

watch(activeTaskCount, (newVal) => {
  emit("update:activeTaskCount", newVal);
});

const handleTerminalStatus = (uid, uploadResult) => {
  if (!uploadResult || !uploadResult.data) {
    return false;
  }
  const statusCode = uploadResult.data.status;
  if (!isTerminalUploadStatus(statusCode)) {
    return false;
  }
  const currentFile = getFileByUid(uid);
  if (!currentFile) {
    return true;
  }
  currentFile.uploadSize = currentFile.totalSize;
  currentFile.uploadProgress = 100;
  currentFile.chunkIndex = Math.ceil(currentFile.totalSize / chunkSize);
  
  if (statusCode === STATUS.upload_finish.value) {
    // Start polling for transfer status
    startTransferPolling(uid);
  } else {
    // UPLOAD_SECONDS or other done states
    emit("uploadCallback");
  }
  return true;
};

const addFile = async (file, filePid) => {
  const fileItem = {
    file,
    uid: file.uid,
    md5Progress: 0,
    md5: null,
    fileName: file.name,
    status: STATUS.init.value,
    uploadSize: 0,
    totalSize: file.size,
    uploadProgress: 0,
    pause: false,
    chunkIndex: 0,
    uploading: false,
    chunkLoadedMap: {},
    fileId: createUploadFileId(),
    filePid,
    errorMsg: null,
    md5Worker: null,
    isResume: false,
    uploadedChunks: [],
  };

  fileList.value.unshift(fileItem);
  if (fileItem.totalSize === 0) {
    fileItem.status = STATUS.emptyfile.value;
    return;
  }

  const md5FileUid = await computeMd5(fileItem);
  if (md5FileUid == null) {
    return;
  }
  
  await checkUploadedChunks(md5FileUid);
  uploadFile(md5FileUid);
};

const checkUploadedChunks = async (uid) => {
  const currentFile = getFileByUid(uid);
  if (!currentFile) {
    return;
  }
  
  try {
    const result = await proxy.Request({
      url: api.uploadedChunks,
      showLoading: false,
      params: {
        fileId: currentFile.fileId,
        filePid: currentFile.filePid,
      },
    });
    
    if (!result || !result.data) {
      return;
    }
    
    const uploadedChunks = result.data || [];
    if (uploadedChunks.length > 0) {
      currentFile.isResume = true;
      currentFile.uploadedChunks = uploadedChunks;
      
      const chunks = Math.ceil(currentFile.totalSize / chunkSize);
      uploadedChunks.forEach(chunkIndex => {
        if (chunkIndex >= 0 && chunkIndex < chunks) {
          const start = chunkIndex * chunkSize;
          const end = Math.min(start + chunkSize, currentFile.totalSize);
          currentFile.chunkLoadedMap[chunkIndex] = end - start;
        }
      });
      
      syncUploadProgress(currentFile);
      
      const maxUploadedIndex = Math.max(...uploadedChunks, -1);
      currentFile.chunkIndex = maxUploadedIndex + 1;
    }
  } catch (error) {
    console.warn('Failed to check uploaded chunks:', error);
  }
};

// 轮询转码状态
const startTransferPolling = (uid) => {
  const currentFile = getFileByUid(uid);
  if (!currentFile) return;

  currentFile.status = STATUS.transferring.value;
  let pollCount = 0;
  const maxPolls = 60; // 3 minutes timeout

  const poll = async () => {
    if (pollCount >= maxPolls) {
      // Timeout, but don't mark as fail, just stop polling
      return; 
    }
    
    // Check if component unmounted or file removed
    const latestFile = getFileByUid(uid);
    if (!latestFile || latestFile.status !== STATUS.transferring.value) {
      return;
    }

    try {
      const result = await proxy.Request({
        url: api.transferStatus,
        showLoading: false,
        params: {
          fileId: latestFile.fileId,
        },
      });

      if (result && result.data) {
        const status = result.data.status;
        // FileStatusEnums: USING(2), TRANSFER_FAIL(1)
        if (status === 2) {
          latestFile.status = STATUS.transfer_done.value;
          emit("uploadCallback");
          return;
        } else if (status === 1) {
          latestFile.status = STATUS.transfer_fail.value;
          return;
        }
      }
    } catch (e) {
      console.error("Poll transfer status failed", e);
    }
    
    pollCount++;
    latestFile.transferTimer = setTimeout(poll, 3000);
  };

  poll();
};

const stopTransferPolling = (uid) => {
  const currentFile = getFileByUid(uid);
  if (currentFile && currentFile.transferTimer) {
    clearTimeout(currentFile.transferTimer);
    currentFile.transferTimer = null;
  }
};

defineExpose({ addFile, activeTaskCount });

const startUpload = (uid) => {
  const currentFile = getFileByUid(uid);
  if (!currentFile) {
    return;
  }
  currentFile.pause = false;
  uploadFile(uid, currentFile.chunkIndex);
};

const pauseUpload = (uid) => {
  const currentFile = getFileByUid(uid);
  if (!currentFile) {
    return;
  }
  currentFile.pause = true;
};

const delUpload = (uid, index) => {
  const currentFile = getFileByUid(uid);
  clearMd5Worker(currentFile);
  if (!delList.value.includes(uid)) {
    delList.value.push(uid);
  }
  fileList.value.splice(index, 1);
};

onUnmounted(() => {
  fileList.value.forEach((item) => {
    clearMd5Worker(item);
    stopTransferPolling(item.uid);
  });
});

const computeMd5 = (fileItem) => {
  return new Promise((resolve) => {
    const worker = createMd5Worker();
    fileItem.md5Worker = worker;

    const finish = (result) => {
      clearMd5Worker(fileItem);
      resolve(result);
    };

    worker.onmessage = (event) => {
      const data = event.data || {};
      if (data.uid !== fileItem.uid) {
        return;
      }

      const latestFile = getFileByUid(fileItem.uid);
      if (!latestFile) {
        finish(null);
        return;
      }

      if (data.type === "progress") {
        latestFile.md5Progress = data.progress;
        return;
      }

      if (data.type === "done") {
        latestFile.md5Progress = 100;
        latestFile.status = STATUS.uploading.value;
        latestFile.md5 = data.md5;
        finish(fileItem.uid);
        return;
      }

      if (data.type === "cancelled") {
        finish(null);
        return;
      }

      latestFile.md5Progress = -1;
      latestFile.status = STATUS.fail.value;
      latestFile.errorMsg = data.errorMsg || "MD5 计算失败";
      finish(null);
    };

    worker.onerror = () => {
      const latestFile = getFileByUid(fileItem.uid);
      if (latestFile) {
        latestFile.md5Progress = -1;
        latestFile.status = STATUS.fail.value;
        latestFile.errorMsg = "MD5 计算失败";
      }
      finish(null);
    };

    worker.postMessage({
      type: "compute",
      uid: fileItem.uid,
      file: fileItem.file,
      chunkSize,
    });
  });
};

const uploadSingleChunk = async (uid, chunkIndex, chunks) => {
  let currentFile = getFileByUid(uid);
  if (!currentFile || currentFile.pause || isFileDeleted(uid)) {
    return null;
  }

  const file = currentFile.file;
  const fileSize = currentFile.totalSize;
  const start = chunkIndex * chunkSize;
  const end = start + chunkSize >= fileSize ? fileSize : start + chunkSize;
  const chunkFile = file.slice(start, end);
  const chunkRealSize = end - start;

  currentFile.chunkLoadedMap[chunkIndex] = currentFile.chunkLoadedMap[chunkIndex] || 0;
  syncUploadProgress(currentFile);

  // 重试逻辑：最多重试 3 次
  const maxRetries = 3;
  let retryCount = 0;
  let lastError = null;

  while (retryCount <= maxRetries) {
    try {
      const uploadResult = await proxy.Request({
        url: api.upload,
        showLoading: false,
        dataType: "file",
        params: {
          file: chunkFile,
          fileName: file.name,
          fileMd5: currentFile.md5,
          chunkIndex,
          chunks,
          fileId: currentFile.fileId,
          filePid: currentFile.filePid,
        },
        showError: false,
        errorCallback: (errorMsg) => {
          lastError = errorMsg;
          const latestFile = getFileByUid(uid);
          if (!latestFile) {
            return;
          }
          
          // 错误分类
          if (errorMsg.includes("网络") || errorMsg.includes("timeout") || errorMsg.includes("Network")) {
            latestFile.status = STATUS.network_error.value;
            latestFile.errorMsg = "网络连接失败，请检查网络后重试";
          } else if (errorMsg.includes("401") || errorMsg.includes("403") || errorMsg.includes("鉴权")) {
            latestFile.status = STATUS.auth_error.value;
            latestFile.errorMsg = "登录已过期，请重新登录";
          } else if (errorMsg.includes("500") || errorMsg.includes("服务器")) {
            latestFile.status = STATUS.server_error.value;
            latestFile.errorMsg = "服务器错误，请稍后重试";
          } else {
            latestFile.status = STATUS.fail.value;
            latestFile.errorMsg = errorMsg;
          }
        },
        uploadProgressCallback: (event) => {
          const latestFile = getFileByUid(uid);
          if (!latestFile) {
            return;
          }
          let loaded = event.loaded;
          if (loaded > chunkRealSize) {
            loaded = chunkRealSize;
          }
          latestFile.chunkLoadedMap[chunkIndex] = loaded;
          syncUploadProgress(latestFile);
        },
      });

      if (!uploadResult) {
        // 如果是网络错误且还有重试次数，则重试
        if (retryCount < maxRetries && lastError && 
            (lastError.includes("网络") || lastError.includes("timeout") || lastError.includes("Network"))) {
          retryCount++;
          const latestFile = getFileByUid(uid);
          if (latestFile) {
            latestFile.status = STATUS.retrying.value;
            latestFile.errorMsg = `网络错误，正在重试 (${retryCount}/${maxRetries})`;
          }
          // 指数退避：1s, 2s, 4s
          await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount - 1) * 1000));
          continue;
        }
        return null;
      }

      currentFile = getFileByUid(uid);
      if (!currentFile || isFileDeleted(uid)) {
        return null;
      }

      currentFile.fileId = uploadResult.data.fileId || currentFile.fileId;
      currentFile.chunkLoadedMap[chunkIndex] = chunkRealSize;
      syncUploadProgress(currentFile);
      if (STATUS[uploadResult.data.status]) {
        currentFile.status = STATUS[uploadResult.data.status].value;
      }
      currentFile.chunkIndex = Math.max(currentFile.chunkIndex, chunkIndex + 1);
      return uploadResult;
    } catch (error) {
      lastError = error.message || "未知错误";
      if (retryCount < maxRetries) {
        retryCount++;
        const latestFile = getFileByUid(uid);
        if (latestFile) {
          latestFile.status = STATUS.retrying.value;
          latestFile.errorMsg = `上传失败，正在重试 (${retryCount}/${maxRetries})`;
        }
        await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount - 1) * 1000));
        continue;
      }
      
      const latestFile = getFileByUid(uid);
      if (latestFile) {
        latestFile.status = STATUS.fail.value;
        latestFile.errorMsg = lastError;
      }
      return null;
    }
  }

  return null;
};

const uploadChunksWithWindow = async (uid, startChunkIndex, endChunkIndex, chunks) => {
  let nextChunkIndex = startChunkIndex;
  const running = new Set();

  const launchOne = (chunkIndex) => {
    const promise = uploadSingleChunk(uid, chunkIndex, chunks).then((uploadResult) => ({
      chunkIndex,
      uploadResult,
    }));
    running.add(promise);
    promise.finally(() => {
      running.delete(promise);
    });
  };

  while (nextChunkIndex <= endChunkIndex && running.size < uploadWindowSize) {
    launchOne(nextChunkIndex);
    nextChunkIndex++;
  }

  while (running.size > 0) {
    const { uploadResult } = await Promise.race(running);
    if (!uploadResult) {
      return;
    }
    if (handleTerminalStatus(uid, uploadResult)) {
      return;
    }

    const currentFile = getFileByUid(uid);
    if (!currentFile || currentFile.pause || currentFile.status === STATUS.fail.value || isFileDeleted(uid)) {
      return;
    }

    while (nextChunkIndex <= endChunkIndex && running.size < uploadWindowSize) {
      launchOne(nextChunkIndex);
      nextChunkIndex++;
    }
  }
};

const uploadFile = async (uid, chunkIndex) => {
  const hasChunkIndex = typeof chunkIndex === "number" && !Number.isNaN(chunkIndex);
  const startChunkIndex = hasChunkIndex ? chunkIndex : 0;

  let currentFile = getFileByUid(uid);
  if (!currentFile || currentFile.uploading) {
    return;
  }
  currentFile.uploading = true;

  try {
    const fileSize = currentFile.totalSize;
    const chunks = Math.ceil(fileSize / chunkSize);
    let nextChunkIndex = Math.max(startChunkIndex, currentFile.chunkIndex || 0);

    if (nextChunkIndex >= chunks) {
      return;
    }

    if (chunks > 1 && nextChunkIndex === 0) {
      const firstResult = await uploadSingleChunk(uid, 0, chunks);
      if (!firstResult || handleTerminalStatus(uid, firstResult)) {
        return;
      }
      nextChunkIndex = 1;
    }

    currentFile = getFileByUid(uid);
    if (!currentFile || currentFile.pause || currentFile.status === STATUS.fail.value || isFileDeleted(uid)) {
      return;
    }

    if (chunks === 1) {
      const singleResult = await uploadSingleChunk(uid, 0, 1);
      if (singleResult) {
        handleTerminalStatus(uid, singleResult);
      }
      return;
    }

    const lastChunkIndex = chunks - 1;
    if (nextChunkIndex < lastChunkIndex) {
      await uploadChunksWithWindow(uid, nextChunkIndex, lastChunkIndex - 1, chunks);
    }

    currentFile = getFileByUid(uid);
    if (!currentFile || currentFile.pause || currentFile.status === STATUS.fail.value || isFileDeleted(uid)) {
      return;
    }

    if (currentFile.chunkIndex <= lastChunkIndex) {
      const lastResult = await uploadSingleChunk(uid, lastChunkIndex, chunks);
      if (lastResult) {
        handleTerminalStatus(uid, lastResult);
      }
    }
  } finally {
    const latestFile = getFileByUid(uid);
    if (latestFile) {
      latestFile.uploading = false;
    }
    clearDeletedMark(uid);
  }
};
</script>

<style lang="scss" scoped>
.uploader-panel {
  .uploader-title {
    border-bottom: 1px solid var(--border-color);
    line-height: 44px;
    padding: 0 12px;
    font-size: 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;

    .tips {
      font-size: 13px;
      color: var(--text-light);
    }

    .title-actions {
      display: flex;
      gap: 8px;
    }
  }

  .file-list {
    overflow: auto;
    padding: 10px 0;
    min-height: calc(100vh / 2);
    max-height: calc(100vh - 120px);

    .file-item {
      position: relative;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 6px 12px;
      background-color: var(--bg-card);
      border-bottom: 1px solid var(--border-color);
    }

    .file-item:nth-child(even) {
      background-color: var(--bg-hover);
    }

    .upload-panel {
      flex: 1;

      .file-name {
        color: var(--text-main);
      }

      .upload-status {
        display: flex;
        align-items: center;
        margin-top: 5px;

        .loading-spinner {
          width: 12px;
          height: 12px;
          border: 2px solid rgba(0, 0, 0, 0.15);
          border-top-color: var(--warning);
          border-radius: 50%;
          animation: spin 0.9s linear infinite;
          margin-right: 6px;
          flex: 0 0 auto;
        }

        .iconfont {
          margin-right: 3px;
        }

        .status {
          color: var(--danger);
          font-size: 13px;
        }

        .upload-info {
          margin-left: 5px;
          font-size: 12px;
          color: var(--text-light);
        }

        .resume-badge {
          margin-left: 5px;
          padding: 1px 6px;
          font-size: 11px;
          background: linear-gradient(45deg, var(--primary), var(--success));
          color: #fff;
          border-radius: var(--border-radius-sm);
        }
      }

      .progress {
        height: 10px;
      }
    }

    .op {
      width: 100px;
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .op-btn {
        .btn-item {
          cursor: pointer;
        }

        .del,
        .clean {
          margin-left: 5px;
        }
      }
    }
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
