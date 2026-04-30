package com.github.henriquemb.ticketsystem.telegram.model;

import java.util.Locale;

public enum TelegramRole {
    SUPER_ADMIN("super_admin", "суперадмин"),
    ADMIN("admin", "администратор"),
    MODERATOR("moderator", "модератор");

    private final String databaseValue;
    private final String displayName;

    TelegramRole(String databaseValue, String displayName) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canAnswerTickets() {
        return this == SUPER_ADMIN || this == ADMIN || this == MODERATOR;
    }

    public boolean canReviewTickets() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public boolean canManageUsers() {
        return this == SUPER_ADMIN;
    }

    public boolean canViewStatistics() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    public static TelegramRole fromDatabaseValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;

        String normalized = value.trim().toLowerCase(Locale.US);
        for (TelegramRole role : values()) {
            if (role.databaseValue.equals(normalized)) return role;
        }

        return null;
    }
}
