package com.github.henriquemb.ticketsystem.events;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.SuggestionController;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;
import com.github.henriquemb.ticketsystem.util.ResponseMessages;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class SuggestionListener implements Listener {
    private final Model m = TicketSystem.getModel();
    private final SuggestionController controller = new SuggestionController();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @EventHandler
    public void onVerifyResponse(PlayerJoinEvent e) {
        List<SuggestionModel> suggestions = controller.fetchNotSendToPlayer(e.getPlayer().getName());

        if (suggestions.isEmpty()) return;

        Player p = e.getPlayer();

        StringBuilder str = new StringBuilder();
        str.append(messages.getString("suggestion.response.message.header"));
        for (SuggestionModel suggestion : suggestions) {
            str.append(new ResponseMessages().getSuggestionResponse(suggestion));

            suggestion.setSend(true);
            controller.update(suggestion);
        }
        str.append(messages.getString("suggestion.response.message.footer"));

        m.sendMessage(p, str.toString());
    }

    @EventHandler
    public void onCheck(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPermission("ticketsystem.report.staff")) return;

        List<SuggestionModel> suggestions = controller.fetchNotAnswered();

        if (!suggestions.isEmpty()) m.sendMessage(e.getPlayer(), messages.getString("suggestion.pending").replace("<total>", String.valueOf(suggestions.size())));
    }
}
