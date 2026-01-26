<script setup lang="ts">
// 可复用的表格组件，封装搜索、表格与分页。
import { computed, ref, watch } from "vue";

export type DataColumn = {
  label: string;
  prop: string;
  width?: string | number;
  align?: "left" | "center" | "right";
  slot?: string;
  formatter?: (row: Record<string, unknown>) => string;
};

const props = withDefaults(
  defineProps<{
    columns: DataColumn[];
    rows: Record<string, unknown>[];
    loading?: boolean;
    total?: number;
    page?: number;
    pageSize?: number;
    searchable?: boolean;
    searchPlaceholder?: string;
  }>(),
  {
    loading: false,
    total: 0,
    page: 1,
    pageSize: 20,
    searchable: false,
    searchPlaceholder: "请输入关键词",
  }
);

const emit = defineEmits<{
  (event: "update:page", value: number): void;
  (event: "update:pageSize", value: number): void;
  (event: "search", value: string): void;
  (event: "refresh"): void;
}>();

const keyword = ref("");

const paginationTotal = computed(() => props.total ?? 0);

const handleSearch = () => {
  emit("search", keyword.value.trim());
};

watch(
  () => props.searchable,
  (value) => {
    if (!value) {
      keyword.value = "";
    }
  }
);
</script>

<template>
  <div class="admin-card">
    <div class="toolbar" v-if="searchable || $slots.toolbar">
      <el-input
        v-if="searchable"
        v-model="keyword"
        :placeholder="searchPlaceholder"
        clearable
        style="max-width: 260px"
        @keyup.enter="handleSearch"
      />
      <el-button v-if="searchable" type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="emit('refresh')">刷新</el-button>
      <slot name="toolbar" />
    </div>

    <el-table :data="rows" :loading="loading" style="width: 100%; margin-top: 16px;">
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :align="column.align"
      >
        <template #default="scope">
          <slot v-if="column.slot" :name="column.slot" v-bind="scope" />
          <span v-else>{{ column.formatter ? column.formatter(scope.row) : scope.row[column.prop] }}</span>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
      <el-pagination
        :total="paginationTotal"
        :current-page="page"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="emit('update:page', $event)"
        @size-change="emit('update:pageSize', $event)"
      />
    </div>
  </div>
</template>
