package com.github.henriquemb.ticketsystem.telegram.model;

import java.util.UUID;

public class ResponderInfo {
    private final ResponderSource source;
    private final String displayName;
    private final UUID minecraftUuid;
    private final Long telegramId;
    private final String telegramUsername;
    private final String telegramFirstName;
    private final String telegramLastName;

    public ResponderInfo(ResponderSource source, String displayName, UUID minecraftUuid, Long telegramId,
                         String telegramUsername, String telegramFirstName, String telegramLastName) {
        this.source = source;
        this.displayName = displayName;
        this.minecraftUuid = minecraftUuid;
        this.telegramId = telegramId;
        this.telegramUsername = telegramUsername;
        this.telegramFirstName = telegramFirstName;
        this.telegramLastName = telegramLastName;
    }

    public static ResponderInfo game(String displayName, UUID minecraftUuid) {
        return new ResponderInfo(ResponderSource.GAME, displayName, minecraftUuid, null, null, null, null);
    }

    public static ResponderInfo telegram(TelegramUserProfile user, String displayName) {
        return new ResponderInfo(
                ResponderSource.TELEGRAM,
                displayName,
                null,
                user.getTelegramId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    public ResponderSource getSource() {
        return source;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getMinecraftUuid() {
        return minecraftUuid;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public String getTelegramFirstName() {
        return telegramFirstName;
    }

    public String getTelegramLastName() {
        return telegramLastName;
    }
}
