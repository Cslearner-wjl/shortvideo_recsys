<script setup lang="ts">
import ReactionBar from "./ReactionBar.vue";
import CommentBox from "./CommentBox.vue";

const props = defineProps<{
  video: {
    id: string;
    title: string;
    author: string;
    coverUrl?: string;
    playCount: number;
    likeCount: number;
    favoriteCount: number;
    commentCount: number;
  };
  pending?: boolean;
}>();

const emit = defineEmits<{
  (event: "play", id: string): void;
  (event: "like", id: string): void;
  (event: "favorite", id: string): void;
  (event: "comment", id: string, content: string): void;
}>();
</script>

<template>
  <article class="card video-card fade-up">
    <div class="video-cover" @click="emit('play', props.video.id)">
      <img v-if="props.video.coverUrl" :src="props.video.coverUrl" :alt="props.video.title" />
      <div v-else class="cover-placeholder">暂无封面</div>
      <div class="play-chip">播放</div>
    </div>
    <div class="video-meta">
      <div class="video-title">{{ props.video.title }}</div>
      <div class="muted">作者：{{ props.video.author }}</div>
    </div>
    <ReactionBar
      :play-count="props.video.playCount"
      :like-count="props.video.likeCount"
      :favorite-count="props.video.favoriteCount"
      :comment-count="props.video.commentCount"
      :pending="props.pending"
      @like="emit('like', props.video.id)"
      @favorite="emit('favorite', props.video.id)"
    />
    <CommentBox @submit="(content) => emit('comment', props.video.id, content)" />
  </article>
</template>

<style scoped>
.video-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.video-cover {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #f1f1f1;
  cursor: pointer;
  min-height: 200px;
}

.video-cover img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7a7a7a;
  font-weight: 600;
}

.play-chip {
  position: absolute;
  bottom: 12px;
  right: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #ffffff;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.video-title {
  font-size: 18px;
  font-weight: 700;
}
</style>
