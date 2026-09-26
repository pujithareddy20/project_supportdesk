package com.supportdesk.repository;

import com.supportdesk.entity.Ticket;
import com.supportdesk.enums.TicketCategory;
import com.supportdesk.enums.TicketPriority;
import com.supportdesk.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.time.LocalTime;

public class TicketSpecification {

    public static Specification<Ticket> filterTickets(
            Long customerId,
            String search,
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            Long assignedAgentId,
            Boolean unassigned,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Customer Scoping (if customerId is present, strictly restrict to this customer)
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }

            // 2. Search by Ticket Number, Title, or ID
            if (StringUtils.hasText(search)) {
                String searchTrimmed = search.trim();
                String searchPattern = "%" + searchTrimmed.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate numberMatch = cb.like(cb.lower(root.get("ticketNumber")), searchPattern);

                if (searchTrimmed.matches("\\d+")) {
                    try {
                        Long idVal = Long.parseLong(searchTrimmed);
                        Predicate idMatch = cb.equal(root.get("id"), idVal);
                        predicates.add(cb.or(titleMatch, numberMatch, idMatch));
                    } catch (NumberFormatException e) {
                        predicates.add(cb.or(titleMatch, numberMatch));
                    }
                } else {
                    predicates.add(cb.or(titleMatch, numberMatch));
                }
            }

            // 3. Filter by Status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 4. Filter by Priority
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 5. Filter by Category
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // 6. Filter by Assigned Agent
            if (assignedAgentId != null) {
                predicates.add(cb.equal(root.get("assignedAgent").get("id"), assignedAgentId));
            } else if (Boolean.TRUE.equals(unassigned)) {
                predicates.add(cb.isNull(root.get("assignedAgent")));
            }

            // 7. Filter by Date Range (startDate and endDate)
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
