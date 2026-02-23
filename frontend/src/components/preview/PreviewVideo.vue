<template>
  <div class="video-preview-container">
    <div v-if="loading" class="video-loading">
      <div class="loading-spinner"></div>
      <span>{{ loadingText }}</span>
    </div>
    <div v-if="error" class="video-error">
      <span class="iconfont icon-error"></span>
      <span>{{ errorMessage }}</span>
      <el-button size="small" @click="retryLoad">重试</el-button>
    </div>
    <div id="player" ref="player" :class="{ 'player-ready': !loading && !error }"></div>
  </div>
</template>

<script setup lang="ts">
import DPlayer from 'dplayer'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserInfoStore } from '@/stores/userInfoStore'
import { toApiPath } from '@/utils/url'
import type Hls from 'hls.js'

type HlsConstructor = typeof Hls
type HlsInstance = InstanceType<HlsConstructor>

interface DPlayerInstance {
  video?: HTMLVideoElement
  fullScreen: { toggle(mode: string): void }
  on(event: string, handler: (...args: unknown[]) => void): void
  toggle(): void
  seek(time: number): void
  destroy(): void
}

const props = defineProps({
  url: {
    type: String,
    default: '',
  },
})

const player = ref<HTMLElement | null>(null)
const loading = ref(true)
const error = ref(false)
const errorMessage = ref('视频加载失败')
const bufferPercent = ref(0)

let dpInstance: DPlayerInstance | null = null
let hlsInstance: HlsInstance | null = null
let hlsConstructor: HlsConstructor | null = null
let hlsConstructorPromise: Promise<HlsConstructor> | null = null
let loadTimeoutTimer: ReturnType<typeof setTimeout> | null = null
let mediaErrorRecoveryAttempts = 0

const MAX_RECOVERY_ATTEMPTS = 3
const userInfoStore = useUserInfoStore()

const loadingText = computed(() => {
  if (bufferPercent.value > 0 && bufferPercent.value < 100) {
    return `加载视频中... ${bufferPercent.value}%`
  }
  return '加载视频中...'
})

const getAdaptiveBufferConfig = () => {
  const connection = (navigator as Navigator & { connection?: { downlink?: number } }).connection
  if (connection) {
    const downlink = connection.downlink || 10
    if (downlink < 1) {
      return { maxBufferLength: 10, maxMaxBufferLength: 30 }
    }
    if (downlink < 5) {
      return { maxBufferLength: 20, maxMaxBufferLength: 45 }
    }
  }
  return { maxBufferLength: 30, maxMaxBufferLength: 60 }
}

const loadHlsConstructor = async (): Promise<HlsConstructor> => {
  if (hlsConstructor) {
    return hlsConstructor
  }
  if (!hlsConstructorPromise) {
    hlsConstructorPromise = import('hls.js/dist/hls.light.mjs').then(module => {
      hlsConstructor = module.default
      return module.default
    })
  }
  return hlsConstructorPromise
}

const handleHlsError = (_event: string, data: { fatal: boolean; type: string; details: string }) => {
  if (!data.fatal) {
    return
  }

  const HlsCtor = hlsConstructor
  if (!HlsCtor) {
    return
  }

  if (data.type === HlsCtor.ErrorTypes.MEDIA_ERROR && mediaErrorRecoveryAttempts < MAX_RECOVERY_ATTEMPTS) {
    mediaErrorRecoveryAttempts += 1
    if (mediaErrorRecoveryAttempts === 1) {
      hlsInstance?.recoverMediaError()
    } else if (mediaErrorRecoveryAttempts === 2) {
      hlsInstance?.swapAudioCodec()
      hlsInstance?.recoverMediaError()
    } else {
      hlsInstance?.startLoad()
    }
    return
  }

  if (data.type === HlsCtor.ErrorTypes.NETWORK_ERROR && mediaErrorRecoveryAttempts < MAX_RECOVERY_ATTEMPTS) {
    mediaErrorRecoveryAttempts += 1
    setTimeout(() => {
      hlsInstance?.startLoad()
    }, 1000)
    return
  }

  errorMessage.value =
    data.type === HlsCtor.ErrorTypes.NETWORK_ERROR
      ? '网络连接失败，请检查网络后重试'
      : '视频格式不支持或文件已损坏'
  error.value = true
  loading.value = false
}

