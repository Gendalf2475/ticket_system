package com.github.henriquemb.ticketsystem;

import de.themoep.minedown.MineDown;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Data
public class Model {
    private final FileConfiguration messages = TicketSystem.getMessages();

    private final String ticketPrefix = messages.getString("prefix.ticket");
    private final String reportPrefix = messages.getString("prefix.report");
    private final String suggestionPrefix = messages.getString("prefix.suggestion");

    private final Map<Player, Timestamp> supportCommandDelay = new HashMap<>();

    public void sendMessage(Player p, String message) {
        try {
            p.spigot().sendMessage(MineDown.parse(message));
        }
        catch (Exception e) {
            TicketSystem.getMain().getServer().getConsoleSender().sendMessage(Color.RED + "[TicketSystem] Custom message not found, please check.");
            p.spigot().sendMessage(MineDown.parse("&cInternal error."));
        }
    }

    public void sendMessage(Player p, String message, String prefix) {
        try {
            sendMessage(p, messages.getString(String.format("prefix.%s", prefix)).concat(message));
        }
        catch (Exception e) {
            TicketSystem.getMain().getServer().getConsoleSender().sendMessage(Color.RED + "[TicketSystem] Custom message not found, please check.");
            p.spigot().sendMessage(MineDown.parse("&cInternal error."));
        }
    }

    public void broadcastMessage(String message) {
        Bukkit.getOnlinePlayers().forEach(player ->
                player.spigot().sendMessage(MineDown.parse(message)));
    }
}
