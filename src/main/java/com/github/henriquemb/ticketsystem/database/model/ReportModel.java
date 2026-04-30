package com.github.henriquemb.ticketsystem.database.model;

import java.sql.Timestamp;

public class ReportModel {
    private Integer id;
    private String player;
    private String reported;
    private String reason;
    private String evidence;
    private boolean verified;
    private String verifiedBy;
    private Timestamp verifiedAt;
    private Integer status;
    private Timestamp timestamp;

    public ReportModel() {
    }

    public ReportModel(String player, String reported) {
        this.player = player;
        this.reported = reported;
    }

    public ReportModel(Integer id, String player, String reported, String reason, String evidence, boolean verified, String verifiedBy, Timestamp verifiedAt, Integer status, Timestamp timestamp) {
        this.id = id;
        this.player = player;
        this.reported = reported;
        this.reason = reason;
        this.evidence = evidence;
        this.verified = verified;
        this.verifiedBy = verifiedBy;
        this.verifiedAt = verifiedAt;
        this.status = status;
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

    public String getReported() {
        return reported;
    }

    public void setReported(String reported) {
        this.reported = reported;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Timestamp getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Timestamp verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
