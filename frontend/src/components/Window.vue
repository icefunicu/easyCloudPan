<template>
    <Teleport to="body">
        <Transition name="window-fade">
            <div v-if="show" class="window">
                <Transition name="mask-fade">
                    <div v-if="show" class="window-mask" @click="close"></div>
                </Transition>
                <button type="button" class="close" @click="close" aria-label="关闭">
                    <span class="iconfont icon-close2"></span>
                </button>
                <Transition name="window-slide">
                    <div
                        v-if="show"
                        class="window-content"
                        :style="contentStyle"
                    >
                        <div class="title">
                            <span class="title-text">{{ title }}</span>
                        </div>
                        <div class="content-body" :style="{ alignItems: align }">
                            <slot></slot>
                        </div>
                    </div>
                </Transition>
            </div>
        </Transition>
    </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";

const props = defineProps({
    show: {
        type: Boolean,
    },
    width: {
        type: Number,
        default: 1000,
    },
    title: {
        type: String,
    },
    align: {
        type: String,
        default: "top",
    },
    maxWidth: {
        type: Number,
        default: 0, // 0 means no limit
    },
    padding: {
        type: Number,
        default: 16, // padding from screen edge on mobile
    },
});

const emit = defineEmits(["close"]);

const windowWidth = ref(window.innerWidth);
const windowHeight = ref(window.innerHeight);

const isMobile = computed(() => windowWidth.value < 768);

const effectiveWidth = computed(() => {
    const maxAllowed = props.maxWidth > 0 ? props.maxWidth : props.width;
    const available = windowWidth.value - (isMobile.value ? props.padding * 2 : 0);
    return Math.min(props.width, maxAllowed, available);
});

const windowContentWidth = computed(() => {
    if (isMobile.value) {
        return windowWidth.value - props.padding * 2;
    }
    return effectiveWidth.value;
});

const windowContentLeft = computed(() => {
    if (isMobile.value) {
        return props.padding;
    }
    const left = windowWidth.value - windowContentWidth.value;
    return Math.max(0, left / 2);
});

const contentStyle = computed(() => ({
    top: isMobile.value ? `${props.padding}px` : '0px',
    left: `${windowContentLeft.value}px`,
    width: `${windowContentWidth.value}px`,
    maxHeight: isMobile.value ? `${windowHeight.value - props.padding * 2}px` : 'none',
}));

const close = () => {
    emit("close");
};

const handleKeydown = (e) => {
    if (e.key === 'Escape' && props.show) {
        close();
    }
};

const resizeWindow = () => {
    windowWidth.value = window.innerWidth;
    windowHeight.value = window.innerHeight;
};

onMounted(() => {
    window.addEventListener("resize", resizeWindow);
    window.addEventListener("keydown", handleKeydown);
});

onUnmounted(() => {
    window.removeEventListener("resize", resizeWindow);
    window.removeEventListener("keydown", handleKeydown);
});
</script>

<style lang="scss" scoped>
.window {
    position: fixed;
    inset: 0;
    z-index: 200;
    display: flex;
    align-items: flex-start;
    justify-content: center;
}

.window-mask {
    position: fixed;
    inset: 0;
    z-index: 200;
    background: rgba(16, 24, 45, 0.46);
    backdrop-filter: blur(4px);
}

.close {
    z-index: 202;
    cursor: pointer;
    position: fixed;
    top: 24px;
    right: 24px;
    width: 44px;
    height: 44px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(194, 204, 220, 0.9);
    box-shadow: var(--shadow-sm);
    display: flex;
    justify-content: center;
    align-items: center;
    transition: var(--transition-fast);

    &:hover {
        transform: translateY(-1px);
        border-color: var(--primary-light);
    }

    &:focus-visible {
        outline: 2px solid var(--primary);
        outline-offset: 2px;
    }

    .iconfont {
        font-size: 20px;
        color: var(--text-main);
    }
}

.window-content {
    z-index: 201;
    position: fixed;
    border-radius: 18px;
    overflow: hidden;
    border: 1px solid rgba(194, 204, 220, 0.88);
    box-shadow: var(--shadow-lg);
    background: rgba(255, 255, 255, 0.92);
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 32px);

    .title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 18px;
        border-bottom: 1px solid rgba(194, 204, 220, 0.72);
        background: rgba(246, 248, 255, 0.86);
        flex-shrink: 0;

        .title-text {
            font-weight: 700;
            font-family: var(--font-heading);
            color: var(--text-main);
            letter-spacing: 0.04em;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .content-body {
        flex: 1;
        display: flex;
        overflow: auto;
        background: rgba(255, 255, 255, 0.9);
        min-height: 0;
    }
}

// Animations
.window-fade-enter-active,
.window-fade-leave-active {
    transition: opacity 0.28s ease;
}

.window-fade-enter-from,
.window-fade-leave-to {
    opacity: 0;
}

.mask-fade-enter-active,
.mask-fade-leave-active {
    transition: opacity 0.24s ease;
}

.mask-fade-enter-from,
.mask-fade-leave-to {
    opacity: 0;
}

.window-slide-enter-active,
.window-slide-leave-active {
    transition: all 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.window-slide-enter-from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
}

.window-slide-leave-to {
    opacity: 0;
    transform: translateY(-10px) scale(0.99);
}

// Mobile styles
@media screen and (max-width: 768px) {
    .close {
        top: 16px;
        right: 16px;
        width: 40px;
        height: 40px;
        border-radius: 12px;
    }

    .window-content {
        border-radius: 16px;
        max-height: calc(100vh - 32px) !important;

        .title {
            padding: 10px 14px;

            .title-text {
                font-size: 15px;
            }
        }
    }
}

@media screen and (max-width: 480px) {
    .close {
        top: 12px;
        right: 12px;
        width: 36px;
        height: 36px;
    }

    .window-content {
        border-radius: 14px;

        .title {
            padding: 10px 12px;
        }
    }
}
</style>
