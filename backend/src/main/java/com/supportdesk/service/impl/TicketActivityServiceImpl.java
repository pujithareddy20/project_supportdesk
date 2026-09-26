package com.supportdesk.service.impl;

import com.supportdesk.dto.activity.TicketActivityResponse;
import com.supportdesk.entity.Ticket;
import com.supportdesk.entity.TicketActivity;
import com.supportdesk.entity.User;
import com.supportdesk.enums.TicketActivityType;
import com.supportdesk.exception.ResourceNotFoundException;
import com.supportdesk.repository.TicketActivityRepository;
import com.supportdesk.repository.TicketRepository;
import com.supportdesk.security.UserPrincipal;
import com.supportdesk.service.TicketActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketActivityServiceImpl implements TicketActivityService {

    private final TicketActivityRepository activityRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public void logActivity(Ticket ticket, User performer, TicketActivityType activityType, String description) {
        if (ticket == null) return;

        String performerName = performer != null ? performer.getFullName() : "System";

        TicketActivity activity = TicketActivity.builder()
                .ticket(ticket)
                .user(performer)
                .performerName(performerName)
                .activityType(activityType)
                .description(description)
                .build();

        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketActivityResponse> getActivitiesByTicketId(Long ticketId, UserPrincipal currentUser) {
        Ticket ticket = ticketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (currentUser.getRole().isCustomer() && !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view activity history for this ticket");
        }

        return activityRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TicketActivityResponse mapToResponse(TicketActivity activity) {
        User user = activity.getUser();
        return TicketActivityResponse.builder()
                .id(activity.getId())
                .ticketId(activity.getTicket().getId())
                .userId(user != null ? user.getId() : null)
                .performerName(activity.getPerformerName())
                .performerRole(user != null ? user.getRole() : null)
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
