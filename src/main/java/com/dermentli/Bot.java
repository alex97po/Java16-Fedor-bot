package com.dermentli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.internal.cglib.core.$VisibilityPredicate;
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

import static com.dermentli.Constants.*;

@Slf4j
public class Bot extends TelegramLongPollingBot {

    /** Main method for events to handle
     *
     * @param update signal from bot upon any update of bot
     */
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            String updMessage = update.getMessage().getText();
            if (updMessage.equals("/start")) {
                long chatID = update.getMessage().getChatId();
                showMessage(MAIN_MENU_MESSAGE, LANGUAGE_MENU, chatID, false, 0, "menu");
            } else if (updMessage.equals("STOP")) {
                BotSession session = ApiContext.getInstance(BotSession.class);
                session.stop();
            }
        } else if (update.hasCallbackQuery()) {
            String[] callbackData = update.getCallbackQuery().getData().split("-");
            long chatID = update.getCallbackQuery().getMessage().getChatId();
            switch (callbackData[0]) {
                // Show topics
                case "language":
                    String file = String.format(TOPICS, callbackData[1]);
                    showMessage(MAIN_MENU_MESSAGE, file, chatID, false, );
                    break;
                // Show question
                case "topic":
                    file = String.format(QUESTIONS, callbackData[1], callbackData[2]);
                    getQuestion(file, chatID, "default", 1);
                    break;
            }
        }
    }

    /**
     *
     * @param sText
     * @param sButtons
     * @param chatID
     * @param singleLineMenu
     * @throws IOException
     */
    private void showMessage(String sText, String sButtons, long chatID, boolean singleLineMenu, int questionID, String type) throws IOException {
        // creating object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        // reading list of objects from JSON array string
        List<Button> buttons = objectMapper.readValue(new File(sButtons), new TypeReference<List<Button>>(){});
        // creating buttons list
        List<List<InlineKeyboardButton>> buttonsInline = new ArrayList<>();
        // adding iterator for buttons
        Spliterator<Button> spliterator = buttons.spliterator();
        // determine single line or multiline menu
        if (singleLineMenu) {
            // adding row of buttons
            List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
            while(spliterator.tryAdvance((n) -> {
                buttonsRow.add(new InlineKeyboardButton().setText(n.getName()).setCallbackData(n.getCallback()));
            }));
            // adding row to button list
            buttonsInline.add(buttonsRow);
        } else {
            while(spliterator.tryAdvance((n) -> {
                // adding row of buttons
                List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
                buttonsRow.add(new InlineKeyboardButton().setText(n.getName()).setCallbackData(n.getCallback()));
                // adding row to button list
                buttonsInline.add(buttonsRow);
            }));
        }
        // creating markup
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        // setting buttons list to our markup
        markupKeyboard.setKeyboard(buttonsInline);
        //getting text for message
        List<Question> questions = objectMapper.readValue(new File(sText), new TypeReference<List<Question>>(){});
        String text;
        switch (type) {
            case "menu":
                text = sText;
                break;
            case "question":
                text = questions.get(questionID-1).getContent();
                break;
            case "answer":
                text = questions.get(questionID-1).getAnswer();
                break;
            default:
                text = null;
                break;
        }
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

    private void getQuestion(String questionFile, long chatID, String type, int questionID) throws IOException {
        switch (type) {
            case "default":
                showMessage(questionFile, QUESTION_MENU, chatID, true, questionID, true);
                break;
            case "favorite":
                //
                break;
            case "hard":
                //
                break;
            case "random":
                //
                break;
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
