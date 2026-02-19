<template>
    <span 
        :style="{ width: width + 'px', height: width + 'px' }" 
        :class="['icon', { 'icon-hoverable': hoverable, 'icon-loading': loading }]"
        @mouseenter="$emit('hover', true)"
        @mouseleave="$emit('hover', false)"
    >
        <img v-if="cover" v-lazy="getImage()" :style="{ 'object-fit': fit }" @error="handleImageError" />
        <img v-else :src="getImage()" :style="{ 'object-fit': fit }" @error="handleImageError" />
        <div v-if="loading" class="icon-loading-overlay">
            <div class="loading-spinner-small"></div>
        </div>
        <div v-if="showBadge" class="icon-badge">{{ badgeText }}</div>
    </span>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
    fileType: {
        type: Number,
    },
    iconName: {
        type: String,
    },
    cover: {
        type: String,
    },
    width: {
        type: Number,
        default: 32,
    },
    fit: {
        type: String,
        default: "cover",
    },
    shareId: {
        type: String,
    },
    adminUserId: {
        type: String,
    },
    hoverable: {
        type: Boolean,
        default: false,
    },
    loading: {
        type: Boolean,
        default: false,
    },
    showBadge: {
        type: Boolean,
        default: false,
    },
    badgeText: {
        type: String,
        default: "",
    },
});

const emit = defineEmits(["hover"]);

const fileTypeMap = {
    0: { desc: "目录", icon: "folder" },
    1: { desc: "视频", icon: "video" },
    2: { desc: "音频", icon: "music" },
    3: { desc: "图片", icon: "image" },
    4: { desc: "exe", icon: "pdf" },
    5: { desc: "doc", icon: "word" },
    6: { desc: "excel", icon: "excel" },
    7: { desc: "纯文本", icon: "txt" },
    8: { desc: "程序", icon: "code" },
    9: { desc: "压缩包", icon: "zip" },
    10: { desc: "其他文件", icon: "others" },
};

const imageError = ref(false);

const getImage = () => {
    if (props.cover && !imageError.value) {
        if (props.shareId) {
            return `/api/showShare/getImage/${props.shareId}/${props.cover}`;
        }
        if (props.adminUserId) {
            return `/api/admin/getImage/${props.adminUserId}/${props.cover}`;
        }
        // 获取全局配置需要通过 inject 或其他方式
        return `/api/file/getImage/${props.cover}`;
    }
    let icon = "unknow_icon";
    if (props.iconName) {
        icon = props.iconName;
    } else {
        const iconMap = fileTypeMap[props.fileType];
        if (iconMap != undefined) {
            icon = iconMap["icon"];
        }
    }
    return new URL(`/src/assets/icon-image/${icon}.png`, import.meta.url).href;
};

const handleImageError = () => {
    imageError.value = true;
};
</script>

<style lang="scss" scoped>
.icon {
    text-align: center;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.62);
    border: 1px solid rgba(189, 208, 202, 0.58);
    position: relative;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    img {
        width: 100%;
        height: 100%;
        transition: transform 0.2s ease;
    }

    &.icon-hoverable:hover {
        transform: scale(1.08);
        box-shadow: 0 4px 12px rgba(31, 79, 104, 0.2);
        
        img {
            transform: scale(1.05);
        }
    }

    &.icon-loading {
        .icon-loading-overlay {
            position: absolute;
            inset: 0;
            background: rgba(255, 255, 255, 0.8);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .loading-spinner-small {
            width: 16px;
            height: 16px;
            border: 2px solid rgba(31, 79, 104, 0.2);
            border-top-color: var(--primary);
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }
    }

    .icon-badge {
        position: absolute;
        top: -4px;
        right: -4px;
        min-width: 16px;
        height: 16px;
        padding: 0 4px;
        font-size: 10px;
        font-weight: 600;
        color: #fff;
        background: var(--primary);
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}

@keyframes spin {
    to { transform: rotate(360deg); }
}
</style>
