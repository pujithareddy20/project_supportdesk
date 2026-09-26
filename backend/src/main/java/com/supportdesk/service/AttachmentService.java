package com.supportdesk.service;

import com.supportdesk.dto.attachment.AttachmentResponse;
import com.supportdesk.security.UserPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, UserPrincipal currentUser);
    AttachmentResponse getLatestAttachment(Long ticketId, UserPrincipal currentUser);
    Resource downloadAttachment(Long ticketId, Long attachmentId, UserPrincipal currentUser);
}
