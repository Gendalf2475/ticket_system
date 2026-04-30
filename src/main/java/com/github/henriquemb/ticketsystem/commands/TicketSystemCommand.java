package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TicketSystemCommand implements CommandExecutor, TabCompleter {
    private final Model m = TicketSystem.getModel();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ticketsystem.admin")) {
            m.sendMessage((Player) sender, TicketSystem.getMessages().getString("permission.no_permission"));
            return true;
        }

        TicketSystem.getMain().reloadConfig();

        TicketSystem.getMain().getPluginLoader().disablePlugin(TicketSystem.getMain());
        TicketSystem.getMain().getPluginLoader().enablePlugin(TicketSystem.getMain());

        m.sendMessage((Player) sender, TicketSystem.getMessages().getString("reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> tab = new ArrayList<>();
        tab.add("reload");

        return tab;
    }
}
