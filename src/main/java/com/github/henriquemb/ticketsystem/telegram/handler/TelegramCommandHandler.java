package com.github.henriquemb.ticketsystem.telegram.handler;

import com.github.henriquemb.ticketsystem.telegram.model.TelegramRole;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.service.StatisticsService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramApiService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramFormatService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramUserService;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

public class TelegramCommandHandler {
    private final TelegramApiService apiService;
    private final TelegramUserService userService;
    private final TelegramFormatService formatService;
    private final StatisticsService statisticsService;

    public TelegramCommandHandler(TelegramApiService apiService, TelegramUserService userService,
                                  TelegramFormatService formatService, StatisticsService statisticsService) {
        this.apiService = apiService;
        this.userService = userService;
        this.formatService = formatService;
        this.statisticsService = statisticsService;
    }

    public void handle(Message message, TelegramUserProfile user) {
        String text = message.getText();
        String command = commandToken(text);

        switch (command) {
            case "/start":
            case "/access":
                reply(message, formatService.accessMessage(user.getRole()));
                break;
            case "/profile":
                reply(message, formatService.profileMessage(user));
                break;
            case "/setnick":
                setNickname(message, user);
                break;
            case "/delnick":
                deleteNickname(message, user);
                break;
            case "/addadmin":
                addAdmin(message, user);
                break;
            case "/removeadmin":
                removeAdmin(message, user);
                break;
            case "/setrole":
                setRole(message, user);
                break;
            case "/admins":
                listAdmins(message, user);
                break;
            case "/stats":
                stats(message, user, "full");
                break;
            case "/stats_month":
                stats(message, user, "month");
                break;
            case "/stats_all":
                stats(message, user, "all");
                break;
            default:
                reply(message, "Неизвестная команда.");
        }
    }

    private void setNickname(Message message, TelegramUserProfile user) {
        String args = arguments(message.getText());
        if (args.isEmpty()) {
            reply(message, "Использование: /setnick <nickname>");
            return;
        }

        String[] parts = args.split("\\s+", 2);
        Long targetId = parseLong(parts[0]);

        if (targetId != null && parts.length == 2) {
            if (!userService.canManageUsers(user)) {
                reply(message, "❌ У вас нет доступа к этому действию.");
                return;
            }

            userService.setNickname(targetId, parts[1].trim());
            reply(message, "✅ Никнейм пользователя сохранён: " + formatService.escape(parts[1].trim()));
            return;
        }

        if (user.getRole() == null) {
            reply(message, "❌ У вас нет доступа к системе тикетов.");
            return;
        }

        userService.setNickname(user.getTelegramId(), args);
        user.setNickname(args);
        reply(message, "✅ Ваш никнейм сохранён: " + formatService.escape(args));
    }

    private void deleteNickname(Message message, TelegramUserProfile user) {
        String args = arguments(message.getText());

        if (!args.isEmpty()) {
            Long targetId = parseLong(args);
            if (targetId == null) {
                reply(message, "Использование: /delnick <telegram_id>");
                return;
            }
            if (!userService.canManageUsers(user)) {
                reply(message, "❌ У вас нет доступа к этому действию.");
                return;
            }

            userService.deleteNickname(targetId);
            reply(message, "✅ Никнейм пользователя удалён.");
            return;
        }

        if (user.getRole() == null) {
            reply(message, "❌ У вас нет доступа к системе тикетов.");
            return;
        }

        userService.deleteNickname(user.getTelegramId());
        user.setNickname(null);
        reply(message, "✅ Ваш никнейм удалён.");
    }

    private void addAdmin(Message message, TelegramUserProfile user) {
        if (!userService.canManageUsers(user)) {
            reply(message, "❌ У вас нет доступа к этому действию.");
            return;
        }

        String[] args = arguments(message.getText()).split("\\s+");
        if (args.length != 2) {
            reply(message, "Использование: /addadmin <telegram_id> <admin|moderator>");
            return;
        }

        Long targetId = parseLong(args[0]);
        TelegramRole role = userService.parseManageableRole(args[1], false);
        if (targetId == null || role == null) {
            reply(message, "Роль должна быть admin или moderator.");
            return;
        }

        userService.setRole(targetId, role);
        reply(message, "✅ Доступ выдан: " + role.getDisplayName() + ".");
    }

