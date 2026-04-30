package com.github.henriquemb.ticketsystem.database.factory;

import com.github.henriquemb.ticketsystem.TicketSystem;

import java.sql.*;

public class CreateDatabase {
    private static Connection connection = null;

    public CreateDatabase() {
        String url = String.format("jdbc:sqlite:%s/database.db", TicketSystem.getMain().getDataFolder().getAbsolutePath());

        try {
            connection = DriverManager.getConnection(url);

            createTables(connection);

            System.out.println("Banco de dados criado com sucesso");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        finally {
            try {
                if (connection != null) connection.close();
            }
            catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void createTables(Connection conn) {
        final String ticket = "CREATE TABLE IF NOT EXISTS ticket (id INTEGER PRIMARY KEY AUTOINCREMENT, player VARCHAR(30) NOT NULL, request VARCHAR(200) NOT NULL, response VARCHAR(200), respondedBy VARCHAR(30), respondedAt DATETIME, rating DOUBLE, send BOOLEAN DEFAULT FALSE, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        final String report = "CREATE TABLE IF NOT EXISTS report (id INTEGER PRIMARY KEY AUTOINCREMENT, player VARCHAR(30) NOT NULL, reported VARCHAR(30) NOT NULL, reason VARCHAR(200), evidence VARCHAR(200), verified BOOLEAN DEFAULT FALSE, verifiedBy VARCHAR(30), verifiedAt DATETIME, status INTEGER DEFAULT 0, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        final String suggestion = "CREATE TABLE IF NOT EXISTS suggestion (id INTEGER PRIMARY KEY AUTOINCREMENT, player VARCHAR(30) NOT NULL, suggestion VARCHAR(200) NOT NULL, response VARCHAR(200), respondedBy VARCHAR(30), respondedAt DATETIME, send BOOLEAN DEFAULT FALSE, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        Statement stm = null;

        try {
            stm = conn.createStatement();
            stm.setQueryTimeout(30);

            stm.executeUpdate(ticket);
            stm.executeUpdate(report);
            stm.executeUpdate(suggestion);
        }
        catch (Exception e) {
            System.out.println("Erro ao criar tabelas");
            e.printStackTrace();
        }
        finally {
            try {
                if (stm != null) stm.close();
                if (conn != null) conn.close();
            }
            catch (Exception err) {
                System.out.println("Erro ao criar tabelas");
                err.printStackTrace();
            }
        }

    }
}
