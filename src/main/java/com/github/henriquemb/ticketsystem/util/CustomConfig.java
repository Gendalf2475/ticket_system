package com.github.henriquemb.ticketsystem.util;

import com.github.henriquemb.ticketsystem.TicketSystem;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class CustomConfig {
    public static FileConfiguration createCustomConfig(String name) {
        FileConfiguration config = null;
        File f = new File(TicketSystem.getMain().getDataFolder(), name+".yml");

        if (!f.exists()) {
            f.getParentFile().mkdirs();
            TicketSystem.getMain().saveResource(name+".yml", false);
        }

        config = new YamlConfiguration();
        try {
            config.load(f);
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveCustomConfig(String name, FileConfiguration config) {
        new Thread(new BukkitRunnable() {
            @Override
            public void run() {
                File f = new File(TicketSystem.getMain().getDataFolder(), name+".yml");
                try {
                    config.save(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
