<script setup lang="ts">
// 视频管理页，覆盖列表、审核、热门与上传操作。
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Check } from "@element-plus/icons-vue";
import DataTable, { DataColumn } from "../components/DataTable.vue";
import {
  auditVideo,
  deleteVideo,
  fetchVideos,
  setVideoHot,
  uploadVideo,
  type Video,
} from "../services/admin";
import { resolveErrorMessage } from "../services/errors";

const state = reactive({
  rows: [] as Video[],
  total: 0,
  page: 1,
  pageSize: 20,
  sort: "time",
  loading: false,
});

const uploadForm = reactive({
  uploaderUserId: "",
  title: "",
  description: "",
  tags: "",
});

const selectedFile = ref<File | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const uploading = ref(false);

const columns: DataColumn[] = [
  { label: "编号", prop: "id", width: 80 },
  { label: "标题", prop: "title", width: 200 },
  { label: "上传用户", prop: "uploaderUserId", width: 100 },
  { label: "审核状态", prop: "auditStatus", width: 110, slot: "audit" },
  { label: "热门", prop: "isHot", width: 80, slot: "hot" },
  { label: "创建时间", prop: "createdAt", width: 160 },
  { label: "操作", prop: "actions", width: 320, slot: "actions" },
];

const loadVideos = async () => {
  state.loading = true;
  try {
    const data = await fetchVideos({
      page: state.page,
      pageSize: state.pageSize,
      sort: state.sort,
    });
    state.rows = data.items || [];
    state.total = data.total || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载视频失败"));
  } finally {
    state.loading = false;
  }
};

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  selectedFile.value = input.files?.[0] ?? null;
};

const handleUpload = async () => {
  if (!uploadForm.uploaderUserId || !uploadForm.title) {
    ElMessage.warning("请填写上传用户编号与标题");
    return;
  }
  const uploaderId = Number(uploadForm.uploaderUserId);
  if (!Number.isFinite(uploaderId) || uploaderId <= 0) {
    ElMessage.warning("上传用户编号需为有效数字");
    return;
  }
  if (!selectedFile.value) {
    ElMessage.warning("请先选择视频文件");
    return;
  }

  uploading.value = true;
  try {
    await uploadVideo({
      uploaderUserId: uploaderId,
      title: uploadForm.title,
      description: uploadForm.description || undefined,
      tags: uploadForm.tags || undefined,
      file: selectedFile.value,
    });
    ElMessage.success("上传成功，已进入待审状态");
    uploadForm.title = "";
    uploadForm.description = "";
    uploadForm.tags = "";
    selectedFile.value = null;
    if (fileInputRef.value) {
      fileInputRef.value.value = "";
    }
    loadVideos();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "上传失败"));
  } finally {
    uploading.value = false;
  }
};

const handleAudit = async (row: Video, status: "APPROVED" | "REJECTED") => {
  try {
    await ElMessageBox.confirm(`确认要${status === "APPROVED" ? "通过" : "驳回"}该视频吗？`, "审核确认", {
      type: "warning",
    });
    await auditVideo(row.id, status);
    ElMessage.success("审核已更新");
    loadVideos();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(resolveErrorMessage(error, "审核失败"));
    }
  }
};

const handleHot = async (row: Video, next: boolean) => {
  try {
    await setVideoHot(row.id, next);
    ElMessage.success(next ? "已设为热门" : "已取消热门");
    loadVideos();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "热门状态更新失败"));
  }
};

const handleDelete = async (row: Video) => {
  try {
    await ElMessageBox.confirm(`确认删除视频 ${row.title} 吗？`, "删除确认", {
      type: "warning",
    });
    await deleteVideo(row.id);
    ElMessage.success("删除成功");
    loadVideos();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(resolveErrorMessage(error, "删除失败"));
    }
  }
};

const handleDeleteDirect = async (row: Video) => {
  try {
    await deleteVideo(row.id);
    ElMessage.success("删除成功");
    loadVideos();
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "删除失败"));
  }
};

const handlePageChange = (page: number) => {
  state.page = page;
  loadVideos();
};

const handleSizeChange = (size: number) => {
  state.pageSize = size;
  state.page = 1;
  loadVideos();
};

const handleSortChange = () => {
  state.page = 1;
  loadVideos();
};

onMounted(() => {
  loadVideos();
});
</script>

<template>
  <div class="admin-card" style="margin-bottom: 20px;">
    <h3 class="section-title">上传视频</h3>
    <div class="form-grid">
      <el-input v-model="uploadForm.uploaderUserId" placeholder="上传用户编号" />
      <el-input v-model="uploadForm.title" placeholder="标题" />
      <el-input v-model="uploadForm.description" placeholder="描述（可选）" />
      <el-input v-model="uploadForm.tags" placeholder="标签 JSON（可选）" />
      <input ref="fileInputRef" type="file" accept="video/*" @change="handleFileChange" />
    </div>
    <div style="margin-top: 12px;">
      <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      <span style="margin-left: 12px; color: var(--app-muted); font-size: 12px;">
        列表来源于 /api/videos/page，仅显示审核通过的视频。
      </span>
    </div>
  </div>

  <DataTable
    :columns="columns"
    :rows="state.rows"
    :total="state.total"
    :page="state.page"
    :page-size="state.pageSize"
    :loading="state.loading"
    @refresh="loadVideos"
    @update:page="handlePageChange"
    @update:pageSize="handleSizeChange"
  >
    <template #toolbar>
      <el-select v-model="state.sort" style="width: 140px" @change="handleSortChange">
        <el-option label="按时间" value="time" />
        <el-option label="按热门" value="hot" />
      </el-select>
    </template>
    <template #audit="{ row }">
      <el-tag
        :type="row.auditStatus === 'APPROVED' ? 'success' : row.auditStatus === 'REJECTED' ? 'danger' : 'info'"
      >
        {{ row.auditStatus }}
      </el-tag>
    </template>
    <template #hot="{ row }">
      <el-tag :type="row.isHot ? 'warning' : 'info'">{{ row.isHot ? "热门" : "普通" }}</el-tag>
    </template>
    <template #actions="{ row }">
      <div class="action-group">
        <el-button-group>
          <el-button 
            size="small" 
            :type="row.auditStatus === 'APPROVED' ? 'success' : 'default'"
            :plain="row.auditStatus === 'APPROVED'"
            @click="handleAudit(row, 'APPROVED')"
          >
            <el-icon v-if="row.auditStatus === 'APPROVED'"><Check /></el-icon>
            通过
          </el-button>
          <el-button 
            size="small" 
            :type="row.auditStatus === 'REJECTED' ? 'warning' : 'default'"
            :plain="row.auditStatus === 'REJECTED'"
            @click="handleAudit(row, 'REJECTED')"
          >
            驳回
          </el-button>
        </el-button-group>
        <el-button 
          size="small" 
          :type="row.isHot ? 'warning' : 'primary'"
          :plain="!row.isHot"
          @click="handleHot(row, !row.isHot)"
        >
          {{ row.isHot ? "🔥 热门" : "设热门" }}
        </el-button>
        <el-popconfirm
          title="确定删除该视频吗？"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="handleDeleteDirect(row)"
        >
          <template #reference>
            <el-button size="small" type="danger" plain>删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </template>
  </DataTable>
</template>

<style scoped>
.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-group .el-button-group {
  flex-shrink: 0;
}

/* 确保表格中的操作列有足够宽度 */
:deep(.el-table__cell) {
  vertical-align: middle;
}
</style>
