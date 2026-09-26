import { axiosClient } from './axiosClient';
import { ApiResponse } from '../types/api.types';
import { AttachmentResponse } from '../types/ticket.types';

export const attachmentApi = {
  uploadAttachment: async (ticketId: number, file: File): Promise<AttachmentResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const res = await axiosClient.post<ApiResponse<AttachmentResponse>>(
      `/api/tickets/${ticketId}/attachments`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return res.data.data;
  },

  getLatestAttachment: async (ticketId: number): Promise<AttachmentResponse | null> => {
    const res = await axiosClient.get<ApiResponse<AttachmentResponse>>(
      `/api/tickets/${ticketId}/attachments/latest`
    );
    return res.data.data;
  },

  getDownloadUrl: (ticketId: number, attachmentId: number): string => {
    return `/api/tickets/${ticketId}/attachments/${attachmentId}/download`;
  },
};
