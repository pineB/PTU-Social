import apiClient from './client';

export type CertificateType = 'EDUCATION' | 'INCOME' | 'MILITARY' | 'OTHER';
export type CertificateStatus = 'PENDING' | 'IN_PROGRESS' | 'READY' | 'APPROVED' | 'ISSUED' | 'REJECTED';

export interface CertificateRequestView {
  id: string;
  studentId: string;
  studentName: string;
  type: CertificateType;
  customTitle: string | null;
  description: string;
  deliveryMethod: 'PICKUP' | 'DIGITAL';
  status: CertificateStatus;
  rejectionReason: string | null;
  documentName: string | null;
  documentAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function listMyCertificates(): Promise<CertificateRequestView[]> {
  const response = await apiClient.get<CertificateRequestView[]>('/certificates/my');
  return response.data;
}

export async function listCertificates(): Promise<CertificateRequestView[]> {
  const response = await apiClient.get<PageResponse<CertificateRequestView>>('/certificates', {
    params: { page: 0, size: 100 },
  });
  return response.data.content;
}

export async function createCertificateRequest(request: {
  type: CertificateType;
  customTitle: string | null;
  description: string;
  deliveryMethod: 'PICKUP' | 'DIGITAL';
}): Promise<CertificateRequestView> {
  const response = await apiClient.post<CertificateRequestView>('/certificates', request);
  return response.data;
}

export async function uploadCertificateDocument(id: string, file: File): Promise<CertificateRequestView> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await apiClient.post<CertificateRequestView>(`/certificates/${id}/document`, formData);
  return response.data;
}

export async function downloadCertificateDocument(id: string, fileName: string): Promise<void> {
  const response = await apiClient.get<Blob>(`/certificates/${id}/document`, { responseType: 'blob' });
  const objectUrl = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(objectUrl);
}

export async function updateCertificateRequestStatus(
  id: string,
  status: Exclude<CertificateStatus, 'PENDING' | 'APPROVED'>,
  rejectionReason?: string,
): Promise<CertificateRequestView> {
  const response = await apiClient.patch<CertificateRequestView>(`/certificates/${id}/status`, {
    status,
    ...(status === 'REJECTED' ? { rejectionReason } : {}),
  });
  return response.data;
}
