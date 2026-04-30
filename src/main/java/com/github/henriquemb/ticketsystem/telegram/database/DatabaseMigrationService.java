package com.github.henriquemb.ticketsystem.telegram.database;

import com.github.henriquemb.ticketsystem.TicketSystem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class DatabaseMigrationService {
    public void migrate(Connection conn) {
        try (Statement stm = conn.createStatement()) {
            stm.setQueryTimeout(30);

            migrateTicketTable(conn, stm);
            createTelegramUsersTable(stm);
            createTelegramMessagesTable(stm);
            createTicketReviewsTable(stm);
            createIndexes(stm);
            normalizeOldTickets(stm);
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.SEVERE, "Erro ao aplicar migrações Telegram no banco de dados", e);
        }
    }

    private void migrateTicketTable(Connection conn, Statement stm) throws Exception {
        Set<String> columns = getColumns(conn, "ticket");

        addColumnIfMissing(stm, columns, "ticket", "status", "VARCHAR(20) DEFAULT 'open'");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_type", "VARCHAR(20)");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_name", "VARCHAR(200)");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_minecraft_uuid", "VARCHAR(36)");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_telegram_id", "INTEGER");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_telegram_username", "VARCHAR(100)");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_telegram_first_name", "VARCHAR(200)");
        addColumnIfMissing(stm, columns, "ticket", "answered_by_telegram_last_name", "VARCHAR(200)");
        addColumnIfMissing(stm, columns, "ticket", "in_progress_by_telegram_id", "INTEGER");
        addColumnIfMissing(stm, columns, "ticket", "in_progress_by_name", "VARCHAR(200)");
        addColumnIfMissing(stm, columns, "ticket", "in_progress_until", "DATETIME");
        addColumnIfMissing(stm, columns, "ticket", "closed_at", "DATETIME");
        addColumnIfMissing(stm, columns, "ticket", "reviewed_at", "DATETIME");
    }

    private void createTelegramUsersTable(Statement stm) throws Exception {
        stm.executeUpdate("CREATE TABLE IF NOT EXISTS telegram_users (" +
                "telegram_id INTEGER PRIMARY KEY, " +
                "username VARCHAR(100), " +
                "first_name VARCHAR(200), " +
                "last_name VARCHAR(200), " +
                "nickname VARCHAR(200), " +
                "role VARCHAR(20), " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "last_seen_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    private void createTelegramMessagesTable(Statement stm) throws Exception {
        stm.executeUpdate("CREATE TABLE IF NOT EXISTS telegram_messages (" +
                "ticket_id INTEGER PRIMARY KEY, " +
                "new_ticket_chat_id INTEGER, " +
                "new_ticket_thread_id INTEGER, " +
                "new_ticket_message_id INTEGER, " +
                "closed_ticket_chat_id INTEGER, " +
                "closed_ticket_thread_id INTEGER, " +
                "closed_ticket_message_id INTEGER, " +
                "prompt_message_id INTEGER, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    private void createTicketReviewsTable(Statement stm) throws Exception {
        stm.executeUpdate("CREATE TABLE IF NOT EXISTS ticket_reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL UNIQUE, " +
                "rating VARCHAR(20) NOT NULL, " +
                "reviewed_by_telegram_id INTEGER NOT NULL, " +
                "reviewed_by_name VARCHAR(200), " +
                "reviewed_by_telegram_username VARCHAR(100), " +
                "comment TEXT, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    private void createIndexes(Statement stm) throws Exception {
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_response ON ticket(response)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_status ON ticket(status)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_responded_at ON ticket(respondedAt)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_reviewed_at ON ticket(reviewed_at)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_reviews_ticket_id ON ticket_reviews(ticket_id)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ticket_reviews_reviewer ON ticket_reviews(reviewed_by_telegram_id)");
        stm.executeUpdate("CREATE INDEX IF NOT EXISTS idx_telegram_users_role ON telegram_users(role)");
    }

    private void normalizeOldTickets(Statement stm) throws Exception {
        stm.executeUpdate("UPDATE ticket SET status = 'open' WHERE response IS NULL AND (status IS NULL OR status = '')");
        stm.executeUpdate("UPDATE ticket SET status = 'closed', " +
                "closed_at = COALESCE(closed_at, respondedAt), " +
                "answered_by_name = COALESCE(answered_by_name, respondedBy), " +
                "answered_by_type = COALESCE(answered_by_type, 'game') " +
                "WHERE response IS NOT NULL AND (status IS NULL OR status = '' OR status = 'open' OR status = 'in_progress')");
    }

    private Set<String> getColumns(Connection conn, String table) throws Exception {
        Set<String> columns = new HashSet<>();
        try (Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }
        return columns;
    }

    private void addColumnIfMissing(Statement stm, Set<String> columns, String table, String column, String definition) throws Exception {
        if (columns.contains(column.toLowerCase())) return;

        stm.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        columns.add(column.toLowerCase());
    }
}
