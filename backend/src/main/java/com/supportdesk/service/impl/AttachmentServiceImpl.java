package com.supportdesk.service.impl;

import com.supportdesk.dto.attachment.AttachmentResponse;
import com.supportdesk.entity.Ticket;
import com.supportdesk.entity.TicketAttachment;
import com.supportdesk.entity.User;
import com.supportdesk.enums.TicketActivityType;
import com.supportdesk.exception.BadRequestException;
import com.supportdesk.exception.ResourceNotFoundException;
import com.supportdesk.repository.TicketAttachmentRepository;
import com.supportdesk.repository.TicketRepository;
import com.supportdesk.repository.UserRepository;
import com.supportdesk.security.UserPrincipal;
import com.supportdesk.service.AttachmentService;
import com.supportdesk.service.TicketActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketActivityService activityService;

    private static final String UPLOAD_DIR = "uploads/attachments/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "webp", "doc", "docx"
    );

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, UserPrincipal currentUser) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds the maximum limit of 10 MB");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment"
        );
        String extension = getFileExtension(originalFileName).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file type. Allowed formats: PDF, JPG, JPEG, PNG, WEBP, DOC, DOCX");
        }

        Ticket ticket = ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (currentUser.getRole().isCustomer() && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to attach files to this ticket");
        }

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if ticket already has a latest attachment (re-upload case)
        Optional<TicketAttachment> existingLatest = attachmentRepository.findByTicketIdAndIsLatestTrue(ticketId);
        boolean isReplacement = existingLatest.isPresent();

        if (isReplacement) {
            TicketAttachment prev = existingLatest.get();
            prev.setLatest(false);
            attachmentRepository.save(prev);
        }

        // Create storage directory if missing
        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload storage location", e);
        }

        String storageFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path targetPath = uploadPath.resolve(storageFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment file on server", e);
        }

        TicketAttachment attachment = TicketAttachment.builder()
                .ticket(ticket)
                .uploadedBy(uploader)
                .originalFileName(originalFileName)
                .storageFileName(storageFileName)
                .filePath(targetPath.toString())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .isLatest(true)
                .build();

        TicketAttachment savedAttachment = attachmentRepository.save(attachment);

        // Record Activity Log
        TicketActivityType activityType = isReplacement ? TicketActivityType.ATTACHMENT_REPLACED : TicketActivityType.ATTACHMENT_UPLOADED;
        String actionDesc = isReplacement
                ? "Attachment replaced/re-uploaded by " + uploader.getFullName() + " (" + originalFileName + ")"
                : "Attachment uploaded by " + uploader.getFullName() + " (" + originalFileName + ")";

        activityService.logActivity(ticket, uploader, activityType, actionDesc);

        return mapToResponse(savedAttachment);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getLatestAttachment(Long ticketId, UserPrincipal currentUser) {
        Ticket ticket = ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (currentUser.getRole().isCustomer() && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view attachments for this ticket");
        }

        return attachmentRepository.findByTicketIdAndIsLatestTrue(ticketId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long ticketId, Long attachmentId, UserPrincipal currentUser) {
        Ticket ticket = ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (currentUser.getRole().isCustomer() && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to download attachments for this ticket");
        }

        TicketAttachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Could not read file: " + attachment.getOriginalFileName());
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path error for attachment: " + attachment.getOriginalFileName());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1)
                : "";
    }

    private AttachmentResponse mapToResponse(TicketAttachment attachment) {
        User uploader = attachment.getUploadedBy();
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicket().getId())
                .originalFileName(attachment.getOriginalFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .uploadedById(uploader != null ? uploader.getId() : null)
                .uploadedByName(uploader != null ? uploader.getFullName() : "Unknown")
                .isLatest(attachment.isLatest())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
