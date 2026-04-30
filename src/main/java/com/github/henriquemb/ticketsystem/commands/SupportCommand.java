package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Timestamp;

public class SupportCommand implements CommandExecutor {
    private final Model m = TicketSystem.getModel();
    private final FileConfiguration config = TicketSystem.getMain().getConfig();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getString("prefix.ticket") + messages.getString("warnings.only_players"));
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("ticketsystem.ticket.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return true;
        }

        if (m.getSupportCommandDelay().containsKey(p)) {
            long time = m.getSupportCommandDelay().get(p).getTime() + 60000L * config.getInt("support.cooldown");

            if (new Timestamp(System.currentTimeMillis()).before(new Timestamp(time))) {
                m.sendMessage(p, messages.getString("support-announcement.cooldown.message"), "ticket");
                return true;
            }
        }

        m.getSupportCommandDelay().put(p, new Timestamp(System.currentTimeMillis()));

        StringBuilder str = new StringBuilder();
        for (String msg : messages.getStringList("support-announcement.message")) {
            str.append(msg);
            str.append("\n");
        }

        m.broadcastMessage(
                str.toString().replace("<button-help>", String.format("[%s](suggest_command=/ticket %s  hover=%s)", messages.getString("support-announcement.button-help.label"), p.getName(), messages.getString("support-announcement.button-help.hover"))).replace("<player>", p.getName())
        );

        return true;
    }
}
