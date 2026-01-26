// 管理端 API client，封装用户/视频/看板接口。
import { requestApi } from "./http";

export type PageResponse<T> = {
  total: number;
  page: number;
  pageSize: number;
  items: T[];
};

export type AdminUser = {
  id: number;
  username: string;
  phone?: string;
  email?: string;
  status: number;
  createdAt?: string;
};

export type Video = {
  id: number;
  title: string;
  description?: string;
  uploaderUserId: number;
  videoUrl?: string;
  auditStatus: string;
  isHot: boolean;
  createdAt?: string;
};

export type DailyPlay = {
  day: string;
  playCount: number;
};

export type UserGrowth = {
  day: string;
  newUserCount: number;
};

export type ActiveUser = {
  day: string;
  activeUserCount: number;
};

export type VideoPublish = {
  day: string;
  publishCount: number;
};

export type HotTopn = {
  videoId: number;
  title: string;
  playCount: number;
  likeCount: number;
  commentCount: number;
  favoriteCount: number;
  hotScore: number;
};

const buildQuery = (params: Record<string, string | number | undefined>) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.length > 0) {
      query.append(key, String(value));
    }
  });
  return query.toString();
};

export const fetchUsers = async (params: { page: number; size: number; keyword?: string }) => {
  const query = buildQuery(params);
  return requestApi<PageResponse<AdminUser>>(`/api/admin/users?${query}`);
};

export const updateUserStatus = async (id: number, status: number) => {
  return requestApi<void>(`/api/admin/users/${id}/status`, {
    method: "PATCH",
    body: { status },
  });
};

export const fetchVideos = async (params: { page: number; pageSize: number; sort?: string }) => {
  const query = buildQuery(params);
  return requestApi<PageResponse<Video>>(`/api/videos/page?${query}`);
};

export const uploadVideo = async (payload: {
  uploaderUserId: number;
  title: string;
  description?: string;
  tags?: string;
  file: File;
}) => {
  const form = new FormData();
  form.append("uploaderUserId", String(payload.uploaderUserId));
  form.append("title", payload.title);
  if (payload.description) {
    form.append("description", payload.description);
  }
  if (payload.tags) {
    form.append("tags", payload.tags);
  }
  form.append("video", payload.file);
  return requestApi<Video>("/api/admin/videos", {
    method: "POST",
    body: form,
  });
};

export const auditVideo = async (id: number, status: "APPROVED" | "REJECTED") => {
  return requestApi<void>(`/api/admin/videos/${id}/audit`, {
    method: "PATCH",
    body: { status },
  });
};

export const setVideoHot = async (id: number, isHot: boolean) => {
  return requestApi<void>(`/api/admin/videos/${id}/hot`, {
    method: "PATCH",
    body: { isHot },
  });
};

export const deleteVideo = async (id: number) => {
  return requestApi<void>(`/api/admin/videos/${id}`, {
    method: "DELETE",
  });
};

export const fetchDailyPlay = async (from: string, to: string) => {
  const query = buildQuery({ from, to });
  return requestApi<DailyPlay[]>(`/api/admin/analytics/daily-play?${query}`);
};

export const fetchUserGrowth = async (from: string, to: string) => {
  const query = buildQuery({ from, to });
  return requestApi<UserGrowth[]>(`/api/admin/analytics/user-growth?${query}`);
};

export const fetchActiveUsers = async (from: string, to: string) => {
  const query = buildQuery({ from, to });
  return requestApi<ActiveUser[]>(`/api/admin/analytics/active-users?${query}`);
};

export const fetchHotTopn = async (n: number) => {
  const query = buildQuery({ n });
  return requestApi<HotTopn[]>(`/api/admin/analytics/hot-topn?${query}`);
};

export const fetchVideoPublish = async (from: string, to: string) => {
  const query = buildQuery({ from, to });
  return requestApi<VideoPublish[]>(`/api/admin/analytics/video-publish?${query}`);
};
