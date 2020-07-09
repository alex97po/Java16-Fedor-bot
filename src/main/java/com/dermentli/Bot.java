package com.dermentli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
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
    String sortingWay = "default";

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
                    language = callbackData[1];
                    file = String.format(TOPICS, language);
                    getMenu(MAIN_MENU_MESSAGE, file, chatID);
                    break;
                case "back":
                    language = callbackData[3];
                    file = String.format(TOPICS, language);
                    getMenu(MAIN_MENU_MESSAGE, file, chatID);
                    break;
                // Show question
                case "topic":
                    language = callbackData[1];
                    subject = callbackData[2];
                    file = String.format(QUESTIONS, language, subject);
                    getQuestion(file, 1,sortingWay, chatID);
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
                case "default":
                    getMessage(DEFAULT, null, chatID);
                    break;
                case "toplikes":
                    sortingWay = "likes";
                    getMessage(LIKES, null, chatID);
                    break;
                case "topmuscle":
                    sortingWay = "muscles";
                    getMessage(MUSCLE, null, chatID);
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

    /**
     * This is general method to display message to bot
     * @param text text to display in bot window
     * @param buttons array of inline buttons to show
     * @param chatID current user chat identifer
     */
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

    /**
     * This method is used to generate questions
     * @param file file with questions/answers
     * @param questionOrderNumber current question, used while iterating through next button, different from id
     * @param sorting method of sorting questions
     * @param chatID current user chat identifer
     * @throws IOException possible exception
     */
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

    /**
     * This method is used to generate answer
     * @param file file with questions/answers
     * @param questionID the id of question to find answer for
     * @param questionOrderNumber current question, used while iterating through next button, different from id
     * @param sorting method of sorting questions
     * @param chatID current user chat identifer
     * @throws IOException possible exception
     */
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

    /**
     * This method generates the TEXT content of question/answer
     * @param questions list of questions
     * @param questionOrderNumber current question, used while iterating through next button, different from id
     * @param objectMapper used to parse json
     * @param questionID the id of the question
     * @param sorting method of sorting questions
     * @param text text to display
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
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

    /**
     * Generated inline keyboards
     * @param text text to display
     * @param sButtons file with buttons
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
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

    /**
     * Method to register new users
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
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

    /**
     * Checks if user wants to like or muscle question
     * @param language language
     * @param subject  topic
     * @param questionID the id of the question
     * @param chatID current user chat identifier
     * @param isLike boolean for like or muscle
     * @throws IOException possible exception
     */
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
            rateProcessing(chatID, language, subject, document, file, spliterator, usersList, questionID, objectMapper, "likes");
        } else {
            rateProcessing(chatID, language, subject, document, file, spliterator, usersList, questionID, objectMapper, "muscle");
        }
    }

    /**
     * Main rate analyzer, FOR USER SECTION
     * @param chatID current user chat identifier
     * @param language language
     * @param subject topic
     * @param document the parsed json
     * @param file file with users
     * @param spliterator spliterator to iterate through List<User>
     * @param usersList list of users from ratingAnalyzer
     * @param questionID the id of the question
     * @param objectMapper used to parse json
     * @param substitute is like or muscle
     * @throws IOException possible exception
     */
    private void rateProcessing(long chatID, String language, String subject, Object document, File file, Spliterator<User> spliterator, List<User> usersList, int questionID, ObjectMapper objectMapper, String substitute) throws IOException {
        String jsonPath = "$.[?(@.idUser == '" + chatID + "')].ratedQuestions[?(@.language == '" + language + "' && @.subject == '" + subject + "' && @.id == '" + questionID + "')]." + substitute;
        Object test = JsonPath.read(document, jsonPath);
        String value = test.toString();
        log.info(value);
        if(value.equals("[1]")) {
            JsonPath.parse(document).set(jsonPath, 0);
            FileWriter writer = new FileWriter(file);
            writer.write(document.toString());
            writer.close();
            switch (substitute) {
                case "likes":
                    try {
                        rate(false, language, subject, questionID, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "muscle":
                    try {
                        rate(false, language, subject, questionID, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            log.info("question " + substitute + " down");
        } else if(value.equals("[0]")) {
            JsonPath.parse(document).set(jsonPath, 1);
            FileWriter writer = new FileWriter(file);
            writer.write(document.toString());
            writer.close();
            switch (substitute) {
                case "likes":
                    try {
                        rate(true, language, subject, questionID, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "muscle":
                    try {
                        rate(true, language, subject, questionID, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            log.info("question " + substitute + " up");
        } else if(value.equals("[]")) {
            while(spliterator.tryAdvance((n) -> {
                switch (substitute) {
                    case "likes":
                        if (n.getIdUser() == chatID) n.ratedQuestions.add(new Question(questionID, language, subject, 0, 1));
                        try {
                            rate(true, language, subject, questionID, true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "muscle":
                        if (n.getIdUser() == chatID) n.ratedQuestions.add(new Question(questionID, language, subject, 1, 0));
                        try {
                            rate(true, language, subject, questionID, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }));
            objectMapper.writeValue(file, usersList);
            log.info("question " + substitute + " up + registered");
        }
    }

    /**
     * Main rate analyzer, FOR QUESTION SECTION
     * @param rateUp rate up or down
     * @param language language
     * @param subject topic
     * @param questionID the id of the question
     * @param isLike like or muscle
     * @throws IOException possible exception
     */
    private void rate(boolean rateUp, String language, String subject, int questionID, boolean isLike) throws IOException {
        log.info("rate started");
        String source = String.format(QUESTIONS, language, subject);
        log.info(source);
        File file = new File(source);
        String jsonPath;
        String json = new String(Files.readAllBytes(Paths.get(source)), StandardCharsets.UTF_8);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        if(isLike) {
            jsonPath = "$.[?(@.id == '" + questionID + "')].likes";
        } else {
            jsonPath = "$.[?(@.id == '" + questionID + "')].muscle";
        }
        JSONArray oCurrentValueLikes = JsonPath.read(document, jsonPath);
        int currentValue = (Integer) oCurrentValueLikes.get(0);
        log.info(String.valueOf(currentValue));
        if(rateUp) {
            JsonPath.parse(document).set(jsonPath, currentValue + 1);
        } else {
            JsonPath.parse(document).set(jsonPath, currentValue - 1);
        }
        FileWriter writer = new FileWriter(file);
        writer.write(document.toString());
        writer.close();
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
