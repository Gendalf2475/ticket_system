package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.telegram.model.ReviewRating;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramRole;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.model.TicketRow;

public class TelegramFormatService {
    public String newTicketMessage(TicketRow ticket) {
        return "💬 Новый тикет на рассмотрение\n\n" +
                "ID: #" + ticket.getId() + "\n" +
                "Игрок: " + escape(ticket.getPlayer()) + "\n" +
                "Проблема: " + escape(ticket.getRequest());
    }

    public String newTicketClosedMessage(TicketRow ticket) {
        return "✅ Тикет закрыт\n\n" +
                "ID: #" + ticket.getId() + "\n" +
                "Игрок: " + escape(ticket.getPlayer()) + "\n" +
                "Проблема: " + escape(ticket.getRequest()) + "\n" +
                "Ответил: " + formatTicketResponder(ticket) + "\n\n" +
                "Перенесён в \"Закрытые тикеты\".";
    }

    public String closedTicketMessage(TicketRow ticket) {
        return "✅ Тикет закрыт\n\n" +
                "ID: #" + ticket.getId() + "\n" +
                "Игрок: " + escape(ticket.getPlayer()) + "\n" +
                "Проблема: " + escape(ticket.getRequest()) + "\n" +
                "Ответ: " + escape(ticket.getResponse()) + "\n" +
                "Ответил: " + formatTicketResponder(ticket);
    }

    public String reviewedTicketMessage(TicketRow ticket) {
        StringBuilder message = new StringBuilder();
        message.append("⭐ Тикет проверен\n\n")
                .append("ID: #").append(ticket.getId()).append("\n")
                .append("Игрок: ").append(escape(ticket.getPlayer())).append("\n")
                .append("Проблема: ").append(escape(ticket.getRequest())).append("\n")
                .append("Ответ: ").append(escape(ticket.getResponse())).append("\n")
                .append("Ответил: ").append(formatTicketResponder(ticket)).append("\n")
                .append("Проверил: ").append(formatStoredTelegramUser(
                        ticket.getReviewedByTelegramId(),
                        ticket.getReviewedByTelegramUsername(),
                        ticket.getReviewedByName())).append("\n")
                .append("Оценка: ").append(ticket.getReviewRating() == null ? "не задана" : ticket.getReviewRating().getDisplayName());

        if (ticket.getReviewComment() != null && !ticket.getReviewComment().trim().isEmpty()) {
            message.append("\nКомментарий: ").append(escape(ticket.getReviewComment()));
        }

        return message.toString();
    }

    public String criticsTicketMessage(TicketRow ticket) {
        return "⚠️ КРИТЫ\n\n" + reviewedTicketMessage(ticket);
    }

    public String profileMessage(TelegramUserProfile profile) {
        String nickname = profile.getNickname() == null || profile.getNickname().trim().isEmpty()
                ? "не задан"
                : escape(profile.getNickname());
        String username = profile.getUsername() == null || profile.getUsername().trim().isEmpty()
                ? "без username"
                : "@" + escape(profile.getUsername());
        String role = profile.getRole() == null ? "нет доступа" : profile.getRole().getDisplayName();

        return "👤 Профиль\n\n" +
                "Роль: " + role + "\n" +
                "Никнейм: " + nickname + "\n" +
                "Telegram: " + username + "\n" +
                "Отображение: " + formatTelegramUser(profile);
    }

    public String accessMessage(TelegramRole role) {
        if (role == TelegramRole.SUPER_ADMIN) {
            return "✅ Вы суперадмин\n" +
                    "Доступны все функции:\n" +
                    "- Ответы на тикеты\n" +
                    "- Проверка тикетов\n" +
                    "- Управление пользователями\n" +
                    "- Статистика";
        }

        if (role == TelegramRole.ADMIN) {
            return "✅ Вы администратор\n" +
                    "Доступны функции:\n" +
                    "- Ответы на тикеты\n" +
                    "- Проверка тикетов\n" +
                    "- Статистика";
        }

        if (role == TelegramRole.MODERATOR) {
            return "✅ Вы модератор\n" +
                    "Доступны функции:\n" +
                    "- Ответы на тикеты";
        }

        return "❌ У вас нет доступа к системе тикетов.";
    }

    public String formatTelegramUser(TelegramUserProfile profile) {
        if (profile == null) return "Пользователь";
        return formatStoredTelegramUser(profile.getTelegramId(), profile.getUsername(), plainTelegramDisplayName(profile));
    }

    public String plainTelegramDisplayName(TelegramUserProfile profile) {
        if (profile == null) return "Пользователь";
        if (profile.getNickname() != null && !profile.getNickname().trim().isEmpty()) return profile.getNickname().trim();
        if (profile.getUsername() != null && !profile.getUsername().trim().isEmpty()) return profile.getUsername().trim();

        String firstName = profile.getFirstName() == null ? "" : profile.getFirstName().trim();
        String lastName = profile.getLastName() == null ? "" : profile.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isEmpty()) return fullName;

        return "Пользователь";
    }

    public String formatTicketResponder(TicketRow ticket) {
        if ("telegram".equalsIgnoreCase(ticket.getAnsweredByType()) || ticket.getAnsweredByTelegramId() != null) {
            String displayName = firstNotBlank(ticket.getAnsweredByName(), ticket.getRespondedBy(),
                    joinNames(ticket.getAnsweredByTelegramFirstName(), ticket.getAnsweredByTelegramLastName()), "Пользователь");
            return formatStoredTelegramUser(ticket.getAnsweredByTelegramId(), ticket.getAnsweredByTelegramUsername(), displayName);
        }

        return escape(firstNotBlank(ticket.getAnsweredByName(), ticket.getRespondedBy(), "Неизвестно"));
    }

    public String formatStoredTelegramUser(Long telegramId, String username, String displayName) {
        String safeDisplay = escape(firstNotBlank(displayName, username, "Пользователь"));
        String safeUsername = sanitizeUsername(username);

        if (safeUsername != null) {
            return "<a href=\"https://t.me/" + safeUsername + "\">" + safeDisplay + "</a>";
        }

        if (telegramId != null && telegramId > 0) {
            return "<a href=\"tg://user?id=" + telegramId + "\">" + safeDisplay + "</a>";
        }

        return safeDisplay;
    }

    public String escape(String value) {
        if (value == null) return "";

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String joinNames(String firstName, String lastName) {
        return firstNotBlank(((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim());
    }

    private String sanitizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;

        String normalized = username.trim();
        if (normalized.startsWith("@")) normalized = normalized.substring(1);
        if (!normalized.matches("[A-Za-z0-9_]{5,32}")) return null;
        return normalized;
    }
}
