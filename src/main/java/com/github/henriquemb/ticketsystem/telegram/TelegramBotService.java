package com.github.henriquemb.ticketsystem.telegram;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.handler.TelegramCallbackHandler;
import com.github.henriquemb.ticketsystem.telegram.handler.TelegramCommandHandler;
import com.github.henriquemb.ticketsystem.telegram.handler.TelegramMessageHandler;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.model.UserState;
import com.github.henriquemb.ticketsystem.telegram.service.StatisticsService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramApiService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramFormatService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramKeyboardService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramUserService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketAnswerService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketReviewService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketSyncService;
import org.bukkit.plugin.Plugin;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class TelegramBotService implements LongPollingSingleThreadUpdateConsumer {
    private final Plugin plugin;
    private final TelegramSettings settings;
    private final TelegramRepository repository;
    private final TicketAnswerService answerService;
    private final ExecutorService executorService;
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    private TelegramBotsLongPollingApplication botsApplication;
    private TelegramApiService apiService;
    private TelegramUserService userService;
    private TicketSyncService ticketSyncService;
    private TelegramCallbackHandler callbackHandler;
    private TelegramMessageHandler messageHandler;
    private volatile boolean running;

    public TelegramBotService(Plugin plugin, TelegramSettings settings, TelegramRepository repository,
                              TicketAnswerService answerService) {
        this.plugin = plugin;
        this.settings = settings;
        this.repository = repository;
        this.answerService = answerService;
        this.executorService = Executors.newFixedThreadPool(4, new TelegramThreadFactory());
    }

    public void start() {
        if (!settings.isEnabled()) {
            plugin.getLogger().info("Telegram integração desativada no config.yml");
            return;
        }

        if (!settings.hasToken()) {
            plugin.getLogger().warning("Telegram está ativado, mas bot-token não foi configurado.");
            return;
        }

        try {
            TelegramFormatService formatService = new TelegramFormatService();
            TelegramKeyboardService keyboardService = new TelegramKeyboardService();
            apiService = new TelegramApiService(settings.getBotToken(), settings);
            userService = new TelegramUserService(repository, settings);
            StatisticsService statisticsService = new StatisticsService(formatService);

            ticketSyncService = new TicketSyncService(
                    plugin,
                    repository,
                    apiService,
                    formatService,
                    keyboardService,
                    settings,
                    executorService
            );
            TicketReviewService reviewService = new TicketReviewService(repository, formatService, ticketSyncService);
            TelegramCommandHandler commandHandler = new TelegramCommandHandler(apiService, userService, formatService, statisticsService);
            callbackHandler = new TelegramCallbackHandler(apiService, repository, userService, formatService, reviewService, settings, userStates);
            messageHandler = new TelegramMessageHandler(commandHandler, apiService, repository, formatService, answerService, reviewService, settings, userStates);

            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(settings.getBotToken(), this);
            running = true;

            ticketSyncService.start();
            plugin.getLogger().info("Telegram bot iniciado com long polling.");
        }
        catch (Exception e) {
            running = false;
            if (botsApplication != null) {
                try {
                    botsApplication.close();
                }
                catch (Exception closeError) {
                    plugin.getLogger().log(Level.WARNING, "Erro ao fechar polling Telegram após falha de inicialização", closeError);
                }
            }
            plugin.getLogger().log(Level.SEVERE, "Erro ao iniciar Telegram bot", e);
        }
    }

    public void stop() {
        running = false;

        if (ticketSyncService != null) {
            try {
                ticketSyncService.stop();
            }
            catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao parar sincronização Telegram", e);
            }
        }

        if (botsApplication != null) {
            try {
                botsApplication.close();
            }
            catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao parar polling Telegram", e);
            }
        }

        executorService.shutdownNow();
    }

    @Override
    public void consume(Update update) {
        executorService.submit(() -> handleUpdate(update));
    }

    private void handleUpdate(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                TelegramUserProfile user = userService.rememberUser(update.getCallbackQuery().getFrom());
                callbackHandler.handle(update.getCallbackQuery(), user);
                return;
            }

            if (update.hasMessage() && update.getMessage().getFrom() != null) {
                TelegramUserProfile user = userService.rememberUser(update.getMessage().getFrom());
                messageHandler.handle(update.getMessage(), user);
            }
        }
        catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao processar update Telegram", e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public TicketSyncService getTicketSyncService() {
        return ticketSyncService;
    }

    private static class TelegramThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "TicketSystem-Telegram-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
