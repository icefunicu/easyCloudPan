<template>
  <div class="main">
      <div class="top">
          <div class="top-op">
              <div class="search-panel">
                <!-- 搜索文件 -->
                <el-input
                  v-model="fileNameFuzzy"
                  clearable
                  placeholder="请输入文件名搜索"
                  @keyup.enter="search"
                  class="glass-input"
                >
                    <template #suffix>
                        <i class="iconfont icon-search" @click="search"></i>
                    </template>
                </el-input>
                <el-button
                  class="advanced-search-toggle"
                  :class="{ active: showAdvancedSearch }"
                  text
                  size="small"
                  @click="showAdvancedSearch = !showAdvancedSearch"
                >
                  <span class="iconfont icon-filter"></span>
                </el-button>
              </div>
              <div class="view-switch" role="group" aria-label="文件视图切换">
                <button
                  type="button"
                  :class="['switch-btn', viewMode === 'list' ? 'active' : '']"
                  @click="setViewMode('list')"
                >
                  列表
                </button>
                <button
                  type="button"
                  :class="['switch-btn', viewMode === 'icon' ? 'active' : '']"
                  @click="setViewMode('icon')"
                >
                  图标
                </button>
              </div>
              <div class="iconfont icon-refresh" @click="loadDataList"></div>
          </div>
          <!-- 导航 -->
          <Navigation ref="navigationRef" @nav-change="navChange"></Navigation>
          <!-- 高级搜索面板 -->
          <AdvancedSearch
            :visible="showAdvancedSearch"
            :category="category"
            @search="handleAdvancedSearch"
            @reset="handleAdvancedReset"
          />
      </div>
      <div
        v-if="tableData.list && tableData.list.length > 0"
        class="file-list"
        @drop="handleDrop"
        @dragover.prevent="isDragOver = true"
        @dragleave="isDragOver = false"
        :class="{ 'drag-over': isDragOver }"
      >
        <div class="drop-mask" v-if="isDragOver">
            <span class="iconfont icon-upload"></span>
            <div class="text">松开鼠标上传文件</div>
        </div>
        <!-- 批量操作工具栏 -->
        <BatchToolbar
          :selected-count="selectFileIdList.length"
          :total-count="totalFileCount"
          :is-all-selected="isAllSelected"
          :is-partial-selected="isPartialSelected"
          @select-all="handleSelectAll"
          @invert-select="handleInvertSelect"
          @move="moveFolderBatch"
          @delete="delFileBatch"
          @share="shareBatch"
          @download="downloadBatch"
          @clear="clearSelection"
        />
        <transition name="view-switch-fade" mode="out-in">
          <Table
            v-if="viewMode === 'list'"
            key="list"
            ref="dataTableRef"
            :columns="columns"
            :data-source="tableData"
            :fetch="loadDataList"
            :init-fetch="false"
            :options="tableOptions"
            :loading="isLoading"
            @row-selected="rowSelected"
          >
          <template #fileName="{ index, row }">
              <div
                v-touch="{
                  onLongPress: () => showOp(row),
                  onSwipeLeft: () => delFile(row),
                  onSwipeRight: () => share(row)
                }"
                :class="['file-item', row.showOp ? 'show-op' : '', row.showEdit ? 'editing' : '']"
                @mouseenter="showOp(row)"
                @mouseleave="cancelShowOp(row)"
                @contextmenu.prevent="e => contextMenuRef?.show(e, row)"
              >
                <template
                  v-if="(row.fileType == 3 || row.fileType == 1) && row.status == 2"
                >
                  <Icon :cover="row.fileCover" :width="32"></Icon>
                </template>
                <template v-else>
                  <Icon v-if="row.folderType == 0" :file-type="row.fileType"></Icon>
                  <Icon v-if="row.folderType == 1" :file-type="0"></Icon>
                </template>
                <span v-if="!row.showEdit" class="file-name" :title="row.fileName">
                  <span @click="preview(row)">{{ row.fileName }}</span>
                  <span v-if="row.status == 0" class="transfer-status processing">转码中</span>
                  <span 
                    v-if="row.status == 1" 
                    class="transfer-status transfer-fail"
                    @click="retryTranscode(row)"
                  >转码失败</span>
                </span>
                <div v-if="row.showEdit" class="edit-panel">
                  <el-input
                    ref="editNameRef"
                    v-model.trim="row.fileNameReal"
                    :maxlength="190"
                    @keyup.enter="saveNameEdit(index)"
                  >
                    <template #suffix>{{ row.fileSuffix }}</template>
                  </el-input>
                  <span
                    :class="[
                      'iconfont icon-right1',
                      row.fileNameReal ? '' : 'not-allow',
                    ]"
                    @click="saveNameEdit(index)"
                  ></span>
                  <span
                    class="iconfont icon-error"
                    @click="cancelNameEdit(index)"
                  ></span>
                </div>
                <span class="op">
                  <template v-if="row.showOp && row.fileId && row.status == 2">
                      <span class="iconfont icon-share1" @click="share(row)"
                        >分享</span
                      >
                      <span
                        v-if="row.folderType == 0"
                        class="iconfont icon-download"
                        @click="download(row)"
                        >下载</span
                      >
                      <span class="iconfont icon-del" @click="delFile(row)"
                        >删除</span
                      >
                      <span class="iconfont icon-edit" @click="editFileName(index)"
                        >重命名</span
                      >
                      <span class="iconfont icon-move" @click="moveFolder(row)">移动</span>
                  </template>
                </span>
              </div>
          </template>
          <template #fileSize="{ row }">
              <span v-if="row.fileSize">{{
                proxy.Utils.size2Str(row.fileSize)
              }}</span>
          </template>
          </Table>
          <div v-else key="icon" class="file-grid">
            <div
              v-for="(row, index) in tableData.list"
              :key="row.fileId || `${row.fileName}-${index}`"
              :class="['grid-item', row.showOp ? 'show-op' : '']"
              @mouseenter="showOp(row)"
              @mouseleave="cancelShowOp(row)"
              @contextmenu.prevent="e => contextMenuRef?.show(e, row)"
            >
              <div class="grid-check" @click.stop>
                <el-checkbox
                  :model-value="isGridSelected(row.fileId)"
                  @change="value => toggleGridSelect(row.fileId, value)"
                />
              </div>
              <div class="grid-icon" @click="preview(row)">
                <template v-if="(row.fileType == 3 || row.fileType == 1) && row.status == 2">
                  <Icon :cover="row.fileCover" :width="52"></Icon>
                </template>
                <template v-else>
                  <Icon v-if="row.folderType == 0" :file-type="row.fileType" :width="52"></Icon>
                  <Icon v-if="row.folderType == 1" :file-type="0" :width="52"></Icon>
                </template>
              </div>
              <div class="grid-name" :title="row.fileName" @click="preview(row)">{{ row.fileName }}</div>
              <div class="grid-meta">
                <span>{{ row.folderType == 1 ? "文件夹" : proxy.Utils.size2Str(row.fileSize || 0) }}</span>
                <span v-if="row.status == 0" class="transfer-status processing">转码中</span>
                <span 
                  v-if="row.status == 1" 
                  class="transfer-status transfer-fail"
                  @click="retryTranscode(row)"
                >转码失败</span>
              </div>
              <div class="grid-op">
                <span v-if="row.status == 2" class="iconfont icon-share1" @click.stop="share(row)">分享</span>
                <span v-if="row.folderType == 0 && row.status == 2" class="iconfont icon-download" @click.stop="download(row)">下载</span>
                <span class="iconfont icon-del" @click.stop="delFile(row)">删除</span>
                <span class="iconfont icon-edit" @click.stop="openRenameFromGrid(index)">重命名</span>
                <span class="iconfont icon-move" @click.stop="moveFolder(row)">移动</span>
              </div>
            </div>
          </div>
        </transition>
      </div>
      <div 
        v-else 
        class="no-data"
        @drop="handleDrop"
        @dragover.prevent="isDragOver = true"
        @dragleave="isDragOver = false"
        :class="{ 'drag-over': isDragOver }"
      >
        <div class="drop-mask" v-if="isDragOver">
            <span class="iconfont icon-upload"></span>
            <div class="text">松开鼠标上传文件</div>
        </div>
        <div class="no-data-inner">
          <Icon icon-name="no_data" :width="120" fit="fill"></Icon>
          <div class="tips">当前目录为空，上传你的第一个文件吧</div>
          <div class="op-list">
            <el-upload
              :show-file-list="false"
              :with-credentials="true"
              :multiple="true"
              :http-request="addFile"
              :accept="fileAccept"
            >
              <div class="op-item">
                <Icon icon-name="file" :width="60"></Icon>
                <div>上传文件</div>
              </div>
            </el-upload>
            <div v-if="category == 'all'" class="op-item" @click="newFolder">
              <Icon icon-name="folder" :width="60"></Icon>
              <div>新建目录</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 悬浮操作岛 (Floating Action Bar) -->
      <div class="floating-action-bar">
          <div class="fab-inner">
              <el-upload
                  class="fab-btn-wrap"
                  :show-file-list="false"
                  :with-credentials="true"
                  :multiple="true"
                  :http-request="addFile"
                  :accept="fileAccept"
              >
                  <button class="fab-btn primary" title="上传文件">
                      <span class="iconfont icon-upload"></span>
                      <span class="fab-text">上传</span>
                  </button>
              </el-upload>
              <div class="fab-divider"></div>
              <button class="fab-btn" title="新建文件夹" @click="newFolder">
                  <span class="iconfont icon-folder-add"></span>
                  <span class="fab-text">新建</span>
              </button>
              <button class="fab-btn danger" title="批量删除" :disabled="selectFileIdList.length == 0" @click="delFileBatch">
                  <span class="iconfont icon-del"></span>
                  <span class="fab-text">删除</span>
              </button>
              <button class="fab-btn warning" title="批量移动" :disabled="selectFileIdList.length == 0" @click="moveFolderBatch">
                  <span class="iconfont icon-move"></span>
                  <span class="fab-text">移动</span>
              </button>
          </div>
      </div>

      <FolderSelect
        ref="folderSelectRef"
        @folder-select="moveFolderDone"
      ></FolderSelect>
      <!-- 预览 -->
      <Preview ref="previewRef"></Preview>
      <!-- 分享 -->
      <ShareFile ref="shareRef"></ShareFile>
      <!-- 右键菜单 -->
      <ContextMenu ref="contextMenuRef" @action="handleContextMenuAction" />
  </div>
