package com.dermentli;

import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Set variables
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            System.out.println(update.getCallbackQuery().getChatInstance());
            if (message_text.equals("/start")) {
                System.out.println("start was pressed");
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите язык программирования");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Set each button, you can also use KeyboardButton objects if you need something else than text
                row.add(EmojiParser.parseToUnicode(":coffee: Java2"));
                row.add(EmojiParser.parseToUnicode(":shield: HTML+CSS"));
                row.add(EmojiParser.parseToUnicode(":atom_symbol: ReactJS"));
                row.add(EmojiParser.parseToUnicode(":smiley_cat: NodeJS"));
                row.add(EmojiParser.parseToUnicode(":no_entry_sign: STOP"));
                // Add the first row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals(EmojiParser.parseToUnicode("☕ Java"))) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите тему");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Buttons
                row.add("ООП");
                row.add("Примитивы");
                row.add("Классы и методы");
                row.add("Коллекции");
                row.add("Многопоточность");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Adding seconf row of keyboard
                row = new KeyboardRow();
                // Buttons
                row.add("Random");
                row.add("Назад");
                row.add("STOP");
                row.add("ТОП любимых");
                row.add("ТОП сложных");
                // Add the second row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals(EmojiParser.parseToUnicode("🛡 HTML+CSS"))) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите тему");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Buttons
                row.add("Назад");
                row.add("STOP");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals(EmojiParser.parseToUnicode("⚛ ReactJS"))) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите тему");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Buttons
                row.add("Назад");
                row.add("STOP");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals(EmojiParser.parseToUnicode("😺 NodeJS"))) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите тему");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Buttons
                row.add("Назад");
                row.add("STOP");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals(EmojiParser.parseToUnicode("🚫 STOP"))) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("Пожалуйста выберите тему");
                // Create ReplyKeyboardMarkup object
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                // Resizing keyboard to smaller one
                keyboardMarkup.setResizeKeyboard(true);
                // Create the keyboard (list of keyboard rows)
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                // Create a keyboard row
                KeyboardRow row = new KeyboardRow();
                // Buttons
                row.add("Назад");
                row.add("STOP");
                // Add the first row to the keyboard
                keyboard.add(row);
                // Set the keyboard to the markup
                keyboardMarkup.setKeyboard(keyboard);
                // Add it to the message
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (message_text.equals("STOP")) {
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
                KeyboardRow row = new KeyboardRow();
                row.add("/start");
                keyboard.add(row);
                keyboardMarkup.setKeyboard(keyboard);
                BotSession session = ApiContext.getInstance(BotSession.class);
                session.stop();
            }

        } else if (update.hasCallbackQuery()) {

        }
    }

    @Override
    public String getBotUsername() {
        // Return bot username
        return "goit_self_check_bot";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "1363383923:AAGdsysEy7Lc_KVnaeeRSe6ffF-i6ATkLrE";
    }

}
