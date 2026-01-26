<script setup lang="ts">
// 推荐视频流页面，支持上下滑切换与播放上报。
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import CommentBox from "../components/CommentBox.vue";
import {
  VideoItem,
  favoriteVideo,
  fetchFeedPage,
  likeVideo,
  unlikeVideo,
  unfavoriteVideo,
  reportPlay,
} from "../services/video";
import { getToken } from "../services/token";

const router = useRouter();
const videos = ref<VideoItem[]>([]);
const loading = ref(true);
const errorMessage = ref("");
const pendingIds = ref(new Set<string>());
const pageState = reactive({
  page: 1,
  pageSize: 20,
  cursor: null as string | null,
  hasMore: true,
  source: "recommendations" as "recommendations" | "videos",
});
const currentIndex = ref(0);
const videoRef = ref<HTMLVideoElement | null>(null);
const modal = reactive({
  open: false,
  title: "操作失败",
  message: "",
  retry: null as null | (() => void),
});

// 评论面板状态
const showComments = ref(false);

// 播放上报状态
const playState = reactive({
  videoId: "",
  startedAt: 0,
  elapsedMs: 0,
  playing: false,
  counted: false,
});

const switchGuard = reactive({
  lastSwitchAt: 0,
  threshold: 500,
});

const loadFeed = async (reset = false) => {
  if (reset) {
    videos.value = [];
    pageState.page = 1;
    pageState.cursor = null;
    pageState.hasMore = true;
    currentIndex.value = 0;
  }
  loading.value = true;
  errorMessage.value = "";

  try {
    const page = await fetchFeedPage(pageState.page, pageState.pageSize, pageState.cursor ?? undefined);
    pageState.source = page.source;
    pageState.cursor = page.nextCursor ?? null;
    pageState.page = page.page + 1;
    pageState.hasMore = computeHasMore(page);
    videos.value = mergeVideos(videos.value, page.items);
  } catch (error) {
    errorMessage.value = "加载推荐失败";
  } finally {
    loading.value = false;
    autoplayCurrent();
  }
};

const computeHasMore = (page: { source: "recommendations" | "videos"; page: number; pageSize: number; total?: number; nextCursor?: string | null; items: VideoItem[]; }) => {
  if (page.source === "videos") {
    if (page.total != null) {
      return page.page * page.pageSize < page.total;
    }
    return page.items.length >= page.pageSize;
  }
  if (page.nextCursor) {
    return true;
  }
  return page.items.length >= page.pageSize;
};

const mergeVideos = (current: VideoItem[], incoming: VideoItem[]) => {
  const map = new Map(current.map((item) => [item.id, item]));
  incoming.forEach((item) => {
    map.set(item.id, item);
  });
  return Array.from(map.values());
};

const currentVideo = computed(() => videos.value[currentIndex.value] ?? null);

const adjustCount = (id: string, key: keyof VideoItem, delta: number) => {
  videos.value = videos.value.map((item) =>
    item.id === id ? { ...item, [key]: Math.max(0, Number(item[key] ?? 0) + delta) } : item
  );
};

const updateVideo = (id: string, updater: (item: VideoItem) => VideoItem) => {
  videos.value = videos.value.map((item) => (item.id === id ? updater(item) : item));
};

const withPending = async (id: string, action: () => Promise<void>) => {
  pendingIds.value.add(id);
  try {
    await action();
  } finally {
    pendingIds.value.delete(id);
  }
};

const showRetry = (message: string, retry: () => void) => {
  modal.open = true;
  modal.message = message;
  modal.retry = retry;
};

const requireLogin = () => {
  if (getToken()) {
    return true;
  }
  modal.title = "请先登录";
  showRetry("登录后才可以评论/点赞/收藏", () => router.push("/login"));
  return false;
};

const resetPlayState = () => {
  playState.videoId = "";
  playState.startedAt = 0;
  playState.elapsedMs = 0;
  playState.playing = false;
  playState.counted = false;
};

const startPlaySession = (id: string) => {
  if (playState.videoId !== id) {
    resetPlayState();
    playState.videoId = id;
  }
  if (!playState.counted) {
    adjustCount(id, "playCount", 1);
    playState.counted = true;
  }
  if (!playState.playing) {
    playState.startedAt = Date.now();
    playState.playing = true;
  }
};

const pausePlaySession = () => {
  if (!playState.playing) return;
  const elapsed = Date.now() - playState.startedAt;
  playState.elapsedMs += Math.max(0, elapsed);
  playState.playing = false;
};

const flushPlayReport = async (id: string, completed = false) => {
  if (!id || playState.videoId !== id) return;
  pausePlaySession();
  const durationMs = Math.round(playState.elapsedMs);
  if (durationMs <= 0 && !completed) {
    resetPlayState();
    return;
  }
  try {
    await reportPlay(id, {
      durationMs: durationMs > 0 ? durationMs : undefined,
      isCompleted: completed || undefined,
    });
  } catch (error) {
    showRetry("播放上报失败", () => flushPlayReport(id, completed));
  }
  resetPlayState();
};

const handlePlay = (id: string) => {
  startPlaySession(id);
};

const handlePause = () => {
  pausePlaySession();
};

const handleLike = (id: string) => {
  if (!requireLogin()) return;
  const video = videos.value.find((item) => item.id === id);
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
      showRetry(nextLiked ? "点赞失败" : "取消点赞失败", () => handleLike(id));
    }
  });
};

