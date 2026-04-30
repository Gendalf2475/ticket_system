package com.github.henriquemb.ticketsystem.enums;

import com.github.henriquemb.ticketsystem.TicketSystem;
import org.bukkit.configuration.file.FileConfiguration;

public enum ReportStatusEnum {
    WAITING(0),
    ACCEPTED(1),
    REVIEW(2),
    REJECT(3);

    private final FileConfiguration messages = TicketSystem.getMessages();
    private final int id;

    ReportStatusEnum(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getColor() {
        return messages.getString(String.format("status.report.%s.color", this).toLowerCase());
    }

    public String getName() {
        return messages.getString(String.format("status.report.%s.name", this).toLowerCase());
    }

    public String format() {
        return getColor() + getName();
    }
}
