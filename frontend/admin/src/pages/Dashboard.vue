<script setup lang="ts">
// 数据看板页面，展示趋势与 TopN 数据。
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import ChartPanel from "../components/ChartPanel.vue";
import {
  fetchActiveUsers,
  fetchDailyPlay,
  fetchHotTopn,
  fetchUserGrowth,
  fetchVideoPublish,
  fetchUsers,
  fetchVideos,
  type ActiveUser,
  type DailyPlay,
  type HotTopn,
  type UserGrowth,
  type VideoPublish,
} from "../services/admin";
import { resolveErrorMessage } from "../services/errors";

const range = ref<[string, string]>(getDefaultRange());

const loading = reactive({
  daily: false,
  growth: false,
  active: false,
  publish: false,
  hot: false,
  summary: false,
});

const dataState = reactive({
  daily: [] as DailyPlay[],
  growth: [] as UserGrowth[],
  active: [] as ActiveUser[],
  publish: [] as VideoPublish[],
  hot: [] as HotTopn[],
});

// 统计摘要数据
const summary = reactive({
  totalUsers: 0,
  totalVideos: 0,
  todayPlays: 0,
  todayNewUsers: 0,
  todayActiveUsers: 0,
  todayPublished: 0,
  pendingAudit: 0,
});

// 计算汇总数据
const totalPlays = computed(() => dataState.daily.reduce((sum, d) => sum + d.playCount, 0));
const totalNewUsers = computed(() => dataState.growth.reduce((sum, d) => sum + d.newUserCount, 0));
const avgActiveUsers = computed(() => {
  if (dataState.active.length === 0) return 0;
  return Math.round(dataState.active.reduce((sum, d) => sum + d.activeUserCount, 0) / dataState.active.length);
});

const loadSummary = async () => {
  loading.summary = true;
  try {
    const [usersRes, videosRes] = await Promise.all([
      fetchUsers({ page: 1, size: 1 }),
      fetchVideos({ page: 1, pageSize: 1 }),
    ]);
    summary.totalUsers = usersRes.total;
    summary.totalVideos = videosRes.total;
  } catch (error) {
    // 静默处理
  } finally {
    loading.summary = false;
  }
};

const loadDailyPlay = async () => {
  if (!range.value || !range.value[0] || !range.value[1]) return;
  
  loading.daily = true;
  try {
    dataState.daily = await fetchDailyPlay(range.value[0], range.value[1]);
    // 获取今日播放
    const today = formatDate(new Date());
    const todayData = dataState.daily.find(d => d.day === today);
    summary.todayPlays = todayData?.playCount || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载播放趋势失败"));
  } finally {
    loading.daily = false;
  }
};

const loadUserGrowth = async () => {
  if (!range.value || !range.value[0] || !range.value[1]) return;
  
  loading.growth = true;
  try {
    dataState.growth = await fetchUserGrowth(range.value[0], range.value[1]);
    // 获取今日新增
    const today = formatDate(new Date());
    const todayData = dataState.growth.find(d => d.day === today);
    summary.todayNewUsers = todayData?.newUserCount || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载用户增长失败"));
  } finally {
    loading.growth = false;
  }
};

const loadActiveUsers = async () => {
  if (!range.value || !range.value[0] || !range.value[1]) return;
  
  loading.active = true;
  try {
    dataState.active = await fetchActiveUsers(range.value[0], range.value[1]);
    // 获取今日活跃
    const today = formatDate(new Date());
    const todayData = dataState.active.find(d => d.day === today);
    summary.todayActiveUsers = todayData?.activeUserCount || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载活跃度失败"));
  } finally {
    loading.active = false;
  }
};

const loadVideoPublish = async () => {
  if (!range.value || !range.value[0] || !range.value[1]) return;

  loading.publish = true;
  try {
    dataState.publish = await fetchVideoPublish(range.value[0], range.value[1]);
    const today = formatDate(new Date());
    const todayData = dataState.publish.find(d => d.day === today);
    summary.todayPublished = todayData?.publishCount || 0;
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载发布量失败"));
  } finally {
    loading.publish = false;
  }
};

const loadHotTop10 = async () => {
  loading.hot = true;
  try {
    dataState.hot = await fetchHotTopn(10);
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, "加载热门 Top10 失败"));
  } finally {
    loading.hot = false;
  }
};

