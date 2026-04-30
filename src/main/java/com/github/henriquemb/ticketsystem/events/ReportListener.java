package com.github.henriquemb.ticketsystem.events;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.ReportController;
import com.github.henriquemb.ticketsystem.database.model.ReportModel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;

public class ReportListener implements Listener {
    private final Model m = TicketSystem.getModel();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @EventHandler
    public void onCheck(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPermission("ticketsystem.report.staff")) return;

        ReportController controller = new ReportController();

        List<String> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            players.add(player.getName());
        });

        List<ReportModel> reports = controller.fetchNotVerified(players);

        if (!reports.isEmpty()) m.sendMessage(e.getPlayer(), messages.getString("report.pending").replace("<total>", String.valueOf(reports.size())));
    }
}
