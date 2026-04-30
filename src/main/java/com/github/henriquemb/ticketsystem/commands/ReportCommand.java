package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.ReportController;
import com.github.henriquemb.ticketsystem.database.model.ReportModel;
import com.github.henriquemb.ticketsystem.enums.ReportStatusEnum;
import com.github.henriquemb.ticketsystem.util.PrepareMessages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ReportCommand implements CommandExecutor, TabCompleter {
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

        if (args.length <= 0) {
            m.sendMessage(p, messages.getString("report.use_correct"), "report");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                status(p, validateId(p, args), args);
                break;
            case "teleport":
            case "teleportar":
            case "tp":
                teleport(p, validateId(p, args));
                break;
            case "view":
            case "ver":
                view(p, validateId(p, args));
                break;
            case "help":
            case "ajuda":
                help(p);
                break;
            default:
                String reason = null;
                String evidence = null;

                if (args.length >= 1 && (args[1].toLowerCase().startsWith("https://") || args[1].toLowerCase().startsWith("http://"))) evidence = args[1];
                if (args.length >= 2) reason = String.join(" ", Arrays.copyOfRange(args, evidence != null ? 2 : 1, args.length));

                create(p, args[0], evidence, reason);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tb = new ArrayList<>();

        if (sender.hasPermission("ticketsystem.report.staff")) {
            if (args.length <= 1) {
                tb.add("help");
                tb.add("status");
                tb.add("teleport");
                tb.add("view");
            }

            if (args[0].equalsIgnoreCase("status") && args.length > 2) {
                for (ReportStatusEnum r : ReportStatusEnum.values()) tb.add(r.toString());

                return tb;
            }

            switch (args[0].toLowerCase()) {
                case "status":
                case "teleport":
                case "tp":
                case "view":
                case "ver":
                    List<String> players = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));

                    controller.fetchNotVerified(players).forEach(r -> tb.add(String.valueOf(r.getId())));
            }
        }

        if (args.length <= 0) {
            Bukkit.getOnlinePlayers().forEach(p -> tb.add(p.getName()));
        }

        return tb;
    }

    private void create(Player p, String reported, String reason, String evidence) {
        if (!p.hasPermission("ticketsystem.report.use")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "report");
            return;
        }

        if (p.getName().equalsIgnoreCase(reported)) {
            m.sendMessage(p, messages.getString("report.not_yourself"), "report");
            return;
        }

        Player t = Bukkit.getPlayerExact(reported);

        if (t == null) {
            m.sendMessage(p, messages.getString("player.not_found"), "report");
            return;
        }

        controller.create(p.getName(), reported, reason, evidence);
        m.sendMessage(p, messages.getString("report.success"), "report");

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("ticketsystem.report.staff")) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                m.sendMessage(                        player,
                        Objects.requireNonNull(messages.getString("report.new_report"))
                                .replace("<button-list>",
                                        String.format("[%s](/reports hover=%s)",
                                                messages.getString("report.buttons.list.label"),
                                                messages.getString("report.buttons.list.hover"))
                                ),
                        "report"
                );
            }
        });
    }

    private void status(Player p, int id, String[] args) {
        if (!p.hasPermission("ticketsystem.report.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "report");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "report");
            return;
        }

        ReportModel report = controller.fetchById(id);

        if (report == null || report.isVerified()) {
            m.sendMessage(p, messages.getString("report.not_found"), "report");
            return;
        }

        if (args.length < 2 || ReportStatusEnum.valueOf(args[2]) == null) {
            m.sendMessage(p, messages.getString("report.status.invalid"), "report");
            return;
        }

        ReportStatusEnum reportStatus = ReportStatusEnum.valueOf(args[2]);

        report.setStatus(reportStatus.getId());

        if (reportStatus == ReportStatusEnum.ACCEPTED || reportStatus == ReportStatusEnum.REJECT) {
            report.setVerified(true);
            report.setVerifiedBy(p.getName());
            report.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        }

        controller.update(report);

        m.sendMessage(p, String.format("%s", messages.getString("report.status.change"))
                .replace("<status>", reportStatus.format()), "report");
    }

    private void teleport(Player p, int id) {
        if (!p.hasPermission("ticketsystem.report.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission", "report"));
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "report");
            return;
        }

        ReportModel report = controller.fetchById(id);

        if (report == null || report.isVerified()) {
            m.sendMessage(p, messages.getString("report.not_found"), "report");
            return;
        }

        Player t = Bukkit.getPlayerExact(report.getReported());

        if (t == null || !t.isOnline()) {
            m.sendMessage(p, messages.getString("player.not_found"), "report");
            return;
        }

        StringBuilder str = new StringBuilder();
        str.append(messages.getString("report.status.change_to"));
        for (ReportStatusEnum rse : ReportStatusEnum.values()) {
            if (!rse.equals(ReportStatusEnum.WAITING))
                str.append(String.format("[%s[%s]](/report status %d %s hover=%s) ", rse.getColor(), rse.getName(), report.getId(), rse, rse.format()));
        }

        m.sendMessage(p, str.toString());
        p.teleport(t.getLocation());
    }

    private void view(Player p, int id) {
        if (!p.hasPermission("ticketsystem.report.staff")) {
            m.sendMessage(p, messages.getString("permission.no-permission"));
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "report");
            return;
        }

        ReportModel report = controller.fetchById(id);

        if (report == null) {
            m.sendMessage(p, messages.getString("report.not_found"), "report");
            return;
        }

        StringBuilder str = new StringBuilder();

        if (report.getStatus() != ReportStatusEnum.WAITING.getId()) {
            for (String msg : messages.getStringList("report.view-response")) {
                str.append(msg.concat("\n"));
            }

            m.sendMessage(p, new PrepareMessages().reportViewResponseMessage(str.toString(), report));
            return;
        }

        for (String msg : messages.getStringList("report.view")) {
            str.append(msg.concat("\n"));
        }

        m.sendMessage(p, new PrepareMessages().reportViewMessage(str.toString(), report));
    }

    private void help(Player p) {
        StringBuilder str = new StringBuilder();
        for (String s : messages.getStringList("report.help")) {
            str.append(s);
            str.append("\n");
        }

        m.sendMessage(p, str.toString());
    }

    private int validateId(Player p, String[] args) {
        try {
            return Integer.parseInt(args[1]);
        }
        catch (Exception e) {
            return 0;
        }
    }
}
