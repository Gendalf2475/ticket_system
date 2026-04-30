package com.github.henriquemb.ticketsystem.telegram.service;

import com.github.henriquemb.ticketsystem.TicketSystem;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramSettings;
import com.github.henriquemb.ticketsystem.telegram.model.TelegramTopic;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.logging.Level;

public class TelegramApiService {
    private final TelegramClient telegramClient;
    private final TelegramSettings settings;

    public TelegramApiService(String botToken, TelegramSettings settings) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.settings = settings;
    }

    public Message sendTopicMessage(TelegramTopic topic, String text, InlineKeyboardMarkup markup) {
        if (topic == null || !topic.hasChat()) {
            TicketSystem.getMain().getLogger().warning("Telegram chat-id não configurado para o tópico");
            return null;
        }

        return sendMessage(topic.getChatId(), topic.getThreadId(), text, markup);
    }

    public Message sendMessage(long chatId, Integer threadId, String text, InlineKeyboardMarkup markup) {
        try {
            SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                    .chatId(chatId)
                    .text(text);
            if (settings.getParseMode() != null && !settings.getParseMode().trim().isEmpty()) {
                builder.parseMode(settings.getParseMode());
            }
            if (threadId != null && threadId > 0) builder.messageThreadId(threadId);
            if (markup != null) builder.replyMarkup(markup);

            return telegramClient.execute(builder.build());
        }
        catch (TelegramApiException e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao enviar mensagem para Telegram", e);
            return null;
        }
    }

    public boolean editMessageText(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        try {
            EditMessageText.EditMessageTextBuilder<?, ?> builder = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text);
            if (settings.getParseMode() != null && !settings.getParseMode().trim().isEmpty()) {
                builder.parseMode(settings.getParseMode());
            }
            if (markup != null) builder.replyMarkup(markup);

            telegramClient.execute(builder.build());
            return true;
        }
        catch (TelegramApiException e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao editar mensagem Telegram", e);
            return false;
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        try {
            DeleteMessage deleteMessage = DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build();
            telegramClient.execute(deleteMessage);
        }
        catch (TelegramApiException e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao deletar mensagem Telegram", e);
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text, boolean alert) {
        try {
            AnswerCallbackQuery.AnswerCallbackQueryBuilder<?, ?> builder = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .showAlert(alert);
            if (text != null && !text.trim().isEmpty()) builder.text(text);

            telegramClient.execute(builder.build());
        }
        catch (TelegramApiException e) {
            TicketSystem.getMain().getLogger().log(Level.WARNING, "Erro ao responder callback_query Telegram", e);
        }
    }
}
