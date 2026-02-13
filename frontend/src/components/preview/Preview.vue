<template>
    <PreviewImage
      v-if="fileInfo.fileCategory == 3"
      ref="imageViewRef"
      :image-list="[imageUrl]"
    >
    </PreviewImage>
    <Window
      v-else
      :show="windowShow"
      :width="fileInfo.fileCategory == 1 ? 1500 : 900"
      :title="fileInfo.fileName"
      :align="fileInfo.fileCategory == 1 ? 'center' : 'top'"
      @close="closeWindow"
    >
    <PreviewVideo v-if="fileInfo.fileCategory == 1" :url="url"></PreviewVideo>
    <PreviewDoc v-if="fileInfo.fileType == 5" :url="url"></PreviewDoc>
    <PreviewExcel v-if="fileInfo.fileType == 6" :url="url"></PreviewExcel>
    <PreviewPdf v-if="fileInfo.fileType == 4" :url="url"></PreviewPdf>
    <PreviewTxt
      v-if="fileInfo.fileType == 7 || fileInfo.fileType == 8"
      :url="url"
    ></PreviewTxt>
    <PreviewMusic
      v-if="fileInfo.fileCategory == 2"
      :url="url"
      :file-name="fileInfo.fileName"
    ></PreviewMusic>

    <PreviewDownload
      v-if="fileInfo.fileCategory == 5 && fileInfo.fileType != 8"
      :create-download-url="createDownloadUrl"
      :download-url="downloadUrl"
      :file-info="fileInfo"
    ></PreviewDownload>
    </Window>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, computed, nextTick, defineAsyncComponent } from "vue";
const PreviewVideo = defineAsyncComponent(() => import("@/components/preview/PreviewVideo.vue"));
const PreviewDoc = defineAsyncComponent(() => import("@/components/preview/PreviewDoc.vue"));
const PreviewExcel = defineAsyncComponent(() => import("@/components/preview/PreviewExcel.vue"));
const PreviewPdf = defineAsyncComponent(() => import("@/components/preview/PreviewPdf.vue"));
const PreviewTxt = defineAsyncComponent(() => import("@/components/preview/PreviewTxt.vue"));
const PreviewMusic = defineAsyncComponent(() => import("@/components/preview/PreviewMusic.vue"));
const PreviewDownload = defineAsyncComponent(() => import("@/components/preview/PreviewDownload.vue"));

import PreviewImage from "./PreviewImage.vue";

const { proxy } = getCurrentInstance();

const imageUrl = computed( () => {
    return (
        proxy.globalInfo.imageUrl + fileInfo.value.fileCover.replaceAll("_.", ".")
    );
});

const windowShow = ref(false);
const closeWindow = () => {
    windowShow.value = false;
};

const FILE_URL_MAP = {
    0: {
        fileUrl: "/file/getFile",
        videoUrl: "/file/ts/getVideoInfo",
        createDownloadUrl: "/file/createDownloadUrl",
        downloadUrl: "/api/file/download",
    },
    1: {
        fileUrl: "/admin/getFile",
        videoUrl: "/admin/ts/getVideoInfo",
        createDownloadUrl: "/admin/createDownloadUrl",
        downloadUrl: "/api/admin/download",
    },
    2: {
        fileUrl: "/showShare/getFile",
        videoUrl: "/showShare/ts/getVideoInfo",
        createDownloadUrl: "/showShare/createDownloadUrl",
        downloadUrl: "/api/showShare/download",
    },
};

const url = ref(null);
const createDownloadUrl = ref(null);
const downloadUrl = ref(null);

const fileInfo = ref({});
const imageViewRef = ref();
const showPreview = (data, showPart) => {
    fileInfo.value = data;
    if (data.fileCategory == 3) {
        nextTick( () => {
            imageViewRef.value.show(0);
        });
    } else {
        windowShow.value = true;
        let _url = FILE_URL_MAP[showPart].fileUrl;
        let _createDownloadUrl = FILE_URL_MAP[showPart].createDownloadUrl;
        const _downloadUrl = FILE_URL_MAP[showPart].downloadUrl;

        if (data.fileCategory == 1) {
            _url = FILE_URL_MAP[showPart].videoUrl;
        }
        if (showPart == 0) {
            _url = _url + "/" + data.fileId;
            _createDownloadUrl = _createDownloadUrl + "/" + data.fileId;
        } else if (showPart == 1) {
            _url = _url + "/" + data.userId + "/" + data.fileId;
            _createDownloadUrl =
              _createDownloadUrl + "/" + data.userId + "/" + data.fileId;
        } else if (showPart == 2) {
            _url = _url + "/" + data.shareId + "/" + data.fileId;
            _createDownloadUrl =
              _createDownloadUrl + "/" + data.shareId + "/" + data.fileId;
        }
        url.value = _url;
        createDownloadUrl.value = _createDownloadUrl;
        downloadUrl.value = _downloadUrl;
    }
};

defineExpose({ showPreview });
</script>

<style lang="scss" scoped>
</style>
