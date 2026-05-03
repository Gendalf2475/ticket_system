package com.github.henriquemb.ticketsystem.telegram.model;

public enum ReviewRating {
    EXCELLENT("excellent", "🟢 Отлично"),
    GOOD("good", "🟡 Хорошо"),
    BAD("bad", "🔴 Плохо");

    private final String databaseValue;
    private final String displayName;

    ReviewRating(String databaseValue, String displayName) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScore() {
        switch (this) {
            case EXCELLENT:
                return 3;
            case GOOD:
                return 2;
            case BAD:
                return 1;
            default:
                return 0;
        }
    }

    public boolean requiresComment() {
        return this == GOOD || this == BAD;
    }

    public boolean shouldSendToCriticsTopic() {
        return this == GOOD || this == BAD;
    }

    public static ReviewRating fromCallback(String callbackPrefix) {
        if ("review_excellent".equals(callbackPrefix)) return EXCELLENT;
        if ("review_good".equals(callbackPrefix)) return GOOD;
        if ("review_bad".equals(callbackPrefix)) return BAD;
        return null;
    }
}
