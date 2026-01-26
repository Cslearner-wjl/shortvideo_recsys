<script setup lang="ts">
// 视频详情页，展示视频信息、互动数据与评论列表。
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import CommentBox from "../components/CommentBox.vue";
import {
  fetchVideoDetail,
  favoriteVideo,
  likeVideo,
  reportPlay,
  unlikeVideo,
  unfavoriteVideo,
  type VideoItem,
} from "../services/video";
import { getToken } from "../services/token";

const route = useRoute();
const router = useRouter();

const videoRef = ref<HTMLVideoElement | null>(null);
const video = ref<VideoItem | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const pending = ref(false);

const playState = reactive({
  startedAt: 0,
  elapsedMs: 0,
  playing: false,
  counted: false,
});

const requireLogin = () => {
  if (getToken()) {
    return true;
  }
  errorMessage.value = "请先登录";
  router.push("/login");
  return false;
};

const loadVideo = async () => {
  const id = String(route.params.id || "");
  if (!id) {
    errorMessage.value = "无效的视频 ID";
    loading.value = false;
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    video.value = await fetchVideoDetail(id);
    playState.startedAt = 0;
    playState.elapsedMs = 0;
    playState.playing = false;
    playState.counted = false;
  } catch (error: any) {
    errorMessage.value = error?.message || "加载视频失败";
  } finally {
    loading.value = false;
  }
};

const adjustCount = (key: keyof VideoItem, delta: number) => {
  if (!video.value) return;
  const next = Math.max(0, Number(video.value[key] ?? 0) + delta);
  (video.value as any)[key] = next;
};

const handleLike = async () => {
  if (!video.value || pending.value) return;
  if (!requireLogin()) return;
  pending.value = true;
  const nextLiked = !video.value.liked;
  video.value.liked = nextLiked;
  adjustCount("likeCount", nextLiked ? 1 : -1);
  try {
    if (nextLiked) {
      await likeVideo(video.value.id);
    } else {
      await unlikeVideo(video.value.id);
    }
  } catch (error: any) {
    video.value.liked = !nextLiked;
    adjustCount("likeCount", nextLiked ? -1 : 1);
    errorMessage.value = error?.message || (nextLiked ? "点赞失败" : "取消点赞失败");
  } finally {
    pending.value = false;
  }
};

const handleFavorite = async () => {
  if (!video.value || pending.value) return;
  if (!requireLogin()) return;
  pending.value = true;
  const nextFavorited = !video.value.favorited;
  video.value.favorited = nextFavorited;
  adjustCount("favoriteCount", nextFavorited ? 1 : -1);
  try {
    if (nextFavorited) {
      await favoriteVideo(video.value.id);
    } else {
      await unfavoriteVideo(video.value.id);
    }
  } catch (error: any) {
    video.value.favorited = !nextFavorited;
    adjustCount("favoriteCount", nextFavorited ? -1 : 1);
    errorMessage.value = error?.message || (nextFavorited ? "收藏失败" : "取消收藏失败");
  } finally {
    pending.value = false;
  }
};

const handleCommented = () => {
  adjustCount("commentCount", 1);
};

const handleCommentsLoaded = (total: number) => {
  if (!video.value) return;
  video.value.commentCount = Math.max(0, Number(total ?? 0));
};

const startPlaySession = () => {
  if (!video.value) return;
  if (!playState.counted) {
    adjustCount("playCount", 1);
    playState.counted = true;
  }
  if (!playState.playing) {
    playState.startedAt = Date.now();
    playState.playing = true;
  }
};

const pausePlaySession = () => {
  if (!playState.playing) return;
  playState.elapsedMs += Math.max(0, Date.now() - playState.startedAt);
  playState.playing = false;
};

