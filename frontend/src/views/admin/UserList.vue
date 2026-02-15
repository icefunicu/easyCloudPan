<template>
    <div class="user-list-panel">
    <div class="top-panel">
        <el-card>
            <el-form
              ref="formDataRef"
              :model="searchFormData"
              :rules="rules"
              label-width="80px"
              @submit.prevent
            >
              <el-row :gutter="10" align="middle">
                <el-col :span="6">
                  <!-- 模糊搜索 -->
                  <el-form-item label="用户昵称">
                    <el-input
                      v-model.trim="searchFormData.nickNameFuzzy"
                      clearable
                      placeholder="支持模糊搜索"
                      @keyup.enter="loadDataList"
                    ></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="5">
                    <!-- 下拉框 -->
                    <el-form-item label="状态">
                        <el-select
                          v-model="searchFormData.status"
                          clearable
                          placeholder="请选择状态"
                        >
                          <el-option :value="1" label="启用"></el-option>
                          <el-option :value="0" label="禁用"></el-option>
                        </el-select>
                    </el-form-item>
                </el-col>
                <el-col :span="13" :style="{ 'padding-left': '10px' }">
                  <el-button type="primary" @click="loadDataList">查询</el-button>
                  <el-button type="success" @click="batchUpdateStatus(1)" :disabled="selectUserIdList.length == 0">批量启用</el-button>
                  <el-button type="danger" @click="batchUpdateStatus(0)" :disabled="selectUserIdList.length == 0">批量禁用</el-button>
                </el-col>
              </el-row>
            </el-form>
        </el-card>
    </div>
        <div class="file-list">
          <Table
            ref="dataTableRef"
            :columns="columns"
            :data-source="tableData"
            :fetch="loadDataList"
            :init-fetch="true"
            :options="tableOptions"
            @row-selected="rowSelected"
          >
            <template #avatar="{ row }">
              <div class="avatar">
                <Avatar :user-id="row.userId" :avatar="row.qqAvatar"></Avatar>
              </div>
            </template>
            <template #space="{ row }">
              {{ proxy.Utils.size2Str(row.useSpace) }} / {{
                proxy.Utils.size2Str(row.totalSpace)
              }}
            </template>
            <template #status="{ row }">
              <el-tag v-if="row.status == 1" type="success">启用</el-tag>
              <el-tag v-if="row.status == 0" type="danger">禁用</el-tag>
            </template>
            <template #op="{ row }">
              <span class="a-link" @click="updateSpace(row)">分配空间</span>
              <el-divider direction="vertical"></el-divider>
              <span class="a-link" @click="updateUserStatus(row)">{{
                row.status == 0 ? "启用" : "禁用"
              }}</span>
            </template>
          </Table>
        </div>
        <Dialog
          :show="dialogConfig.show"
          :title="dialogConfig.title"
          :buttons="dialogConfig.buttons"
          width="400px"
          :show-cancel="false"
          @close="dialogConfig.show = false"
        >
          <el-form
            ref="formDataRef"
            :model="formData"
            :rules="rules"
            label-width="80px"
            @submit.prevent
          >
            <el-form-item label="昵称">
              {{ formData.nickName }}
            </el-form-item>
            <!-- 空间分配 -->
            <el-form-item label="空间大小">
               <el-input
                v-model.trim="spaceSize"
                clearable
                placeholder="请输入空间大小"
              >
                 <template #append>
                    <el-select v-model="spaceUnit" style="width: 100px">
                        <el-option 
                            v-for="item in spaceUnits" 
                            :key="item.value" 
                            :label="item.label" 
                            :value="item.value"
                        />
                    </el-select>
                 </template>
              </el-input>
              <div class="space-presets">
                  <span class="label">快速选择：</span>
                  <div class="preset-tags">
                     <el-tag
                        v-for="item in presets" 
                        :key="item.label" 
                        class="preset-tag"
                        effect="plain"
                        @click="selectPreset(item)"
                     >
                        {{ item.label }}
                     </el-tag>
                  </div>
              </div>
            </el-form-item>
          </el-form>
        </Dialog>
    </div>
</template>

<script setup>
import { ref, getCurrentInstance, nextTick } from "vue";
import * as adminService from "@/services/adminService";
const { proxy } = getCurrentInstance();


const columns = [
    {
        label: "头像",
        prop: "avatar",
        width: 80,
        scopedSlots: "avatar",
    },
    {
        label: "昵称",
        prop: "nickName",
    },
    {
        label: "邮箱",
        prop: "email",
    },
    {
        label: "空间使用",
        prop: "space",
        scopedSlots: "space",
    },
    {
        label: "加入时间",
        prop: "joinTime",
    },
    {
        label: "最后登录时间",
        prop: "lastLoginTime",
    },
    {
        label: "状态",
        prop: "status",
        scopedSlots: "status",
        width: 80,
    },
    {
        label: "操作",
        prop: "op",
        width: 150,
        scopedSlots: "op",
    },
];

const searchFormData = ref({});

const tableData = ref({
  list: [],
  pageNo: 1,
  pageSize: 15,
  totalCount: 0,
  pageTotal: 0,
});
const tableOptions = {
    extHeight: 20,
    selectType: "checkbox",
    tableHeight: "calc(100% - 50px)",
};
// 多选
const selectUserIdList = ref([]);
const rowSelected = (rows) => {
  selectUserIdList.value = [];
  rows.forEach((item) => {
    selectUserIdList.value.push(item.userId);
  });
};

