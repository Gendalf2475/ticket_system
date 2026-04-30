package com.github.henriquemb.ticketsystem.telegram.model;

public class ReviewTicketResult {
    public enum Status {
        SUCCESS,
        NOT_FOUND,
        NOT_CLOSED,
        ALREADY_REVIEWED,
        ERROR
    }

    private final Status status;
    private final TicketRow ticket;

    public ReviewTicketResult(Status status, TicketRow ticket) {
        this.status = status;
        this.ticket = ticket;
    }

    public Status getStatus() {
        return status;
    }

    public TicketRow getTicket() {
        return ticket;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
