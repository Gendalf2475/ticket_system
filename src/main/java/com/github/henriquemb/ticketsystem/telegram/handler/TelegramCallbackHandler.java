package com.github.henriquemb.ticketsystem.telegram.handler;

import com.github.henriquemb.ticketsystem.telegram.database.TelegramRepository;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewRating;
import com.github.henriquemb.ticketsystem.telegram.model.ReviewTicketResult;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramUserProfile;
import com.github.henriquemb.ticketsystem.telegram.model.TicketLockResult;
import com.github.henriquemb.ticketsystem.telegram.model.TicketRow;
import com.github.henriquemb.ticketsystem.telegram.model.UserState;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramApiService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramFormatService;
import com.github.henriquemb.ticketsystem.telegram.service.TelegramUserService;
import com.github.henriquemb.ticketsystem.telegram.service.TicketReviewService;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Map;
import java.util.Optional;

public class TelegramCallbackHandler {
    private final TelegramApiService apiService;
    private final TelegramRepository repository;
    private final TelegramUserService userService;
    private final TelegramFormatService formatService;
    private final TicketReviewService reviewService;
    private final TelegramSettings settings;
    private final Map<Long, UserState> userStates;

    public TelegramCallbackHandler(TelegramApiService apiService, TelegramRepository repository,
                                   TelegramUserService userService, TelegramFormatService formatService,
                                   TicketReviewService reviewService, TelegramSettings settings,
                                   Map<Long, UserState> userStates) {
        this.apiService = apiService;
        this.repository = repository;
        this.userService = userService;
        this.formatService = formatService;
        this.reviewService = reviewService;
        this.settings = settings;
        this.userStates = userStates;
    }

    public void handle(CallbackQuery callbackQuery, TelegramUserProfile user) {
        String data = callbackQuery.getData();
        if (data == null || !data.contains(":")) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Неизвестное действие.", true);
            return;
        }

        String[] parts = data.split(":", 2);
        Integer ticketId = parseTicketId(parts[1]);
        if (ticketId == null) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Некорректный ID тикета.", true);
            return;
        }

        if ("answer_ticket".equals(parts[0])) {
            handleAnswerTicket(callbackQuery, user, ticketId);
            return;
        }

        ReviewRating rating = ReviewRating.fromCallback(parts[0]);
        if (rating != null) {
            handleReview(callbackQuery, user, ticketId, rating);
            return;
        }

        apiService.answerCallbackQuery(callbackQuery.getId(), "Неизвестное действие.", true);
    }

    private void handleAnswerTicket(CallbackQuery callbackQuery, TelegramUserProfile user, int ticketId) {
        if (!userService.canAnswerTickets(user)) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "❌ У вас нет доступа к этому действию.", true);
            return;
        }

        TicketLockResult lockResult = repository.acquireTicketForTelegramAnswer(
                ticketId,
                user,
                formatService.plainTelegramDisplayName(user),
                settings.getInProgressTimeoutSeconds()
        );

        switch (lockResult.getStatus()) {
            case ACQUIRED:
                sendAnswerPrompt(callbackQuery, user, ticketId);
                apiService.answerCallbackQuery(callbackQuery.getId(), "Введите ответ на тикет.", false);
                break;
            case NOT_FOUND:
                apiService.answerCallbackQuery(callbackQuery.getId(), "Тикет не найден.", true);
                break;
            case ALREADY_CLOSED:
            case ALREADY_REVIEWED:
                apiService.answerCallbackQuery(callbackQuery.getId(), "Этот тикет уже закрыт.", true);
                break;
            case LOCKED_BY_OTHER:
                apiService.answerCallbackQuery(callbackQuery.getId(), "⚠️ Этот тикет уже отвечает другой модератор.", true);
                break;
            default:
                apiService.answerCallbackQuery(callbackQuery.getId(), "Ошибка обработки тикета.", true);
        }
    }

    private void sendAnswerPrompt(CallbackQuery callbackQuery, TelegramUserProfile user, int ticketId) {
        Message callbackMessage = accessibleMessage(callbackQuery);
        long chatId = callbackMessage == null ? settings.getNewTicketsTopic().getChatId() : callbackMessage.getChatId();
        Integer threadId = callbackMessage == null ? settings.getNewTicketsTopic().getThreadId() : callbackMessage.getMessageThreadId();

        Message prompt = apiService.sendMessage(
                chatId,
                threadId,
                "✍️ Введите ответ на тикет #" + ticketId + ".\nМаксимум " + settings.getAnswerMaxLength() + " символов.",
                null
        );

        Integer promptMessageId = prompt == null ? null : prompt.getMessageId();
        userStates.put(user.getTelegramId(), new UserState(
                UserState.Type.WAITING_FOR_TICKET_ANSWER,
                ticketId,
                chatId,
                threadId,
                promptMessageId
        ));
        repository.savePromptMessage(ticketId, promptMessageId);
    }

    private void handleReview(CallbackQuery callbackQuery, TelegramUserProfile user, int ticketId, ReviewRating rating) {
        if (!userService.canReviewTickets(user)) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "❌ У вас нет доступа к этому действию.", true);
            return;
        }

        Optional<TicketRow> ticket = repository.findTicketById(ticketId);
        if (!ticket.isPresent()) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Тикет не найден.", true);
            return;
        }
        if (!ticket.get().isAnswered()) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Тикет ещё не закрыт.", true);
            return;
        }
        if (ticket.get().isReviewed()) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "⚠️ Этот тикет уже проверен.", true);
            return;
        }

        if (rating == ReviewRating.BAD) {
            sendBadReviewPrompt(callbackQuery, user, ticketId);
            apiService.answerCallbackQuery(callbackQuery.getId(), "Введите комментарий к оценке.", false);
            return;
        }

        ReviewTicketResult result = reviewService.reviewTicket(ticketId, rating, user, null);
        if (result.isSuccess()) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Тикет проверен.", false);
        }
        else if (result.getStatus() == ReviewTicketResult.Status.ALREADY_REVIEWED) {
            apiService.answerCallbackQuery(callbackQuery.getId(), "⚠️ Этот тикет уже проверен.", true);
        }
        else {
            apiService.answerCallbackQuery(callbackQuery.getId(), "Не удалось проверить тикет.", true);
        }
    }

    private void sendBadReviewPrompt(CallbackQuery callbackQuery, TelegramUserProfile user, int ticketId) {
        Message callbackMessage = accessibleMessage(callbackQuery);
        long chatId = callbackMessage == null ? settings.getClosedTicketsTopic().getChatId() : callbackMessage.getChatId();
        Integer threadId = callbackMessage == null ? settings.getClosedTicketsTopic().getThreadId() : callbackMessage.getMessageThreadId();

        Message prompt = apiService.sendMessage(
                chatId,
                threadId,
                "📝 Укажите, что именно плохо в ответе на тикет #" + ticketId + ".",
                null
        );

        Integer promptMessageId = prompt == null ? null : prompt.getMessageId();
        userStates.put(user.getTelegramId(), new UserState(
                UserState.Type.WAITING_FOR_BAD_REVIEW_COMMENT,
                ticketId,
                chatId,
                threadId,
                promptMessageId
        ));
        repository.savePromptMessage(ticketId, promptMessageId);
    }

    private Integer parseTicketId(String value) {
        try {
            int ticketId = Integer.parseInt(value);
            return ticketId > 0 ? ticketId : null;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private Message accessibleMessage(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage maybeMessage = callbackQuery.getMessage();
        if (maybeMessage instanceof Message) return (Message) maybeMessage;
        return null;
    }
}
