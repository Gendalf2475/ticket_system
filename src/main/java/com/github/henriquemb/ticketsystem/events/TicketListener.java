package com.github.henriquemb.ticketsystem.events;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.TicketController;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.util.ResponseMessages;
import org.bukkit.Bukkit;
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
    private final TelegramRepository telegramRepository = new TelegramRepository();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @EventHandler
    public void onVerifyResponse(PlayerJoinEvent e) {
        String playerName = e.getPlayer().getName();

        Bukkit.getScheduler().runTaskAsynchronously(TicketSystem.getMain(), () -> {
            List<TicketModel> tickets = controller.fetchNotSendToPlayer(playerName);
            if (tickets.isEmpty()) return;

            Bukkit.getScheduler().runTask(TicketSystem.getMain(), () -> {
                Player p = Bukkit.getPlayerExact(playerName);
                if (p == null || !p.isOnline()) return;

                StringBuilder str = new StringBuilder();
                str.append(messages.getString("ticket.response.message.header"));
                for (TicketModel ticket : tickets) {
                    str.append(new ResponseMessages().getTicketResponse(ticket));
                }
                str.append(messages.getString("ticket.response.message.footer"));

                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                m.sendMessage(p, str.toString());

                Bukkit.getScheduler().runTaskAsynchronously(TicketSystem.getMain(), () ->
                        tickets.forEach(ticket -> telegramRepository.markTicketSentToPlayer(ticket.getId())));
            });
        });
    }

    @EventHandler
    public void onCheck(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPermission("ticketsystem.report.staff")) return;

        String playerName = e.getPlayer().getName();

        Bukkit.getScheduler().runTaskAsynchronously(TicketSystem.getMain(), () -> {
            List<TicketModel> tickets = controller.fetchNotAnswered();
            if (tickets.isEmpty()) return;

            Bukkit.getScheduler().runTask(TicketSystem.getMain(), () -> {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null && player.isOnline()) {
                    m.sendMessage(player, messages.getString("ticket.pending").replace("<total>", String.valueOf(tickets.size())));
                }
            });
        });
    }
}
