<template>
  <div class="virtual-table" ref="containerRef" @scroll="handleScroll">
    <div class="virtual-table-header" :style="{ height: headerHeight + 'px' }">
      <div
        v-for="(col, idx) in columns"
        :key="idx"
        class="virtual-table-cell header-cell"
        :style="{ width: col.width ? col.width + 'px' : 'auto', flex: col.width ? 'none' : 1 }"
      >
        <el-checkbox
          v-if="idx === 0 && selectType === 'checkbox'"
          :model-value="isAllSelected"
          :indeterminate="isIndeterminate"
          @change="(val: import('element-plus').CheckboxValueType) => handleSelectAll(Boolean(val))"
        />
        <span v-else>{{ col.label }}</span>
      </div>
    </div>
    <div class="virtual-table-body" :style="{ height: bodyHeight + 'px' }">
      <div class="virtual-table-content" :style="{ height: totalHeight + 'px' }">
        <div
          v-for="(row, idx) in visibleRows"
          :key="getRowKey(row, startIndex + idx)"
          class="virtual-table-row"
          :style="{ transform: `translateY(${(startIndex + idx) * rowHeight}px)` }"
          :class="{ selected: isSelected(row), 'row-hover': hoveredIndex === startIndex + idx }"
          @mouseenter="hoveredIndex = startIndex + idx"
          @mouseleave="hoveredIndex = -1"
          @click="handleRowClick(row)"
        >
          <div
            v-for="(col, colIdx) in columns"
            :key="colIdx"
            class="virtual-table-cell"
            :style="{ width: col.width ? col.width + 'px' : 'auto', flex: col.width ? 'none' : 1 }"
          >
            <el-checkbox
              v-if="colIdx === 0 && selectType === 'checkbox'"
              :model-value="isSelected(row)"
              @change="(val: import('element-plus').CheckboxValueType) => handleSelect(row, Boolean(val))"
              @click.stop
            />
            <slot v-else-if="col.scopedSlots" :name="col.scopedSlots" :row="row" :index="startIndex + idx">
              {{ row[col.prop as string] }}
            </slot>
            <span v-else>{{ row[col.prop as string] }}</span>
          </div>
        </div>
      </div>
      <div v-if="loading" class="virtual-table-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="!dataSource?.list?.length" class="virtual-table-empty">
        <el-empty description="暂无数据" />
      </div>
    </div>
    <div v-if="showPagination && dataSource?.totalCount" class="virtual-table-pagination">
      <el-pagination
        :current-page="dataSource.pageNo"
        background
        :total="dataSource.totalCount"
        :page-sizes="[50, 100, 200, 500]"
        :page-size="dataSource.pageSize"
        layout="total, sizes, prev, pager, next"
        @size-change="handlePageSizeChange"
        @current-change="handlePageNoChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, type PropType } from 'vue'
import { Loading } from '@element-plus/icons-vue'

interface TableColumn {
  prop?: string
  label?: string
  width?: number
  scopedSlots?: string
}

interface DataSource {
  list: Record<string, unknown>[]
  totalCount?: number
  pageNo?: number
  pageSize?: number
}