</template>

<script setup>
import CategoryInfo from "@/js/CategoryInfo.js";
import ShareFile from "./ShareFile.vue";
import BatchToolbar from "@/components/BatchToolbar.vue";
import AdvancedSearch from "@/components/AdvancedSearch.vue";
import ContextMenu from "@/components/ContextMenu.vue";
import { ref, getCurrentInstance, nextTick, computed, onMounted, onUnmounted, watch } from "vue";
import * as fileService from "@/services/fileService";
import EventBus from '@/utils/EventBus';
import { useSWR } from "@/composables/useSWR";

const { proxy } = getCurrentInstance();

const emit = defineEmits(["addFile"]);
const addFile = (fileData) => {
  emit("addFile", { file: fileData.file, filePid: currentFolder.value.fileId });
};

const VIEW_MODE_STORAGE_KEY = "main_file_view_mode";
const viewMode = ref(localStorage.getItem(VIEW_MODE_STORAGE_KEY) === "icon" ? "icon" : "list");
const setViewMode = mode => {
  viewMode.value = mode === "icon" ? "icon" : "list";
  localStorage.setItem(VIEW_MODE_STORAGE_KEY, viewMode.value);
};

// 拖拽上传
const isDragOver = ref(false);
const handleDrop = (e) => {
  e.preventDefault();
  isDragOver.value = false;
  if (e.dataTransfer.files.length > 0) {
    for (let i = 0; i < e.dataTransfer.files.length; i++) {
        emit('addFile', {
            file: e.dataTransfer.files[i],
            filePid: currentFolder.value.fileId
        });
    }
  }
};

