package com.github.henriquemb.ticketsystem.telegram.model;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TelegramSettings {
    private final boolean enabled;
    private final String botToken;
    private final Set<Long> superAdmins;
    private final int answerMaxLength;
    private final int inProgressTimeoutSeconds;
    private final int pollingIntervalSeconds;
    private final TelegramTopic newTicketsTopic;
    private final TelegramTopic closedTicketsTopic;
    private final TelegramTopic criticsTopic;
    private final boolean criticsTopicEnabled;
    private final boolean deleteUserInput;
    private final boolean deleteBotPrompts;
    private final String parseMode;

    public TelegramSettings(FileConfiguration config) {
        this.enabled = config.getBoolean("telegram.enabled", false);
        this.botToken = config.getString("telegram.bot-token", "");
        this.superAdmins = readSuperAdmins(config);
        this.answerMaxLength = Math.max(1, config.getInt("telegram.answer-max-length", 237));
        this.inProgressTimeoutSeconds = Math.max(1, config.getInt("telegram.in-progress-timeout-seconds", 300));
        this.pollingIntervalSeconds = Math.max(1, config.getInt("telegram.polling-interval-seconds", 5));
        this.newTicketsTopic = readTopic(config, "telegram.topics.new-tickets");
        this.closedTicketsTopic = readTopic(config, "telegram.topics.closed-tickets");
        this.criticsTopic = readTopicWithFallback(config, "telegram.topics.critics", "critics_topic");
        this.criticsTopicEnabled = config.getBoolean("telegram.topics.critics.enabled",
                config.getBoolean("critics_topic.enabled", false));
        this.deleteUserInput = config.getBoolean("telegram.cleanup.delete-user-input", true);
        this.deleteBotPrompts = config.getBoolean("telegram.cleanup.delete-bot-prompts", true);
        this.parseMode = config.getString("telegram.messages.parse-mode", "HTML");
    }

    private Set<Long> readSuperAdmins(FileConfiguration config) {
        Set<Long> ids = new HashSet<>();
        List<?> values = config.getList("telegram.super-admins");
        if (values == null) return ids;

        for (Object value : values) {
            if (value == null) continue;

            try {
                ids.add(Long.parseLong(String.valueOf(value)));
            }
            catch (NumberFormatException ignored) {
                // Invalid IDs are ignored so a config typo does not break the plugin startup.
            }
        }

        return ids;
    }

    private TelegramTopic readTopic(FileConfiguration config, String path) {
        long chatId = config.getLong(path + ".chat-id", 0L);
        int configuredThreadId = config.getInt(path + ".thread-id", 0);
        Integer threadId = configuredThreadId > 0 ? configuredThreadId : null;
        return new TelegramTopic(chatId, threadId);
    }

    private TelegramTopic readTopicWithFallback(FileConfiguration config, String primaryPath, String fallbackPath) {
        long chatId = config.getLong(primaryPath + ".chat-id", config.getLong(fallbackPath + ".chat_id", 0L));
        int configuredThreadId = config.getInt(primaryPath + ".thread-id", config.getInt(fallbackPath + ".thread_id", 0));
        Integer threadId = configuredThreadId > 0 ? configuredThreadId : null;
        return new TelegramTopic(chatId, threadId);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public Set<Long> getSuperAdmins() {
        return superAdmins;
    }

    public int getAnswerMaxLength() {
        return answerMaxLength;
    }

    public int getInProgressTimeoutSeconds() {
        return inProgressTimeoutSeconds;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public TelegramTopic getNewTicketsTopic() {
        return newTicketsTopic;
    }

    public TelegramTopic getClosedTicketsTopic() {
        return closedTicketsTopic;
    }

    public TelegramTopic getCriticsTopic() {
        return criticsTopic;
    }

    public boolean isCriticsTopicEnabled() {
        return criticsTopicEnabled;
    }

    public boolean isDeleteUserInput() {
        return deleteUserInput;
    }

    public boolean isDeleteBotPrompts() {
        return deleteBotPrompts;
    }

    public String getParseMode() {
        return parseMode;
    }

    public boolean hasToken() {
        return botToken != null && !botToken.trim().isEmpty();
    }
}
