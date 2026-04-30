package com.github.henriquemb.ticketsystem.util;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.enums.TicketRatingEnum;
import org.bukkit.configuration.file.FileConfiguration;

public class ResponseMessages {
    private final FileConfiguration messages = TicketSystem.getMessages();

    public String getTicketResponse(TicketModel ticket) {
        StringBuilder ratings = new StringBuilder();
        for (TicketRatingEnum tre : TicketRatingEnum.values()) {
            if (tre != TicketRatingEnum.CANCELED && tre != TicketRatingEnum.UNAVAILABLE)
                ratings.append(String.format("[%s[%s]](/ticket rate %d %s hover=%s) ", tre.getColor(), tre.getName(), ticket.getId(), tre, tre.format()));
        }

        StringBuilder str = new StringBuilder();
        for (String msg : messages.getStringList("ticket.response.message.content")) {
            str.append(msg);
            str.append("\n");
        }

        return new PrepareMessages().ticketViewMessage(str.toString(), ticket).replace("<ratings>", ratings);
    }

    public String getSuggestionResponse(SuggestionModel suggestion) {
        StringBuilder str = new StringBuilder();
        for (String msg : messages.getStringList("suggestion.response.message.content")) {
            str.append(msg);
            str.append("\n");
        }

        return new PrepareMessages().suggestionViewMessage(str.toString(), suggestion);
    }
}
