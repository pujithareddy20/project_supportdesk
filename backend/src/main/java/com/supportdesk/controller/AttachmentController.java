package com.supportdesk.controller;

import com.supportdesk.dto.attachment.AttachmentResponse;
import com.supportdesk.dto.common.ApiResponse;
import com.supportdesk.security.UserPrincipal;
import com.supportdesk.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ROLE_CUSTOMER', 'AGENT', 'ROLE_AGENT', 'SUPPORT_AGENT', 'ROLE_SUPPORT_AGENT', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        AttachmentResponse response = attachmentService.uploadAttachment(ticketId, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attachment uploaded successfully", response));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<AttachmentResponse>> getLatestAttachment(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        AttachmentResponse response = attachmentService.getLatestAttachment(ticketId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Resource fileResource = attachmentService.downloadAttachment(ticketId, attachmentId, currentUser);

        AttachmentResponse metadata = attachmentService.getLatestAttachment(ticketId, currentUser);
        String fileName = (metadata != null) ? metadata.getOriginalFileName() : fileResource.getFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(fileResource);
    }
}
