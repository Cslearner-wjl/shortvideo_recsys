<script setup lang="ts">
// 用户管理页，提供列表检索与冻结解封。
import { onMounted, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import DataTable, { DataColumn } from "../components/DataTable.vue";
import { fetchUsers, updateUserStatus, type AdminUser } from "../services/admin";
import { resolveErrorMessage } from "../services/errors";

const state = reactive({
  rows: [] as AdminUser[],
  total: 0,
  page: 1,
  pageSize: 20,
  keyword: "",
  loading: false,
});

const columns: DataColumn[] = [
  { label: "编号", prop: "id", width: 80 },
  { label: "用户名", prop: "username", width: 140 },
  { label: "手机号", prop: "phone" },
  { label: "邮箱", prop: "email" },
  { label: "状态", prop: "status", width: 90, slot: "status" },
  { label: "创建时间", prop: "createdAt", width: 180 },
  { label: "操作", prop: "actions", width: 180, slot: "actions" },
];

const loadUsers = async () => {
  state.loading = true;
  try {
    const data = await fetchUsers({
      page: state.page,
      size: state.pageSize,
      keyword: state.keyword || undefined,
    });
    state.rows = data.items || [];
    state.total = data.total || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载用户失败"));
  } finally {
    state.loading = false;
  }
};

const handleSearch = (keyword: string) => {
  state.keyword = keyword;
  state.page = 1;
  loadUsers();
};

const handleStatusChange = async (row: AdminUser, nextStatus: number) => {
  const actionLabel = nextStatus === 0 ? "冻结" : "解封";
  try {
    await ElMessageBox.confirm(`确认要${actionLabel}用户 ${row.username} 吗？`, "操作确认", {
      type: "warning",
      confirmButtonText: "确认",
      cancelButtonText: "取消",
    });
    await updateUserStatus(row.id, nextStatus);
    ElMessage.success(`${actionLabel}成功`);
    loadUsers();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(resolveErrorMessage(error, `${actionLabel}失败`));
    }
  }
};

const handlePageChange = (page: number) => {
  state.page = page;
  loadUsers();
};

const handleSizeChange = (size: number) => {
  state.pageSize = size;
  state.page = 1;
  loadUsers();
};

onMounted(() => {
  loadUsers();
});
</script>

<template>
  <DataTable
    :columns="columns"
    :rows="state.rows"
    :total="state.total"
    :page="state.page"
    :page-size="state.pageSize"
    :loading="state.loading"
    searchable
    search-placeholder="输入用户名/邮箱/手机号"
    @search="handleSearch"
    @refresh="loadUsers"
    @update:page="handlePageChange"
    @update:pageSize="handleSizeChange"
  >
    <template #status="{ row }">
      <el-tag :type="row.status === 0 ? 'danger' : 'success'">
        {{ row.status === 0 ? "冻结" : "正常" }}
      </el-tag>
    </template>
    <template #actions="{ row }">
      <el-button
        v-if="row.status !== 0"
        size="small"
        type="warning"
        @click="handleStatusChange(row, 0)"
      >
        冻结
      </el-button>
      <el-button v-else size="small" type="success" @click="handleStatusChange(row, 1)">
        解封
      </el-button>
    </template>
  </DataTable>
</template>
