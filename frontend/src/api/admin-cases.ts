import apiClient from './client';

export type CosmeticType = 'avatar' | 'avatar-frame' | 'profile-background' | 'badge';
export type CosmeticRarity = 'Обычный' | 'Редкий' | 'Эпический';

export interface AdminCaseReward {
  id: string;
  name: string;
  cosmeticType: CosmeticType;
  rarity: CosmeticRarity;
  weight: number;
  chance: number;
  imageUrl: string;
}

export interface AdminCase {
  id: string;
  name: string;
  description: string;
  price: number;
  active: boolean;
  imageUrl: string;
  rewards: AdminCaseReward[];
}

export interface SaveAdminCase {
  name: string;
  description: string;
  price: number;
  image?: File | null;
}

export interface SaveAdminCaseReward {
  name: string;
  cosmeticType: CosmeticType;
  rarity: CosmeticRarity;
  weight: number;
  image?: File | null;
}

function caseFormData(value: SaveAdminCase): FormData {
  const form = new FormData();
  form.append('name', value.name);
  form.append('description', value.description);
  form.append('price', String(value.price));
  if (value.image) form.append('image', value.image);
  return form;
}

function rewardFormData(value: SaveAdminCaseReward): FormData {
  const form = new FormData();
  form.append('name', value.name);
  form.append('cosmeticType', value.cosmeticType);
  form.append('rarity', value.rarity);
  form.append('weight', String(value.weight));
  if (value.image) form.append('image', value.image);
  return form;
}

export async function listAdminCases(): Promise<AdminCase[]> {
  const response = await apiClient.get<AdminCase[]>('/admin/cases');
  return response.data;
}

export async function createAdminCase(value: SaveAdminCase): Promise<AdminCase> {
  const response = await apiClient.post<AdminCase>('/admin/cases', caseFormData(value));
  return response.data;
}

export async function updateAdminCase(id: string, value: SaveAdminCase): Promise<AdminCase> {
  const response = await apiClient.put<AdminCase>(`/admin/cases/${id}`, caseFormData(value));
  return response.data;
}

export async function deactivateAdminCase(id: string): Promise<void> {
  await apiClient.delete(`/admin/cases/${id}`);
}

export async function createAdminCaseReward(caseId: string, value: SaveAdminCaseReward): Promise<AdminCaseReward> {
  const response = await apiClient.post<AdminCaseReward>(
    `/admin/cases/${caseId}/rewards`,
    rewardFormData(value),
  );
  return response.data;
}

export async function updateAdminCaseReward(
  caseId: string,
  rewardId: string,
  value: SaveAdminCaseReward,
): Promise<AdminCaseReward> {
  const response = await apiClient.put<AdminCaseReward>(
    `/admin/cases/${caseId}/rewards/${rewardId}`,
    rewardFormData(value),
  );
  return response.data;
}

export async function deleteAdminCaseReward(caseId: string, rewardId: string): Promise<void> {
  await apiClient.delete(`/admin/cases/${caseId}/rewards/${rewardId}`);
}
