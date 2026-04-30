package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.TicketController;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.exceptions.PaginationException;
import com.github.henriquemb.ticketsystem.util.Pagination;
import com.github.henriquemb.ticketsystem.util.PrepareMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TicketsCommand implements CommandExecutor, TabCompleter {
    private final Model m = TicketSystem.getModel();
    private final TicketController controller = new TicketController();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getString("prefix.report") + messages.getString("warnings.only_players"));
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("ticketsystem.ticket.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return true;
        }

        List<TicketModel> tickets = null;
        String options = "";
        int pag = 1;

        if (args.length >= 1) {
            try {
                pag = Integer.parseInt(args[0]);

                if (args.length == 2) options = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
            catch (Exception e) {
                options = String.join(" ", args);
            }

            switch (options.split(" ")[0]) {
                case "-a":
                    tickets = controller.fetchAllAnswered();
                    break;
                case "-p":
                    if (options.split(" ").length < 2) {
                        m.sendMessage(p, messages.getString("player.not_found"), "ticket");
                        break;
                    }

                    tickets = controller.fetchAnsweredBy(options.split(" ")[1]);
                    break;
                default:
                    tickets = controller.fetchNotAnswered();
            }
        }
        else tickets = controller.fetchNotAnswered();

        if (tickets == null || tickets.isEmpty()) {
            m.sendMessage(p, messages.getString("ticket.empty"), "ticket");
            return true;
        }

        Pagination<TicketModel> pagination = new Pagination<>(tickets, 10);

        try {
            StringBuilder str = new StringBuilder();
            str.append(messages.getString("pagination.header").replace("<self>", String.valueOf(pag)).replace("<total>", String.valueOf(pagination.length())).concat("\n"));

            for (TicketModel t : pagination.getPag(pag)) {
                str.append(new PrepareMessages().ticketViewMessage(messages.getString("ticket.list"), t).concat("\n"));
            }

            String footer = String.format("%s", messages.getString("pagination.footer"))
                    .replace("<button-back>", pag > 1 && pagination.length() > 1
                            ? String.format("[%s](/tickets %d %s hover=%s)",
                            messages.getString("pagination.buttons.back.label"),
                            pag - 1,
                            options,
                            messages.getString("pagination.buttons.back.hover")) : "")
                    .replace("<button-next>", pagination.length() > pag
                            ? String.format("[%s](/tickets %d %s hover=%s)",
                            messages.getString("pagination.buttons.next.label"),
                            pag + 1,
                            options,
                            messages.getString("pagination.buttons.next.hover")) : "");

            str.append(pagination.length() == 1 ? "" : footer);

            m.sendMessage(p, str.toString());
        }
        catch (PaginationException e) {
            m.sendMessage(p, messages.getString("pagination.error.not_found"), "report");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("-p")) {
            Set<String> staffers = new HashSet<>();
            controller.fetchAllAnswered().forEach(t -> {
                staffers.add(t.getRespondedBy());
            });

            return List.copyOf(staffers);
        }

        return null;
    }
}
