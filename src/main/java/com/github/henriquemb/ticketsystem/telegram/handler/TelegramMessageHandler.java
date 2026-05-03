package com.github.henriquemb.ticketsystem.telegram.handler;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.AnswerTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.ResponderInfo;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewRating;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.model.UserState;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramApiService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramFormatService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketAnswerService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketReviewService;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Map;

public class TelegramMessageHandler {
    private final TelegramCommandHandler commandHandler;
    private final TelegramApiService apiService;
    private final TelegramRepository repository;
    private final TelegramFormatService formatService;
    private final TicketAnswerService answerService;
    private final TicketReviewService reviewService;
    private final TelegramSettings settings;
    private final Map<Long, UserState> userStates;

    public TelegramMessageHandler(TelegramCommandHandler commandHandler, TelegramApiService apiService,
                                  TelegramRepository repository, TelegramFormatService formatService,
                                  TicketAnswerService answerService, TicketReviewService reviewService,
                                  TelegramSettings settings, Map<Long, UserState> userStates) {
        this.commandHandler = commandHandler;
        this.apiService = apiService;
        this.repository = repository;
        this.formatService = formatService;
        this.answerService = answerService;
        this.reviewService = reviewService;
        this.settings = settings;
        this.userStates = userStates;
    }

    public void handle(Message message, TelegramUserProfile user) {
        if (!message.hasText()) return;

        String text = message.getText();
        if (text.trim().startsWith("/")) {
            commandHandler.handle(message, user);
            return;
        }

        UserState state = userStates.get(user.getTelegramId());
        if (state == null) return;

        if (state.getType() == UserState.Type.WAITING_FOR_TICKET_ANSWER) {
            handleTicketAnswer(message, user, state, text);
            return;
        }

        if (state.getType() == UserState.Type.WAITING_FOR_REVIEW_COMMENT) {
            handleReviewComment(message, user, state, text);
        }
    }

    private void handleTicketAnswer(Message message, TelegramUserProfile user, UserState state, String answer) {
        if (answer.trim().isEmpty()) {
            deleteUserInput(message);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "⚠️ Ответ не может быть пустым.", null);
            return;
        }

        if (answer.codePointCount(0, answer.length()) > settings.getAnswerMaxLength()) {
            deleteUserInput(message);
            apiService.sendMessage(
                    message.getChatId(),
                    message.getMessageThreadId(),
                    "⚠️ Ответ слишком длинный. Максимум " + settings.getAnswerMaxLength() + " символов. Сократите ответ и отправьте снова.",
                    null
            );
            return;
        }

        AnswerTicketResult result = answerService.answerTicket(
                state.getTicketId(),
                answer,
                ResponderInfo.telegram(user, formatService.plainTelegramDisplayName(user))
        );

        if (result.isSuccess()) {
            userStates.remove(user.getTelegramId());
            deleteUserInput(message);
            cleanupPrompt(state);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "✅ Ответ сохранён, тикет закрыт.", null);
            return;
        }

        if (result.getStatus() == AnswerTicketResult.Status.ALREADY_CLOSED ||
                result.getStatus() == AnswerTicketResult.Status.ALREADY_REVIEWED) {
            userStates.remove(user.getTelegramId());
            cleanupPrompt(state);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "⚠️ Этот тикет уже закрыт.", null);
            return;
        }

        if (result.getStatus() == AnswerTicketResult.Status.NOT_LOCKED_BY_USER ||
                result.getStatus() == AnswerTicketResult.Status.LOCKED_BY_OTHER) {
            userStates.remove(user.getTelegramId());
            cleanupPrompt(state);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "⚠️ Тикет больше не закреплён за вами. Нажмите кнопку ответа ещё раз.", null);
            return;
        }

        apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "Не удалось сохранить ответ. Попробуйте позже.", null);
    }

    private void handleReviewComment(Message message, TelegramUserProfile user, UserState state, String comment) {
        if (comment.trim().isEmpty()) {
            deleteUserInput(message);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "⚠️ Комментарий не может быть пустым.", null);
            return;
        }

        ReviewRating rating = state.getReviewRating();
        if (rating == null) {
            userStates.remove(user.getTelegramId());
            cleanupPrompt(state);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "Не удалось определить оценку. Нажмите кнопку оценки ещё раз.", null);
            return;
        }

        ReviewTicketResult result = reviewService.reviewTicket(state.getTicketId(), rating, user, comment.trim());
        if (result.isSuccess()) {
            userStates.remove(user.getTelegramId());
            deleteUserInput(message);
            cleanupPrompt(state);
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "✅ Оценка сохранена.", null);
            return;
        }

        userStates.remove(user.getTelegramId());
        cleanupPrompt(state);
        if (result.getStatus() == ReviewTicketResult.Status.ALREADY_REVIEWED) {
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "⚠️ Этот тикет уже проверен.", null);
        }
        else {
            apiService.sendMessage(message.getChatId(), message.getMessageThreadId(), "Не удалось сохранить оценку.", null);
        }
    }

    private void deleteUserInput(Message message) {
        if (!settings.isDeleteUserInput()) return;
        apiService.deleteMessage(message.getChatId(), message.getMessageId());
    }

    private void cleanupPrompt(UserState state) {
        repository.clearPromptMessage(state.getTicketId());
        if (!settings.isDeleteBotPrompts() || state.getPromptMessageId() == null) return;
        apiService.deleteMessage(state.getChatId(), state.getPromptMessageId());
    }
}
