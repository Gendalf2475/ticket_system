package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramRole;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelegramUserService {
    private final TelegramRepository repository;
    private final TelegramSettings settings;

    public TelegramUserService(TelegramRepository repository, TelegramSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public TelegramUserProfile rememberUser(User user) {
        TelegramUserProfile profile = repository.upsertTelegramUser(
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName()
        );
        applyConfigAccess(profile);
        return profile;
    }

    public TelegramUserProfile resolveUser(long telegramId) {
        TelegramUserProfile profile = repository.findTelegramUser(telegramId).orElseGet(() -> {
            TelegramUserProfile empty = new TelegramUserProfile();
            empty.setTelegramId(telegramId);
            return empty;
        });
        applyConfigAccess(profile);
        return profile;
    }

    public boolean canAnswerTickets(TelegramUserProfile profile) {
        return profile.getRole() != null && profile.getRole().canAnswerTickets();
    }

    public boolean canReviewTickets(TelegramUserProfile profile) {
        return profile.getRole() != null && profile.getRole().canReviewTickets();
    }

    public boolean canManageUsers(TelegramUserProfile profile) {
        return profile.getRole() != null && profile.getRole().canManageUsers();
    }

    public boolean canViewStatistics(TelegramUserProfile profile) {
        return profile.getRole() != null && profile.getRole().canViewStatistics();
    }

    public boolean hasPermanentFullAccess(long telegramId) {
        return settings.getSuperAdmins().contains(telegramId);
    }

    public void setRole(long telegramId, TelegramRole role) {
        repository.setTelegramUserRole(telegramId, role);
    }

    public void removeRole(long telegramId) {
        repository.removeTelegramUserRole(telegramId);
    }

    public void setNickname(long telegramId, String nickname) {
        repository.setTelegramUserNickname(telegramId, nickname);
    }

    public void deleteNickname(long telegramId) {
        repository.deleteTelegramUserNickname(telegramId);
    }

    public List<TelegramUserProfile> listUsersWithAccess() {
        Map<Long, TelegramUserProfile> users = new LinkedHashMap<>();

        for (TelegramUserProfile profile : repository.listTelegramUsersWithRoles()) {
            applyConfigAccess(profile);
            users.put(profile.getTelegramId(), profile);
        }

        for (Long superAdminId : settings.getSuperAdmins()) {
            TelegramUserProfile profile = users.get(superAdminId);
            if (profile == null) profile = resolveUser(superAdminId);
            profile.setRole(TelegramRole.SUPER_ADMIN);
            users.put(superAdminId, profile);
        }

        return new ArrayList<>(users.values());
    }

    public TelegramRole parseManageableRole(String value, boolean allowSuperAdmin) {
        TelegramRole role = TelegramRole.fromDatabaseValue(value);
        if (role == TelegramRole.SUPER_ADMIN && !allowSuperAdmin) return null;
        return role;
    }

    private void applyConfigAccess(TelegramUserProfile profile) {
        if (settings.getSuperAdmins().contains(profile.getTelegramId())) {
            profile.setRole(TelegramRole.SUPER_ADMIN);
        }
    }
}
