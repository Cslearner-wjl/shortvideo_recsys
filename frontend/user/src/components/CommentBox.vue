<script setup lang="ts">
// 评论列表与输入框组件，支持分页加载与点赞。
import { computed, onMounted, ref, watch } from "vue";
import {
  commentVideo,
  fetchComments,
  likeComment,
  unlikeComment,
  type CommentItem,
} from "../services/video";
import { getToken } from "../services/token";

const props = defineProps<{
  videoId: string | number;
  active?: boolean;
  allowPost?: boolean;
  pageSize?: number;
  showHeader?: boolean;
}>();

const emit = defineEmits<{
  (event: "commented"): void;
  (event: "loaded", total: number): void;
}>();

const comments = ref<CommentItem[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const hasMore = ref(true);
const errorMessage = ref("");
const content = ref("");
const pendingLikeIds = ref(new Set<number>());

const isActive = computed(() => props.active !== false);
const canPost = computed(() => props.allowPost !== false);
const showHeader = computed(() => props.showHeader !== false);
const resolvedPageSize = computed(() => Math.max(10, props.pageSize ?? 20));

const loadComments = async (reset = false) => {
  if (!props.videoId || !isActive.value) return;
  if (loading.value) return;

  loading.value = true;
  errorMessage.value = "";
  try {
    if (reset) {
      comments.value = [];
      page.value = 1;
      hasMore.value = true;
    }
    const result = await fetchComments(String(props.videoId), page.value, resolvedPageSize.value);
    total.value = result.total;
    if (reset) {
      comments.value = result.items;
    } else {
      comments.value = [...comments.value, ...result.items];
    }
    hasMore.value = result.items.length >= resolvedPageSize.value;
    page.value += 1;
    emit("loaded", result.total);
  } catch (error: any) {
    errorMessage.value = error?.message || "加载评论失败";
  } finally {
    loading.value = false;
  }
};

const submit = async () => {
  const value = content.value.trim();
  if (!value) return;
  if (!props.videoId) return;
  if (!canPost.value) return;
  if (!getToken()) {
    errorMessage.value = "请先登录后发表评论";
    return;
  }
  try {
    await commentVideo(String(props.videoId), value);
    content.value = "";
    emit("commented");
    await loadComments(true);
  } catch (error: any) {
    errorMessage.value = error?.message || "发送评论失败";
  }
};

const toggleLike = async (comment: CommentItem) => {
  if (!comment?.id) return;
  if (pendingLikeIds.value.has(comment.id)) return;
  pendingLikeIds.value.add(comment.id);
  try {
    if (comment.liked) {
      await unlikeComment(comment.id);
      comment.liked = false;
      comment.likeCount = Math.max(0, comment.likeCount - 1);
    } else {
      await likeComment(comment.id);
      comment.liked = true;
      comment.likeCount += 1;
    }
  } catch (error: any) {
    errorMessage.value = error?.message || "点赞操作失败";
  } finally {
    pendingLikeIds.value.delete(comment.id);
  }
};

const displayAvatar = (comment: CommentItem) => {
  const avatar = comment.user?.avatarUrl;
  if (avatar) {
    return { type: "image", value: avatar };
  }
  const name = comment.user?.username || "U";
  return { type: "text", value: name.slice(0, 1).toUpperCase() };
};

const handleLoadMore = () => {
  if (loading.value || !hasMore.value) return;
  loadComments(false);
};

watch(
  () => props.videoId,
  () => {
    if (isActive.value) {
      loadComments(true);
    }
  }
);

watch(
  () => props.active,
  (value) => {
    if (value) {
      loadComments(true);
    }
  }
);

onMounted(() => {
  if (isActive.value) {
    loadComments(true);
  }
});
</script>

<template>
  <div class="comment-panel">
    <div v-if="showHeader" class="comment-header">
      <h3>评论 {{ total ? `(${total})` : "" }}</h3>
      <span class="muted" v-if="loading">加载中...</span>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="comment-list">
      <div v-if="loading && comments.length === 0" class="muted empty">加载中...</div>
      <div v-if="!loading && comments.length === 0" class="muted empty">暂无评论，快来抢沙发！</div>
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="avatar">
          <img
            v-if="displayAvatar(comment).type === 'image'"
            :src="displayAvatar(comment).value"
            alt="avatar"
          />
          <span v-else>{{ displayAvatar(comment).value }}</span>
        </div>
        <div class="comment-body">
          <div class="comment-meta">
            <span class="username">{{ comment.user?.username || "匿名用户" }}</span>
            <span class="time">{{ comment.createdAt }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <button class="like-button" type="button" :disabled="pendingLikeIds.has(comment.id)" @click="toggleLike(comment)">
            <span class="icon">{{ comment.liked ? "❤️" : "🤍" }}</span>
            <span>{{ comment.likeCount }}</span>
          </button>
        </div>
      </div>

      <button v-if="hasMore" class="load-more" type="button" @click="handleLoadMore">
        {{ loading ? "加载中..." : "加载更多" }}
      </button>
    </div>

    <div v-if="canPost" class="comment-box">
      <input v-model="content" class="input" type="text" placeholder="写下评论..." @keyup.enter="submit" />
      <button class="primary-button" type="button" @click="submit">发送</button>
    </div>
  </div>
</template>

<style scoped>
.comment-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: var(--surface);
  border: 1px solid var(--border);
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #efece6;
  color: #8d857b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
}

.comment-content {
  font-size: 14px;
  color: var(--text);
}

.like-button {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: var(--accent);
  cursor: pointer;
  font-size: 12px;
  padding: 0;
}

.like-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.load-more {
  align-self: center;
  border: none;
  background: transparent;
  color: var(--accent);
  cursor: pointer;
  font-size: 14px;
}

.comment-box {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 4px;
}

.comment-box .input {
  flex: 1;
}

.empty {
  text-align: center;
  padding: 12px 0;
}
</style>