const flushPlayReport = async (completed = false) => {
  if (!video.value) return;
  pausePlaySession();
  const durationMs = Math.round(playState.elapsedMs);
  if (durationMs <= 0 && !completed) return;
  try {
    await reportPlay(video.value.id, {
      durationMs: durationMs > 0 ? durationMs : undefined,
      isCompleted: completed || undefined,
    });
  } catch (error: any) {
    errorMessage.value = error?.message || "播放上报失败";
  } finally {
    playState.startedAt = 0;
    playState.elapsedMs = 0;
    playState.playing = false;
    playState.counted = false;
  }
};

const handleEnded = async () => {
  await flushPlayReport(true);
};

const createdAtText = computed(() => video.value?.createdAt || "-");

watch(
  () => route.params.id,
  () => {
    flushPlayReport(false);
    loadVideo();
  }
);

onMounted(() => {
  loadVideo();
});

onBeforeUnmount(() => {
  flushPlayReport(false);
});
</script>

<template>
  <section class="detail-page">
    <div class="detail-header">
      <button class="ghost-button" type="button" @click="router.back()">返回</button>
      <div>
        <h1 class="section-title">视频详情</h1>
        <p class="muted">完整信息与互动数据</p>
      </div>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div v-if="loading" class="muted">加载中...</div>
    <div v-else-if="video" class="detail-grid">
      <div class="video-panel">
        <div class="video-wrapper">
          <video
            ref="videoRef"
            :src="video.videoUrl"
            :poster="video.coverUrl"
            controls
            playsinline
            @play="startPlaySession"
            @pause="pausePlaySession"
            @ended="handleEnded"
          ></video>
        </div>
        <div class="video-meta">
          <h2>{{ video.title }}</h2>
          <p class="muted">作者：{{ video.author || `用户#${video.uploaderUserId ?? "-"}` }}</p>
          <p class="muted">发布时间：{{ createdAtText }}</p>
          <p v-if="video.description" class="description">{{ video.description }}</p>
        </div>
      </div>

      <div class="info-panel">
        <div class="stats-grid">
          <div class="stat-item">
            <span class="label">播放</span>
            <span class="value">{{ video.playCount }}</span>
          </div>
          <div class="stat-item">
            <span class="label">点赞</span>
            <span class="value">{{ video.likeCount }}</span>
          </div>
          <div class="stat-item">
            <span class="label">评论</span>
            <span class="value">{{ video.commentCount }}</span>
          </div>
          <div class="stat-item">
            <span class="label">收藏</span>
            <span class="value">{{ video.favoriteCount }}</span>
          </div>
        </div>

        <div class="action-row">
          <button class="primary-button action-button" :disabled="pending" @click="handleLike">
            <span class="icon icon-like" :class="{ active: video.liked }">♥</span>
            <span>点赞</span>
          </button>
          <button class="ghost-button action-button" :disabled="pending" @click="handleFavorite">
            <span class="icon icon-favorite" :class="{ active: video.favorited }">★</span>
            <span>收藏</span>
          </button>
        </div>

        <div class="comment-panel">
          <CommentBox :video-id="video.id" :active="true" @commented="handleCommented" @loaded="handleCommentsLoaded" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.detail-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 20px;
}

.video-panel,
.info-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow);
}

.video-wrapper {
  border-radius: 14px;
  overflow: hidden;
  background: #000;
}

.video-wrapper video {
  width: 100%;
  display: block;
  background: #000;
}

.video-meta {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.video-meta h2 {
  margin: 0;
  font-size: 22px;
}

.description {
  color: var(--text);
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  background: #f7f4ee;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-item .label {
  font-size: 12px;
  color: var(--muted);
}

.stat-item .value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.action-row {
  display: flex;
  gap: 12px;
  margin: 16px 0;
}

.action-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.action-button .icon {
  font-size: 16px;
}

.action-button .icon-like,
.action-button .icon-favorite {
  color: #9b9b9b;
}

.action-button .icon-like.active {
  color: #ff4d4f;
}

.action-button .icon-favorite.active {
  color: #f6c343;
}

.comment-panel {
  margin-top: 8px;
}

@media (max-width: 960px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
