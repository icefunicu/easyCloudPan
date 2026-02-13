<template>
    <div id="player" ref="player"></div>
</template>

<script setup>
import DPlayer from "dplayer";
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from "vue";
const { proxy } = getCurrentInstance();

const props = defineProps({
    url: {
        type: String,
    },
});

const videoInfo = ref({
    video: null,
});

const player = ref();

const loadHlsScript = () => {
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
    await loadHlsScript();
    const dp = new DPlayer({
        element: player.value,
        theme: "#b7daff",
        screenshot: true,
        video: {
            url: `/api${props.url}`,
            type: "customHls",
            customType: {
                customHls: function(video, player) {
                    const hls = new window.Hls();
                    hls.loadSource(video.src);
                    hls.attachMedia(video);
                },
            },
        },
    });
};

onMounted(() => {
    initPlayer();
});
</script>

<style lang="scss" scoped>
#player {
    width: 100%;
    :deep .dplayer-video-wrap {
        text-align: center;
        .dplayer-video {
            margin: 0px auto;
            max-height: calc(100vh - 41px);
        }
    }
}
</style>