// 添加文件回调
const reload = () => {
  showLoading.value = false;
  loadDataList();
};
defineExpose({
  reload,
});
// 当前目录
const currentFolder = ref({ fileId: "0" });

const fileAccept = computed( () => {
  const categoryItem = CategoryInfo[category.value];
  return categoryItem ? categoryItem.accept : "*";
});

const columns = [
    {
        label: "文件名",
        prop: "fileName",
        scopedSlots: "fileName",
        sortable: true,
    },
    {
        label: "修改时间",
        prop: "lastUpdateTime",
        width: 200,
        className: "hidden-mobile",
        sortable: true,
    },
    {
        label: "大小",
        prop: "fileSize",
        scopedSlots: "fileSize",
        width: 200,
        className: "hidden-mobile",
        sortable: true,
    },
];

// 搜索
const search = () => {
  showLoading.value = true;
  loadDataList();
};

// 高级搜索
const showAdvancedSearch = ref(false);
const advancedFilters = ref({});
const handleAdvancedSearch = (filters) => {
  advancedFilters.value = filters;
  showLoading.value = true;
  loadDataList();
};
const handleAdvancedReset = () => {
  advancedFilters.value = {};
};

const tableData = ref({});
const tableOptions = ref({
    extHeight: 50,
    selectType: "checkbox",
    rowKey: "fileId",
    tableHeight: "calc(100% - 50px)",
});

