import apiClient from './client';

export type PassRewardTrack = 'free' | 'premium';
export type PassRewardKind = 'case' | 'cosmetic';
export type PassCosmeticType = 'avatar' | 'avatar-frame' | 'profile-background' | 'badge';
export type PassRewardRarity = 'Обычный' | 'Редкий' | 'Эпический';

export interface AdminPassReward {
  id: string;
  track: PassRewardTrack;
  level: number;
  title: string;
  rewardKind: PassRewardKind;
  cosmeticType: PassCosmeticType | null;
  rarity: PassRewardRarity;
  caseId: string | null;
}

export interface AdminPassConfiguration {
  seasonId: string;
  seasonName: string;
  startsAt: string;
  endsAt: string;
  premiumPrice: number;
  seasons: AdminPassSeason[];
  rewards: AdminPassReward[];
  cases: Array<{ id: string; name: string }>;
}

export interface AdminPassSeason {
  id: string;
  seasonName: string;
  startsAt: string;
  endsAt: string;
  premiumPrice: number;
  active: boolean;
}

export interface SavePassRewardRequest {
  track: PassRewardTrack;
  level: number;
  title: string;
  rewardKind: PassRewardKind;
  cosmeticType: PassCosmeticType | null;
  rarity: PassRewardRarity;
  caseId: string | null;
}

export async function getAdminPassConfiguration(): Promise<AdminPassConfiguration> {
  const response = await apiClient.get<AdminPassConfiguration>('/admin/campus/pass');
  return response.data;
}

export async function saveAdminPassSeason(request: {
  seasonName: string;
  startsAt: string;
  endsAt: string;
  premiumPrice: number;
}): Promise<AdminPassConfiguration> {
  const response = await apiClient.put<AdminPassConfiguration>('/admin/campus/pass/season', request);
  return response.data;
}

export async function createAdminPassSeason(request: {
  seasonName: string;
  startsAt: string;
  endsAt: string;
  premiumPrice: number;
  copyRewards: boolean;
}): Promise<AdminPassConfiguration> {
  const response = await apiClient.post<AdminPassConfiguration>('/admin/campus/pass/seasons', request);
  return response.data;
}

export async function createAdminPassReward(request: SavePassRewardRequest): Promise<AdminPassReward> {
  const response = await apiClient.post<AdminPassReward>('/admin/campus/pass/rewards', request);
  return response.data;
}

export async function updateAdminPassReward(id: string, request: SavePassRewardRequest): Promise<AdminPassReward> {
  const response = await apiClient.put<AdminPassReward>(`/admin/campus/pass/rewards/${id}`, request);
  return response.data;
}

export async function deleteAdminPassReward(id: string): Promise<void> {
  await apiClient.delete(`/admin/campus/pass/rewards/${id}`);
}
