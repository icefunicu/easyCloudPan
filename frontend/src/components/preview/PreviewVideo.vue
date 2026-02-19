<template>
    <div class="video-preview-container">
        <div v-if="loading" class="video-loading">
            <div class="loading-spinner"></div>
            <span>加载视频中...</span>
        </div>
        <div v-if="error" class="video-error">
            <span class="iconfont icon-error"></span>
            <span>视频加载失败</span>
            <el-button size="small" @click="retryLoad">重试</el-button>
        </div>
        <div id="player" ref="player" :class="{ 'player-ready': !loading && !error }"></div>
    </div>
</template>

<script setup lang="ts">
import DPlayer from "dplayer";
import { ref, onMounted, onBeforeUnmount } from "vue";

interface HlsInstance {
  loadSource(url: string): void
  attachMedia(video: HTMLVideoElement): void
  on(event: string, handler: (event: string, data: { fatal: boolean }) => void): void
  Events: { ERROR: string }
}

interface HlsConstructor {
  new (options?: { maxBufferLength?: number; maxMaxBufferLength?: number }): HlsInstance
  Events: { ERROR: string }
}

declare global {
  interface Window {
    Hls?: HlsConstructor
  }
}

const props = defineProps({
    url: {
        type: String,
    },
});

const player = ref<HTMLElement | null>(null);
const loading = ref(true);
const error = ref(false);
let dpInstance: DPlayer | null = null;

const loadHlsScript = (): Promise<void> => {
    return new Promise((resolve, reject) => {
        if (window.Hls) {
            resolve();
            return;
        }
        const script = document.createElement("script");
        script.src = "/hls.min.js";
        script.onload = () => resolve();
        script.onerror = () => reject(new Error("Failed to load HLS script"));
        document.head.appendChild(script);
    });
};

const initPlayer = async () => {
    loading.value = true;
    error.value = false;
    
    try {
        await loadHlsScript();
        
        if (dpInstance) {
            dpInstance.destroy();
            dpInstance = null;
        }
        
        const Hls = window.Hls!
        dpInstance = new DPlayer({
            element: player.value!,
            theme: "#1f7e9f",
            screenshot: true,
            autoplay: false,
            preload: "auto",
            volume: 0.7,
            video: {
                url: `/api${props.url}`,
                type: "customHls",
                customType: {
                    customHls: function(video: HTMLVideoElement, _player: unknown) {
                        const hls = new Hls({
                            maxBufferLength: 30,
                            maxMaxBufferLength: 60,
                        });
                        hls.loadSource(video.src);
                        hls.attachMedia(video);
                        hls.on(Hls.Events.ERROR, (_event: string, data: { fatal: boolean }) => {
                            if (data.fatal) {
                                error.value = true;
                                loading.value = false;
                            }
                        });
                    },
                },
            },
        });
        
        // 监听视频事件
        dpInstance.on("canplay", () => {
            loading.value = false;
        });
        
        dpInstance.on("error", () => {
            error.value = true;
            loading.value = false;
        });
        
        // 超时处理
        setTimeout(() => {
            if (loading.value) {
                loading.value = false;
            }
        }, 8000);
        
    } catch {
        error.value = true;
        loading.value = false;
    }
};

const retryLoad = () => {
    initPlayer();
};

onMounted(() => {
    initPlayer();
});

onBeforeUnmount(() => {
    if (dpInstance) {
        dpInstance.destroy();
        dpInstance = null;
    }
});
</script>

<style lang="scss" scoped>
.video-preview-container {
    width: 100%;
    padding: 6px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(189, 208, 202, 0.78);
    box-shadow: var(--shadow-sm);
    position: relative;
    min-height: 200px;
    flex: 1;
    display: flex;
    flex-direction: column;
}

.video-loading,
.video-error {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    color: var(--text-secondary);
    z-index: 10;
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

.video-error {
    .iconfont {
        font-size: 32px;
        color: var(--danger);
    }
}

#player {
    width: 100%;
    flex: 1;
    opacity: 0;
    transition: opacity 0.3s ease;
    
    &.player-ready {
        opacity: 1;
    }

    :deep(.dplayer-video-wrap) {
        text-align: center;
        background: #000;
        border-radius: 8px;
        overflow: hidden;
        height: 100%;

        .dplayer-video {
            margin: 0 auto;
            max-height: calc(100vh - 180px);
            width: auto;
            max-width: 100%;
        }
    }
    
    :deep(.dplayer-controller) {
        border-radius: 0 0 8px 8px;
    }
    
    :deep(.dplayer-notice) {
        border-radius: 6px;
    }
}

// Mobile responsive
@media screen and (max-width: 768px) {
    .video-preview-container {
        padding: 4px;
        min-height: 150px;
    }

    #player :deep(.dplayer-video-wrap) .dplayer-video {
        max-height: calc(100vh - 140px);
    }
}
</style>
