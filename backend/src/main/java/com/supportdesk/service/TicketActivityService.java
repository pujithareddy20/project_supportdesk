package com.supportdesk.service;

import com.supportdesk.dto.activity.TicketActivityResponse;
import com.supportdesk.entity.Ticket;
import com.supportdesk.entity.User;
import com.supportdesk.enums.TicketActivityType;
import com.supportdesk.security.UserPrincipal;

import java.util.List;

public interface TicketActivityService {
    void logActivity(Ticket ticket, User performer, TicketActivityType activityType, String description);
    List<TicketActivityResponse> getActivitiesByTicketId(Long ticketId, UserPrincipal currentUser);
}
