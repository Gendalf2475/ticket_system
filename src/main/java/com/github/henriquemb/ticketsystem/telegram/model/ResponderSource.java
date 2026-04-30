package com.github.henriquemb.ticketsystem.telegram.model;

public enum ResponderSource {
    GAME("game"),
    TELEGRAM("telegram");

    private final String databaseValue;

    ResponderSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
}
