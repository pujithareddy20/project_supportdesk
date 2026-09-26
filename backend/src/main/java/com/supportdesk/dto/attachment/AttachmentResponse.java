package com.supportdesk.dto.attachment;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentResponse {
    private Long id;
    private Long ticketId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private Long uploadedById;
    private String uploadedByName;
    private boolean isLatest;
    private LocalDateTime createdAt;
}
