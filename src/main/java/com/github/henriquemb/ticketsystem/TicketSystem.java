package com.github.henriquemb.ticketsystem;

import com.github.henriquemb.ticketsystem.commands.CommandRegister;
import com.github.henriquemb.ticketsystem.database.factory.CreateDatabase;
import com.github.henriquemb.ticketsystem.events.ListenerRegister;
import com.github.henriquemb.ticketsystem.telegram.TelegramBotService;
import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.service.TicketAnswerService;
import com.github.henriquemb.ticketsystem.util.CustomConfig;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

public final class TicketSystem extends JavaPlugin {
    @Getter @Setter
    private static TicketSystem main;
    @Getter @Setter
    private static Model model;
    @Getter @Setter
    private static FileConfiguration messages;
    @Getter @Setter
    private static TicketAnswerService ticketAnswerService;
    @Getter @Setter
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
}
