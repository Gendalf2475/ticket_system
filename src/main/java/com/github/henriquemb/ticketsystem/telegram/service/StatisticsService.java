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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        TelegramUsersIndex users = loadTelegramUsersIndex();
        Map<String, AnswerStatsRow> rows = new LinkedHashMap<>();
        String sql = "SELECT t.answered_by_telegram_id, COALESCE(t.answered_by_telegram_username, '') AS username, " +
                "COALESCE(t.answered_by_name, t.respondedBy, 'Неизвестно') AS display_name, tr.rating AS review_rating " +
                "FROM ticket t LEFT JOIN ticket_reviews tr ON tr.ticket_id = t.id " +
                "WHERE t.response IS NOT NULL " +
                (since == null ? "" : "AND t.respondedAt >= ? ");

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            if (since != null) pstm.setTimestamp(1, since);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Long telegramId = getLongObject(rs, "answered_by_telegram_id");
                    String username = rs.getString("username");
                    String displayName = rs.getString("display_name");
                    StatsUser user = users.find(telegramId, username, displayName);

                    AnswerStatsRow row = rows.computeIfAbsent(statsKey(user, telegramId, displayName), key ->
                            new AnswerStatsRow(user, telegramId, username, displayName));
                    row.answers++;

                    String rating = rs.getString("review_rating");
                    if ("excellent".equalsIgnoreCase(rating)) row.excellent++;
                    else if ("good".equalsIgnoreCase(rating)) row.good++;
                    else if ("bad".equalsIgnoreCase(rating)) row.bad++;
                }
            }
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao carregar estatísticas de respostas Telegram", e);
        }

        List<AnswerStatsRow> sortedRows = new ArrayList<>(rows.values());
        sortedRows.sort(Comparator
                .comparingInt((AnswerStatsRow row) -> row.answers).reversed()
                .thenComparing(row -> normalize(row.displayName)));
        return sortedRows;
    }

    private List<ReviewStatsRow> loadReviewRows(Timestamp since) {
        TelegramUsersIndex users = loadTelegramUsersIndex();
        Map<String, ReviewStatsRow> rows = new LinkedHashMap<>();
        String sql = "SELECT tr.reviewed_by_telegram_id, COALESCE(tr.reviewed_by_telegram_username, '') AS username, " +
                "COALESCE(tr.reviewed_by_name, 'Пользователь') AS display_name " +
                "FROM ticket_reviews tr " +
                (since == null ? "" : "WHERE tr.created_at >= ? ");

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            if (since != null) pstm.setTimestamp(1, since);

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    Long telegramId = getLongObject(rs, "reviewed_by_telegram_id");
                    String username = rs.getString("username");
                    String displayName = rs.getString("display_name");
                    StatsUser user = users.find(telegramId, username, displayName);

                    ReviewStatsRow row = rows.computeIfAbsent(statsKey(user, telegramId, displayName), key ->
                            new ReviewStatsRow(user, telegramId, username, displayName));
                    row.reviews++;
                }
            }
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao carregar estatísticas проверок Telegram", e);
        }

        List<ReviewStatsRow> sortedRows = new ArrayList<>(rows.values());
        sortedRows.sort(Comparator
                .comparingInt((ReviewStatsRow row) -> row.reviews).reversed()
                .thenComparing(row -> normalize(row.displayName)));
        return sortedRows;
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

    private TelegramUsersIndex loadTelegramUsersIndex() {
        TelegramUsersIndex index = new TelegramUsersIndex();

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("SELECT telegram_id, username, first_name, last_name, nickname FROM telegram_users");
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) {
                StatsUser user = new StatsUser();
                user.telegramId = rs.getLong("telegram_id");
                user.username = rs.getString("username");
                user.firstName = rs.getString("first_name");
                user.lastName = rs.getString("last_name");
                user.nickname = rs.getString("nickname");
                index.add(user);
            }
        }
        catch (Exception e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao carregar usuários Telegram para estatísticas", e);
        }

        return index;
    }

    private String statsKey(StatsUser user, Long telegramId, String fallbackName) {
        // Telegram user_id is the stable statistics key; /setnick and usernames are display-only and may change.
        if (user != null) return "tg:" + user.telegramId;
        if (telegramId != null && telegramId > 0) return "tg:" + telegramId;
        return "name:" + normalize(fallbackName);
    }

    private Long getLongObject(ResultSet rs, String column) throws Exception {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private class AnswerStatsRow {
        private Long telegramId;
        private String username;
        private String displayName;
        private int answers;
        private int excellent;
        private int good;
        private int bad;

        private AnswerStatsRow(StatsUser user, Long telegramId, String username, String displayName) {
            this.telegramId = user == null ? telegramId : user.telegramId;
            this.username = user == null ? username : user.username;
            this.displayName = user == null ? displayName : user.displayName();
        }
    }

    private class ReviewStatsRow {
        private Long telegramId;
        private String username;
        private String displayName;
        private int reviews;

        private ReviewStatsRow(StatsUser user, Long telegramId, String username, String displayName) {
            this.telegramId = user == null ? telegramId : user.telegramId;
            this.username = user == null ? username : user.username;
            this.displayName = user == null ? displayName : user.displayName();
        }
    }

    private class TelegramUsersIndex {
        private final Map<Long, StatsUser> byId = new HashMap<>();
        private final Map<String, StatsUser> byName = new HashMap<>();

        private void add(StatsUser user) {
            byId.put(user.telegramId, user);
            putName(user.nickname, user);
            putName(user.username, user);
            putName(user.displayName(), user);
        }

        private StatsUser find(Long telegramId, String username, String displayName) {
            if (telegramId != null && byId.containsKey(telegramId)) return byId.get(telegramId);

            StatsUser user = byName.get(normalize(username));
            if (user != null) return user;

            return byName.get(normalize(displayName));
        }

        private void putName(String name, StatsUser user) {
            String normalized = normalize(name);
            if (!normalized.isEmpty()) byName.putIfAbsent(normalized, user);
        }
    }

    private class StatsUser {
        private long telegramId;
        private String username;
        private String firstName;
        private String lastName;
        private String nickname;

        private String displayName() {
            String fullName = formatService.firstNotBlank(
                    nickname,
                    username,
                    ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim()
            );
            return fullName.isEmpty() ? "Пользователь" : fullName;
        }
    }
}