const batchUpdateStatus = (status) => {
    if (selectUserIdList.value.length == 0) return;
    proxy.Confirm(
        `你确定要批量${status == 0 ? "禁用" : "启用"}这些用户吗?`,
        async () => {
             const result = await adminService.updateUserStatus({
                userId: selectUserIdList.value.join(','), // Assuming backend supports CSV
                status: status,
            });
            if (!result) return;
            proxy.Message.success("操作成功");
            loadDataList();
        }
    );
};

const loadDataList = async () => {
    const params = {
        pageNo: tableData.value.pageNo || 1,
        pageSize: tableData.value.pageSize || 15,
    };
    Object.assign(params, searchFormData.value);
    const result = await adminService.loadUserList(params);
    if (!result) {
        return;
    }
    tableData.value = result;
};
// 修改状态
const updateUserStatus = (row) => {
    proxy.Confirm(
        `你确定要【${row.status == 0 ? "启用" : "禁用"}】吗?`,
        async () => {
             // Optimistic UI
             const oldStatus = row.status;
             const newStatus = row.status == 0 ? 1 : 0;
             row.status = newStatus;

            const result = await adminService.updateUserStatus({
                userId: row.userId,
                status: newStatus,
            });
            if (!result) {
                // Revert
                row.status = oldStatus;
                return;
            }
            // Success, no need to reload
        }
    );
};

const dialogConfig = ref({
    show: false,
    title: "修改空间大小",
    buttons: [
        {
            type: "primary",
            text: "确定",
            click: () => {
                submitForm();
            },
        },
    ],
});

const formData = ref({});
const formDataRef = ref();
const rules = {
};

const spaceSize = ref();
const spaceUnit = ref("MB"); // 默认单位
const spaceUnits = [
    { label: "MB", value: "MB" },
    { label: "GB", value: "GB" },
    { label: "TB", value: "TB" },
];

const presets = [
    { label: "100MB", size: 100, unit: "MB" },
    { label: "1GB", size: 1, unit: "GB" },
    { label: "10GB", size: 10, unit: "GB" },
    { label: "50GB", size: 50, unit: "GB" },
    { label: "100GB", size: 100, unit: "GB" },
    { label: "2TB", size: 2, unit: "TB" },
];

const updateSpace = (data) => {
    dialogConfig.value.show = true;
    nextTick(() => {
        formDataRef.value.resetFields();
        formData.value = Object.assign({}, data);
        
        const totalSpaceBytes = data.totalSpace;
        if (totalSpaceBytes) {
            if (totalSpaceBytes >= 1024 * 1024 * 1024 * 1024) {
                spaceSize.value = parseFloat((totalSpaceBytes / (1024 * 1024 * 1024 * 1024)).toFixed(2));
                spaceUnit.value = "TB";
            } else if (totalSpaceBytes >= 1024 * 1024 * 1024) {
                spaceSize.value = parseFloat((totalSpaceBytes / (1024 * 1024 * 1024)).toFixed(2));
                spaceUnit.value = "GB";
            } else {
                spaceSize.value = parseFloat((totalSpaceBytes / (1024 * 1024)).toFixed(2));
                spaceUnit.value = "MB";
            }
        } else {
            spaceSize.value = 0;
            spaceUnit.value = "MB";
        }
    });
};

const selectPreset = (preset) => {
    spaceSize.value = preset.size;
    spaceUnit.value = preset.unit;
};

const submitForm = () => {
    formDataRef.value.validate(async (valid) => {
        // changeSpace 校验移除或者改为校验 spaceSize
        // 由于 rules 绑定在 formData.changeSpace 上，而 Input 绑定了 spaceSize
        // 需要手动校验 spaceSize 或者调整 rules
        // 这里简单处理：手动校验 spaceSize
        
        let totalMB = 0;
        const size = Number(spaceSize.value);
        if (isNaN(size) || size < 0) {
             proxy.Message.warning("请输入有效的空间大小");
             return;
        }

        switch (spaceUnit.value) {
            case "MB":
                totalMB = size;
                break;
            case "GB":
                totalMB = size * 1024;
                break;
            case "TB":
                totalMB = size * 1024 * 1024;
                break;
            default:
                totalMB = size;
        }

        const params = {
            userId: formData.value.userId,
            changeSpace: Math.floor(totalMB),
        };
        
        const result = await adminService.updateUserSpace(params);
        if (!result) {
            return;
        }
        dialogConfig.value.show = false;
        proxy.Message.success("操作成功");
        loadDataList();
    });
};
</script>

<style lang="scss" scoped>
.user-list-panel {
    height: 100%;
    display: flex;
    flex-direction: column;
}
.top-panel {
    margin-top: 10px;
}
.file-list {
    margin-top: 10px;
    flex: 1;
    height: 0;
    overflow: hidden;
}
.avatar {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    overflow: hidden;
    transition: transform 0.3s;
    cursor: pointer;
    
    &:hover {
        transform: scale(1.1);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
    
    img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }
}

.space-presets {
    margin-top: 10px;
    display: flex;
    align-items: flex-start;
    .label {
        font-size: 12px;
        color: #999;
        margin-right: 10px;
        white-space: nowrap;
        margin-top: 4px;
    }
    .preset-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        .preset-tag {
            cursor: pointer;
            transition: all 0.3s;
            &:hover {
                color: var(--el-color-primary);
                border-color: var(--el-color-primary);
                transform: translateY(-2px);
            }
        }
    }
}
</style>