const props = defineProps({
  dataSource: {
    type: Object as PropType<DataSource>,
    default: () => ({ list: [], pageNo: 1, pageSize: 50 })
  },
  columns: {
    type: Array as PropType<TableColumn[]>,
    default: () => []
  },
  rowHeight: {
    type: Number,
    default: 48
  },
  headerHeight: {
    type: Number,
    default: 48
  },
  bufferSize: {
    type: Number,
    default: 20
  },
  selectType: {
    type: String,
    default: ''
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  showPagination: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  bodyHeight: {
    type: Number,
    default: 400
  }
})

const emit = defineEmits<{
  (e: 'rowClick', row: Record<string, unknown>): void
  (e: 'rowSelected', rows: Record<string, unknown>[]): void
  (e: 'pageChange', pageNo: number): void
  (e: 'pageSizeChange', pageSize: number): void
}>()

const containerRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const hoveredIndex = ref(-1)
const selectedRows = ref<Set<string>>(new Set())
const selectedCount = ref(0) // Maintain count to avoid full iteration

const totalHeight = computed(() => {
  return (props.dataSource?.list?.length || 0) * props.rowHeight
})

const startIndex = computed(() => {
  const index = Math.floor(scrollTop.value / props.rowHeight)
  return Math.max(0, index - Math.floor(props.bufferSize / 4))
})

const endIndex = computed(() => {
  return Math.min(
    props.dataSource?.list?.length || 0,
    startIndex.value + props.bufferSize + Math.floor(props.bufferSize / 2)
  )
})

const visibleRows = computed(() => {
  const list = props.dataSource?.list || []
  return list.slice(startIndex.value, endIndex.value)
})

const getRowKey = (row: Record<string, unknown>, index: number): string => {
  return String(row[props.rowKey] || index)
}

const isSelected = (row: Record<string, unknown>): boolean => {
  return selectedRows.value.has(getRowKey(row, 0))
}

const isAllSelected = computed(() => {
  const listLen = props.dataSource?.list?.length || 0
  return listLen > 0 && selectedCount.value === listLen
})

const isIndeterminate = computed(() => {
  const listLen = props.dataSource?.list?.length || 0
  return selectedCount.value > 0 && selectedCount.value < listLen
})

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement
  scrollTop.value = target.scrollTop
}

const handleSelect = (row: Record<string, unknown>, selected: boolean) => {
  const key = getRowKey(row, 0)
  if (selected) {
    if (!selectedRows.value.has(key)) {
      selectedRows.value.add(key)
      selectedCount.value++
    }
  } else {
    if (selectedRows.value.has(key)) {
      selectedRows.value.delete(key)
      selectedCount.value--
    }
  }
  emitSelectedRows()
}

const handleSelectAll = (selected: boolean) => {
  const list = props.dataSource?.list || []
  if (selected) {
    list.forEach(row => {
      const key = getRowKey(row, 0)
      if (!selectedRows.value.has(key)) {
        selectedRows.value.add(key)
        selectedCount.value++
      }
    })
  } else {
    selectedRows.value.clear()
    selectedCount.value = 0
  }
  emitSelectedRows()
}

const emitSelectedRows = () => {
  const list = props.dataSource?.list || []
  const selected = list.filter(row => isSelected(row))
  emit('rowSelected', selected)
}

const handleRowClick = (row: Record<string, unknown>) => {
  emit('rowClick', row)
}

const handlePageSizeChange = (size: number) => {
  emit('pageSizeChange', size)
}

const handlePageNoChange = (pageNo: number) => {
  emit('pageChange', pageNo)
}

const clearSelection = () => {
  selectedRows.value.clear()
  selectedCount.value = 0
}

defineExpose({ clearSelection })

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.scrollTop = 0
  }
})

watch(() => props.dataSource?.list, () => {
  scrollTop.value = 0
  if (containerRef.value) {
    containerRef.value.scrollTop = 0
  }
  // Reset selection when data changes
  selectedRows.value.clear()
  selectedCount.value = 0
})
</script>

<style lang="scss" scoped>
.virtual-table {
  width: 100%;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.75);
}

.virtual-table-header {
  display: flex;
  align-items: center;
  background: rgba(31, 79, 104, 0.1);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 10;
}

.virtual-table-body {
  overflow-y: auto;
  position: relative;
}

.virtual-table-content {
  position: relative;
}

.virtual-table-row {
  position: absolute;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  height: 48px;
  border-bottom: 1px solid rgba(194, 204, 220, 0.6);
  transition: background 0.15s ease;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }

  &.row-hover {
    background: rgba(31, 79, 104, 0.06);
  }

  &.selected {
    background: rgba(36, 95, 124, 0.12);
  }
}

.virtual-table-cell {
  padding: 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--text-main);

  &.header-cell {
    font-weight: 700;
    color: var(--text-main);
  }
}

.virtual-table-loading,
.virtual-table-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.virtual-table-pagination {
  padding: 14px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
}
</style>
