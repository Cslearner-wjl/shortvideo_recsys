<script setup lang="ts">
// 管理员账号管理页面
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Edit, Delete, Key } from "@element-plus/icons-vue";
import DataTable, { DataColumn } from "../components/DataTable.vue";
import { request } from "../services/http";
import { resolveErrorMessage } from "../services/errors";

interface Admin {
  id: number;
  username: string;
  createdAt: string;
}

const state = reactive({
  rows: [] as Admin[],
  total: 0,
  page: 1,
  pageSize: 20,
  loading: false,
  keyword: "",
});

const dialogVisible = ref(false);
const dialogType = ref<"create" | "edit">("create");
const editingId = ref<number | null>(null);
const submitting = ref(false);

const formData = reactive({
  username: "",
  password: "",
});

const passwordDialogVisible = ref(false);
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});
const changingPassword = ref(false);

const columns: DataColumn[] = [
  { label: "ID", prop: "id", width: 80 },
  { label: "用户名", prop: "username", width: 200 },
  { label: "创建时间", prop: "createdAt", width: 180 },
  { label: "操作", prop: "actions", width: 240, slot: "actions" },
];

const loadAdmins = async () => {
  state.loading = true;
  try {
    const params = new URLSearchParams({
      page: String(state.page),
      size: String(state.pageSize),
    });
    if (state.keyword) {
      params.set("keyword", state.keyword);
    }
    const resp = await request(`/api/admin/admins?${params.toString()}`);
    const data = resp?.data ?? resp;
    state.rows = data.items || [];
    state.total = data.total || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载管理员列表失败"));
  } finally {
    state.loading = false;
  }
};

const openCreateDialog = () => {
  dialogType.value = "create";
  editingId.value = null;
  formData.username = "";
  formData.password = "";
  dialogVisible.value = true;
};

const openEditDialog = (row: Admin) => {
  dialogType.value = "edit";
  editingId.value = row.id;
  formData.username = row.username;
  formData.password = "";
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formData.username.trim()) {
    ElMessage.warning("请输入用户名");
    return;
  }
  if (dialogType.value === "create" && !formData.password) {
    ElMessage.warning("请输入密码");
    return;
  }

  submitting.value = true;
  try {
    if (dialogType.value === "create") {
      await request("/api/admin/admins", {
        method: "POST",
        body: {
          username: formData.username.trim(),
          password: formData.password,
        },
      });
      ElMessage.success("创建成功");
    } else {
      await request(`/api/admin/admins/${editingId.value}`, {
        method: "PUT",
        body: {
          username: formData.username.trim(),
          password: formData.password || undefined,
        },
      });
      ElMessage.success("更新成功");
    }
    dialogVisible.value = false;
    loadAdmins();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "操作失败"));
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (row: Admin) => {
  try {
    await ElMessageBox.confirm(`确定删除管理员 "${row.username}" 吗？`, "删除确认", {
      type: "warning",
    });
    await request(`/api/admin/admins/${row.id}`, { method: "DELETE" });
    ElMessage.success("删除成功");
    loadAdmins();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(resolveErrorMessage(error, "删除失败"));
    }
  }
};

const openPasswordDialog = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordDialogVisible.value = true;
};

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning("请填写完整");
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning("新密码至少6位");
    return;
  }

  changingPassword.value = true;
  try {
    await request("/api/admin/admins/me/password", {
      method: "POST",
      body: {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      },
    });
    ElMessage.success("密码修改成功");
    passwordDialogVisible.value = false;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "密码修改失败"));
  } finally {
    changingPassword.value = false;
  }
};

const handlePageChange = (page: number) => {
  state.page = page;
  loadAdmins();
};

const handleSizeChange = (size: number) => {
  state.pageSize = size;
  state.page = 1;
  loadAdmins();
};

const handleSearch = () => {
  state.page = 1;
  loadAdmins();
};

onMounted(() => {
  loadAdmins();
});
</script>

<template>
  <div class="admin-card" style="margin-bottom: 20px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h3 class="section-title">管理员账号管理</h3>
      <div style="display: flex; gap: 12px;">
        <el-button type="primary" :icon="Key" @click="openPasswordDialog">修改我的密码</el-button>
        <el-button type="success" :icon="Plus" @click="openCreateDialog">新增管理员</el-button>
      </div>
    </div>
  </div>

  <DataTable
    :columns="columns"
    :rows="state.rows"
    :total="state.total"
    :page="state.page"
    :page-size="state.pageSize"
    :loading="state.loading"
    @refresh="loadAdmins"
    @update:page="handlePageChange"
    @update:pageSize="handleSizeChange"
  >
    <template #toolbar>
      <el-input
        v-model="state.keyword"
        placeholder="搜索用户名"
        style="width: 200px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </template>
    <template #actions="{ row }">
      <el-button size="small" type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
      <el-popconfirm
        title="确定删除该管理员吗？"
        confirm-button-text="确定"
        cancel-button-text="取消"
        @confirm="handleDelete(row)"
      >
        <template #reference>
          <el-button size="small" type="danger" :icon="Delete" plain>删除</el-button>
        </template>
      </el-popconfirm>
    </template>
  </DataTable>

  <!-- 新增/编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogType === 'create' ? '新增管理员' : '编辑管理员'"
    width="450px"
  >
    <el-form label-width="80px">
      <el-form-item label="用户名" required>
        <el-input v-model="formData.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item :label="dialogType === 'create' ? '密码' : '新密码'" :required="dialogType === 'create'">
        <el-input
          v-model="formData.password"
          type="password"
          show-password
          :placeholder="dialogType === 'create' ? '请输入密码' : '留空则不修改密码'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ dialogType === "create" ? "创建" : "保存" }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="passwordDialogVisible" title="修改我的密码" width="450px">
    <el-form label-width="100px">
      <el-form-item label="原密码" required>
        <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" required>
        <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
      </el-form-item>
      <el-form-item label="确认新密码" required>
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.section-title {
  margin: 0;
}
</style>