const fileNameFuzzy = ref();
const showLoading = ref(true);
const isLoading = ref(false);
const category = ref();

// 根据参数快照触发 SWR
const loadParamsString = computed(() => {
    return JSON.stringify({
        pageNo: tableData.value.pageNo || 1,
        pageSize: tableData.value.pageSize || 15,
        fileNameFuzzy: fileNameFuzzy.value,
        filePid: currentFolder.value.fileId,
        category: category.value
    });
});

const { data: swrData, isLoading: swrIsLoading, revalidate } = useSWR(
    () => `/api/loadDataList?params=${loadParamsString.value}`,
    async () => {
        const params = {
            pageNo: tableData.value.pageNo,
            pageSize: tableData.value.pageSize,
            fileNameFuzzy: fileNameFuzzy.value,
            filePid: currentFolder.value.fileId,
            category: category.value,
        };
        if (params.category !== "all") {
            delete params.filePid;
        }
        return await fileService.loadDataList(params, showLoading.value);
    },
    { dedupingInterval: 1500 }
);

watch(swrIsLoading, (loading) => {
    isLoading.value = loading;
    if (!tableData.value.list || tableData.value.list.length === 0 || showLoading.value) {
        tableData.value.loading = loading;
    }
    // 请求完毕后立刻撤除全局遮罩，让后续交互走静默刷新
    if (!loading) showLoading.value = false;
});

watch(swrData, (result) => {
    if (!result) {
        tableData.value.loading = false;
        return;
    }
    tableData.value.list = result.list;
    tableData.value.totalCount = result.totalCount;
    tableData.value.pageNo = result.pageNo;
    tableData.value.pageSize = result.pageSize;
    tableData.value.loading = false;

    const currentIds = new Set((tableData.value.list || []).map(item => item.fileId));
    selectFileIdList.value = selectFileIdList.value.filter(id => currentIds.has(id));
});

const loadDataList = () => {
    revalidate({ force: true });
};

// 展示操作按钮
const showOp = (row) => {
  const nextShow = !row.showOp;
  tableData.value.list.forEach((element) => {
    element.showOp = false;
  });
  row.showOp = nextShow;
};

const cancelShowOp = (row) => {
    row.showOp = false;
};

// 编辑行
const editing = ref(false);
const editNameRef = ref();
// 新建文件夹
const newFolder = () => {
    if (editing.value) {
        return;
    }
    if (viewMode.value === "icon") {
        setViewMode("list");
    }
    tableData.value.list.forEach(element => {
        element.showEdit = false;
    });
    editing.value = true;
    tableData.value.list.unshift({
        showEdit: true,
        fileType: 0,
        fileId: "",
        filePid: 0,
    });
    nextTick(() => {
        editNameRef.value.focus();
    });
};
const cancelNameEdit = (index) => {
    const fileData = tableData.value.list[index];
    if (fileData.fileId) {
        fileData.showEdit = false;
    } else {
        tableData.value.list.splice(index, 1);
    }
    editing.value = false;
};

const saveNameEdit = async (index) => {
    const { fileId, filePid, fileNameReal } = tableData.value.list[index];
    if (fileNameReal == "" || fileNameReal.indexOf("/") != -1) {
        proxy.Message.warning("文件名不能为空且不能含有斜杠");
        return;
    }

    // Optimistic UI for Rename
    const oldFileName = tableData.value.list[index].fileName;
    if (fileId != "") {
        tableData.value.list[index].fileName = fileNameReal;
    }
    // End Optimistic

    let result;
    try {
        if (fileId == "") {
            result = await fileService.newFolder({
                filePid: filePid,
                fileName: fileNameReal,
            });
        } else {
            result = await fileService.rename({
                fileId: fileId,
                fileName: fileNameReal,
            });
        }
        if (!result) {
            // Revert on null result
            if (fileId != "") {
                tableData.value.list[index].fileName = oldFileName;
            }
            return;
        }
        tableData.value.list[index] = result;
        editing.value = false;
    } catch (e) {
        // Revert on error
        if (fileId != "") {
             tableData.value.list[index].fileName = oldFileName;
        }
    }
};

