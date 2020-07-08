package com.dermentli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;

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
        String file;
        String sorting;
        int questionOrderNumber;
        long chatID;
        int questionID;
        String language;
        String subject;

        if (update.hasMessage()) {
            String updMessage = update.getMessage().getText();
            if (updMessage.equals("/start")) {
                chatID = update.getMessage().getChatId();
                getMenu(MAIN_MENU_MESSAGE, LANGUAGE_MENU, chatID);
            } else if (updMessage.equals("STOP")) {
                BotSession session = ApiContext.getInstance(BotSession.class);
                session.stop();
            }
        } else if (update.hasCallbackQuery()) {
            String[] callbackData = update.getCallbackQuery().getData().split("-");
            chatID = update.getCallbackQuery().getMessage().getChatId();
            switch (callbackData[0]) {
                // Show topics
                case "language":
                case "back":
                    language = callbackData[1];
                    file = String.format(TOPICS, language);
                    getMenu(MAIN_MENU_MESSAGE, file, chatID);
                    break;
                // Show question
                case "topic":
                    language = callbackData[1];
                    subject = callbackData[2];
                    file = String.format(QUESTIONS, language, subject);
                    getQuestion(file, 1,"likes", chatID);
                    break;
                case "next":
                    language = callbackData[3];
                    subject = callbackData[4];
                    file = String.format(QUESTIONS, language, subject);
                    questionOrderNumber = Integer.parseInt(callbackData[2]) + 1;
                    sorting = callbackData[5];
                    getQuestion(file, questionOrderNumber, sorting, chatID);
                    break;
                case "answer":
                    language = callbackData[3];
                    subject = callbackData[4];
                    file = String.format(QUESTIONS, language, subject);
                    questionOrderNumber = Integer.parseInt(callbackData[2]);
                    questionID = Integer.parseInt(callbackData[1]);
                    sorting = callbackData[5];
                    getAnswer(file, questionID, questionOrderNumber, sorting, chatID);
                    break;
                case "help":
                    getMessage(HELP_PAGE, null, chatID);
                    break;
                case "stop":
                    BotSession session = ApiContext.getInstance(BotSession.class);
                    session.stop();
                    break;
                case "like":
                    language = callbackData[3];
                    subject = callbackData[4];
                    file = String.format(QUESTIONS, language, subject);
                    questionID = Integer.parseInt(callbackData[1]);
                    if(isAllowedToRate(language, subject, questionID, chatID)) {
                        rate(file, questionID, true);
                    } else {
                        underRate(file, questionID, true);
                    }

                case "muscle":
                    language = callbackData[3];
                    subject = callbackData[4];
                    file = String.format(QUESTIONS, language, subject);
                    questionID = Integer.parseInt(callbackData[1]);
                    if(isAllowedToRate(language, subject, questionID, chatID)) {
                        rate(file, questionID, true);
                    } else {
                        underRate(file, questionID, true);
                    }
            }
        }
    }

    private void getMessage(String text, InlineKeyboardMarkup buttons, long chatID) {
        //creating message
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatID)
                .setText(text)
                .setReplyMarkup(buttons);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void getQuestion(String file, int questionOrderNumber, String sorting, long chatID) throws IOException {
        // creating object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        // reading list of objects from JSON array string
        List<Question> questions = objectMapper.readValue(new File(file), new TypeReference<List<Question>>(){});
        // changing sorting based on order desire
        switch (sorting) {
            case "likes":
                questions.sort(Comparator.comparingInt(Question::getLikes).reversed());
                break;
            case "muscles":
                questions.sort(Comparator.comparingInt(Question::getDifficulty).reversed());
                break;
        }

        String text = questions.get(questionOrderNumber-1).getContent();
        int questionID = questions.get(questionOrderNumber-1).getId();
        questionBlock(questions, questionOrderNumber, objectMapper, questionID, sorting, text, chatID);
    }

    private void getAnswer(String file, int questionID, int questionOrderNumber, String sorting, long chatID) throws IOException {
        // creating object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        // reading list of objects from JSON array string
        List<Question> questions = objectMapper.readValue(new File(file), new TypeReference<List<Question>>(){});
        List<Question> question = questions.stream()
                .filter(line -> line.getId() == questionID)
                .collect(Collectors.toList());
        String answer = question.get(0).getAnswer();
        questionBlock(questions, questionOrderNumber, objectMapper, questionID, sorting, answer, chatID);
    }

    private void questionBlock(List<Question> questions, int questionOrderNumber, ObjectMapper objectMapper, int questionID, String sorting, String text, long chatID) throws IOException {
        String language = questions.get(questionOrderNumber-1).getLanguage();
        String subject = questions.get(questionOrderNumber-1).getSubject();
        // reading list of question's buttons from JSON array string
        List<Button> buttons = objectMapper.readValue(new File(QUESTION_MENU), new TypeReference<List<Button>>(){});
        // creating buttons list
        List<List<InlineKeyboardButton>> buttonsInline = new ArrayList<>();
        // adding iterator for buttons
        Spliterator<Button> spliterator = buttons.spliterator();
        // adding row of buttons
        List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
        while(spliterator.tryAdvance((n) -> {
            buttonsRow.add(new InlineKeyboardButton().setText(n.getName()).setCallbackData(n.getCallback() + "-" + questionID + "-" + questionOrderNumber + "-" + language + "-" + subject + "-" + sorting));
        }));
        // adding row to button list
        buttonsInline.add(buttonsRow);
        // creating markup
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        // setting buttons list to our markup
        markupKeyboard.setKeyboard(buttonsInline);
        getMessage(text, markupKeyboard, chatID);
    }

    private void getMenu(String text, String sButtons, long chatID) throws IOException {
        // creating object mapper
        ObjectMapper objectMapper = new ObjectMapper();
        // reading list of objects from JSON array string
        List<Button> buttons = objectMapper.readValue(new File(sButtons), new TypeReference<List<Button>>(){});
        // creating buttons list
        List<List<InlineKeyboardButton>> buttonsInline = new ArrayList<>();
        // adding iterator for buttons
        Spliterator<Button> spliterator = buttons.spliterator();
        while(spliterator.tryAdvance((n) -> {
            // adding row of buttons
            List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
            buttonsRow.add(new InlineKeyboardButton().setText(n.getName()).setCallbackData(n.getCallback()));
            // adding row to button list
            buttonsInline.add(buttonsRow);
        }));
        // creating markup
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        // setting buttons list to our markup
        markupKeyboard.setKeyboard(buttonsInline);
        getMessage(text, markupKeyboard, chatID);
    }

    private boolean isAllowedToRate(String language, String subject, int questionID, long chatID) {
        String json = USERS;
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        JsonPath.
        String author0 = JsonPath.read(document, "$.store.book[0].author");
        return true;
    }

    private void rate(String file, int questionID, boolean isLike) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Question> questions = objectMapper.readValue(new File(file), new TypeReference<List<Question>>(){});
        Spliterator<Question> spliterator = questions.spliterator();
        if (isLike) {
            while(spliterator.tryAdvance((n) -> {
                if (n.getId() == questionID) n.setLikes(n.getLikes() + 1);
            }));
        } else {
            while(spliterator.tryAdvance((n) -> {
                if (n.getId() == questionID) n.setDifficulty(n.getDifficulty() + 1);
            }));
        }
        objectMapper.writeValue(new File(file), questions);
    }

    private void underRate(String file, int questionID, boolean isLike) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Question> questions = objectMapper.readValue(new File(file), new TypeReference<List<Question>>(){});
        Spliterator<Question> spliterator = questions.spliterator();
        if (isLike) {
            while(spliterator.tryAdvance((n) -> {
                if (n.getId() == questionID) n.setLikes(n.getLikes() - 1);
            }));
        } else {
            while(spliterator.tryAdvance((n) -> {
                if (n.getId() == questionID) n.setDifficulty(n.getDifficulty() - 1);
            }));
        }
        objectMapper.writeValue(new File(file), questions);
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
