// 用户端视频与评论接口封装，统一处理播放与互动。
import { HttpError, request } from "./http";

export type VideoItem = {
  id: string;
  title: string;
  description?: string;
  author: string;
  uploaderUserId?: number;
  coverUrl?: string;
  videoUrl?: string;
  createdAt?: string | null;
  hotScore?: number;
  playCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  liked?: boolean;
  favorited?: boolean;
};

const normalizeVideo = (raw: any): VideoItem => {
  return {
    id: String(raw.id ?? raw.videoId ?? raw.uuid ?? ""),
    title: String(raw.title ?? raw.name ?? "未命名"),
    description: raw.description ?? "",
    author: String(raw.author ?? raw.uploaderName ?? raw.uploader ?? raw.owner ?? "未知作者"),
    uploaderUserId: raw.uploaderUserId != null ? Number(raw.uploaderUserId) : undefined,
    coverUrl: raw.coverUrl ?? raw.cover ?? raw.thumbnail,
    videoUrl: raw.videoUrl ?? raw.video_url ?? raw.url ?? raw.playUrl,
    createdAt: raw.createdAt ?? raw.created_at ?? null,
    hotScore: raw.hotScore != null ? Number(raw.hotScore) : undefined,
    playCount: Number(raw.playCount ?? raw.stats?.playCount ?? 0),
    likeCount: Number(raw.likeCount ?? raw.stats?.likeCount ?? 0),
    favoriteCount: Number(raw.favoriteCount ?? raw.stats?.favoriteCount ?? 0),
    commentCount: Number(raw.commentCount ?? raw.stats?.commentCount ?? 0),
    liked: Boolean(raw.liked ?? raw.isLiked ?? false),
    favorited: Boolean(raw.favorited ?? raw.isFavorited ?? false),
  };
};

export type FeedPage = {
  items: VideoItem[];
  page: number;
  pageSize: number;
  nextCursor?: string | null;
  total?: number;
  source: "recommendations" | "videos";
};

export type CommentUser = {
  id: number;
  username: string;
  avatarUrl?: string | null;
};

export type CommentItem = {
  id: number;
  content: string;
  createdAt: string;
  likeCount: number;
  liked: boolean;
  user: CommentUser;
};

export type CommentPage = {
  total: number;
  page: number;
  pageSize: number;
  items: CommentItem[];
};

const normalizePage = (payload: any, source: FeedPage["source"]): FeedPage => {
  const data = payload?.data ?? payload ?? {};
  const items = Array.isArray(data.items)
    ? data.items
    : Array.isArray(data)
      ? data
      : Array.isArray(data.data)
        ? data.data
        : [];
  return {
    items: items.map(normalizeVideo),
    page: Number(data.page ?? 1),
    pageSize: Number(data.pageSize ?? 20),
    nextCursor: data.nextCursor ?? null,
    total: data.total != null ? Number(data.total) : undefined,
    source,
  };
};

export const fetchFeedPage = async (page = 1, pageSize = 12, cursor?: string | null): Promise<FeedPage> => {
  try {
    const params = new URLSearchParams({
      page: String(page),
      pageSize: String(pageSize),
    });
    if (cursor) {
      params.set("cursor", cursor);
    }
    const data = await request(`/api/recommendations?${params.toString()}`);
    return normalizePage(data, "recommendations");
  } catch (error) {
    // 推荐接口在后端侧通常要求登录；未登录/未实现时降级到分页视频列表
    if (error instanceof HttpError && ![401, 403, 404].includes(error.status)) {
      throw error;
    }
  }
  const fallback = await request(`/api/videos/page?page=${page}&pageSize=${pageSize}`);
  return normalizePage(fallback, "videos");
};

export const reportPlay = (id: string, payload?: { durationMs?: number; isCompleted?: boolean }) => {
  return request(`/api/videos/${id}/play`, {
    method: "POST",
    body: payload,
  });
};

export const likeVideo = (id: string) => {
  return request(`/api/videos/${id}/like`, { method: "POST" });
};

export const unlikeVideo = (id: string) => {
  return request(`/api/videos/${id}/like`, { method: "DELETE" });
};

export const favoriteVideo = (id: string) => {
  return request(`/api/videos/${id}/favorite`, { method: "POST" });
};

export const unfavoriteVideo = (id: string) => {
  return request(`/api/videos/${id}/favorite`, { method: "DELETE" });
};

export const commentVideo = (id: string, content: string) => {
  return request(`/api/videos/${id}/comments`, {
    method: "POST",
    body: { content },
  });
};

export const fetchComments = async (videoId: string, page = 1, pageSize = 20): Promise<CommentPage> => {
  const data = await request(`/api/videos/${videoId}/comments?page=${page}&pageSize=${pageSize}`);
  const payload = data?.data ?? data ?? {};
  const items = Array.isArray(payload.items) ? payload.items : [];
  return {
    total: Number(payload.total ?? 0),
    page: Number(payload.page ?? page),
    pageSize: Number(payload.pageSize ?? pageSize),
    items: items.map((item) => ({
      id: Number(item.id ?? 0),
      content: String(item.content ?? ""),
      createdAt: String(item.createdAt ?? ""),
      likeCount: Number(item.likeCount ?? 0),
      liked: Boolean(item.liked),
      user: item.user
        ? {
            id: Number(item.user.id ?? 0),
            username: String(item.user.username ?? "匿名用户"),
            avatarUrl: item.user.avatarUrl ?? null,
          }
        : {
            id: 0,
            username: "匿名用户",
            avatarUrl: null,
          },
    })),
  };
};

export const fetchVideoDetail = async (videoId: string): Promise<VideoItem> => {
  const data = await request(`/api/videos/${videoId}`);
  const payload = data?.data ?? data ?? {};
  return normalizeVideo(payload);
};

export const likeComment = (commentId: number) => {
  return request(`/api/comments/${commentId}/likes`, { method: "POST" });
};

export const unlikeComment = (commentId: number) => {
  return request(`/api/comments/${commentId}/likes`, { method: "DELETE" });
};
