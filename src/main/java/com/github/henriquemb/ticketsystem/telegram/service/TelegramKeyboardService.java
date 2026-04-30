package com.github.henriquemb.ticketsystem.telegram.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class TelegramKeyboardService {
    public InlineKeyboardMarkup answerTicketKeyboard(int ticketId) {
        return singleButton("✍️ Ответить на тикет", "answer_ticket:" + ticketId);
    }

    public InlineKeyboardMarkup reviewKeyboard(int ticketId) {
        InlineKeyboardButton excellent = button("🟢 Отлично", "review_excellent:" + ticketId);
        InlineKeyboardButton good = button("🟡 Хорошо", "review_good:" + ticketId);
        InlineKeyboardButton bad = button("🔴 Плохо", "review_bad:" + ticketId);

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(excellent, good, bad))
                .build();
    }

    private InlineKeyboardMarkup singleButton(String label, String callbackData) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button(label, callbackData)))
                .build();
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(label)
                .callbackData(callbackData)
                .build();
    }
}
