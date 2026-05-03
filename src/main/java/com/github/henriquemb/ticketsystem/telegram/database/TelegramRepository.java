package com.github.henriquemb.ticketsystem.telegram.database;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.database.factory.ConnectionFactory;
import com.github.henriquemb.ticketsystem.telegram.model.AnswerTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.ResponderInfo;
import com.github.henriquemb.ticketsystem.telegram.model.ResponderSource;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewRating;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramMessageRecord;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramRole;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.model.TicketLockResult;
import com.github.henriquemb.ticketsystem.telegram.model.TicketRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class TelegramRepository {
    public Optional<TicketRow> findTicketById(int ticketId) {
        String sql = ticketSelectSql() + " WHERE t.id = ?";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, ticketId);

            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapTicket(rs));
            }
        }
        catch (Exception e) {
            log(Level.SEVERE, "Erro ao buscar ticket para Telegram", e);
            return Optional.empty();
        }
    }

    public List<TicketRow> findNewTicketsPendingTelegramSync(int limit) {
        String sql = ticketSelectSql() +
                " LEFT JOIN telegram_messages tm ON tm.ticket_id = t.id " +
                " WHERE t.response IS NULL " +
                " AND (t.status IS NULL OR t.status = '' OR t.status = 'open' OR t.status = 'in_progress') " +
                " AND tm.new_ticket_message_id IS NULL " +
                " ORDER BY t.timestamp ASC LIMIT ?";

        return findTickets(sql, limit);
    }

    public List<TicketRow> findClosedTicketsPendingTelegramSync(int limit) {
        String sql = ticketSelectSql() +
                " LEFT JOIN telegram_messages tm ON tm.ticket_id = t.id " +
                " WHERE t.response IS NOT NULL " +
                " AND tm.closed_ticket_message_id IS NULL " +
                " ORDER BY COALESCE(t.closed_at, t.respondedAt, t.timestamp) ASC LIMIT ?";

        return findTickets(sql, limit);
    }

    private List<TicketRow> findTickets(String sql, int limit) {
        List<TicketRow> tickets = new ArrayList<>();

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, Math.max(1, limit));

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapTicket(rs));
                }
            }
        }
        catch (Exception e) {
            log(Level.SEVERE, "Erro ao buscar tickets para sincronização Telegram", e);
        }

        return tickets;
    }

    public TicketLockResult acquireTicketForTelegramAnswer(int ticketId, TelegramUserProfile user, String displayName, int timeoutSeconds) {
        Timestamp now = now();
        Timestamp until = new Timestamp(now.getTime() + (Math.max(1, timeoutSeconds) * 1000L));
        String sql = "UPDATE ticket SET status = 'in_progress', in_progress_by_telegram_id = ?, " +
                "in_progress_by_name = ?, in_progress_until = ? " +
                "WHERE id = ? AND response IS NULL AND reviewed_at IS NULL AND (" +
                "status IS NULL OR status = '' OR status = 'open' OR " +
                "(status = 'in_progress' AND (in_progress_by_telegram_id = ? OR in_progress_until IS NULL OR in_progress_until <= ?))" +
                ")";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, user.getTelegramId());
            pstm.setString(2, displayName);
            pstm.setTimestamp(3, until);
            pstm.setInt(4, ticketId);
            pstm.setLong(5, user.getTelegramId());
            pstm.setTimestamp(6, now);

            int updated = pstm.executeUpdate();
            Optional<TicketRow> ticket = findTicketById(ticketId);

            if (updated > 0) {
                return new TicketLockResult(TicketLockResult.Status.ACQUIRED, ticket.orElse(null));
            }

            if (!ticket.isPresent()) return new TicketLockResult(TicketLockResult.Status.NOT_FOUND, null);
            if (ticket.get().isReviewed()) return new TicketLockResult(TicketLockResult.Status.ALREADY_REVIEWED, ticket.get());
            if (ticket.get().isAnswered()) return new TicketLockResult(TicketLockResult.Status.ALREADY_CLOSED, ticket.get());
            return new TicketLockResult(TicketLockResult.Status.LOCKED_BY_OTHER, ticket.get());
        }
        catch (Exception e) {
            log(Level.SEVERE, "Erro ao bloquear ticket para resposta Telegram", e);
            return new TicketLockResult(TicketLockResult.Status.ERROR, null);
        }
    }

    public AnswerTicketResult answerTicketAtomically(int ticketId, String answer, ResponderInfo responder) {
        Timestamp now = now();

        try (Connection conn = ConnectionFactory.createConnection()) {
            conn.setAutoCommit(false);

            Optional<TicketRow> current = findTicketById(conn, ticketId);
            if (!current.isPresent()) {
                conn.rollback();
                return new AnswerTicketResult(AnswerTicketResult.Status.NOT_FOUND, null);
            }

            TicketRow ticket = current.get();
            if (ticket.isReviewed()) {
                conn.rollback();
                return new AnswerTicketResult(AnswerTicketResult.Status.ALREADY_REVIEWED, ticket);
            }
            if (ticket.isAnswered()) {
                conn.rollback();
                return new AnswerTicketResult(AnswerTicketResult.Status.ALREADY_CLOSED, ticket);
            }
            if (responder.getSource() == ResponderSource.TELEGRAM && !isLockedByResponder(ticket, responder, now)) {
                conn.rollback();
                return new AnswerTicketResult(AnswerTicketResult.Status.NOT_LOCKED_BY_USER, ticket);
            }

            TelegramUserProfile linkedGameUser = responder.getSource() == ResponderSource.GAME
                    ? findTelegramUserByDisplayName(conn, responder.getDisplayName()).orElse(null)
                    : null;
            Long answeredByTelegramId = responder.getTelegramId();
            String answeredByTelegramUsername = responder.getTelegramUsername();
            String answeredByTelegramFirstName = responder.getTelegramFirstName();
            String answeredByTelegramLastName = responder.getTelegramLastName();
            if (linkedGameUser != null) {
                answeredByTelegramId = linkedGameUser.getTelegramId();
                answeredByTelegramUsername = linkedGameUser.getUsername();
                answeredByTelegramFirstName = linkedGameUser.getFirstName();
                answeredByTelegramLastName = linkedGameUser.getLastName();
            }

            String sql = "UPDATE ticket SET response = ?, respondedBy = ?, respondedAt = ?, send = ?, " +
                    "status = 'closed', closed_at = ?, answered_by_type = ?, answered_by_name = ?, " +
                    "answered_by_minecraft_uuid = ?, answered_by_telegram_id = ?, answered_by_telegram_username = ?, " +
                    "answered_by_telegram_first_name = ?, answered_by_telegram_last_name = ?, " +
                    "in_progress_by_telegram_id = NULL, in_progress_by_name = NULL, in_progress_until = NULL " +
                    "WHERE id = ? AND response IS NULL AND reviewed_at IS NULL";

            if (responder.getSource() == ResponderSource.TELEGRAM) {
                sql += " AND in_progress_by_telegram_id = ? AND in_progress_until > ?";
            }

            try (PreparedStatement pstm = conn.prepareStatement(sql)) {
                pstm.setString(1, answer);
                pstm.setString(2, responder.getDisplayName());
                pstm.setTimestamp(3, now);
                pstm.setBoolean(4, false);
                pstm.setTimestamp(5, now);
                pstm.setString(6, responder.getSource().getDatabaseValue());
                pstm.setString(7, responder.getDisplayName());
                pstm.setString(8, responder.getMinecraftUuid() == null ? null : responder.getMinecraftUuid().toString());
                setLongOrNull(pstm, 9, answeredByTelegramId);
                pstm.setString(10, answeredByTelegramUsername);
                pstm.setString(11, answeredByTelegramFirstName);
                pstm.setString(12, answeredByTelegramLastName);
                pstm.setInt(13, ticketId);

                if (responder.getSource() == ResponderSource.TELEGRAM) {
                    pstm.setLong(14, responder.getTelegramId());
                    pstm.setTimestamp(15, now);
                }

                int updated = pstm.executeUpdate();
                if (updated <= 0) {
                    conn.rollback();
                    Optional<TicketRow> fresh = findTicketById(conn, ticketId);
                    if (!fresh.isPresent()) return new AnswerTicketResult(AnswerTicketResult.Status.NOT_FOUND, null);
                    if (fresh.get().isReviewed()) return new AnswerTicketResult(AnswerTicketResult.Status.ALREADY_REVIEWED, fresh.get());
                    if (fresh.get().isAnswered()) return new AnswerTicketResult(AnswerTicketResult.Status.ALREADY_CLOSED, fresh.get());
                    return new AnswerTicketResult(AnswerTicketResult.Status.NOT_LOCKED_BY_USER, fresh.get());
                }
            }

            conn.commit();
            return new AnswerTicketResult(AnswerTicketResult.Status.SUCCESS, findTicketById(conn, ticketId).orElse(null));
        }
        catch (Exception e) {
            log(Level.SEVERE, "Erro ao responder ticket de forma atômica", e);
            return new AnswerTicketResult(AnswerTicketResult.Status.ERROR, null);
        }
    }

    public ReviewTicketResult reviewTicketAtomically(int ticketId, ReviewRating rating, TelegramUserProfile reviewer,
                                                     String reviewerDisplayName, String comment) {
        Timestamp now = now();

        try (Connection conn = ConnectionFactory.createConnection()) {
            conn.setAutoCommit(false);

            Optional<TicketRow> current = findTicketById(conn, ticketId);
            if (!current.isPresent()) {
                conn.rollback();
                return new ReviewTicketResult(ReviewTicketResult.Status.NOT_FOUND, null);
            }
            if (!current.get().isAnswered()) {
                conn.rollback();
                return new ReviewTicketResult(ReviewTicketResult.Status.NOT_CLOSED, current.get());
            }
            if (current.get().isReviewed()) {
                conn.rollback();
                return new ReviewTicketResult(ReviewTicketResult.Status.ALREADY_REVIEWED, current.get());
            }

            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE ticket SET status = 'reviewed', reviewed_at = ? " +
                            "WHERE id = ? AND response IS NOT NULL AND reviewed_at IS NULL " +
                            "AND NOT EXISTS (SELECT 1 FROM ticket_reviews WHERE ticket_id = ?)")) {
                update.setTimestamp(1, now);
                update.setInt(2, ticketId);
                update.setInt(3, ticketId);

                int updated = update.executeUpdate();
                if (updated <= 0) {
                    conn.rollback();
                    Optional<TicketRow> fresh = findTicketById(conn, ticketId);
                    if (!fresh.isPresent()) return new ReviewTicketResult(ReviewTicketResult.Status.NOT_FOUND, null);
                    if (fresh.get().isReviewed()) return new ReviewTicketResult(ReviewTicketResult.Status.ALREADY_REVIEWED, fresh.get());
                    return new ReviewTicketResult(ReviewTicketResult.Status.NOT_CLOSED, fresh.get());
                }
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO ticket_reviews (ticket_id, rating, reviewed_by_telegram_id, reviewed_by_name, " +
                            "reviewed_by_telegram_username, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                insert.setInt(1, ticketId);
                insert.setString(2, rating.getDatabaseValue());
                insert.setLong(3, reviewer.getTelegramId());
                insert.setString(4, reviewerDisplayName);
                insert.setString(5, reviewer.getUsername());
                insert.setString(6, comment);
                insert.setTimestamp(7, now);
                insert.executeUpdate();
            }

            conn.commit();
            return new ReviewTicketResult(ReviewTicketResult.Status.SUCCESS, findTicketById(conn, ticketId).orElse(null));
        }
        catch (Exception e) {
            log(Level.SEVERE, "Erro ao revisar ticket de forma atômica", e);
            return new ReviewTicketResult(ReviewTicketResult.Status.ERROR, null);
        }
    }

    public void markTicketSentToPlayer(int ticketId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("UPDATE ticket SET send = ? WHERE id = ?")) {
            pstm.setBoolean(1, true);
            pstm.setInt(2, ticketId);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao marcar resposta enviada ao jogador", e);
        }
    }

    public Optional<TelegramMessageRecord> findTelegramMessage(int ticketId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("SELECT * FROM telegram_messages WHERE ticket_id = ?")) {
            pstm.setInt(1, ticketId);

            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapTelegramMessage(rs));
            }
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao buscar mensagem Telegram do ticket", e);
            return Optional.empty();
        }
    }

    public void saveNewTicketMessage(int ticketId, long chatId, Integer threadId, int messageId) {
        String sql = "INSERT INTO telegram_messages (ticket_id, new_ticket_chat_id, new_ticket_thread_id, new_ticket_message_id, updated_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(ticket_id) DO UPDATE SET new_ticket_chat_id = excluded.new_ticket_chat_id, " +
                "new_ticket_thread_id = excluded.new_ticket_thread_id, new_ticket_message_id = excluded.new_ticket_message_id, " +
                "updated_at = CURRENT_TIMESTAMP";

        executeMessageUpsert(sql, ticketId, chatId, threadId, messageId);
    }

    public void saveClosedTicketMessage(int ticketId, long chatId, Integer threadId, int messageId) {
        String sql = "INSERT INTO telegram_messages (ticket_id, closed_ticket_chat_id, closed_ticket_thread_id, closed_ticket_message_id, updated_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(ticket_id) DO UPDATE SET closed_ticket_chat_id = excluded.closed_ticket_chat_id, " +
                "closed_ticket_thread_id = excluded.closed_ticket_thread_id, closed_ticket_message_id = excluded.closed_ticket_message_id, " +
                "updated_at = CURRENT_TIMESTAMP";

        executeMessageUpsert(sql, ticketId, chatId, threadId, messageId);
    }

    public void savePromptMessage(int ticketId, Integer messageId) {
        String sql = "INSERT INTO telegram_messages (ticket_id, prompt_message_id, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(ticket_id) DO UPDATE SET prompt_message_id = excluded.prompt_message_id, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, ticketId);
            if (messageId == null) pstm.setNull(2, java.sql.Types.INTEGER);
            else pstm.setInt(2, messageId);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao salvar mensagem de prompt Telegram", e);
        }
    }

    public void clearPromptMessage(int ticketId) {
        savePromptMessage(ticketId, null);
    }

    private void executeMessageUpsert(String sql, int ticketId, long chatId, Integer threadId, int messageId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setInt(1, ticketId);
            pstm.setLong(2, chatId);
            if (threadId == null) pstm.setNull(3, java.sql.Types.INTEGER);
            else pstm.setInt(3, threadId);
            pstm.setInt(4, messageId);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao salvar mensagem Telegram do ticket", e);
        }
    }

    public TelegramUserProfile upsertTelegramUser(long telegramId, String username, String firstName, String lastName) {
        String sql = "INSERT INTO telegram_users (telegram_id, username, first_name, last_name, created_at, updated_at, last_seen_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(telegram_id) DO UPDATE SET username = excluded.username, first_name = excluded.first_name, " +
                "last_name = excluded.last_name, updated_at = CURRENT_TIMESTAMP, last_seen_at = CURRENT_TIMESTAMP";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, telegramId);
            pstm.setString(2, username);
            pstm.setString(3, firstName);
            pstm.setString(4, lastName);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao atualizar usuário Telegram", e);
        }

        return findTelegramUser(telegramId).orElseGet(() -> {
            TelegramUserProfile profile = new TelegramUserProfile();
            profile.setTelegramId(telegramId);
            profile.setUsername(username);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            return profile;
        });
    }

    public Optional<TelegramUserProfile> findTelegramUser(long telegramId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("SELECT * FROM telegram_users WHERE telegram_id = ?")) {
            pstm.setLong(1, telegramId);

            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapTelegramUser(rs));
            }
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao buscar usuário Telegram", e);
            return Optional.empty();
        }
    }

    public void setTelegramUserRole(long telegramId, TelegramRole role) {
        String sql = "INSERT INTO telegram_users (telegram_id, role, created_at, updated_at, last_seen_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(telegram_id) DO UPDATE SET role = excluded.role, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, telegramId);
            pstm.setString(2, role.getDatabaseValue());
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao definir cargo Telegram", e);
        }
    }

    public void removeTelegramUserRole(long telegramId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("UPDATE telegram_users SET role = NULL, updated_at = CURRENT_TIMESTAMP WHERE telegram_id = ?")) {
            pstm.setLong(1, telegramId);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao remover cargo Telegram", e);
        }
    }

    public void setTelegramUserNickname(long telegramId, String nickname) {
        String sql = "INSERT INTO telegram_users (telegram_id, nickname, created_at, updated_at, last_seen_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(telegram_id) DO UPDATE SET nickname = excluded.nickname, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setLong(1, telegramId);
            pstm.setString(2, nickname);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao definir nickname Telegram", e);
        }
    }

    public void deleteTelegramUserNickname(long telegramId) {
        try (Connection conn = ConnectionFactory.createConnection();
             PreparedStatement pstm = conn.prepareStatement("UPDATE telegram_users SET nickname = NULL, updated_at = CURRENT_TIMESTAMP WHERE telegram_id = ?")) {
            pstm.setLong(1, telegramId);
            pstm.executeUpdate();
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao remover nickname Telegram", e);
        }
    }

    public List<TelegramUserProfile> listTelegramUsersWithRoles() {
        List<TelegramUserProfile> users = new ArrayList<>();

        try (Connection conn = ConnectionFactory.createConnection();
             Statement stm = conn.createStatement();
             ResultSet rs = stm.executeQuery("SELECT * FROM telegram_users WHERE role IS NOT NULL AND role <> '' ORDER BY role, nickname, username, first_name")) {
            while (rs.next()) users.add(mapTelegramUser(rs));
        }
        catch (Exception e) {
            log(Level.WARNING, "Erro ao listar administradores Telegram", e);
        }

        return users;
    }

    private Optional<TicketRow> findTicketById(Connection conn, int ticketId) throws SQLException {
        try (PreparedStatement pstm = conn.prepareStatement(ticketSelectSql() + " WHERE t.id = ?")) {
            pstm.setInt(1, ticketId);
            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapTicket(rs));
            }
        }
    }

    private Optional<TelegramUserProfile> findTelegramUserByDisplayName(Connection conn, String displayName) throws SQLException {
        if (displayName == null || displayName.trim().isEmpty()) return Optional.empty();

        String sql = "SELECT * FROM telegram_users " +
                "WHERE LOWER(nickname) = LOWER(?) OR LOWER(username) = LOWER(?) " +
                "ORDER BY CASE WHEN LOWER(nickname) = LOWER(?) THEN 0 ELSE 1 END LIMIT 1";

        try (PreparedStatement pstm = conn.prepareStatement(sql)) {
            pstm.setString(1, displayName.trim());
            pstm.setString(2, displayName.trim());
            pstm.setString(3, displayName.trim());

            try (ResultSet rs = pstm.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapTelegramUser(rs));
            }
        }
    }

    private boolean isLockedByResponder(TicketRow ticket, ResponderInfo responder, Timestamp now) {
        if (ticket.getInProgressByTelegramId() == null || responder.getTelegramId() == null) return false;
        if (!ticket.getInProgressByTelegramId().equals(responder.getTelegramId())) return false;
        return ticket.getInProgressUntil() != null && ticket.getInProgressUntil().after(now);
    }

    private String ticketSelectSql() {
        return "SELECT t.*, tr.rating AS review_rating, tr.comment AS review_comment, " +
                "tr.reviewed_by_telegram_id AS review_by_telegram_id, tr.reviewed_by_name AS review_by_name, " +
                "tr.reviewed_by_telegram_username AS review_by_telegram_username " +
                "FROM ticket t LEFT JOIN ticket_reviews tr ON tr.ticket_id = t.id";
    }

    private TicketRow mapTicket(ResultSet rs) throws SQLException {
        TicketRow ticket = new TicketRow();
        ticket.setId(rs.getInt("id"));
        ticket.setPlayer(rs.getString("player"));
        ticket.setRequest(rs.getString("request"));
        ticket.setResponse(rs.getString("response"));
        ticket.setRespondedBy(rs.getString("respondedBy"));
        ticket.setRespondedAt(rs.getTimestamp("respondedAt"));
        ticket.setRating(getDoubleObject(rs, "rating"));
        ticket.setSend(rs.getBoolean("send"));
        ticket.setCreatedAt(rs.getTimestamp("timestamp"));
        ticket.setStatus(rs.getString("status"));
        ticket.setAnsweredByType(rs.getString("answered_by_type"));
        ticket.setAnsweredByName(rs.getString("answered_by_name"));
        ticket.setAnsweredByMinecraftUuid(rs.getString("answered_by_minecraft_uuid"));
        ticket.setAnsweredByTelegramId(getLongObject(rs, "answered_by_telegram_id"));
        ticket.setAnsweredByTelegramUsername(rs.getString("answered_by_telegram_username"));
        ticket.setAnsweredByTelegramFirstName(rs.getString("answered_by_telegram_first_name"));
        ticket.setAnsweredByTelegramLastName(rs.getString("answered_by_telegram_last_name"));
        ticket.setInProgressByTelegramId(getLongObject(rs, "in_progress_by_telegram_id"));
        ticket.setInProgressByName(rs.getString("in_progress_by_name"));
        ticket.setInProgressUntil(rs.getTimestamp("in_progress_until"));
        ticket.setClosedAt(rs.getTimestamp("closed_at"));
        ticket.setReviewedAt(rs.getTimestamp("reviewed_at"));
        ticket.setReviewRating(reviewRatingFromDatabase(rs.getString("review_rating")));
        ticket.setReviewComment(rs.getString("review_comment"));
        ticket.setReviewedByTelegramId(getLongObject(rs, "review_by_telegram_id"));
        ticket.setReviewedByName(rs.getString("review_by_name"));
        ticket.setReviewedByTelegramUsername(rs.getString("review_by_telegram_username"));
        return ticket;
    }

    private TelegramMessageRecord mapTelegramMessage(ResultSet rs) throws SQLException {
        TelegramMessageRecord record = new TelegramMessageRecord();
        record.setTicketId(rs.getInt("ticket_id"));
        record.setNewTicketChatId(getLongObject(rs, "new_ticket_chat_id"));
        record.setNewTicketThreadId(getIntegerObject(rs, "new_ticket_thread_id"));
        record.setNewTicketMessageId(getIntegerObject(rs, "new_ticket_message_id"));
        record.setClosedTicketChatId(getLongObject(rs, "closed_ticket_chat_id"));
        record.setClosedTicketThreadId(getIntegerObject(rs, "closed_ticket_thread_id"));
        record.setClosedTicketMessageId(getIntegerObject(rs, "closed_ticket_message_id"));
        record.setPromptMessageId(getIntegerObject(rs, "prompt_message_id"));
        return record;
    }

    private TelegramUserProfile mapTelegramUser(ResultSet rs) throws SQLException {
        TelegramUserProfile profile = new TelegramUserProfile();
        profile.setTelegramId(rs.getLong("telegram_id"));
        profile.setUsername(rs.getString("username"));
        profile.setFirstName(rs.getString("first_name"));
        profile.setLastName(rs.getString("last_name"));
        profile.setNickname(rs.getString("nickname"));
        profile.setRole(TelegramRole.fromDatabaseValue(rs.getString("role")));
        profile.setCreatedAt(rs.getTimestamp("created_at"));
        profile.setUpdatedAt(rs.getTimestamp("updated_at"));
        profile.setLastSeenAt(rs.getTimestamp("last_seen_at"));
        return profile;
    }

    private ReviewRating reviewRatingFromDatabase(String value) {
        if (value == null) return null;
        for (ReviewRating rating : ReviewRating.values()) {
            if (rating.getDatabaseValue().equalsIgnoreCase(value)) return rating;
        }
        return null;
    }

    private Long getLongObject(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerObject(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Double getDoubleObject(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private void setLongOrNull(PreparedStatement pstm, int index, Long value) throws SQLException {
        if (value == null) pstm.setNull(index, java.sql.Types.BIGINT);
        else pstm.setLong(index, value);
    }

    private Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private void log(Level level, String message, Throwable throwable) {
        TicketSystem.getMain().getLogger().log(level, message, throwable);
    }
}
