import React, { useRef, useState } from 'react';
import { AttachmentResponse } from '../../types/ticket.types';
import { attachmentApi } from '../../api/attachmentApi';
import { Button } from '../ui/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/Card';
import {
  Paperclip,
  Download,
  Upload,
  RefreshCw,
  FileText,
  File,
  FileImage,
  Loader2,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

interface TicketAttachmentSectionProps {
  ticketId: number;
  attachment: AttachmentResponse | null;
  isCustomer: boolean;
  onAttachmentUpdated: () => void;
}

export const TicketAttachmentSection: React.FC<TicketAttachmentSectionProps> = ({
  ticketId,
  attachment,
  isCustomer,
  onAttachmentUpdated,
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  const getFileIcon = (contentType?: string) => {
    if (contentType?.startsWith('image/')) {
      return <FileImage className="h-5 w-5 text-indigo-500" />;
    }
    if (contentType?.includes('pdf')) {
      return <FileText className="h-5 w-5 text-rose-500" />;
    }
    return <File className="h-5 w-5 text-blue-500" />;
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsUploading(true);
      setError('');
      setSuccessMessage('');

      await attachmentApi.uploadAttachment(ticketId, file);
      setSuccessMessage(attachment ? 'Attachment replaced successfully!' : 'Attachment uploaded successfully!');
      setTimeout(() => setSuccessMessage(''), 3000);
      onAttachmentUpdated();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to upload attachment. Please check file type and size.';
      setError(msg);
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDownload = () => {
    if (!attachment) return;
    const downloadUrl = attachmentApi.getDownloadUrl(ticketId, attachment.id);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', attachment.originalFileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <Card>
      <CardHeader className="p-5 pb-3 border-b border-slate-100 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
            <Paperclip className="h-4 w-4 text-blue-600" />
            Ticket Attachment
          </CardTitle>
          {attachment && (
            <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 dark:bg-emerald-950 dark:text-emerald-300 dark:border-emerald-900">
              Current Attachment
            </span>
          )}
        </div>
      </CardHeader>

      <CardContent className="p-5 space-y-3">
        {error && (
          <div className="text-xs text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/30 p-2.5 rounded-lg border border-rose-200 dark:border-rose-900 flex items-start gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {successMessage && (
          <div className="text-xs text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/30 p-2.5 rounded-lg border border-emerald-200 dark:border-emerald-900 flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {attachment ? (
          <div className="space-y-3">
            <div className="p-3 bg-slate-50 dark:bg-slate-900/50 rounded-xl border border-slate-200/80 dark:border-slate-800 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <div className="h-9 w-9 rounded-lg bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center shrink-0">
                  {getFileIcon(attachment.contentType)}
                </div>
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-slate-900 dark:text-slate-100 truncate" title={attachment.originalFileName}>
                    {attachment.originalFileName}
                  </div>
                  <div className="text-[11px] text-slate-400 flex items-center gap-2 mt-0.5">
                    <span>{formatFileSize(attachment.fileSize)}</span>
                    <span>•</span>
                    <span>Uploaded {formatDate(attachment.createdAt)}</span>
                  </div>
                </div>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={handleDownload}
                className="shrink-0 text-blue-600 border-blue-200 hover:bg-blue-50 dark:border-blue-900 dark:text-blue-300 dark:hover:bg-blue-950"
              >
                <Download className="h-3.5 w-3.5 mr-1" />
                Download
              </Button>
            </div>

            {isCustomer && (
              <div className="pt-1">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept=".pdf,.jpg,.jpeg,.png,.webp,.doc,.docx"
                  className="hidden"
                />
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                  isLoading={isUploading}
                  className="w-full text-xs text-slate-600 hover:text-slate-900 border border-dashed border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-400 dark:hover:bg-slate-800"
                >
                  <RefreshCw className="h-3.5 w-3.5 mr-1.5 text-blue-600" />
                  Re-upload / Replace Attachment
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center py-3 space-y-3">
            <p className="text-xs text-slate-400 italic">
              No file attached to this ticket.
            </p>

            {isCustomer && (
              <div>
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept=".pdf,.jpg,.jpeg,.png,.webp,.doc,.docx"
                  className="hidden"
                />
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                  isLoading={isUploading}
                  className="w-full text-xs"
                >
                  <Upload className="h-3.5 w-3.5 mr-1.5 text-blue-600" />
                  Upload Attachment (Optional)
                </Button>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
