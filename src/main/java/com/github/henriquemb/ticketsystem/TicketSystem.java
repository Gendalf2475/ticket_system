package com.github.henriquemb.ticketsystem;

import com.github.henriquemb.ticketsystem.commands.CommandRegister;
import com.github.henriquemb.ticketsystem.database.factory.CreateDatabase;
import com.github.henriquemb.ticketsystem.events.ListenerRegister;
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

    @Override
    public void onEnable() {
        Locale.setDefault(Locale.US);

        setMain(this);

        if (!new File(getDataFolder().getAbsolutePath().concat("/config.yml")).exists()) {
            getConfig().options().copyDefaults(true);
            getMain().saveConfig();
        }

        CustomConfig.createCustomConfig("language/portuguese");
        CustomConfig.createCustomConfig("language/english");

        if (new File(getDataFolder().getAbsolutePath().concat("/language/") + getConfig().getString("language") + ".yml").exists())
            setMessages(CustomConfig.createCustomConfig("language/".concat(getConfig().getString("language"))));
        else setMessages(CustomConfig.createCustomConfig("language/portuguese"));

        setModel(new Model());

        new CreateDatabase();

        new CommandRegister(this);
        new ListenerRegister(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
