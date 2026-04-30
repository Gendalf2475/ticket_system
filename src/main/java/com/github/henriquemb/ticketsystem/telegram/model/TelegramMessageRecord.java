package com.github.henriquemb.ticketsystem.telegram.model;

public class TelegramMessageRecord {
    private int ticketId;
    private Long newTicketChatId;
    private Integer newTicketThreadId;
    private Integer newTicketMessageId;
    private Long closedTicketChatId;
    private Integer closedTicketThreadId;
    private Integer closedTicketMessageId;
    private Integer promptMessageId;

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public Long getNewTicketChatId() {
        return newTicketChatId;
    }

    public void setNewTicketChatId(Long newTicketChatId) {
        this.newTicketChatId = newTicketChatId;
    }

    public Integer getNewTicketThreadId() {
        return newTicketThreadId;
    }

    public void setNewTicketThreadId(Integer newTicketThreadId) {
        this.newTicketThreadId = newTicketThreadId;
    }

    public Integer getNewTicketMessageId() {
        return newTicketMessageId;
    }

    public void setNewTicketMessageId(Integer newTicketMessageId) {
        this.newTicketMessageId = newTicketMessageId;
    }

    public Long getClosedTicketChatId() {
        return closedTicketChatId;
    }

    public void setClosedTicketChatId(Long closedTicketChatId) {
        this.closedTicketChatId = closedTicketChatId;
    }

    public Integer getClosedTicketThreadId() {
        return closedTicketThreadId;
    }

    public void setClosedTicketThreadId(Integer closedTicketThreadId) {
        this.closedTicketThreadId = closedTicketThreadId;
    }

    public Integer getClosedTicketMessageId() {
        return closedTicketMessageId;
    }

    public void setClosedTicketMessageId(Integer closedTicketMessageId) {
        this.closedTicketMessageId = closedTicketMessageId;
    }

    public Integer getPromptMessageId() {
        return promptMessageId;
    }

    public void setPromptMessageId(Integer promptMessageId) {
        this.promptMessageId = promptMessageId;
    }
}
