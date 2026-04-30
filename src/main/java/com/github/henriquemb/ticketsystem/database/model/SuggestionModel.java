package com.github.henriquemb.ticketsystem.database.model;

import java.sql.Timestamp;

public class SuggestionModel {
    private Integer id;
    private String player;
    private String suggestion;
    private String response;
    private String respondedBy;
    private Timestamp respondedAt;
    private Boolean send;
    private Timestamp timestamp;

    public SuggestionModel() {
    }

    public SuggestionModel(String player, String suggestion) {
        this.player = player;
        this.suggestion = suggestion;
    }

    public SuggestionModel(Integer id, String player, String suggestion, String response, String respondedBy, Timestamp respondedAt, Boolean send, Timestamp timestamp) {
        this.id = id;
        this.player = player;
        this.suggestion = suggestion;
        this.response = response;
        this.respondedBy = respondedBy;
        this.respondedAt = respondedAt;
        this.send = send;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(String respondedBy) {
        this.respondedBy = respondedBy;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Boolean getSend() {
        return send;
    }

    public void setSend(Boolean send) {
        this.send = send;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
