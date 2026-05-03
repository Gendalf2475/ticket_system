package com.github.henriquemb.ticketsystem;

import com.github.henriquemb.ticketsystem.commands.CommandRegister;
import com.github.henriquemb.ticketsystem.database.factory.CreateDatabase;
import com.github.henriquemb.ticketsystem.events.ListenerRegister;
import com.github.henriquemb.ticketsystem.telegram.TelegramBotService;
import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.service.TicketAnswerService;
import com.github.henriquemb.ticketsystem.util.CustomConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

public final class TicketSystem extends JavaPlugin {
    private static TicketSystem main;
    private static Model model;
    private static FileConfiguration messages;
    private static TicketAnswerService ticketAnswerService;
    private static TelegramBotService telegramBotService;

    @Override
    public void onEnable() {
        Locale.setDefault(Locale.US);

        setMain(this);

        getConfig().options().copyDefaults(true);
        getMain().saveConfig();

        CustomConfig.createCustomConfig("language/portuguese");
        CustomConfig.createCustomConfig("language/english");
        CustomConfig.createCustomConfig("language/russian");

        if (new File(getDataFolder().getAbsolutePath().concat("/language/") + getConfig().getString("language") + ".yml").exists())
            setMessages(CustomConfig.createCustomConfig("language/".concat(getConfig().getString("language"))));
        else setMessages(CustomConfig.createCustomConfig("language/russian"));

        setModel(new Model());

        new CreateDatabase();

        TelegramRepository telegramRepository = new TelegramRepository();
        setTicketAnswerService(new TicketAnswerService(this, telegramRepository));
        setTelegramBotService(new TelegramBotService(this, new TelegramSettings(getConfig()), telegramRepository, getTicketAnswerService()));

        new CommandRegister(this);
        new ListenerRegister(this);

        getTelegramBotService().start();
    }

    @Override
    public void onDisable() {
        if (getTelegramBotService() != null) getTelegramBotService().stop();
    }

    public static TicketSystem getMain() {
        return main;
    }

    public static void setMain(TicketSystem main) {
        TicketSystem.main = main;
    }

    public static Model getModel() {
        return model;
    }

    public static void setModel(Model model) {
        TicketSystem.model = model;
    }

    public static FileConfiguration getMessages() {
        return messages;
    }

    public static void setMessages(FileConfiguration messages) {
        TicketSystem.messages = messages;
    }

    public static TicketAnswerService getTicketAnswerService() {
        return ticketAnswerService;
    }

    public static void setTicketAnswerService(TicketAnswerService ticketAnswerService) {
        TicketSystem.ticketAnswerService = ticketAnswerService;
    }

    public static TelegramBotService getTelegramBotService() {
        return telegramBotService;
    }

    public static void setTelegramBotService(TelegramBotService telegramBotService) {
        TicketSystem.telegramBotService = telegramBotService;
    }
}
