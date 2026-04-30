package com.github.henriquemb.ticketsystem.telegram.model;

public class TelegramTopic {
    private final long chatId;
    private final Integer threadId;

    public TelegramTopic(long chatId, Integer threadId) {
        this.chatId = chatId;
        this.threadId = threadId;
    }

    public long getChatId() {
        return chatId;
    }

    public Integer getThreadId() {
        return threadId;
    }

    public boolean hasChat() {
        return chatId != 0L;
    }
}
