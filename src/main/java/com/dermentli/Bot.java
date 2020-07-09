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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                // register new user
                registeredUser(chatID);
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
                case "likes":
                    language = callbackData[3].toLowerCase();
                    subject = callbackData[4];
                    questionID = Integer.parseInt(callbackData[1]);
                    ratingAnalyzer(language, subject, questionID, chatID, true);
                    break;
                case "muscle":
                    language = callbackData[3].toLowerCase();
                    subject = callbackData[4];
                    questionID = Integer.parseInt(callbackData[1]);
                    ratingAnalyzer(language, subject, questionID, chatID, false);
                    break;
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
                questions.sort(Comparator.comparingInt(Question::getMuscle).reversed());
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

    private void registeredUser(long chatID) throws IOException {
        File file = new File(USERS);
        ObjectMapper objectMapper = new ObjectMapper();
        List<User> usersList = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        String json = new String(Files.readAllBytes(Paths.get(USERS)), StandardCharsets.UTF_8);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        String jsonPath = "$.[?(@.idUser == '" + chatID + "')].idUser";
        Object test = JsonPath.read(document, jsonPath);
        String value = test.toString();
        if (value.equals("[]")) {
            log.info("Registering new user");
            usersList.add(new User(chatID));
            objectMapper.writeValue(file, usersList);
        } else {
            log.info("User is already registered");
        }
    }

    private void ratingAnalyzer(String language, String subject, int questionID, long chatID, boolean isLike) throws IOException {
        log.info("ratingAnalyzer started");
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(USERS);
        List<User> usersList = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        String json = new String(Files.readAllBytes(Paths.get(USERS)), StandardCharsets.UTF_8);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        Spliterator<User> spliterator = usersList.spliterator();
        if(isLike) {
            log.info("staring rate");
            rate(chatID, language, subject, document, file, spliterator, usersList, questionID, objectMapper, "likes");
        } else {
            rate(chatID, language, subject, document, file, spliterator, usersList, questionID, objectMapper, "muscle");
        }
    }

    private void rate(long chatID, String language, String subject, Object document, File file, Spliterator<User> spliterator, List<User> usersList, int questionID, ObjectMapper objectMapper, String substitute) throws IOException {
        String jsonPath = "$.[?(@.idUser == '" + chatID + "')].ratedQuestions[?(@.language == '" + language + "' && @.subject == '" + subject + "' && @.id == '" + questionID + "')]." + substitute;
        Object test = JsonPath.read(document, jsonPath);
        String value = test.toString();
        log.info(value);
        if(value.equals("[1]")) {
            JsonPath.parse(document).set(jsonPath, 0);
            FileWriter writer = new FileWriter(file);
            writer.write(document.toString());
            writer.close();
            log.info("question " + substitute + " down");
        } else if(value.equals("[0]")) {
            JsonPath.parse(document).set(jsonPath, 1);
            FileWriter writer = new FileWriter(file);
            writer.write(document.toString());
            writer.close();
            log.info("question " + substitute + " up");
        } else if(value.equals("[]")) {
            while(spliterator.tryAdvance((n) -> {
                switch (substitute) {
                    case "like":
                        if (n.getIdUser() == chatID) n.ratedQuestions.add(new Question(questionID, language, subject, 0, 1));
                        break;
                    case "muscle":
                        if (n.getIdUser() == chatID) n.ratedQuestions.add(new Question(questionID, language, subject, 1, 0));
                        break;
                }
            }));
            objectMapper.writeValue(file, usersList);
            log.info("question " + substitute + " up + registered");
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