const loadAll = async () => {
  await Promise.all([loadSummary(), loadDailyPlay(), loadUserGrowth(), loadActiveUsers(), loadVideoPublish(), loadHotTop10()]);
};

const dailyOption = () => dataState.daily.length > 0 ? buildLineOption("播放量", dataState.daily.map((item) => item.day), dataState.daily.map((item) => item.playCount)) : undefined;
const growthOption = () => dataState.growth.length > 0 ? buildLineOption("新增用户", dataState.growth.map((item) => item.day), dataState.growth.map((item) => item.newUserCount)) : undefined;
const activeOption = () => dataState.active.length > 0 ? buildLineOption("活跃用户", dataState.active.map((item) => item.day), dataState.active.map((item) => item.activeUserCount)) : undefined;
const publishOption = () => dataState.publish.length > 0 ? buildLineOption("视频发布", dataState.publish.map((item) => item.day), dataState.publish.map((item) => item.publishCount)) : undefined;

// 修复热门前N图表配置，旋转X轴标签避免遮挡
const hotOption = () => {
  if (dataState.hot.length === 0) return undefined;
  return {
    tooltip: { 
      trigger: "axis",
      formatter: (params: any) => {
        const data = params[0];
        const item = dataState.hot[data.dataIndex];
        if (!item) return data.name;
        return `<strong>${item.title}</strong><br/>
          热度分: ${item.hotScore}<br/>
          播放: ${item.playCount}<br/>
          点赞: ${item.likeCount}<br/>
          评论: ${item.commentCount}<br/>
          收藏: ${item.favoriteCount}`;
      }
    },
    grid: { left: 50, right: 20, bottom: 30, top: 50 },
    xAxis: { 
      type: "category", 
      data: dataState.hot.map((item, index) => `#${index + 1}`),
      axisLabel: {
        rotate: 0,
        fontSize: 12,
      }
    },
    yAxis: { type: "value", name: "热度分" },
    series: [
      {
        name: "热度",
        type: "bar",
        data: dataState.hot.map((item) => item.hotScore),
        itemStyle: { 
          color: (params: any) => {
            const colors = ['#ff4d4f', '#ff7a45', '#ffa940', '#ffc53d', '#ffec3d', '#bae637', '#73d13d', '#36cfc9', '#40a9ff', '#597ef7'];
            return colors[params.dataIndex % colors.length];
          }
        },
        label: {
          show: true,
          position: 'top',
          formatter: (params: any) => {
            const item = dataState.hot[params.dataIndex];
            return item ? item.title.substring(0, 6) + (item.title.length > 6 ? '...' : '') : '';
          },
          fontSize: 10,
          color: '#666'
        }
      },
    ],
  };
};

const handleRangeChange = (val: [string, string] | null) => {
  // 只有当日期范围完整有效时才加载数据
  if (val && val[0] && val[1]) {
    loadDailyPlay();
    loadUserGrowth();
    loadActiveUsers();
    loadVideoPublish();
  }
};

onMounted(() => {
  loadAll();
});

function buildLineOption(name: string, labels: string[], values: number[]) {
  return {
    tooltip: { trigger: "axis" },
    grid: { left: 50, right: 20, bottom: 30, top: 30 },
    xAxis: { type: "category", data: labels },
    yAxis: { type: "value" },
    series: [
      {
        name,
        type: "line",
        data: values,
        smooth: true,
        showSymbol: true,
        symbolSize: 6,
        itemStyle: { color: "#1f6feb" },
        areaStyle: { color: "rgba(31, 111, 235, 0.1)" },
      },
    ],
  };
}

function getDefaultRange(): [string, string] {
  const today = new Date();
  const start = new Date();
  start.setDate(today.getDate() - 6);
  return [formatDate(start), formatDate(today)];
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}
</script>

