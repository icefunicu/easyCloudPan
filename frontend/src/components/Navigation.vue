<template>
    <div class="top-navigation">
        <template v-if="folderList.length > 0">
            <span class="back link" @click="backParent">返回上一级</span>
            <el-divider direction="vertical"></el-divider>
        </template>
        <span v-if="folderList.length == 0" class="all-file">全部文件</span>
        <span
          v-if="folderList.length > 0"
          class="link"
          @click="setCurrentFolder(-1)"
        >
          全部文件
        </span>
        <template v-for="(item, index) in folderList" :key="item.fileId || index">
            <span class="iconfont icon-right"></span>
            <span
              v-if="index < folderList.length - 1"
              class="link"
              @click="setCurrentFolder(index)"
              >{{ item.fileName }}</span
            >
            <span v-if="index == folderList.length - 1" class="text">{{
              item.fileName
            }}</span>
        </template>
    </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { getFileFolderInfo, getShareFolderInfo, getAdminFolderInfo } from "@/services";
import { useRouter, useRoute } from "vue-router";
const router = useRouter();
const route = useRoute();

const props = defineProps({
    watchPath: {
        type: Boolean,
        default: true,
    },
    shareId: {
        type: String,
    },
    adminShow: {
        type: Boolean,
        default: false,
    },
});

// 分类
const category = ref();
// 目录集合
const folderList = ref([]);
// 当前目录
const currentFolder = ref({fileId: "0"});

const init = () => {
    folderList.value = [];
    currentFolder.value = {fileId: "0"};
    doCallback();
};

const openFolder = (data) => {
    const { fileId, fileName } = data;
    const folder = {
        fileName: fileName,
        fileId: fileId,
    };
    folderList.value.push(folder);
    currentFolder.value = folder;
    setPath();
};
defineExpose({ openFolder });

// 返回上一级
const backParent = () => {
    let currentIndex = null;
    for (let i = 0; i < folderList.value.length; i++) {
        if (folderList.value[i].fileId == currentFolder.value.fileId) {
            currentIndex = i;
            break;
        }
    }
    setCurrentFolder(currentIndex - 1);
};
// 点击导航  设置当前目录
const setCurrentFolder = (index) => {
    if (index == -1) {
      // 返回全部
      currentFolder.value = { fileId: "0" };
      folderList.value = [];
    } else {
        currentFolder.value = folderList.value[index];
        folderList.value.splice(index + 1, folderList.value.length);
    }
    setPath();
};

const setPath = () => {
    if (!props.watchPath) {
        // TODO 设置不监听路由回调方法
        doCallback();
        return;
    }
    const pathArray = [];
    folderList.value.forEach(item => {
        pathArray.push(item.fileId);
    })
    router.push({
        path: route.path,
        query: pathArray.length == 0 ? "" : { path: pathArray.join("/") },
    });
};

// 获取当前路径的目录
const getNavigationFolder = async (path) => {
    let result = null;
    if (props.shareId) {
        result = await getShareFolderInfo(props.shareId, path);
    } else if (props.adminShow) {
        result = await getAdminFolderInfo(path);
    } else {
        result = await getFileFolderInfo(path);
    }
    if (!result) {
        return;
    }
    folderList.value = result;
};

const emit = defineEmits(["navChange"]);
const doCallback = () => {
    emit("navChange", {
        categoryId: category.value,
        curFolder: currentFolder.value,
    });
};

watch(
    () => [route.query.path, route.params.category, route.path],
    (newValues, oldValues) => {
        if (!props.watchPath) {
            return;
        }
        const [path, categoryId, routePath] = newValues;
        
        if (routePath.indexOf("/main") === -1 &&
            routePath.indexOf("/settings/fileList") === -1 &&
            routePath.indexOf("/share") === -1
        ) {
            return;
        }
        category.value = categoryId;
        if (path == undefined) {
            init();
        } else {
            getNavigationFolder(path);
            const pathArray = path.split("/");
            currentFolder.value = {
                fileId: pathArray[pathArray.length - 1],
            };
            doCallback();
        }
    },
    { immediate: true, deep: false }
);
</script>

<style lang="scss" scoped>
.top-navigation {
    font-size: 14px;
    display: flex;
    align-items: center;
    line-height: 40px;
    color: var(--text-secondary);
    
    .all-file {
        font-weight: 600;
        color: var(--text-main);
    }
    
    .link {
        color: var(--primary);
        cursor: pointer;
        transition: var(--transition-fast);
        
        &:hover {
            color: var(--primary-light);
            text-decoration: underline;
        }
    }
    
    .icon-right {
        color: var(--text-light);
        padding: 0px 8px;
        font-size: 12px;
    }
    
    .text {
        color: var(--text-secondary);
        font-weight: 500;
        cursor: default;
    }
}
</style>
