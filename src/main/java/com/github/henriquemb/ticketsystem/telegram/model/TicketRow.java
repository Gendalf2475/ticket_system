package com.github.henriquemb.ticketsystem.telegram.model;

import java.sql.Timestamp;

public class TicketRow {
    private int id;
    private String player;
    private String request;
    private String response;
    private String respondedBy;
    private Timestamp respondedAt;
    private Double rating;
    private Boolean send;
    private Timestamp createdAt;
    private String status;
    private String answeredByType;
    private String answeredByName;
    private String answeredByMinecraftUuid;
    private Long answeredByTelegramId;
    private String answeredByTelegramUsername;
    private String answeredByTelegramFirstName;
    private String answeredByTelegramLastName;
    private Long inProgressByTelegramId;
    private String inProgressByName;
    private Timestamp inProgressUntil;
    private Timestamp closedAt;
    private Timestamp reviewedAt;
    private ReviewRating reviewRating;
    private String reviewComment;
    private Long reviewedByTelegramId;
    private String reviewedByName;
    private String reviewedByTelegramUsername;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAnsweredByType() {
        return answeredByType;
    }

    public void setAnsweredByType(String answeredByType) {
        this.answeredByType = answeredByType;
    }

    public String getAnsweredByName() {
        return answeredByName;
    }

    public void setAnsweredByName(String answeredByName) {
        this.answeredByName = answeredByName;
    }

    public String getAnsweredByMinecraftUuid() {
        return answeredByMinecraftUuid;
    }

    public void setAnsweredByMinecraftUuid(String answeredByMinecraftUuid) {
        this.answeredByMinecraftUuid = answeredByMinecraftUuid;
    }

    public Long getAnsweredByTelegramId() {
        return answeredByTelegramId;
    }

    public void setAnsweredByTelegramId(Long answeredByTelegramId) {
        this.answeredByTelegramId = answeredByTelegramId;
    }

    public String getAnsweredByTelegramUsername() {
        return answeredByTelegramUsername;
    }

    public void setAnsweredByTelegramUsername(String answeredByTelegramUsername) {
        this.answeredByTelegramUsername = answeredByTelegramUsername;
    }

    public String getAnsweredByTelegramFirstName() {
        return answeredByTelegramFirstName;
    }

    public void setAnsweredByTelegramFirstName(String answeredByTelegramFirstName) {
        this.answeredByTelegramFirstName = answeredByTelegramFirstName;
    }

    public String getAnsweredByTelegramLastName() {
        return answeredByTelegramLastName;
    }

    public void setAnsweredByTelegramLastName(String answeredByTelegramLastName) {
        this.answeredByTelegramLastName = answeredByTelegramLastName;
    }

    public Long getInProgressByTelegramId() {
        return inProgressByTelegramId;
    }

    public void setInProgressByTelegramId(Long inProgressByTelegramId) {
        this.inProgressByTelegramId = inProgressByTelegramId;
    }

    public String getInProgressByName() {
        return inProgressByName;
    }

    public void setInProgressByName(String inProgressByName) {
        this.inProgressByName = inProgressByName;
    }

    public Timestamp getInProgressUntil() {
        return inProgressUntil;
    }

    public void setInProgressUntil(Timestamp inProgressUntil) {
        this.inProgressUntil = inProgressUntil;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public ReviewRating getReviewRating() {
        return reviewRating;
    }

    public void setReviewRating(ReviewRating reviewRating) {
        this.reviewRating = reviewRating;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Long getReviewedByTelegramId() {
        return reviewedByTelegramId;
    }

    public void setReviewedByTelegramId(Long reviewedByTelegramId) {
        this.reviewedByTelegramId = reviewedByTelegramId;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public String getReviewedByTelegramUsername() {
        return reviewedByTelegramUsername;
    }

    public void setReviewedByTelegramUsername(String reviewedByTelegramUsername) {
        this.reviewedByTelegramUsername = reviewedByTelegramUsername;
    }

    public boolean isAnswered() {
        return response != null && !response.trim().isEmpty();
    }

    public boolean isReviewed() {
        return reviewedAt != null || "reviewed".equalsIgnoreCase(status) || reviewRating != null;
    }
}
