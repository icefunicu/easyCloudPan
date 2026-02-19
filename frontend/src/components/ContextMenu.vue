<template>
  <Teleport to="body">
    <Transition name="context-menu">
      <div
        v-if="visible"
        ref="menuRef"
        class="context-menu"
        :style="menuStyle"
        @click.stop
      >
        <div class="menu-item" @click="handleAction('preview')">
          <span class="iconfont icon-eye"></span>
          <span>预览</span>
        </div>
        <div class="menu-item" @click="handleAction('share')">
          <span class="iconfont icon-share1"></span>
          <span>分享</span>
        </div>
        <div v-if="!isFolder" class="menu-item" @click="handleAction('download')">
          <span class="iconfont icon-download"></span>
          <span>下载</span>
        </div>
        <div class="menu-divider"></div>
        <div class="menu-item" @click="handleAction('rename')">
          <span class="iconfont icon-edit"></span>
          <span>重命名</span>
        </div>
        <div class="menu-item" @click="handleAction('move')">
          <span class="iconfont icon-move"></span>
          <span>移动到</span>
        </div>
        <div class="menu-divider"></div>
        <div class="menu-item danger" @click="handleAction('delete')">
          <span class="iconfont icon-del"></span>
          <span>删除</span>
        </div>
        <div class="menu-divider"></div>
        <div class="menu-item" @click="handleAction('properties')">
          <span class="iconfont icon-info"></span>
          <span>属性</span>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';

const visible = ref(false);
const position = ref({ x: 0, y: 0 });
const menuRef = ref<HTMLElement | null>(null);
const currentItem = ref<Record<string, unknown> | null>(null);
const isFolder = computed(() => currentItem.value?.folderType === 1);

const menuStyle = computed(() => ({
  left: `${position.value.x}px`,
  top: `${position.value.y}px`,
}));

const emit = defineEmits<{
  (e: 'action', action: string, item: Record<string, unknown>): void;
}>();

const show = (event: MouseEvent, item: Record<string, unknown>) => {
  event.preventDefault();
  currentItem.value = item;
  
  // Calculate position to avoid overflow
  let x = event.clientX;
  let y = event.clientY;
  
  // Adjust if near right edge
  if (x + 180 > window.innerWidth) {
    x = window.innerWidth - 190;
  }
  
  // Adjust if near bottom edge
  if (y + 280 > window.innerHeight) {
    y = window.innerHeight - 290;
  }
  
  position.value = { x, y };
  visible.value = true;
};

const hide = () => {
  visible.value = false;
  currentItem.value = null;
};

const handleAction = (action: string) => {
  if (currentItem.value) {
    emit('action', action, currentItem.value);
  }
  hide();
};

// Close on click outside
const handleClickOutside = (event: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    hide();
  }
};

// Close on escape key
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    hide();
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
  document.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
  document.removeEventListener('keydown', handleKeydown);
});

defineExpose({
  show,
  hide,
});
</script>

<style lang="scss" scoped>
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 160px;
  padding: 6px 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(194, 204, 216, 0.88);
  box-shadow: 0 12px 32px rgba(18, 35, 49, 0.18);
  backdrop-filter: blur(12px);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: var(--transition-fast);

  .iconfont {
    font-size: 14px;
    width: 18px;
    text-align: center;
  }

  &:hover {
    background: rgba(31, 79, 104, 0.08);
    color: var(--text-main);
  }

  &.danger {
    color: var(--danger);

    &:hover {
      background: rgba(178, 81, 81, 0.1);
    }
  }
}

.menu-divider {
  height: 1px;
  margin: 4px 8px;
  background: rgba(194, 204, 216, 0.6);
}

// Animation
.context-menu-enter-active,
.context-menu-leave-active {
  transition: all 0.15s ease;
}

.context-menu-enter-from,
.context-menu-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-4px);
}
</style>