const editFileName = (index) => {
    if (tableData.value.list[0].fileId == "") {
        tableData.value.list.splice(0, 1);
        index = index - 1;
    }
    tableData.value.list.forEach((element) => {
        element.showEdit = false;
    });
    const currentData = tableData.value.list[index];
    currentData.showEdit = true;
    // 编辑文件
    if (currentData.folderType == 0) {
        currentData.fileNameReal = currentData.fileName.substring(
          0,
          currentData.fileName.indexOf(".")
        );
        currentData.fileSuffix = currentData.fileName.substring(
          currentData.fileName.indexOf(".")
        );
    } else {
        currentData.fileNameReal = currentData.fileName;
        currentData.fileSuffix = "";
    }
    editing.value = true;
    nextTick(() => {
        editNameRef.value.focus();
    });
};
const openRenameFromGrid = (index) => {
  setViewMode("list");
  nextTick(() => {
    editFileName(index);
  });
};

// 多选
const selectFileIdList = ref([]);
const rowSelected = (rows) => {
  selectFileIdList.value = [];
  rows.forEach((item) => {
    selectFileIdList.value.push(item.fileId);
  });
};

// 批量选择相关计算属性
const totalFileCount = computed(() => (tableData.value.list || []).filter(item => item.fileId).length);
const isAllSelected = computed(() => {
  return totalFileCount.value > 0 && selectFileIdList.value.length === totalFileCount.value;
});
const isPartialSelected = computed(() => {
  return selectFileIdList.value.length > 0 && selectFileIdList.value.length < totalFileCount.value;
});

// 全选/反选/清除
const handleSelectAll = (value) => {
  if (value) {
    selectFileIdList.value = (tableData.value.list || [])
      .filter(item => item.fileId)
      .map(item => item.fileId);
  } else {
    selectFileIdList.value = [];
  }
};
const handleInvertSelect = () => {
  const allIds = (tableData.value.list || []).filter(item => item.fileId).map(item => item.fileId);
  const selectedSet = new Set(selectFileIdList.value);
  selectFileIdList.value = allIds.filter(id => !selectedSet.has(id));
};
const clearSelection = () => {
  selectFileIdList.value = [];
};

// 批量分享
const shareBatch = () => {
  if (selectFileIdList.value.length === 0) return;
  const firstFile = tableData.value.list.find(item => selectFileIdList.value.includes(item.fileId));
  if (firstFile) {
    shareRef.value.show(firstFile);
  }
};

// 批量下载
const downloadBatch = async () => {
  if (selectFileIdList.value.length === 0) return;
  proxy.Message.info('批量下载功能开发中，请逐个下载');
};

const isGridSelected = fileId => {
  return selectFileIdList.value.includes(fileId);
};
const toggleGridSelect = (fileId, selected) => {
  if (!fileId) {
    return;
  }
  const exists = selectFileIdList.value.includes(fileId);
  if (selected && !exists) {
    selectFileIdList.value.push(fileId);
  }
  if (!selected && exists) {
    selectFileIdList.value = selectFileIdList.value.filter(id => id !== fileId);
  }
};
// 删除
const delFile = (row) => {
  proxy.Confirm(
    `你确定要删除【${row.fileName}】吗? 删除的文件可在10天内通过回收站还原`,
    async () => {
      // Optimistic UI Delete
      const index = tableData.value.list.findIndex(item => item.fileId === row.fileId);
      if (index !== -1) {
          tableData.value.list.splice(index, 1);
      }
      
      const result = await fileService.delFile(row.fileId);
      if (!result) {
        loadDataList(); // Revert/Reload if failed
        return;
      }
      // loadDataList(); // Removed to avoid reload flash
    }
  );
};

