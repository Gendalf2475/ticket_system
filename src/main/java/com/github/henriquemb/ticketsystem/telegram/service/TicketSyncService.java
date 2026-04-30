package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramMessageRecord;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TicketRow;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class TicketSyncService {
    private static final int SYNC_LIMIT = 20;

    private final Plugin plugin;
    private final TelegramRepository repository;
    private final TelegramApiService apiService;
    private final TelegramFormatService formatService;
    private final TelegramKeyboardService keyboardService;
    private final TelegramSettings settings;
    private final ExecutorService executorService;
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private BukkitTask pollingTask;

    public TicketSyncService(Plugin plugin, TelegramRepository repository, TelegramApiService apiService,
                             TelegramFormatService formatService, TelegramKeyboardService keyboardService,
                             TelegramSettings settings, ExecutorService executorService) {
        this.plugin = plugin;
        this.repository = repository;
        this.apiService = apiService;
        this.formatService = formatService;
        this.keyboardService = keyboardService;
        this.settings = settings;
        this.executorService = executorService;
    }

    public void start() {
        if (pollingTask != null) return;

        long intervalTicks = Math.max(20L, settings.getPollingIntervalSeconds() * 20L);
        pollingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::syncPendingTicketsSafely, 40L, intervalTicks);
        syncPendingTicketsAsync();
    }

    public void stop() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
    }

    public void syncPendingTicketsAsync() {
        executorService.submit(this::syncPendingTicketsSafely);
    }

    public void syncClosedTicketAsync(int ticketId) {
        executorService.submit(() -> {
            try {
                Optional<TicketRow> ticket = repository.findTicketById(ticketId);
                ticket.filter(TicketRow::isAnswered).ifPresent(this::syncClosedTicket);
            }
            catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao sincronizar ticket fechado no Telegram", e);
            }
        });
    }

    public void syncReviewedTicketAsync(int ticketId) {
        executorService.submit(() -> {
            try {
                Optional<TicketRow> ticket = repository.findTicketById(ticketId);
                if (!ticket.isPresent() || !ticket.get().isReviewed()) return;

                Optional<TelegramMessageRecord> record = repository.findTelegramMessage(ticketId);
                if (record.isPresent() && record.get().getClosedTicketChatId() != null && record.get().getClosedTicketMessageId() != null) {
                    apiService.editMessageText(
                            record.get().getClosedTicketChatId(),
                            record.get().getClosedTicketMessageId(),
                            formatService.reviewedTicketMessage(ticket.get()),
                            null
                    );
                }
            }
            catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao atualizar revisão do ticket no Telegram", e);
            }
        });
    }

    private void syncPendingTicketsSafely() {
        if (!syncRunning.compareAndSet(false, true)) return;

        try {
            syncNewTickets();
            syncClosedTickets();
        }
        catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao sincronizar tickets com Telegram", e);
        }
        finally {
            syncRunning.set(false);
        }
    }

    private void syncNewTickets() {
        if (!settings.getNewTicketsTopic().hasChat()) return;

        List<TicketRow> tickets = repository.findNewTicketsPendingTelegramSync(SYNC_LIMIT);
        for (TicketRow ticket : tickets) {
            Message message = apiService.sendTopicMessage(
                    settings.getNewTicketsTopic(),
                    formatService.newTicketMessage(ticket),
                    keyboardService.answerTicketKeyboard(ticket.getId())
            );

            if (message != null) {
                repository.saveNewTicketMessage(
                        ticket.getId(),
                        settings.getNewTicketsTopic().getChatId(),
                        settings.getNewTicketsTopic().getThreadId(),
                        message.getMessageId()
                );
            }
        }
    }

    private void syncClosedTickets() {
        if (!settings.getClosedTicketsTopic().hasChat()) return;

        List<TicketRow> tickets = repository.findClosedTicketsPendingTelegramSync(SYNC_LIMIT);
        for (TicketRow ticket : tickets) {
            syncClosedTicket(ticket);
        }
    }

    private void syncClosedTicket(TicketRow ticket) {
        if (!settings.getClosedTicketsTopic().hasChat()) return;

        Optional<TelegramMessageRecord> existingMessage = repository.findTelegramMessage(ticket.getId());

        if (!existingMessage.isPresent() || existingMessage.get().getClosedTicketMessageId() == null) {
            Message message = apiService.sendTopicMessage(
                    settings.getClosedTicketsTopic(),
                    formatService.closedTicketMessage(ticket),
                    keyboardService.reviewKeyboard(ticket.getId())
            );

            if (message != null) {
                repository.saveClosedTicketMessage(
                        ticket.getId(),
                        settings.getClosedTicketsTopic().getChatId(),
                        settings.getClosedTicketsTopic().getThreadId(),
                        message.getMessageId()
                );
            }
        }

        updateNewTicketMessageAfterClose(ticket, existingMessage.orElse(null));
    }

    private void updateNewTicketMessageAfterClose(TicketRow ticket, TelegramMessageRecord record) {
        TelegramMessageRecord actualRecord = record;
        if (actualRecord == null) {
            Optional<TelegramMessageRecord> fresh = repository.findTelegramMessage(ticket.getId());
            if (!fresh.isPresent()) return;
            actualRecord = fresh.get();
        }

        if (actualRecord.getNewTicketChatId() == null || actualRecord.getNewTicketMessageId() == null) return;

        apiService.editMessageText(
                actualRecord.getNewTicketChatId(),
                actualRecord.getNewTicketMessageId(),
                formatService.newTicketClosedMessage(ticket),
                null
        );
    }
}
