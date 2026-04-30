package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.ReportController;
import com.github.henriquemb.ticketsystem.database.model.ReportModel;
import com.github.henriquemb.ticketsystem.exceptions.PaginationException;
import com.github.henriquemb.ticketsystem.util.Pagination;
import com.github.henriquemb.ticketsystem.util.PrepareMessages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportsCommand implements CommandExecutor {
    private final Model m = TicketSystem.getModel();
    private final ReportController controller = new ReportController();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getString("prefix.report") + messages.getString("warnings.only_players"));
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("ticketsystem.report.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "report");
            return true;
        }

        List<String> players = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> {
            players.add(player.getName());
        });

        List<ReportModel> reports;
        String options = "";
        int pag = 1;

        if (args.length >= 1) {
            try {
                pag = Integer.parseInt(args[0]);

                if (args.length == 2) options = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } catch (Exception e) {
                options = String.join(" ", args);
            }
        }

        if (!options.equals("") && options.split(" ")[0].equalsIgnoreCase("-a"))
            reports = controller.fetchAll();
        else reports = controller.fetchNotVerified(players);

        if (reports.isEmpty()) {
            m.sendMessage(p, messages.getString("report.empty"), "report");
            return true;
        }

        Pagination<ReportModel> pagination = new Pagination<>(reports, 10);

        try {
            StringBuilder str = new StringBuilder();
            str.append(messages.getString("pagination.header").replace("<self>", String.valueOf(pag)).replace("<total>", String.valueOf(pagination.length())).concat("\n"));

            for (ReportModel r : pagination.getPag(pag)) {
                str.append(new PrepareMessages().reportViewMessage(messages.getString("report.list"), r).concat("\n"));
            }

            String footer = String.format("%s", messages.getString("pagination.footer"))
                    .replace("<button-back>", pag > 1 && pagination.length() > 1
                            ? String.format("[%s](/reports %d %s hover=%s)",
                            messages.getString("pagination.buttons.back.label"),
                            pag - 1,
                            options,
                            messages.getString("pagination.buttons.back.hover")) : "")
                    .replace("<button-next>", pagination.length() > pag
                            ? String.format("[%s](/reports %d %s hover=%s)",
                            messages.getString("pagination.buttons.next.label"),
                            pag + 1,
                            options,
                            messages.getString("pagination.buttons.next.hover")) : "");

            str.append(pagination.length() == 1 ? "\n" : footer);

            m.sendMessage(p, str.toString());
        }
        catch (PaginationException e) {
            m.sendMessage(p, messages.getString("pagination.error.not_found"), "report");
        }

        return true;
    }
}