const delFileBatch = () => {
  if (selectFileIdList.value.length == 0) {
    return;
  }
  proxy.Confirm(
    `你确定要删除这些文件吗? 删除的文件可在10天内通过回收站还原`,
    async () => {
      // Optimistic UI Batch Delete
      const ids = selectFileIdList.value;
      const backupList = [...tableData.value.list];
      tableData.value.list = tableData.value.list.filter(item => !ids.includes(item.fileId));

      const result = await fileService.delFile(selectFileIdList.value.join(","));
      if (!result) {
        tableData.value.list = backupList; // Revert
        return;
      }
      loadDataList(); // Safe reload to accept server state
    }
  );
};

const folderSelectRef = ref();
const currentMoveFile = ref({});

const moveFolder = (data) => {
  currentMoveFile.value = data;
  folderSelectRef.value.showFolderDialog(currentFolder.value.fileId);
};

const moveFolderBatch = () => {
  currentMoveFile.value = {};
  folderSelectRef.value.showFolderDialog(currentFolder.value.fileId);
};

const moveFolderDone  = async (folderId) => {
  if (currentFolder.value.fileId == folderId) {
    proxy.Message.warning("文件正在当前目录，无需移动");
    return;
  }
  let fileIdsArray = [];
  if (currentMoveFile.value.fileId) {
    fileIdsArray.push(currentMoveFile.value.fileId);
  } else {
    fileIdsArray = fileIdsArray.concat(selectFileIdList.value);
  }
  const result = await fileService.changeFileFolder({
    fileIds: fileIdsArray.join(","),
    filePid: folderId,
  });
  if (!result) {
    return;
  }
  folderSelectRef.value.close();
  loadDataList();
};

// 预览
const navigationRef = ref();
const previewRef = ref();
const preview = (data) => {
  // 目录
  if (data.folderType == 1) {
    navigationRef.value.openFolder(data);
    return;
  }
  // 文件
  if (data.status != 2) {
    proxy.Message.warning("文件未完成转码，无法预览");
    return;
  }
  previewRef.value.showPreview(data, 0);
};

const navChange = (data) => {
  const { categoryId, curFolder } = data;
  currentFolder.value = curFolder;
  category.value = categoryId;
  loadDataList();
};

// 下载文件
const download = async (row) => {
  const result = await fileService.createDownloadUrl(row.fileId);
  if (!result) {
      return;
  }
  window.location.href = fileService.getDownloadUrl(result);
};

// 分享
const shareRef = ref();
const share = (row) => {
  shareRef.value.show(row);
};

// 重试转码
const retryTranscode = (row) => {
  proxy.Message.info(`正在重新转码 ${row.fileName}，请稍后刷新查看`);
  // TODO: 调用后端转码重试接口
};

// 右键菜单
const contextMenuRef = ref();
const handleContextMenuAction = (action, item) => {
  switch (action) {
    case 'preview':
      preview(item);
      break;
    case 'share':
      share(item);
      break;
    case 'download':
      download(item);
      break;
    case 'rename': {
      const index = tableData.value.list.findIndex(f => f.fileId === item.fileId);
      if (index !== -1) editFileName(index);
      break;
    }
    case 'move':
      moveFolder(item);
      break;
    case 'delete':
      delFile(item);
      break;
    case 'properties':
      proxy.Message.info(`文件: ${item.fileName}\n大小: ${proxy.Utils.size2Str(item.fileSize || 0)}\n修改时间: ${item.lastUpdateTime}`);
      break;
  }
};

// EventBus Listener
const handleReload = () => {
    showLoading.value = false;
    loadDataList();
};

onMounted(() => {
    EventBus.on('reload_data', handleReload);
});

onUnmounted(() => {
    EventBus.off('reload_data', handleReload);
});
</script>

<style lang="scss" scoped>
@import "@/assets/file.list.scss";

.top {
  .top-op {
    .search-panel {
      display: flex;
      align-items: center;
      gap: 4px;

      .advanced-search-toggle {
        padding: 6px 8px;
        color: var(--text-light);
        border-radius: 10px;
        transition: var(--transition-fast);

        &.active {
          background: rgba(36, 95, 124, 0.16);
          color: var(--primary-dark);
        }

        &:hover {
          background: rgba(36, 95, 124, 0.1);
          color: var(--primary);
        }

        .iconfont {
          font-size: 16px;
        }
      }
    }

    .view-switch {
      display: inline-flex;
      align-items: center;
      padding: 3px;
      border-radius: 12px;
      border: 1px solid rgba(194, 204, 216, 0.9);
      background: rgba(255, 255, 255, 0.96);
      gap: 2px;
    }

    .switch-btn {
      min-width: 48px;
      height: 30px;
      padding: 0 12px;
      border: none;
      border-radius: 9px;
      background: transparent;
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: var(--transition-fast);

      &.active {
        background: rgba(36, 95, 124, 0.16);
        color: var(--primary-dark);
      }

      &:hover {
        background: rgba(36, 95, 124, 0.1);
      }
    }
  }
}

