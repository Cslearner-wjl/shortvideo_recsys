<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import CommentBox from "../components/CommentBox.vue";
import { fetchHotRank } from "../services/rank";
import { VideoItem, favoriteVideo, likeVideo, reportPlay, unlikeVideo, unfavoriteVideo } from "../services/video";
import { getToken } from "../services/token";

const router = useRouter();
const list = ref<VideoItem[]>([]);
const loading = ref(true);
const errorMessage = ref("");
const pendingIds = ref(new Set<string>());
const pageState = reactive({
  page: 1,
  pageSize: 20,
  hasMore: true,
});
const currentIndex = ref(0);
const videoRef = ref<HTMLVideoElement | null>(null);

// 评论相关状态
const showComments = ref(false);

const loadRank = async (reset = false) => {
  if (reset) {
    list.value = [];
    pageState.page = 1;
    pageState.hasMore = true;
    currentIndex.value = 0;
  }
  loading.value = true;
  errorMessage.value = "";

  try {
    const data = await fetchHotRank(pageState.page, pageState.pageSize);
    list.value = mergeVideos(list.value, data);
    pageState.hasMore = data.length >= pageState.pageSize;
    pageState.page += 1;
  } catch (error) {
    errorMessage.value = "加载榜单失败";
  } finally {
    loading.value = false;
    autoplayCurrent();
  }
};

const mergeVideos = (current: VideoItem[], incoming: VideoItem[]) => {
  const map = new Map(current.map((item) => [item.id, item]));
  incoming.forEach((item) => {
    map.set(item.id, item);
  });
  return Array.from(map.values());
};

const currentVideo = computed(() => list.value[currentIndex.value] ?? null);

const requireLogin = () => {
  if (getToken()) {
    return true;
  }
  router.push("/login");
  return false;
};

const syncCommentCount = (total: number) => {
  const video = currentVideo.value;
  if (!video) return;
  list.value = list.value.map((item) =>
    item.id === video.id ? { ...item, commentCount: Math.max(0, Number(total ?? 0)) } : item
  );
};

const updateVideo = (id: string, updater: (item: VideoItem) => VideoItem) => {
  list.value = list.value.map((item) => (item.id === id ? updater(item) : item));
};

const adjustCount = (id: string, key: keyof VideoItem, delta: number) => {
  updateVideo(id, (item) => ({
    ...item,
    [key]: Math.max(0, Number(item[key] ?? 0) + delta),
  }));
};

// 评论功能
const toggleComments = () => {
  if (!requireLogin()) return;
  showComments.value = !showComments.value;
};

const handleCommentsLoaded = (total: number) => {
  syncCommentCount(total);
};

const handleCommented = () => {
  const video = currentVideo.value;
  if (!video) return;
  adjustCount(video.id, "commentCount", 1);
};

const withPending = async (id: string, action: () => Promise<void>) => {
  pendingIds.value.add(id);
  try {
    await action();
  } finally {
    pendingIds.value.delete(id);
  }
};

const handleLike = (id: string) => {
  if (!requireLogin()) return;
  const video = list.value.find((item) => item.id === id);
  if (!video) return;
  const nextLiked = !video.liked;
  updateVideo(id, (item) => ({
    ...item,
    liked: nextLiked,
    likeCount: Math.max(0, Number(item.likeCount ?? 0) + (nextLiked ? 1 : -1)),
  }));
  withPending(id, async () => {
    try {
      if (nextLiked) {
        await likeVideo(id);
      } else {
        await unlikeVideo(id);
      }
    } catch (error) {
      updateVideo(id, (item) => ({
        ...item,
        liked: !nextLiked,
        likeCount: Math.max(0, Number(item.likeCount ?? 0) + (nextLiked ? -1 : 1)),
      }));
    }
  });
};

const handleFavorite = (id: string) => {
  if (!requireLogin()) return;
  const video = list.value.find((item) => item.id === id);
  if (!video) return;
  const nextFavorited = !video.favorited;
  updateVideo(id, (item) => ({
    ...item,
    favorited: nextFavorited,
    favoriteCount: Math.max(0, Number(item.favoriteCount ?? 0) + (nextFavorited ? 1 : -1)),
  }));
  withPending(id, async () => {
    try {
      if (nextFavorited) {
        await favoriteVideo(id);
      } else {
        await unfavoriteVideo(id);
      }
    } catch (error) {
      updateVideo(id, (item) => ({
        ...item,
        favorited: !nextFavorited,
        favoriteCount: Math.max(0, Number(item.favoriteCount ?? 0) + (nextFavorited ? -1 : 1)),
      }));
    }
  });
};

