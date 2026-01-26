import { request } from "./http";
import { VideoItem } from "./video";

const normalizeList = (payload: any): VideoItem[] => {
  const list = payload?.data?.items ?? payload?.data ?? payload?.items ?? payload ?? [];
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map((item) => ({
    id: String(item.id ?? item.videoId ?? ""),
    title: String(item.title ?? item.name ?? "未命名"),
    author: String(item.author ?? item.uploader ?? "未知作者"),
    coverUrl: item.coverUrl ?? item.cover ?? item.thumbnail,
    videoUrl: item.videoUrl ?? item.video_url ?? item.url ?? item.playUrl,
    createdAt: item.createdAt ?? item.created_at ?? null,
    hotScore: item.hotScore != null ? Number(item.hotScore) : undefined,
    playCount: Number(item.playCount ?? item.stats?.playCount ?? 0),
    likeCount: Number(item.likeCount ?? item.stats?.likeCount ?? 0),
    favoriteCount: Number(item.favoriteCount ?? item.stats?.favoriteCount ?? 0),
    commentCount: Number(item.commentCount ?? item.stats?.commentCount ?? 0),
  }));
};

export const fetchHotRank = async (page = 1, pageSize = 12) => {
  const data = await request(`/api/rank/hot?page=${page}&pageSize=${pageSize}`);
  return normalizeList(data);
};
