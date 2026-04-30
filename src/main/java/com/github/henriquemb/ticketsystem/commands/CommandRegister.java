package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.TicketSystem;

public class CommandRegister {
    public CommandRegister(TicketSystem pl) {
        pl.getCommand("ticket").setExecutor(new TicketCommand());
        pl.getCommand("tickets").setExecutor(new TicketsCommand());
        pl.getCommand("support").setExecutor(new SupportCommand());
        pl.getCommand("report").setExecutor(new ReportCommand());
        pl.getCommand("reports").setExecutor(new ReportsCommand());
        pl.getCommand("suggestion").setExecutor(new SuggestionCommand());
        pl.getCommand("suggestions").setExecutor(new SuggestionsCommand());
        pl.getCommand("ticketsystem").setExecutor(new TicketSystemCommand());
    }
}
