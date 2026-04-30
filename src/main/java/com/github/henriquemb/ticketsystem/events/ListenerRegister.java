package com.github.henriquemb.ticketsystem.events;

import com.github.henriquemb.ticketsystem.TicketSystem;

public class ListenerRegister {
    public ListenerRegister(TicketSystem pl) {
        pl.getServer().getPluginManager().registerEvents(new TicketListener(), pl);
        pl.getServer().getPluginManager().registerEvents(new ReportListener(), pl);
        pl.getServer().getPluginManager().registerEvents(new SuggestionListener(), pl);
        pl.getServer().getPluginManager().registerEvents(new UpdateListener(), pl);
    }
}