    private void removeAdmin(Message message, TelegramUserProfile user) {
        if (!userService.canManageUsers(user)) {
            reply(message, "❌ У вас нет доступа к этому действию.");
            return;
        }

        Long targetId = parseLong(arguments(message.getText()));
        if (targetId == null) {
            reply(message, "Использование: /removeadmin <telegram_id>");
            return;
        }
        if (userService.hasPermanentFullAccess(targetId)) {
            reply(message, "Этот пользователь имеет постоянный полный доступ.");
            return;
        }

        userService.removeRole(targetId);
        reply(message, "✅ Доступ пользователя удалён.");
    }

    private void setRole(Message message, TelegramUserProfile user) {
        if (!userService.canManageUsers(user)) {
            reply(message, "❌ У вас нет доступа к этому действию.");
            return;
        }

        String[] args = arguments(message.getText()).split("\\s+");
        if (args.length != 2) {
            reply(message, "Использование: /setrole <telegram_id> <super_admin|admin|moderator>");
            return;
        }

        Long targetId = parseLong(args[0]);
        TelegramRole role = userService.parseManageableRole(args[1], true);
        if (targetId == null || role == null) {
            reply(message, "Роль должна быть super_admin, admin или moderator.");
            return;
        }
        if (userService.hasPermanentFullAccess(targetId) && role != TelegramRole.SUPER_ADMIN) {
            reply(message, "Этот пользователь имеет постоянный полный доступ.");
            return;
        }

        userService.setRole(targetId, role);
        reply(message, "✅ Роль обновлена: " + role.getDisplayName() + ".");
    }

    private void listAdmins(Message message, TelegramUserProfile user) {
        if (user.getRole() == null) {
            reply(message, "❌ У вас нет доступа к системе тикетов.");
            return;
        }

        List<TelegramUserProfile> users = userService.listUsersWithAccess();
        if (users.isEmpty()) {
            reply(message, "Пользователей с доступом пока нет.");
            return;
        }

        StringBuilder builder = new StringBuilder("👥 Пользователи с доступом\n\n");
        for (int i = 0; i < users.size(); i++) {
            TelegramUserProfile listedUser = users.get(i);
            if (i > 0) builder.append("\n");
            builder.append(i + 1)
                    .append(". ")
                    .append(formatService.formatTelegramUser(listedUser))
                    .append(" — ")
                    .append(listedUser.getRole().getDisplayName());

            if (listedUser.getUsername() != null && !listedUser.getUsername().trim().isEmpty()) {
                builder.append(" (@").append(formatService.escape(listedUser.getUsername())).append(")");
            }

            if (user.getRole() == TelegramRole.SUPER_ADMIN) {
                builder.append(" | ID: ").append(listedUser.getTelegramId());
            }
        }

        reply(message, builder.toString());
    }

    private void stats(Message message, TelegramUserProfile user, String mode) {
        if (!userService.canViewStatistics(user)) {
            reply(message, "❌ У вас нет доступа к этому действию.");
            return;
        }

        if ("month".equals(mode)) {
            reply(message, statisticsService.monthStatsMessage());
        }
        else if ("all".equals(mode)) {
            reply(message, statisticsService.allStatsMessage());
        }
        else {
            reply(message, statisticsService.fullStatsMessage());
        }
    }

    private void reply(Message message, String text) {
        apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), text, null);
    }

    private String commandToken(String text) {
        String token = text.trim().split("\\s+", 2)[0];
        int botUsernameIndex = token.indexOf('@');
        if (botUsernameIndex >= 0) token = token.substring(0, botUsernameIndex);
        return token.toLowerCase();
    }

    private String arguments(String text) {
        String trimmed = text.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0) return "";
        return trimmed.substring(firstSpace + 1).trim();
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
