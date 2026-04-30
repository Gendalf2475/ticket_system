package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewRating;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;

public class TicketReviewService {
    private final TelegramRepository repository;
    private final TelegramFormatService formatService;
    private final TicketSyncService ticketSyncService;

    public TicketReviewService(TelegramRepository repository, TelegramFormatService formatService, TicketSyncService ticketSyncService) {
        this.repository = repository;
        this.formatService = formatService;
        this.ticketSyncService = ticketSyncService;
    }

    public ReviewTicketResult reviewTicket(int ticketId, ReviewRating rating, TelegramUserProfile reviewer, String comment) {
        ReviewTicketResult result = repository.reviewTicketAtomically(
                ticketId,
                rating,
                reviewer,
                formatService.plainTelegramDisplayName(reviewer),
                comment
        );

        if (result.isSuccess()) {
            ticketSyncService.syncReviewedTicketAsync(ticketId);
        }

        return result;
    }
}
