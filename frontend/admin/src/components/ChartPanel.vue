<script setup lang="ts">
// ECharts 面板容器，统一标题与空态展示。
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";

const props = withDefaults(
  defineProps<{
    title: string;
    options?: echarts.EChartsOption;
    height?: string;
    loading?: boolean;
    emptyText?: string;
  }>(),
  {
    height: "260px",
    loading: false,
    emptyText: "暂无数据",
  }
);

const containerRef = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const hasData = computed(() => Boolean(props.options));

const resize = () => {
  if (chart) {
    chart.resize();
  }
};

const disposeChart = () => {
  if (chart) {
    chart.dispose();
    chart = null;
  }
};

const renderChart = async () => {
  if (!props.options) {
    disposeChart();
    return;
  }
  await nextTick();
  if (!containerRef.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(containerRef.value);
  }
  chart.setOption(props.options, true);
};

watch(
  () => props.options,
  (newVal) => {
    if (newVal) {
      renderChart();
    } else {
      disposeChart();
    }
  },
  { deep: true }
);

watch(
  () => props.loading,
  (value) => {
    if (!value && props.options) {
      renderChart();
    }
  }
);

onMounted(() => {
  if (props.options) {
    renderChart();
  }
  window.addEventListener("resize", resize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resize);
  disposeChart();
});
</script>

<template>
  <el-card shadow="never" style="border-radius: 12px;">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <div v-if="loading" style="padding: 24px 0;">
      <el-skeleton :rows="4" animated />
    </div>
    <div v-else-if="!hasData" style="padding: 24px 0;">
      <el-empty :description="emptyText" />
    </div>
    <div v-else ref="containerRef" :style="{ width: '100%', height }"></div>
  </el-card>
</template>
