package com.github.henriquemb.ticketsystem.telegram.model;

public class UserState {
    public enum Type {
        WAITING_FOR_TICKET_ANSWER,
        WAITING_FOR_REVIEW_COMMENT
    }

    private final Type type;
    private final int ticketId;
    private final ReviewRating reviewRating;
    private final long chatId;
    private final Integer threadId;
    private final Integer promptMessageId;
    private final long createdAtMillis;

    public UserState(Type type, int ticketId, long chatId, Integer threadId, Integer promptMessageId) {
        this(type, ticketId, null, chatId, threadId, promptMessageId);
    }

    public UserState(Type type, int ticketId, ReviewRating reviewRating, long chatId, Integer threadId, Integer promptMessageId) {
        this.type = type;
        this.ticketId = ticketId;
        this.reviewRating = reviewRating;
        this.chatId = chatId;
        this.threadId = threadId;
        this.promptMessageId = promptMessageId;
        this.createdAtMillis = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public int getTicketId() {
        return ticketId;
    }

    public ReviewRating getReviewRating() {
        return reviewRating;
    }

    public long getChatId() {
        return chatId;
    }

    public Integer getThreadId() {
        return threadId;
    }

    public Integer getPromptMessageId() {
        return promptMessageId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}
