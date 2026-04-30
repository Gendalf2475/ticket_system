package com.github.henriquemb.ticketsystem.database.factory;

import com.github.henriquemb.ticketsystem.TicketSystem;
import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class ConnectionFactory {
    public ConnectionFactory() {
        new CreateDatabase();
    }

    public static Connection createConnection() {
        String url = String.format("jdbc:sqlite:%s/database.db", TicketSystem.getMain().getDataFolder().getAbsolutePath());

        Connection conn = null;

        SQLiteConfig sqLiteConfig = new SQLiteConfig();
        Properties properties = sqLiteConfig.toProperties();
        properties.setProperty(SQLiteConfig.Pragma.DATE_STRING_FORMAT.pragmaName, "yyyy-MM-dd HH:mm:ss");

        try {
            conn = DriverManager.getConnection(url, properties);
        }
        catch (Exception e) {
            System.out.println("Erro ao criar conexão com banco de dados");
        }

        return conn;
    }
}