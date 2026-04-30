package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.TicketController;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.enums.TicketRatingEnum;
import com.github.henriquemb.ticketsystem.exceptions.PaginationException;
import com.github.henriquemb.ticketsystem.util.Pagination;
import com.github.henriquemb.ticketsystem.util.PrepareMessages;
import com.github.henriquemb.ticketsystem.util.ResponseMessages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class TicketCommand implements CommandExecutor, TabCompleter {
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

        if (args.length <= 0) {
            m.sendMessage(p, messages.getString("ticket.use_correct"), "ticket");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "cancelall":
                cancelAllByPlayer(p, args);
                break;
            case "view":
            case "ver":
                view(p, validateId(p, args));
                break;
            case "response":
            case "responder":
                response(p, validateId(p, args), args);
                break;
            case "rate":
            case "avaliar":
                rate(p, validateId(p, args), args);
                break;
            case "stats":
            case "estatisticas":
                stats(p, args);
                break;
            case "teleport":
            case "teleportar":
            case "tp":
                teleport(p, validateId(p, args));
                break;
            case "help":
            case "ajuda":
                help(p);
                break;
            default:
                create(p, args);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tb = new ArrayList<>();

        if (sender.hasPermission("ticketsystem.suggestion.staff")) {
            if (args.length <= 1) {
                tb.add("cancelall");
                tb.add("help");
                tb.add("rate");
                tb.add("response");
                tb.add("stats");
                tb.add("teleport");
                tb.add("view");
            }

            if (args[0].equalsIgnoreCase("cancelall") && args.length >= 2) {
                Set<String> staffers = new HashSet<>();
                controller.fetchAllAnswered().forEach(t -> {
                    staffers.add(t.getRespondedBy());
                });

                return List.copyOf(staffers);
            }

            if (args[0].equalsIgnoreCase("rate") && args.length > 2) {
                for (TicketRatingEnum r : TicketRatingEnum.values()) tb.add(r.toString());

                return tb;
            }

            if (args[0].equalsIgnoreCase("stats") && args.length >= 2) {
                Set<String> staffers = new HashSet<>();
                controller.fetchAllAnswered().forEach(t -> {
                    staffers.add(t.getRespondedBy());
                });

                return List.copyOf(staffers);
            }

            switch (args[0].toLowerCase()) {
                case "rate":
                case "avaliar":
                case "reponse":
                case "responder":
                case "teleport":
                case "teleportar":
                case "view":
                case "ver":
                    controller.fetchNotAnswered().forEach(r -> {
                        tb.add(String.valueOf(r.getId()));
                    });
            }
        }

        return tb;
    }

    private void create(Player p, String[] args) {
        if (!p.hasPermission("ticketsystem.ticket.use")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (args.length == 0 || String.join(" ", args).trim().equals("")) {
            m.sendMessage(p, messages.getString("ticket.use_correct"), "ticket");
            return;
        }

        if (args.length < 4) {
            m.sendMessage(p, messages.getString("ticket.shot"), "ticket");
            return;
        }

        String request = String.join(" ", args);

        controller.create(p.getName(), request);
        m.sendMessage(p, messages.getString("ticket.success"), "ticket");

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("ticketsystem.ticket.staff")) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                m.sendMessage(player,
                        Objects.requireNonNull(messages.getString("ticket.new_ticket"))
                                .replace("<button-list>",
                                        String.format("[%s](/tickets hover=%s)",
                                                messages.getString("ticket.buttons.list.label"),
                                                messages.getString("ticket.buttons.list.hover"))
                                ),
                        "ticket"
                );
            }
        });
    }

    private void view(Player p, int id) {
        if (!p.hasPermission("ticketsystem.ticket.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "ticket");
            return;
        }

        TicketModel ticket = controller.fetchById(id);

        if (ticket == null) {
            m.sendMessage(p, messages.getString("ticket.not_found"), "ticket");
            return;
        }

        StringBuilder str = new StringBuilder();

        if (ticket.getResponse() != null) {
            if (!p.hasPermission("ticketsystem.ticket.staff")) {
                m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
                return;
            }

            for (String msg : messages.getStringList("ticket.view-response")) {
                str.append(msg.concat("\n"));
            }

            m.sendMessage(p, new PrepareMessages().ticketViewResponseMessage(str.toString(), ticket));
            return;
        }

        for (String msg : messages.getStringList("ticket.view")) {
            str.append(msg.concat("\n"));
        }

        m.sendMessage(p, new PrepareMessages().ticketViewMessage(str.toString(), ticket));
    }

    private void response(Player p, int id, String[] args) {
        if (!p.hasPermission("ticketsystem.ticket.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "ticket");
            return;
        }

        TicketModel ticket = controller.fetchById(id);

        if (args.length < 2 || String.join(" ", Arrays.copyOfRange(args, 2, args.length)).isEmpty()) {
            m.sendMessage(p, messages.getString("ticket.response.use_correct"), "ticket");
            return;
        }

        String response = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (ticket == null || ticket.getResponse() != null) {
            m.sendMessage(p, messages.getString("ticket.not_found"));
            return;
        }

        ticket.setResponse(response);
        ticket.setRespondedBy(p.getName());
        ticket.setRespondedAt(new Timestamp(System.currentTimeMillis()));

        controller.update(ticket);

        m.sendMessage(p, messages.getString("ticket.response.success"), "ticket");

        Player t = Bukkit.getPlayerExact(ticket.getPlayer());
        if (!Bukkit.getOnlinePlayers().contains(t)) return;

        ticket.setSend(true);

        controller.update(ticket);

        String str = messages.getString("ticket.response.message.header") +
                new ResponseMessages().getTicketResponse(ticket) +
                messages.getString("ticket.response.message.footer");

        t.playSound(t.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
        m.sendMessage(t, str);
    }

    private void help(Player p) {
        StringBuilder str = new StringBuilder();
        for (String msg : messages.getStringList("ticket.help")) {
            str.append(msg);
            str.append("\n");
        }

        m.sendMessage(p, str.toString());
    }

    private void rate(Player p, int id, String[] args) {
        if (!p.hasPermission("ticketsystem.ticket.use")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "ticket");
            return;
        }

        TicketModel ticket = controller.fetchById(id);

        if (ticket == null) {
            m.sendMessage(p, messages.getString("ticket.not_found"), "ticket");
            return;
        }

        if (args.length < 3) {
            m.sendMessage(p, messages.getString("ticket.rating.use_correct"), "ticket");
            return;
        }

        TicketRatingEnum tre = TicketRatingEnum.valueOf(args[2]);

        if (tre == TicketRatingEnum.CANCELED) {
            if (!p.hasPermission("ticketsystem.ticket.admin")) {
                m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
                return;
            }

            if (TicketRatingEnum.CANCELED.getRate() == ticket.getRating()) {
                m.sendMessage(p, messages.getString("ticket.is_canceled"), "ticket");
                return;
            }

            ticket.setRating((double) tre.getRate());
            controller.update(ticket);

            m.sendMessage(p, messages.getString("ticket.rating_canceled"), "ticket");
            return;
        }

        if (!ticket.getPlayer().equalsIgnoreCase(p.getName())) {
            m.sendMessage(p, messages.getString("ticket.rating.not_author"), "ticket");
            return;
        }

        if (ticket.getRating() != 0) {
            m.sendMessage(p, messages.getString("ticket.rating.rated"), "ticket");
            return;
        }

        if (tre == null) {
            m.sendMessage(p, messages.getString("ticket.rating.invalid"), "ticket");
            return;
        }

        ticket.setRating((double) tre.getRate());
        controller.update(ticket);

        m.sendMessage(p, messages.getString("ticket.rating.success"), "ticket");

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("ticketsystem.ticket.staff")) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                m.sendMessage(player,
                        Objects.requireNonNull(messages.getString("ticket.rating.announcement"))
                                .replace("<player>", ticket.getRespondedBy())
                                .replace("<rating>", tre.format()), "ticket");
            }
        });
    }

    private void teleport(Player p, int id) {
        if (!p.hasPermission("ticketsystem.ticket.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "ticket");
            return;
        }

        TicketModel ticket = controller.fetchById(id);

        if (ticket == null || ticket.getResponse() != null) {
            m.sendMessage(p, messages.getString("ticket.not_found"), "ticket");
            return;
        }

        Player t = Bukkit.getPlayerExact(ticket.getPlayer());

        if (!Bukkit.getOnlinePlayers().contains(t)) {
            m.sendMessage(p, messages.getString("player.offline"), "ticket");
            return;
        }

        m.sendMessage(p, messages.getString("ticket.teleport.success"), "ticket");
        p.teleport(t.getLocation());
    }

    private void stats(Player p, String[] args) {
        if (!p.hasPermission("ticketsystem.ticket.admin")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        int pag = 1;

        try {
            if (args.length == 2) pag = Integer.parseInt(args[1]);

            Map<String, Double> ratings = new HashMap<>();
            Set<String> staffers = new HashSet<>();

            controller.fetchAllAnswered().forEach(t -> {
                staffers.add(t.getRespondedBy());
            });

            Pagination<String> pagination = new Pagination<>(List.copyOf(staffers), 10);

            pagination.getPag(pag).forEach(s -> {
                List<TicketModel> tickets = controller.fetchAnsweredBy(s);
                double sum = tickets.stream().filter(t -> t.getRating() > 0).mapToDouble(TicketModel::getRating).sum();
                int total = Integer.parseInt(String.valueOf(tickets.stream().filter(t -> t.getRating() > 0).count()));
                ratings.put(s, sum / total);
            });

            final Map<String, Double> ratingsSorting = ratings.entrySet()
                    .stream()
                    .filter(r -> r.getValue() > 0)
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            StringBuilder str = new StringBuilder();
            str.append(messages.getString("pagination.header").replace("<self>", String.valueOf(pag)).replace("<total>", String.valueOf(pagination.length())));

            ratingsSorting.forEach((k, v) -> {
                String msg = messages.getString("ticket.stats.list")
                        .replace("<average>", String.format("%.2f", v))
                        .replace("<button-stats>",
                                String.format("[%s](/ticket stats %s hover=%s)",
                                        messages.getString("ticket.buttons.stats.label"), k,
                                        messages.getString("ticket.buttons.stats.hover")))
                        .replace("<player>", k);

                str.append(String.format("\n%s", msg));
            });

            String footer = String.format("%s", messages.getString("pagination.footer"))
                    .replace("<button-back>", pag > 1 && pagination.length() > 1
                            ? String.format("[%s](/ticket stats %d hover=%s)",
                            messages.getString("pagination.buttons.back.label"),
                            pag - 1,
                            messages.getString("pagination.buttons.back.hover")) : "")
                    .replace("<button-next>", pagination.length() > pag
                            ? String.format("[%s](/ticket stats %d hover=%s)",
                            messages.getString("pagination.buttons.next.label"),
                            pag + 1,
                            messages.getString("pagination.buttons.next.hover")) : "");

            str.append(pagination.length() == 1 ? "\n" : footer);

            m.sendMessage(p, str.toString());
        }
        catch (NumberFormatException e) {
            List<TicketModel> tickets = controller.fetchAnsweredBy(args[1]);
            Map<TicketRatingEnum, Integer> ratings = new HashMap<>();

            if (tickets.isEmpty()) {
                m.sendMessage(p, messages.getString("player.not_found"), "ticket");
                return;
            }

            for (TicketRatingEnum tre : TicketRatingEnum.values()) {
                ratings.put(tre, Integer.parseInt(String.valueOf(tickets.stream().filter(t -> t.getRating() == tre.getRate()).count())));
            }

            int ratingCount = (int) tickets.stream().filter(t -> t.getRating() > 0).count();
            int noRatingCount = (int) tickets.stream().filter(t -> t.getRating() == 0).count();
            double avg = tickets.stream().filter(t -> t.getRating() > 0).mapToDouble(TicketModel::getRating).sum() / Integer.parseInt(String.valueOf(tickets.stream().filter(t -> t.getRating() > 0).count()));

            StringBuilder str = new StringBuilder();

            for (String s : messages.getStringList("ticket.stats.detail")) {
                if (s.contains("<ratings>")) {
                    StringBuilder r = new StringBuilder();
                    for (TicketRatingEnum tre : TicketRatingEnum.values()) {
                        r.append(s.replace("<ratings>", String.format("%s: %d%n", tre.format(), ratings.get(tre))));
                    }

                    s = r.toString();
                }

                str.append("\n");
                str.append(s.replace("<player>", args[1])
                        .replace("<average>", String.format("%.2f", avg))
                        .replace("<total>", String.valueOf(tickets.size()))
                        .replace("<total-rate>", String.valueOf(ratingCount))
                        .replace("<total-pending>", String.valueOf(noRatingCount))
                );
            }

            m.sendMessage(p, str.toString());
        }
        catch (PaginationException e) {
            m.sendMessage(p, messages.getString("pagination.error.not_found"), "ticket");
        }
        catch (Exception e) {
            m.sendMessage(p, messages.getString("error"), "ticket");
        }
    }

    private void cancelAllByPlayer(Player p, String[] args) {
        if (!p.hasPermission("ticketsystem.ticket.admin")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "ticket");
            return;
        }

        if (args.length < 2) {
            m.sendMessage(p, messages.getString("player.not_found"), "ticket");
            return;
        }

        List<TicketModel> tickets = controller.fetchAnsweredBy(args[1]);

        if (tickets.isEmpty()) {
            m.sendMessage(p, messages.getString("ticket.cancel_all.empty"), "ticket");
            return;
        }

        tickets.forEach(t -> {
            t.setRating((double) TicketRatingEnum.CANCELED.getRate());
            controller.update(t);
        });

        m.sendMessage(p, messages.getString("ticket.cancel_all.success").replace("<player>", args[1]), "ticket");
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