.view-switch-fade-enter-active,
.view-switch-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.view-switch-fade-enter-from,
.view-switch-fade-leave-to {
  opacity: 0;
  transform: translateY(2px) scale(0.998);
}

@media screen and (max-width: 768px) {
  .top {
    .top-op {
      flex-wrap: wrap;
      gap: 8px;
      
      .btn, .el-button {
        margin-right: 0;
        margin-bottom: 8px;
        flex: 1 1 auto;
      }
      
      .search-panel {
        width: 100%;
        margin-left: 0 !important;
        margin-top: 8px;
      }

      .view-switch {
        width: 100%;
        justify-content: space-between;
      }
      
      .icon-refresh {
          display: none;
      }
    }
  }
  
  .file-list .file-item .op {
      position: absolute;
      right: 0;
      top: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.95);
      display: flex;
      align-items: center;
      padding-right: 10px;
      z-index: 10;
      
      .iconfont {
          width: 44px;
          height: 44px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          margin-left: 0 !important;
          margin-right: 5px;
          
          &:hover {
              background: var(--bg-hover);
              border-radius: 50%;
          }
      }
  }

  .file-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 10px;
    padding: 8px 2px;

    .grid-item {
      padding: 10px 8px;
    }
  }
}

.file-list {
    position: relative;
    &.drag-over {
        border-color: var(--primary);
        box-shadow: 0 0 0 2px rgba(31, 79, 104, 0.24) inset;
    }
    
    .drop-mask {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 100;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.86);
        backdrop-filter: blur(4px);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        
        .iconfont {
            font-size: 56px;
            color: var(--primary);
            margin-bottom: 14px;
        }
        
      .text {
            font-size: 18px;
            color: var(--primary-dark);
            font-weight: 700;
            letter-spacing: 0.04em;
            text-transform: none;
        }
    }

}

.file-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 10px;
    padding: 4px;
    overflow: auto;
    align-items: start;

    .grid-item {
        position: relative;
        border-radius: 12px;
        border: 1px solid rgba(194, 204, 216, 0.88);
        background: rgba(255, 255, 255, 0.84);
        padding: 10px 8px 8px;
        transition: var(--transition-fast);
        height: fit-content;

        &:hover {
            border-color: rgba(31, 79, 104, 0.34);
            box-shadow: 0 8px 14px rgba(31, 79, 104, 0.11);
        }

        .grid-check {
            position: absolute;
            top: 6px;
            left: 6px;
            z-index: 2;
        }

        .grid-icon {
            margin-top: 4px;
            display: flex;
            justify-content: center;
            cursor: pointer;
        }

        .grid-name {
            margin-top: 6px;
            text-align: center;
            font-size: 12px;
            font-weight: 600;
            color: var(--text-main);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            cursor: pointer;
        }

        .grid-meta {
            margin-top: 3px;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 2px;
            font-size: 10px;
            color: var(--text-light);
            min-height: 24px;
        }

        .transfer-status {
            color: #8b6120;
            background: rgba(245, 158, 11, 0.16);
            border-radius: 999px;
            padding: 1px 6px;
        }

        .transfer-fail {
            color: #a22b3c;
            background: rgba(200, 60, 82, 0.14);
        }

        .grid-op {
            margin-top: 6px;
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: 3px;
            opacity: 0;
            pointer-events: none;
            transition: opacity 0.16s ease;

            .iconfont {
                padding: 3px 6px;
                font-size: 11px;
                border-radius: 8px;
                color: var(--text-secondary);
                cursor: pointer;
                background: rgba(255, 255, 255, 0.86);
                border: 1px solid rgba(194, 204, 216, 0.86);
                transition: var(--transition-fast);

                &:hover {
                    color: var(--primary-dark);
                    border-color: rgba(31, 79, 104, 0.4);
                    background: rgba(31, 79, 104, 0.1);
                }
            }
        }

        &.show-op .grid-op,
        &:hover .grid-op {
            opacity: 1;
            pointer-events: auto;
        }
    }
}
</style>

