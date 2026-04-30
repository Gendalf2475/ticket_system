package com.github.henriquemb.ticketsystem.database.model;

import java.sql.Timestamp;

public class TicketModel {
    private Integer id;
    private String player;
    private String request;
    private String response;
    private String respondedBy;
    private Timestamp respondedAt;
    private Double rating;
    private Boolean send;
    private Timestamp timestamp;

    public TicketModel() {
    }

    public TicketModel(String player, String request) {
        this.player = player;
        this.request = request;
    }

    public TicketModel(Integer id, String player, String request, String response, String respondedBy, Timestamp respondedAt, Double rating, Boolean send, Timestamp timestamp) {
        this.id = id;
        this.player = player;
        this.request = request;
        this.response = response;
        this.respondedBy = respondedBy;
        this.respondedAt = respondedAt;
        this.rating = rating;
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

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
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
