<template>
    <div>
        <el-table
          ref="dataTable"
          :data="dataSource?.list || []"
          :height="tableHeight"
          :stripe="options.stripe"
          :border="options.border"
          header-row-class-name="table-header-row"
          highlight-current-row
          @row-click="handleRowClick"
          @selection-change="handleSelectionChange"
          v-bind="rowKeyBind"
        >
          <!-- selection选择框 -->
          <el-table-column
            v-if="options.selectType && options.selectType == 'checkbox'"
            type="selection"
            width="50"
            align="center"
          ></el-table-column>
          <!-- 序号 -->
          <el-table-column
            v-if="options.showIndex"
            label="序号"
            type="index"
            width="60"
            align="center"
          ></el-table-column>
          <!-- 数据列 -->
          <template v-for="(column, index) in columns" :key="index">
            <template v-if="column.scopedSlots">
              <el-table-column
                :prop="column.prop"
                :label="column.label"
                :align="column.align || 'left'"
                :width="column.width"
                :class-name="column.className"
              >
                <template #default="scope">
                  <slot
                    :name="column.scopedSlots"
                    :index="scope.$index"
                    :row="scope.row"
                  >
                  </slot>
                </template>
            </el-table-column>
          </template>
            <template v-else>
            <el-table-column
              :prop="column.prop"
              :label="column.label"
              :align="column.align || 'left'"
              :width="column.width"
              :class-name="column.className"
              :fixed="column.fixed"
            >
            </el-table-column>
          </template>
         </template>
        </el-table>
        <!-- 分页 -->
        <div v-if="showPagination" class="pagination">
          <el-pagination
            v-if="dataSource?.totalCount"
            :current-page="dataSource.pageNo"
            background
            :total="dataSource.totalCount"
            :page-sizes="[15, 30, 50, 100]"
            :page-size="dataSource.pageSize"
            :layout="layout"
            style="text-align: right"
            @size-change="handlePageSizeChange"
            @current-change="handlePageNoChange"
          ></el-pagination>
        </div>
    </div>
</template>
<script setup lang="ts">
import { ref, computed, type PropType, onMounted, onBeforeUnmount, watch } from "vue";

interface TableColumn {
    prop?: string;
    label?: string;
    align?: string;
    width?: number | string;
    fixed?: string | boolean;
    className?: string;
    scopedSlots?: string;
}

interface DataSource {
    list: Record<string, unknown>[];
    totalCount?: number;
    pageNo: number;
    pageSize: number;
}

interface TableOptions {
    extHeight?: number;
    showIndex?: boolean;
    stripe?: boolean;
    border?: boolean;
    selectType?: string;
    tableHeight?: number;
    rowKey?: string;
}

const emit = defineEmits<{
    (e: "rowSelected", row: Record<string, unknown>[]): void;
    (e: "rowClick", row: Record<string, unknown>): void;
    (e: "update:dataSource", value: DataSource): void;
}>();

const props = defineProps({
    dataSource: {
        type: Object as PropType<DataSource>,
        default: () => ({ list: [], pageNo: 1, pageSize: 15 })
    },
    showPagination: {
        type: Boolean,
        default: true,
    },
    showPageSize: {
        type: Boolean,
        default: true,
    },
    options: {
        type: Object as PropType<TableOptions>,
        default: () => ({
            extHeight: 0,
            showIndex: false,
        }),
    },
    columns: {
        type: Array as PropType<TableColumn[]>,
        default: () => []
    },
    fetch: {
        type: Function as PropType<() => void>,
        required: false
    },
    initFetch: {
        type: Boolean,
        default: true,
    },
});

const layout = computed(() => {
    return `total, ${
        props.showPageSize ? "sizes" : ""
    }, prev, pager, next, jumper`;
});
// 顶部 60 , 内容区域距离顶部 20, 内容上下内间距 15*2 分页区域高度 46
const topHeight = 60 + 20 + 30 + 46;

const rowKeyBind = computed(() => {
    return props.options.rowKey ? { "row-key": props.options.rowKey } : {};
});

const tableHeight = ref(
    props.options.tableHeight
    ? props.options.tableHeight
    : window.innerHeight - topHeight - (props.options.extHeight || 0)
);

const updateTableHeight = () => {
    if (props.options.tableHeight) {
        tableHeight.value = props.options.tableHeight;
        return;
    }
    tableHeight.value = window.innerHeight - topHeight - (props.options.extHeight || 0);
};

onMounted(() => {
    window.addEventListener("resize", updateTableHeight, { passive: true });
});

onBeforeUnmount(() => {
    window.removeEventListener("resize", updateTableHeight);
});

watch(
    () => [props.options.extHeight, props.options.tableHeight],
    () => {
        updateTableHeight();
    }
);

// 初始化
const init = () => {
    if (props.initFetch && props.fetch) {
        props.fetch();
    }
};
init();

const dataTable = ref();
// 清除选中
const clearSelection = () => {
    dataTable.value.clearSelection();
};

// 设置行选中
const setCurrentRow = (rowKey: string, rowValue: unknown) => {
    const row = props.dataSource?.list.find((item: Record<string, unknown>) => {
        return item[rowKey] === rowValue;
    });
    dataTable.value.setCurrentRow(row);
};
// 将子组件暴露出去, 否则父组件无法调用
defineExpose({ setCurrentRow, clearSelection });

// 行点击
const handleRowClick = (row: Record<string, unknown>) => {
    emit("rowClick", row);
};

// 多选
const handleSelectionChange = (row: Record<string, unknown>[]) => {
    emit("rowSelected", row);
};

// 切换每页大小
const handlePageSizeChange = (size: number) => {
    emit("update:dataSource", {
        ...props.dataSource,
        pageSize: size,
        pageNo: 1
    } as DataSource);
    props.fetch?.();
};
// 切换页码
const handlePageNoChange = (pageNo: number) => {
    emit("update:dataSource", {
        ...props.dataSource,
        pageNo: pageNo
    } as DataSource);
    props.fetch?.();
};
</script>
<style lang="scss" scoped>
.pagination {
    padding-top: 15px;
    padding-right: 10px;
}
.el-pagination {
    justify-content: right;
}

:deep(.el-table__cell) {
    padding: 8px 0px; 
}

:deep(.table-header-row) {
    background-color: var(--bg-hover);
    color: var(--text-main);
    font-weight: 600;
}
</style>