<template>
  <!-- 统计卡片区域 -->
  <div class="stats-cards">
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
        👥
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.totalUsers.toLocaleString() }}</div>
        <div class="stat-label">总用户数</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
        🎬
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.totalVideos.toLocaleString() }}</div>
        <div class="stat-label">总视频数</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
        ▶️
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.todayPlays.toLocaleString() }}</div>
        <div class="stat-label">今日播放</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
        ➕
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.todayNewUsers.toLocaleString() }}</div>
        <div class="stat-label">今日新增</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);">
        🆕
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.todayPublished.toLocaleString() }}</div>
        <div class="stat-label">今日发布</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
        🔥
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ summary.todayActiveUsers.toLocaleString() }}</div>
        <div class="stat-label">今日活跃</div>
      </div>
    </div>
    <div class="stat-card">
      <div class="stat-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);">
        📊
      </div>
      <div class="stat-info">
        <div class="stat-value">{{ avgActiveUsers.toLocaleString() }}</div>
        <div class="stat-label">日均活跃</div>
      </div>
    </div>
  </div>

  <!-- 筛选工具栏 -->
  <div class="admin-card" style="margin-bottom: 20px;">
    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        :clearable="false"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="handleRangeChange"
      />
    </div>
  </div>

  <!-- 趋势图表区域 -->
  <div class="charts-grid">
    <ChartPanel title="播放趋势" :options="dailyOption()" :loading="loading.daily" height="280px" />
    <ChartPanel title="用户增长" :options="growthOption()" :loading="loading.growth" height="280px" />
    <ChartPanel title="活跃度" :options="activeOption()" :loading="loading.active" height="280px" />
    <ChartPanel title="视频发布量" :options="publishOption()" :loading="loading.publish" height="280px" />
  </div>

  <!-- 热门视频区域 -->
  <div style="margin-top: 20px;">
    <ChartPanel title="热门视频 Top 10" :options="hotOption()" :loading="loading.hot" height="320px" />
  </div>

  <!-- 热门视频列表 -->
  <div class="admin-card" style="margin-top: 20px;" v-if="dataState.hot.length > 0">
    <div class="section-title">🔥 热门视频排行榜</div>
    <el-table :data="dataState.hot" stripe style="width: 100%">
      <el-table-column label="排名" width="70" align="center">
        <template #default="{ $index }">
          <span :class="['rank-badge', `rank-${$index + 1}`]">{{ $index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="视频标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="playCount" label="播放量" width="100" align="right">
        <template #default="{ row }">
          {{ row.playCount.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞" width="80" align="right">
        <template #default="{ row }">
          {{ row.likeCount.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="commentCount" label="评论" width="80" align="right">
        <template #default="{ row }">
          {{ row.commentCount.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="favoriteCount" label="收藏" width="80" align="right">
        <template #default="{ row }">
          {{ row.favoriteCount.toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="hotScore" label="热度分" width="100" align="right">
        <template #default="{ row }">
          <span class="hot-score">{{ row.hotScore.toFixed(1) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- 周期汇总 -->
  <div class="summary-cards" style="margin-top: 20px;">
    <div class="admin-card summary-item">
      <div class="summary-title">📈 周期内总播放</div>
      <div class="summary-value">{{ totalPlays.toLocaleString() }}</div>
      <div class="summary-period">{{ range[0] }} ~ {{ range[1] }}</div>
    </div>
    <div class="admin-card summary-item">
      <div class="summary-title">👤 周期内新增用户</div>
      <div class="summary-value">{{ totalNewUsers.toLocaleString() }}</div>
      <div class="summary-period">{{ range[0] }} ~ {{ range[1] }}</div>
    </div>
    <div class="admin-card summary-item">
      <div class="summary-title">⚡ 日均活跃用户</div>
      <div class="summary-value">{{ avgActiveUsers.toLocaleString() }}</div>
      <div class="summary-period">{{ range[0] }} ~ {{ range[1] }}</div>
    </div>
  </div>
</template>

<style scoped>
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--app-surface);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--app-shadow);
  border: 1px solid var(--app-border);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--app-muted);
  margin-top: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--app-border);
  margin: 0 8px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .charts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #f0f0f0;
  color: #666;
}

.rank-badge.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb300);
  color: #fff;
}

.rank-badge.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: #fff;
}

.rank-badge.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: #fff;
}

.hot-score {
  color: #ff4d4f;
  font-weight: 600;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
}

.summary-item {
  text-align: center;
  padding: 24px;
}

.summary-title {
  font-size: 14px;
  color: var(--app-muted);
  margin-bottom: 8px;
}

.summary-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--app-text);
  margin-bottom: 4px;
}

.summary-period {
  font-size: 12px;
  color: var(--app-muted);
}
</style>
