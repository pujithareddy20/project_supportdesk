package com.supportdesk.dto.activity;

import com.supportdesk.enums.Role;
import com.supportdesk.enums.TicketActivityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketActivityResponse {
    private Long id;
    private Long ticketId;
    private Long userId;
    private String performerName;
    private Role performerRole;
    private TicketActivityType activityType;
    private String description;
    private LocalDateTime createdAt;
}
