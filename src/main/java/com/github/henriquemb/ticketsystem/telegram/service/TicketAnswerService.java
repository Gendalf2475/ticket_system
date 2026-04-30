package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.model.TicketModel;
import com.github.henriquemb.ticketsystem.telegram.TelegramBotService;
import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.AnswerTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.ResponderInfo;
import com.github.henriquemb.ticketsystem.telegram.model.TicketRow;
import com.github.henriquemb.ticketsystem.util.ResponseMessages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TicketAnswerService {
    private final Plugin plugin;
    private final TelegramRepository repository;

    public TicketAnswerService(Plugin plugin, TelegramRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<AnswerTicketResult> answerTicketAsync(int ticketId, String answer, ResponderInfo responder) {
        CompletableFuture<AnswerTicketResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(answerTicket(ticketId, answer, responder));
            }
            catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao responder ticket em tarefa assíncrona", e);
                future.complete(new AnswerTicketResult(AnswerTicketResult.Status.ERROR, null));
            }
        });

        return future;
    }

    public AnswerTicketResult answerTicket(int ticketId, String answer, ResponderInfo responder) {
        AnswerTicketResult result = repository.answerTicketAtomically(ticketId, answer, responder);
        if (!result.isSuccess() || result.getTicket() == null) return result;

        deliverAnswerToOnlinePlayer(result.getTicket());
        syncClosedTicketToTelegram(result.getTicket().getId());
        return result;
    }

    private void deliverAnswerToOnlinePlayer(TicketRow ticket) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(ticket.getPlayer());
            if (player == null || !player.isOnline()) return;

            FileConfiguration messages = TicketSystem.getMessages();
            TicketModel ticketModel = toTicketModel(ticket, true);

            String header = messages.getString("ticket.response.message.header", "");
            String footer = messages.getString("ticket.response.message.footer", "");
            String message = header + new ResponseMessages().getTicketResponse(ticketModel) + footer;

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
            TicketSystem.getModel().sendMessage(player, message);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> repository.markTicketSentToPlayer(ticket.getId()));
        });
    }

    private TicketModel toTicketModel(TicketRow ticket, boolean sent) {
        return new TicketModel(
                ticket.getId(),
                ticket.getPlayer(),
                ticket.getRequest(),
                ticket.getResponse(),
                ticket.getRespondedBy(),
                ticket.getRespondedAt(),
                ticket.getRating() == null ? 0.0 : ticket.getRating(),
                sent,
                ticket.getCreatedAt()
        );
    }

    private void syncClosedTicketToTelegram(int ticketId) {
        TelegramBotService telegramBotService = TicketSystem.getTelegramBotService();
        if (telegramBotService == null || !telegramBotService.isRunning()) return;

        telegramBotService.getTicketSyncService().syncClosedTicketAsync(ticketId);
    }
}
