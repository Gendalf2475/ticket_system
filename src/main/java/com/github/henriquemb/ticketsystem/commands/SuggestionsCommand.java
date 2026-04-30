package com.github.henriquemb.ticketsystem.commands;

import com.github.henriquemb.ticketsystem.Model;
import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.controller.SuggestionController;
import com.github.henriquemb.ticketsystem.database.model.SuggestionModel;
import com.github.henriquemb.ticketsystem.exceptions.PaginationException;
import com.github.henriquemb.ticketsystem.util.Pagination;
import com.github.henriquemb.ticketsystem.util.PrepareMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SuggestionsCommand implements CommandExecutor {
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

        if (!p.hasPermission("ticketsystem.suggestion.staff")) {
            m.sendMessage(p, messages.getString("permission.no_permission"), "suggestion");
            return true;
        }

        List<SuggestionModel> suggestions;
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
            suggestions = controller.fetchAll();
        else suggestions = controller.fetchNotAnswered();

        if (suggestions.isEmpty()) {
            m.sendMessage(p, messages.getString("suggestion.empty"), "suggestion");
            return true;
        }

        Pagination<SuggestionModel> pagination = new Pagination<>(suggestions, 10);

        try {
            StringBuilder str = new StringBuilder();
            str.append(messages.getString("pagination.header").replace("<self>", String.valueOf(pag)).replace("<total>", String.valueOf(pagination.length())));

            for (SuggestionModel s : pagination.getPag(pag)) {
                str.append(new PrepareMessages().suggestionViewMessage(messages.getString("suggestion.list"), s));
            }

            String footer = String.format("%s", messages.getString("pagination.footer"))
                    .replace("<button-back>", pag > 1 && pagination.length() > 1
                            ? String.format("[%s](/suggestions %d %s hover=%s)",
                            messages.getString("pagination.buttons.back.label"),
                            pag - 1,
                            options,
                            messages.getString("pagination.buttons.back.hover")) : "")
                    .replace("<button-next>", pagination.length() > pag
                            ? String.format("[%s](/suggestions %d %s hover=%s)",
                            messages.getString("pagination.buttons.next.label"),
                            pag + 1,
                            options,
                            messages.getString("pagination.buttons.next.hover")) : "");

            str.append(pagination.length() == 1 ? "\n" : footer);

            m.sendMessage(p, str.toString());
        }
        catch (PaginationException e) {
            m.sendMessage(p, messages.getString("pagination.error.not_found"), "suggestion");
        }

        return true;
    }
}