const clearLoadTimeout = () => {
  if (loadTimeoutTimer) {
    clearTimeout(loadTimeoutTimer)
    loadTimeoutTimer = null
  }
}

const initPlayer = async () => {
  loading.value = true
  error.value = false
  mediaErrorRecoveryAttempts = 0
  bufferPercent.value = 0

  if (!props.url || !player.value) {
    error.value = true
    loading.value = false
    errorMessage.value = '视频地址无效'
    return
  }

  try {
    if (dpInstance) {
      dpInstance.destroy()
      dpInstance = null
    }
    if (hlsInstance) {
      hlsInstance.destroy()
      hlsInstance = null
    }

    const videoUrl = toApiPath(props.url)
    const HlsCtor = await loadHlsConstructor()
    const bufferConfig = getAdaptiveBufferConfig()

    dpInstance = new DPlayer({
      element: player.value,
      theme: '#1f7e9f',
      screenshot: true,
      autoplay: false,
      preload: 'auto',
      volume: 0.7,
      video: {
        url: videoUrl,
        type: 'customHls',
        customType: {
          customHls(video: HTMLVideoElement) {
            const hls = new HlsCtor({
              ...bufferConfig,
              startLevel: -1,
              xhrSetup(xhr: XMLHttpRequest) {
                const token = userInfoStore.getToken()
                if (token) {
                  xhr.setRequestHeader('Authorization', `Bearer ${token}`)
                }
              },
            })
            hls.loadSource(video.src)
            hls.attachMedia(video)
            hls.on(HlsCtor.Events.ERROR, handleHlsError)
            hlsInstance = hls
          },
        },
      },
    }) as unknown as DPlayerInstance

    dpInstance.on('canplay', () => {
      loading.value = false
      clearLoadTimeout()
    })

    dpInstance.on('error', () => {
      if (mediaErrorRecoveryAttempts < MAX_RECOVERY_ATTEMPTS) {
        return
      }
      error.value = true
      loading.value = false
    })

    if (dpInstance.video) {
      dpInstance.video.addEventListener('progress', () => {
        const video = dpInstance?.video
        if (video && video.buffered.length > 0 && video.duration > 0) {
          bufferPercent.value = Math.round((video.buffered.end(video.buffered.length - 1) / video.duration) * 100)
        }
      })
    }

    loadTimeoutTimer = setTimeout(() => {
      if (loading.value && !error.value) {
        loading.value = false
      }
    }, 15000)
  } catch {
    error.value = true
    loading.value = false
    errorMessage.value = '播放器初始化失败'
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (!dpInstance || !dpInstance.video) {
    return
  }

  const target = e.target as HTMLElement
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
    return
  }

  switch (e.key) {
    case ' ':
    case 'Spacebar':
      e.preventDefault()
      dpInstance.toggle()
      break
    case 'ArrowLeft':
      e.preventDefault()
      dpInstance.seek(Math.max(0, dpInstance.video.currentTime - 5))
      break
    case 'ArrowRight':
      e.preventDefault()
      dpInstance.seek(Math.min(dpInstance.video.duration, dpInstance.video.currentTime + 5))
      break
    case 'f':
    case 'F':
      e.preventDefault()
      dpInstance.fullScreen.toggle('web')
      break
  }
}

const retryLoad = () => {
  initPlayer()
}

onMounted(() => {
  initPlayer()
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  clearLoadTimeout()
  if (hlsInstance) {
    hlsInstance.destroy()
    hlsInstance = null
  }
  if (dpInstance) {
    dpInstance.destroy()
    dpInstance = null
  }
})
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
  to {
    transform: rotate(360deg);
  }
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
