<template>
  <Transition name="expand">
    <div v-if="visible" class="advanced-search">
      <div class="search-header">
        <span class="title">高级搜索</span>
        <el-button text size="small" @click="handleReset">
          <span class="iconfont icon-refresh"></span>
          重置
        </el-button>
      </div>
      <div class="search-filters">
        <!-- 文件类型 -->
        <div class="filter-item">
          <label>文件类型</label>
          <el-select
            v-model="filters.fileType"
            clearable
            placeholder="全部类型"
            size="small"
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in fileTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <!-- 日期范围 -->
        <div class="filter-item">
          <label>修改时间</label>
          <el-select
            v-model="filters.dateRange"
            clearable
            placeholder="全部时间"
            size="small"
            @change="handleFilterChange"
          >
            <el-option label="今天" value="today" />
            <el-option label="近7天" value="week" />
            <el-option label="近30天" value="month" />
            <el-option label="近3个月" value="quarter" />
          </el-select>
        </div>
        <!-- 大小范围 -->
        <div class="filter-item">
          <label>文件大小</label>
          <el-select
            v-model="filters.sizeRange"
            clearable
            placeholder="全部大小"
            size="small"
            @change="handleFilterChange"
          >
            <el-option label="< 10MB" value="small" />
            <el-option label="10MB - 100MB" value="medium" />
            <el-option label="100MB - 1GB" value="large" />
            <el-option label="> 1GB" value="huge" />
          </el-select>
        </div>
        <!-- 排序方式 -->
        <div class="filter-item">
          <label>排序</label>
          <div class="sort-group">
            <el-select
              v-model="filters.sortBy"
              size="small"
              @change="handleFilterChange"
            >
              <el-option label="名称" value="name" />
              <el-option label="修改时间" value="time" />
              <el-option label="大小" value="size" />
              <el-option label="类型" value="type" />
            </el-select>
            <el-button
              :class="['sort-order-btn', filters.sortOrder === 'desc' ? 'active' : '']"
              size="small"
              @click="toggleSortOrder"
            >
              <span class="iconfont" :class="filters.sortOrder === 'asc' ? 'icon-sort-asc' : 'icon-sort-desc'"></span>
            </el-button>
          </div>
        </div>
      </div>
      <div class="search-actions">
        <el-button size="small" @click="handleReset">重置</el-button>
        <el-button type="primary" size="small" @click="handleSearch">
          <span class="iconfont icon-search"></span>
          搜索
        </el-button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue';

const props = defineProps<{
  visible: boolean;
  category?: string;
}>();

const emit = defineEmits<{
  (e: 'search', filters: SearchFilters): void;
  (e: 'reset'): void;
}>();

interface SearchFilters {
  fileType: string;
  dateRange: string;
  sizeRange: string;
  sortBy: string;
  sortOrder: 'asc' | 'desc';
}

const filters = reactive<SearchFilters>({
  fileType: '',
  dateRange: '',
  sizeRange: '',
  sortBy: 'time',
  sortOrder: 'desc',
});

const fileTypeOptions = [
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '图片', value: 'image' },
  { label: '文档', value: 'doc' },
  { label: '其他', value: 'other' },
];

const toggleSortOrder = () => {
  filters.sortOrder = filters.sortOrder === 'asc' ? 'desc' : 'asc';
  handleFilterChange();
};

const handleFilterChange = () => {
  // 可选：实时搜索
};

const handleSearch = () => {
  emit('search', { ...filters });
};

const handleReset = () => {
  filters.fileType = '';
  filters.dateRange = '';
  filters.sizeRange = '';
  filters.sortBy = 'time';
  filters.sortOrder = 'desc';
  emit('reset');
};

// 当 category 变化时重置筛选
watch(() => props.category, () => {
  handleReset();
});
</script>

<style lang="scss" scoped>
.advanced-search {
  padding: 12px 14px;
  margin-top: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(194, 204, 216, 0.88);
  box-shadow: var(--shadow-sm);
}

.search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  .title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-main);
  }

  .el-button {
    font-size: 12px;
    color: var(--text-light);

    &:hover {
      color: var(--primary);
    }
  }
}

.search-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;

  label {
    font-size: 12px;
    color: var(--text-light);
    font-weight: 500;
  }

  .el-select {
    width: 140px;
  }

  .sort-group {
    display: flex;
    gap: 4px;

    .el-select {
      flex: 1;
    }

    .sort-order-btn {
      width: 32px;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      &.active {
        background: rgba(36, 95, 124, 0.16);
        color: var(--primary-dark);
      }

      .iconfont {
        font-size: 14px;
      }
    }
  }
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(194, 204, 216, 0.5);

  .el-button {
    min-width: 72px;
  }
}

// 展开动画
.expand-enter-active,
.expand-leave-active {
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 200px;
}

// 移动端适配
@media screen and (max-width: 768px) {
  .search-filters {
    flex-direction: column;
    gap: 10px;
  }

  .filter-item {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    label {
      flex-shrink: 0;
      width: 70px;
    }

    .el-select,
    .sort-group {
      flex: 1;
      width: auto;
    }
  }

  .search-actions {
    .el-button {
      flex: 1;
    }
  }
}
</style>
