package com.supportdesk.repository;

import com.supportdesk.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    Optional<TicketAttachment> findByTicketIdAndIsLatestTrue(Long ticketId);
    List<TicketAttachment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    Optional<TicketAttachment> findByIdAndTicketId(Long id, Long ticketId);
}
