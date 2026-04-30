package com.github.henriquemb.ticketsystem.events;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.TicketController;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.util.ResponseMessages;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class TicketListener implements Listener {
    private final Model m = TicketSystem.getModel();
    private final TicketController controller = new TicketController();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @EventHandler
    public void onVerifyResponse(PlayerJoinEvent e) {
        List<TicketModel> tickets = controller.fetchNotSendToPlayer(e.getPlayer().getName());

        if (tickets.isEmpty()) return;

        Player p = e.getPlayer();

        StringBuilder str = new StringBuilder();
        str.append(messages.getString("ticket.response.message.header"));
        for (TicketModel ticket : tickets) {
            str.append(new ResponseMessages().getTicketResponse(ticket));

            ticket.setSend(true);
            controller.update(ticket);
        }
        str.append(messages.getString("ticket.response.message.footer"));

        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
        m.sendMessage(p, str.toString());
    }

    @EventHandler
    public void onCheck(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPermission("ticketsystem.report.staff")) return;

        List<TicketModel> tickets = controller.fetchNotAnswered();

        if (!tickets.isEmpty()) m.sendMessage(e.getPlayer(), messages.getString("ticket.pending").replace("<total>", String.valueOf(tickets.size())));
    }
}