// 当视频切换时关闭评论面板
watch(currentIndex, () => {
  showComments.value = false;
});

const autoplayCurrent = () => {
  const video = videoRef.value;
  if (!video) {
    return;
  }
  video.load();
  const playPromise = video.play();
  if (playPromise) {
    playPromise.catch(() => {
      // 浏览器可能阻止自动播放
    });
  }
};

const ensureNextPage = async () => {
  if (!pageState.hasMore || loading.value) {
    return;
  }
  await loadRank(false);
};

const goNext = async () => {
  if (currentIndex.value < list.value.length - 1) {
    currentIndex.value += 1;
    autoplayCurrent();
    if (currentIndex.value >= list.value.length - 2) {
      await ensureNextPage();
    }
    return;
  }
  await ensureNextPage();
  if (currentIndex.value < list.value.length - 1) {
    currentIndex.value += 1;
    autoplayCurrent();
  }
};

const goPrev = () => {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1;
    autoplayCurrent();
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    goNext();
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    goPrev();
  }
};

const handlePlay = (id: string) => {
  reportPlay(id).catch(() => {
    // 榜单播放上报失败无需阻断
  });
};

onMounted(() => {
  window.addEventListener("keydown", handleKeydown);
  loadRank(true);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<template>
  <section class="fullscreen-feed">
    <!-- 视频区域 -->
    <div class="video-container" v-if="currentVideo">
      <video
        ref="videoRef"
        :src="currentVideo.videoUrl"
        :poster="currentVideo.coverUrl"
        controls
        playsinline
        @play="handlePlay(currentVideo.id)"
        @ended="goNext"
      ></video>
      
      <!-- 视频信息覆盖层 -->
      <div class="video-overlay">
        <div class="video-info-overlay">
          <div class="hot-badge">🔥 热门榜单 #{{ currentIndex + 1 }}</div>
          <h2 class="video-title-overlay">{{ currentVideo.title }}</h2>
          <p class="video-author-overlay">@{{ currentVideo.author }}</p>
        </div>
      </div>

      <!-- 右侧互动栏 -->
      <div class="interaction-bar">
        <button class="interaction-btn" type="button" @click="router.push(`/video/${currentVideo.id}`)">
          <span class="icon">ℹ️</span>
          <span class="count">详情</span>
        </button>
        <button
          class="interaction-btn"
          type="button"
          :disabled="pendingIds.has(currentVideo.id)"
          @click="handleLike(currentVideo.id)"
        >
          <span class="icon icon-like" :class="{ active: currentVideo.liked }">♥</span>
          <span class="count">{{ currentVideo.likeCount }}</span>
        </button>
        <button 
          class="interaction-btn" 
          type="button"
          @click="toggleComments"
        >
          <span class="icon">💬</span>
          <span class="count">{{ currentVideo.commentCount }}</span>
        </button>
        <button
          class="interaction-btn"
          type="button"
          :disabled="pendingIds.has(currentVideo.id)"
          @click="handleFavorite(currentVideo.id)"
        >
          <span class="icon icon-favorite" :class="{ active: currentVideo.favorited }">★</span>
          <span class="count">{{ currentVideo.favoriteCount }}</span>
        </button>
        <div class="interaction-btn stats-only">
          <span class="icon">▶️</span>
          <span class="count">{{ currentVideo.playCount }}</span>
        </div>
      </div>

      <!-- 导航按钮 -->
      <button class="nav-btn nav-prev" @click="goPrev" :disabled="currentIndex === 0">
        <span>↑</span>
      </button>
      <button class="nav-btn nav-next" @click="goNext">
        <span>↓</span>
      </button>

      <!-- 进度指示 -->
      <div class="progress-indicator">
        {{ currentIndex + 1 }} / {{ list.length }}
      </div>

      <!-- 刷新按钮 -->
      <button class="refresh-btn" @click="loadRank(true)">
        🔄 刷新
      </button>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state fullscreen-empty">
      <div class="section-title">暂无热门内容</div>
      <p class="muted">稍后再试或刷新页面</p>
      <button class="primary-button" @click="loadRank(true)">刷新</button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !currentVideo" class="loading-state">
      <p class="muted">加载中...</p>
    </div>

    <!-- 评论面板 -->
    <div v-if="showComments" class="comments-panel">
      <div class="comments-header">
        <h3>评论 ({{ currentVideo?.commentCount || 0 }})</h3>
        <button class="close-btn" @click="toggleComments">✕</button>
      </div>
      <CommentBox
        v-if="currentVideo"
        :video-id="currentVideo.id"
        :active="showComments"
        :allow-post="true"
        :show-header="false"
        @commented="handleCommented"
        @loaded="handleCommentsLoaded"
      />
    </div>
  </section>
</template>

<style scoped>
/* 全屏视频样式 */
.fullscreen-feed {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background: #000;
  overflow: hidden;
}

.video-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-container video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.video-overlay {
  position: absolute;
  bottom: 80px;
  left: 20px;
  right: 100px;
  pointer-events: none;
}

.video-info-overlay {
  color: #fff;
  text-shadow: 0 2px 8px rgba(0,0,0,0.8);
}

.hot-badge {
  display: inline-block;
  background: linear-gradient(135deg, #ff6b35, #f7c531);
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}

.video-title-overlay {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.video-author-overlay {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

/* 右侧互动栏 */
.interaction-bar {
  position: absolute;
  right: 16px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  z-index: 10;
}

.interaction-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(10px);
  border: none;
  border-radius: 50%;
  width: 56px;
  height: 56px;
  cursor: pointer;
  color: #fff;
  transition: all 0.2s ease;
  padding: 0;
  justify-content: center;
}

.interaction-btn:hover:not(.stats-only) {
  background: rgba(255,255,255,0.25);
  transform: scale(1.1);
}

.interaction-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.interaction-btn.stats-only {
  cursor: default;
  background: rgba(255,255,255,0.1);
}

.interaction-btn .icon {
  font-size: 20px;
}

.interaction-btn .icon-like,
.interaction-btn .icon-favorite {
  color: #cfcfcf;
}

.interaction-btn .icon-like.active {
  color: #ff4d4f;
}

.interaction-btn .icon-favorite.active {
  color: #f6c343;
}

.interaction-btn .count {
  font-size: 11px;
  font-weight: 600;
}

/* 导航按钮 */
.nav-btn {
  position: absolute;
  right: 16px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  border: none;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 12;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.35);
  transform: scale(1.1);
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.nav-prev {
  top: 20px;
}

.nav-next {
  bottom: 20px;
}

.progress-indicator {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 12;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.refresh-btn {
  position: absolute;
  top: 20px;
  left: 100px;
  z-index: 12;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background: rgba(255,255,255,0.35);
}

/* 评论面板 */
.comments-panel {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: min(400px, 80vw);
  background: rgba(255,255,255,0.98);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  z-index: 20;
  box-shadow: -4px 0 20px rgba(0,0,0,0.3);
}

.comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.comments-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
  padding: 4px 8px;
}

.close-btn:hover {
  color: #333;
}

.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.comment-item {
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-user {
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.5;
  margin-bottom: 4px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.load-more {
  text-align: center;
  padding: 16px;
  color: var(--accent);
  cursor: pointer;
  font-size: 14px;
}

.load-more:hover {
  text-decoration: underline;
}

/* 空状态和加载状态 */
.fullscreen-empty,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #fff;
  gap: 16px;
}

.fullscreen-empty .section-title {
  color: #fff;
}

.fullscreen-empty .muted {
  color: rgba(255,255,255,0.7);
}

@media (max-width: 720px) {
  .video-overlay {
    bottom: 100px;
    left: 16px;
    right: 80px;
  }
  
  .interaction-bar {
    right: 12px;
    bottom: 100px;
    gap: 16px;
  }
  
  .interaction-btn {
    width: 48px;
    height: 48px;
  }
  
  .interaction-btn .icon {
    font-size: 18px;
  }
  
  .nav-btn {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
  
  .comments-panel {
    width: 100vw;
  }
}
</style>
