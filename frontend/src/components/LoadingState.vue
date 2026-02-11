<template>
  <div class="loading-state-container">
    <!-- Loading 状态 -->
    <div v-if="state === 'loading'" class="state-loading">
      <div class="loading-spinner"></div>
      <p class="loading-text">{{ loadingText || '加载中...' }}</p>
    </div>

    <!-- Empty 状态 -->
    <div v-else-if="state === 'empty'" class="state-empty">
      <div class="empty-icon">📭</div>
      <p class="empty-text">{{ emptyText || '暂无数据' }}</p>
      <slot name="empty-action"></slot>
    </div>

    <!-- Error 状态 -->
    <div v-else-if="state === 'error'" class="state-error">
      <div class="error-icon">⚠️</div>
      <p class="error-text">{{ errorText || '加载失败' }}</p>
      <button v-if="showRetry" class="retry-button" @click="handleRetry">
        重试
      </button>
      <slot name="error-action"></slot>
    </div>

    <!-- Success 状态 - 显示内容 -->
    <div v-else-if="state === 'success'" class="state-success">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  state: {
    type: String,
    required: true,
    validator: (value) => ['loading', 'empty', 'error', 'success'].includes(value)
  },
  loadingText: {
    type: String,
    default: '加载中...'
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  errorText: {
    type: String,
    default: '加载失败，请稍后重试'
  },
  showRetry: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['retry']);

const handleRetry = () => {
  emit('retry');
};
</script>

<style lang="scss" scoped>
.loading-state-container {
  width: 100%;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;

  .state-loading,
  .state-empty,
  .state-error {
    text-align: center;
    padding: 40px 20px;
  }

  .state-loading {
    .loading-spinner {
      width: 40px;
      height: 40px;
      margin: 0 auto 16px;
      border: 3px solid #f3f3f3;
      border-top: 3px solid #409eff;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    .loading-text {
      color: #909399;
      font-size: 14px;
    }
  }

  .state-empty {
    .empty-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }

    .empty-text {
      color: #909399;
      font-size: 14px;
      margin-bottom: 16px;
    }
  }

  .state-error {
    .error-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }

    .error-text {
      color: #f56c6c;
      font-size: 14px;
      margin-bottom: 16px;
    }

    .retry-button {
      padding: 8px 20px;
      background-color: #409eff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;

      &:hover {
        background-color: #66b1ff;
      }

      &:active {
        background-color: #3a8ee6;
      }
    }
  }

  .state-success {
    width: 100%;
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
