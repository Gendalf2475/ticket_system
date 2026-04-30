package com.github.henriquemb.ticketsystem.telegram.model;

public class AnswerTicketResult {
    public enum Status {
        SUCCESS,
        NOT_FOUND,
        ALREADY_CLOSED,
        ALREADY_REVIEWED,
        LOCKED_BY_OTHER,
        NOT_LOCKED_BY_USER,
        ERROR
    }

    private final Status status;
    private final TicketRow ticket;

    public AnswerTicketResult(Status status, TicketRow ticket) {
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
