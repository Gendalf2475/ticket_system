package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StatisticsService {
    private final TelegramFormatService formatService;

    public StatisticsService(TelegramFormatService formatService) {
        this.formatService = formatService;
    }

    public String fullStatsMessage() {
        return "📊 Статистика тикетов\n\n" +
                "За месяц:\n" +
                formatAnswerRows(loadAnswerRows(monthStart())) +
                "\n\nПроверки:\n" +
                formatReviewRows(loadReviewRows(monthStart())) +
                "\n\nЗа всё время:\n" +
                formatAnswerRows(loadAnswerRows(null)) +
                "\n\nПроверки за всё время:\n" +
                formatReviewRows(loadReviewRows(null));
    }

    public String monthStatsMessage() {
        return "📊 Статистика тикетов за месяц\n\n" +
                formatAnswerRows(loadAnswerRows(monthStart())) +
                "\n\nПроверки:\n" +
                formatReviewRows(loadReviewRows(monthStart()));
    }

    public String allStatsMessage() {
        return "📊 Статистика тикетов за всё время\n\n" +
                formatAnswerRows(loadAnswerRows(null)) +
                "\n\nПроверки:\n" +
                formatReviewRows(loadReviewRows(null));
    }

    private List<AnswerStatsRow> loadAnswerRows(Timestamp since) {
        List<AnswerStatsRow> rows = new ArrayList<>();
        String sql = "SELECT t.answered_by_telegram_id, COALESCE(t.answered_by_telegram_username, '') AS username, " +
                "COALESCE(t.answered_by_name, t.respondedBy, 'Неизвестно') AS display_name, " +
                "COUNT(*) AS answers, " +
                "SUM(CASE WHEN tr.rating = 'excellent' THEN 1 ELSE 0 END) AS excellent_count, " +
                "SUM(CASE WHEN tr.rating = 'good' THEN 1 ELSE 0 END) AS good_count, " +
                "SUM(CASE WHEN tr.rating = 'bad' THEN 1 ELSE 0 END) AS bad_count " +
                "FROM ticket t LEFT JOIN ticket_reviews tr ON tr.ticket_id = t.id " +
                "WHERE t.response IS NOT NULL " +
                (since == null ? "" : "AND t.respondedAt >= ? ") +
                "GROUP BY t.answered_by_telegram_id, username, display_name " +
                "ORDER BY answers DESC, display_name ASC";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            if (since != null) pstm.setTimestamp(1, since);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    AnswerStatsRow row = new AnswerStatsRow();
                    long telegramId = rs.getLong("answered_by_telegram_id");
                    row.telegramId = rs.wasNull() ? null : telegramId;
                    row.username = rs.getString("username");
                    row.displayName = rs.getString("display_name");
                    row.answers = rs.getInt("answers");
                    row.excellent = rs.getInt("excellent_count");
                    row.good = rs.getInt("good_count");
                    row.bad = rs.getInt("bad_count");
                    rows.add(row);
                }
            }
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao carregar estatísticas de respostas Telegram", e);
        }

        return rows;
    }

    private List<ReviewStatsRow> loadReviewRows(Timestamp since) {
        List<ReviewStatsRow> rows = new ArrayList<>();
        String sql = "SELECT tr.reviewed_by_telegram_id, COALESCE(tr.reviewed_by_telegram_username, '') AS username, " +
                "COALESCE(tr.reviewed_by_name, 'Пользователь') AS display_name, COUNT(*) AS reviews " +
                "FROM ticket_reviews tr " +
                (since == null ? "" : "WHERE tr.created_at >= ? ") +
                "GROUP BY tr.reviewed_by_telegram_id, username, display_name " +
                "ORDER BY reviews DESC, display_name ASC";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            if (since != null) pstm.setTimestamp(1, since);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    ReviewStatsRow row = new ReviewStatsRow();
                    row.telegramId = rs.getLong("reviewed_by_telegram_id");
                    row.username = rs.getString("username");
                    row.displayName = rs.getString("display_name");
                    row.reviews = rs.getInt("reviews");
                    rows.add(row);
                }
            }
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao carregar estatísticas проверок Telegram", e);
        }

        return rows;
    }

    private String formatAnswerRows(List<AnswerStatsRow> rows) {
        if (rows.isEmpty()) return "нет данных";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            AnswerStatsRow row = rows.get(i);
            if (i > 0) builder.append("\n");
            builder.append(i + 1)
                    .append(". ")
                    .append(formatName(row.telegramId, row.username, row.displayName))
                    .append(" — ")
                    .append(row.answers)
                    .append(" ответов | 🟢 ")
                    .append(row.excellent)
                    .append(" | 🟡 ")
                    .append(row.good)
                    .append(" | 🔴 ")
                    .append(row.bad);
        }

        return builder.toString();
    }

    private String formatReviewRows(List<ReviewStatsRow> rows) {
        if (rows.isEmpty()) return "нет данных";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            ReviewStatsRow row = rows.get(i);
            if (i > 0) builder.append("\n");
            builder.append(i + 1)
                    .append(". ")
                    .append(formatName(row.telegramId, row.username, row.displayName))
                    .append(" — ")
                    .append(row.reviews)
                    .append(" проверок");
        }

        return builder.toString();
    }

    private String formatName(Long telegramId, String username, String displayName) {
        if (telegramId != null && telegramId > 0) {
            return formatService.formatStoredTelegramUser(telegramId, username, displayName);
        }

        return formatService.escape(displayName == null ? "Неизвестно" : displayName);
    }

    private Timestamp monthStart() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime start = firstDay.atStartOfDay();
        return Timestamp.valueOf(start);
    }

    private static class AnswerStatsRow {
        private Long telegramId;
        private String username;
        private String displayName;
        private int answers;
        private int excellent;
        private int good;
        private int bad;
    }

    private static class ReviewStatsRow {
        private long telegramId;
        private String username;
        private String displayName;
        private int reviews;
    }
}
