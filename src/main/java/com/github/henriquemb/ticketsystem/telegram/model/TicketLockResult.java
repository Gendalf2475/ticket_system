package com.github.henriquemb.ticketsystem.telegram.model;

public class TicketLockResult {
    public enum Status {
        ACQUIRED,
        NOT_FOUND,
        ALREADY_CLOSED,
        ALREADY_REVIEWED,
        LOCKED_BY_OTHER,
        ERROR
    }

    private final Status status;
    private final TicketRow ticket;

    public TicketLockResult(Status status, TicketRow ticket) {
        this.status = status;
        this.ticket = ticket;
    }

    public Status getStatus() {
        return status;
    }

    public TicketRow getTicket() {
        return ticket;
    }

    public boolean isAcquired() {
        return status == Status.ACQUIRED;
    }
}
