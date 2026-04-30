package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.SuggestionController;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SuggestionCommand implements CommandExecutor, TabCompleter {
    private final Model m = TicketSystem.getModel();
    private final SuggestionController controller = new SuggestionController();
    private final FileConfiguration messages = TicketSystem.getMessages();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getString("prefix.report") + messages.getString("warnings.only_players"));
            return true;
        }

        Player p = (Player) sender;

        if (args.length <= 0) {
            m.sendMessage(p, messages.getString("suggestion.use_correct"), "suggestion");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "view":
            case "ver":
                view(p, validateId(p, args));
                break;
            case "response":
            case "responder":
                response(p, validateId(p, args), args);
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

        if (args.length <= 1 && sender.hasPermission("ticketsystem.suggestion.staff")) {
            tb.add("help");
            tb.add("response");
            tb.add("view");
        }

        switch (args[0].toLowerCase()) {
            case "response":
            case "responder":
            case "view":
            case "ver":
                controller.fetchNotAnswered().forEach(r -> {
                    tb.add(String.valueOf(r.getId()));
                });
        }

        return tb;
    }

    private void create(Player p, String[] args) {
        if (!p.hasPermission("ticketsystem.suggestion.use")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "suggestion");
            return;
        }

        if (args.length == 0 || String.join(" ", args).trim().isEmpty()) {
            m.sendMessage(p, messages.getString("suggestion.use_correct"), "suggestion");
            return;
        }

        if (args.length < 4) {
            m.sendMessage(p, messages.getString("suggestion.shot"), "suggestion");
            return;
        }

        String reason = String.join(" ", args);

        controller.create(p.getName(), reason);
        m.sendMessage(p, messages.getString("suggestion.success"), "suggestion");

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("ticketsystem.suggestion.staff")) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                m.sendMessage(player, messages.getString("suggestion.new_suggestion")
                        .replace("<button-list>",
                                String.format("[%s](/suggestions hover=%s)",
                                        messages.getString("suggestion.buttons.list.label"),
                                        messages.getString("suggestion.buttons.list.hover"))),"suggestion");
            }
        });
    }

    private void view(Player p, int id) {
        if (!p.hasPermission("ticketsystem.suggestion.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "suggestion");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "suggestion");
            return;
        }

        SuggestionModel suggestion = controller.fetchById(id);

        if (suggestion == null) {
            m.sendMessage(p, messages.getString("suggestion.not_found"), "suggestion");
            return;
        }

        StringBuilder str = new StringBuilder();

        if (suggestion.getResponse() != null) {
            for (String msg : messages.getStringList("suggestion.view-response")) {
                str.append(msg.concat("\n"));
            }

            m.sendMessage(p, new PrepareMessages().suggestionViewResponseMessage(str.toString(), suggestion));
            return;
        }

        for (String msg : messages.getStringList("suggestion.view")) {
            str.append(msg.concat("\n"));
        }

        m.sendMessage(p, new PrepareMessages().suggestionViewMessage(str.toString(), suggestion));
    }

    private void response(Player p, int id, String[] args) {
        if (!p.hasPermission("ticketsystem.suggestion.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "suggestion");
            return;
        }

        if (id <= 0) {
            m.sendMessage(p, messages.getString("warnings.invalid_id"), "suggestion");
            return;
        }

        SuggestionModel suggestion = controller.fetchById(id);

        if (args.length < 2 || String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim().isEmpty()) {
            m.sendMessage(p, messages.getString("suggestion.response.use_correct"), "suggestion");
            return;
        }

        String response = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (suggestion == null || suggestion.getResponse() != null) {
            m.sendMessage(p, messages.getString("suggestion.not_found"), "suggestion");
            return;
        }

        suggestion.setResponse(response);
        suggestion.setRespondedBy(p.getName());
        suggestion.setRespondedAt(new Timestamp(System.currentTimeMillis()));

        controller.update(suggestion);
        m.sendMessage(p, messages.getString("suggestion.response.success"), "suggestion");

        Player t = Bukkit.getPlayerExact(suggestion.getPlayer());

        if (Bukkit.getOnlinePlayers().contains(t)) {
            suggestion.setSend(true);

            controller.update(suggestion);

            String str = messages.getString("suggestion.response.message.header") +
                    new ResponseMessages().getSuggestionResponse(suggestion) +
                    messages.getString("suggestion.response.message.footer");

            m.sendMessage(t, str);
        }
    }

    private void help(Player p) {
        StringBuilder str = new StringBuilder();
        for (String msg : messages.getStringList("suggestion.help")) {
            str.append(msg);
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