const handleFavorite = (id: string) => {
  if (!requireLogin()) return;
  const video = videos.value.find((item) => item.id === id);
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
      showRetry(nextFavorited ? "收藏失败" : "取消收藏失败", () => handleFavorite(id));
    }
  });
};

const handleCommented = () => {
  const video = currentVideo.value;
  if (!video) return;
  adjustCount(video.id, "commentCount", 1);
};

const handleCommentsLoaded = (total: number) => {
  const video = currentVideo.value;
  if (!video) return;
  updateVideo(video.id, (item) => ({
    ...item,
    commentCount: Math.max(0, Number(total ?? 0)),
  }));
};

// 评论面板相关
const toggleComments = () => {
  showComments.value = !showComments.value;
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
  await loadFeed(false);
};

const goNext = async (completed = false) => {
  const currentId = currentVideo.value?.id;
  if (currentId) {
    await flushPlayReport(currentId, completed);
  }
  if (currentIndex.value < videos.value.length - 1) {
    currentIndex.value += 1;
    autoplayCurrent();
    if (currentIndex.value >= videos.value.length - 2) {
      await ensureNextPage();
    }
    return;
  }
  await ensureNextPage();
  if (currentIndex.value < videos.value.length - 1) {
    currentIndex.value += 1;
    autoplayCurrent();
  }
};

const goPrev = async () => {
  const currentId = currentVideo.value?.id;
  if (currentId) {
    await flushPlayReport(currentId, false);
  }
  if (currentIndex.value > 0) {
    currentIndex.value -= 1;
    autoplayCurrent();
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    goNext(false);
  } else if (event.key === "ArrowUp") {
    event.preventDefault();
    goPrev();
  }
};

const touchStartY = ref(0);

const canSwitch = () => Date.now() - switchGuard.lastSwitchAt > switchGuard.threshold;

const markSwitch = () => {
  switchGuard.lastSwitchAt = Date.now();
};

const handleWheel = (event: WheelEvent) => {
  if (!canSwitch()) return;
  const delta = event.deltaY;
  if (Math.abs(delta) < 40) return;
  markSwitch();
  if (delta > 0) {
    goNext(false);
  } else {
    goPrev();
  }
};

const handleTouchStart = (event: TouchEvent) => {
  touchStartY.value = event.touches[0]?.clientY ?? 0;
};

const handleTouchEnd = (event: TouchEvent) => {
  if (!canSwitch()) return;
  const endY = event.changedTouches[0]?.clientY ?? 0;
  const delta = touchStartY.value - endY;
  if (Math.abs(delta) < 50) return;
  markSwitch();
  if (delta > 0) {
    goNext(false);
  } else {
    goPrev();
  }
};

const closeModal = () => {
  modal.open = false;
  modal.retry = null;
};

const retryModal = () => {
  const retry = modal.retry;
  closeModal();
  if (retry) {
    retry();
  }
};

onMounted(() => {
  window.addEventListener("keydown", handleKeydown);
  window.addEventListener("wheel", handleWheel, { passive: true });
  window.addEventListener("touchstart", handleTouchStart, { passive: true });
  window.addEventListener("touchend", handleTouchEnd, { passive: true });
  loadFeed(true);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeydown);
  window.removeEventListener("wheel", handleWheel);
  window.removeEventListener("touchstart", handleTouchStart);
  window.removeEventListener("touchend", handleTouchEnd);
  if (currentVideo.value?.id) {
    flushPlayReport(currentVideo.value.id, false);
  }
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
        @pause="handlePause"
        @ended="goNext(true)"
      ></video>
      
      <!-- 视频信息覆盖层 -->
      <div class="video-overlay">
        <div class="video-info-overlay">
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
          :disabled="pendingIds.has(currentVideo.id)" 
          @click="handleLike(currentVideo.id)"
        >
          <span class="icon icon-like" :class="{ active: currentVideo.liked }">♥</span>
          <span class="count">{{ currentVideo.likeCount }}</span>
        </button>
        <button 
          class="interaction-btn" 
          @click="toggleComments"
        >
          <span class="icon">💬</span>
          <span class="count">{{ currentVideo.commentCount }}</span>
        </button>
        <button 
          class="interaction-btn" 
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
      <button class="nav-btn nav-next" @click="goNext(false)">
        <span>↓</span>
      </button>

      <!-- 进度指示 -->
      <div class="progress-indicator">
        {{ currentIndex + 1 }} / {{ videos.length }}
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state fullscreen-empty">
      <div class="section-title">暂无推荐内容</div>
      <p class="muted">稍后再试或刷新页面</p>
      <button class="primary-button" @click="loadFeed(true)">刷新</button>
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

    <!-- 错误弹窗 -->
    <div v-if="modal.open" class="modal-backdrop">
      <div class="modal">
        <div class="modal-title">{{ modal.title }}</div>
        <p class="muted" style="margin-top: 8px;">{{ modal.message }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="closeModal">取消</button>
          <button class="primary-button" type="button" @click="retryModal">重试</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.single-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.single-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.single-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 20px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.video-wrapper {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: #000;
}

.video-placeholder {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b655f;
  background: #f1f1f1;
}

.video-wrapper video {
  width: 100%;
  max-height: 480px;
  display: block;
  background: #000;
}

.video-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.video-title {
  font-size: 20px;
  font-weight: 700;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.comment-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.nav-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 720px) {
  .stat-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

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

.interaction-btn:hover:not(:disabled) {
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
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
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
