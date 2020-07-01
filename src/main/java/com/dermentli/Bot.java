package com.dermentli;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;

@Slf4j
public class Bot extends TelegramLongPollingBot {

    public static final String LANGUAGE_MENU = "src/main/resources/languages.json";
    public static final String TOPICS = "src/main/resources/%s-topics.json";
    public static final String JAVA_QUESTIONS = "src/main/resources/%s-%s-question.json";


    /** Main method for events to handle
     *
     * @param update signal from bot upon any update of bot
     */
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals("START")) {
                long chatID = update.getMessage().getChatId();
                setMenu(chatID);
                showMenuParts(LANGUAGE_MENU, "Please choose Language to continue", chatID);
            }
        } else if (update.hasMessage() && update.getMessage().getText().equals("STOP")) {
            BotSession session = ApiContext.getInstance(BotSession.class);
            session.stop();
        } else if (update.hasCallbackQuery()) {
            String[] callbackData = update.getCallbackQuery().getData().split("-");
            if (callbackData[0].equals("language")) {
                long chatID = update.getCallbackQuery().getMessage().getChatId();
                String file = String.format(TOPICS, callbackData[1]);
                showMenuParts(file, "Please choose topic", chatID);
            } else if (callbackData[0].equals("topic")) {
                long chatID = update.getCallbackQuery().getMessage().getChatId();
                String file = String.format(TOPICS, callbackData[1], callbackData[2]);

            }

        }
    }

    /** This method is designed to create bottom menu of bot and enable START/STOP buttons
     *
     * @param chatID the unique ID of chat to send messages
     */
    private void setMenu(long chatID) {
        // creating keyboard
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        // making it small
        replyKeyboardMarkup.setResizeKeyboard(true);
        // creating keybord rows
        List<KeyboardRow> keyboard = new ArrayList<>();
        // adding first row keyboard
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        // adding two buttons
        keyboardFirstRow.add("START");
        keyboardFirstRow.add("STOP");
        // adding rows to keyboard
        keyboard.add(keyboardFirstRow);
        // setting List<KeyboardRow> to our keyboard
        replyKeyboardMarkup.setKeyboard(keyboard);
        //creating message
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatID)
                .setText("Main Menu")
                .setReplyMarkup(replyKeyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** Designed to bring main interface to bot
     *
     * @param file json file with buttons and callback data
     * @param text bot message content
     * @param chatID the unique ID of chat to send messages
     * @throws IOException possible exception to be generated
     */
    private void showMenuParts(String file, String text, long chatID) throws IOException {
        // creating object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        // reading list of objects from JSON array string
        List<Button> buttons = objectMapper.readValue(new File(file), new TypeReference<List<Button>>(){});
        // creating buttons list
        List<List<InlineKeyboardButton>> buttonsInline = new ArrayList<>();
        // adding first row of buttons
        List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
        // adding buttons to row
        Spliterator<Button> spliterator = buttons.spliterator();
        while(spliterator.tryAdvance((n) -> buttonsRow.add(new InlineKeyboardButton().setText(n.getName()).setCallbackData(n.getCallback()))));
        // adding first row to button list
        buttonsInline.add(buttonsRow);
        // creating markup
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        // setting buttons list to our markup
        markupKeyboard.setKeyboard(buttonsInline);
        //creating message
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatID)
                .setText(text)
                .setReplyMarkup(markupKeyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
