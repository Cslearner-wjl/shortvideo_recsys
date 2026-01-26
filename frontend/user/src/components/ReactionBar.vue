<script setup lang="ts">
const props = defineProps<{
  playCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  pending?: boolean;
}>();

const emit = defineEmits<{
  (event: "like"): void;
  (event: "favorite"): void;
}>();
</script>

<template>
  <div class="reaction-bar stat-grid">
    <div class="stat-item">
      <span class="stat-label">播放</span>
      <span class="stat-value">{{ props.playCount }}</span>
    </div>
    <button class="stat-item action" type="button" :disabled="props.pending" @click="emit('like')">
      <span class="stat-label">点赞</span>
      <span class="stat-value">{{ props.likeCount }}</span>
    </button>
    <div class="stat-item">
      <span class="stat-label">评论</span>
      <span class="stat-value">{{ props.commentCount }}</span>
    </div>
    <button class="stat-item action" type="button" :disabled="props.pending" @click="emit('favorite')">
      <span class="stat-label">收藏</span>
      <span class="stat-value">{{ props.favoriteCount }}</span>
    </button>
  </div>
</template>

<style scoped>
.reaction-bar {
  display: flex;
  flex-direction: column;
}

.reaction-bar button.stat-item {
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.04);
  text-align: left;
}
</style>
