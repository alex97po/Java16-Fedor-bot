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
import java.util.stream.Collectors;

import static com.dermentli.Constants.*;

@Slf4j
public class Bot extends TelegramLongPollingBot {
    private String sortingWay = "default";
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                registerUser(chatID);
                getMenu(LANGUAGE_MENU, chatID);
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
                    getMenu(file, chatID);
                    break;
                case "back":
                    language = callbackData[3];
                    file = String.format(TOPICS, language);
                    getMenu(file, chatID);
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
                    sendMessage(HELP_PAGE, null, chatID);
                    break;
                case "default":
                    sendMessage(DEFAULT, null, chatID);
                    break;
                case "toplikes":
                    sortingWay = "likes";
                    sendMessage(LIKES, null, chatID);
                    break;
                case "topmuscle":
                    sortingWay = "muscles";
                    sendMessage(MUSCLE, null, chatID);
                    break;
                case "stop":
                    BotSession session = ApiContext.getInstance(BotSession.class);
                    session.stop();
                    break;
                case "likes":
                    language = callbackData[3].toLowerCase();
                    subject = callbackData[4];
                    questionID = Integer.parseInt(callbackData[1]);
                    rateProcessing(chatID, language, subject, questionID,true);
                    break;
                case "muscle":
                    language = callbackData[3].toLowerCase();
                    subject = callbackData[4];
                    questionID = Integer.parseInt(callbackData[1]);
                    rateProcessing(chatID, language, subject, questionID, false);
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
    private void sendMessage(String text, InlineKeyboardMarkup buttons, long chatID) {
        //creating message
        SendMessage message = new SendMessage() // Create a message object object
                .setChatId(chatID)
                .setText(text)
                .setReplyMarkup(buttons);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
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
        String language = questions.get(questionOrderNumber-1).getLanguage();
        String subject = questions.get(questionOrderNumber-1).getSubject();
        int questionID = questions.get(questionOrderNumber-1).getId();
        String callbackSuffix = String.format("-%s-%s-%s-%s-%s", questionID, questionOrderNumber, language, subject, sorting);
        questionBlock(callbackSuffix, text, chatID);
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
        // reading list of objects from JSON array string
        List<Question> questions = objectMapper.readValue(new File(file), new TypeReference<List<Question>>(){});
        Question question = questions.stream()
                .filter(line -> line.getId() == questionID)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Question not found by id: " + questionID));
        String answer = question.getAnswer();
        String language = questions.get(questionOrderNumber-1).getLanguage();
        String subject = questions.get(questionOrderNumber-1).getSubject();
        String callbackSuffix = String.format("-%s-%s-%s-%s-%s", questionID, questionOrderNumber, language, subject, sorting);
        questionBlock(callbackSuffix, answer, chatID);
    }

    /**
     * This method generates the TEXT content of question/answer
     * @param callbackSuffix list of callback data necessary for buttons
     * @param text text to display
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
    private void questionBlock(String callbackSuffix, String text, long chatID) throws IOException {
        List<Button> buttons = objectMapper.readValue(new File(QUESTION_MENU), new TypeReference<List<Button>>(){});
        buttons = buttons.stream()
                .peek(button -> button.setCallback(button.getCallback() + callbackSuffix))
                .collect(Collectors.toList());
        InlineKeyboardMarkup markupKeyboard = createButtonsKeyboard(buttons);
        sendMessage(text, markupKeyboard, chatID);
    }

    private InlineKeyboardMarkup createButtonsKeyboard(List<Button> buttons) throws IOException {
        List<List<InlineKeyboardButton>> buttonsInline = new ArrayList<>();
        buttons.stream().forEach(button -> {
            List<InlineKeyboardButton> buttonsRow = new ArrayList<>();
            buttonsRow.add(new InlineKeyboardButton().setText(button.getName()).setCallbackData(button.getCallback()));
            buttonsInline.add(buttonsRow);
        });
        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttonsInline);
        return markupKeyboard;
    }

    /**
     * Generated inline keyboards
     * @param sButtons file with buttons
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
    private void getMenu(String sButtons, long chatID) throws IOException {
        List<Button> buttons = objectMapper.readValue(new File(sButtons), new TypeReference<List<Button>>(){});
        InlineKeyboardMarkup markupKeyboard = createButtonsKeyboard(buttons);
        sendMessage(MAIN_MENU_MESSAGE, markupKeyboard, chatID);
    }

    /**
     * Method to register new users
     * @param chatID current user chat identifier
     * @throws IOException possible exception
     */
    private void registerUser(long chatID) throws IOException {
        File file = new File(USERS);
        List<User> usersList = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        boolean userExist = usersList.stream().anyMatch(user -> user.getIdUser() == chatID);
        if (!userExist) {
            log.info("Registering new user");
            usersList.add(new User(chatID));
            objectMapper.writeValue(file, usersList);
        } else {
            log.info("User is already registered");
        }
    }

    /**
     * Main rate analyzer, FOR USER SECTION
     * @param chatID current user chat identifier
     * @param language language
     * @param subject topic
     * @param questionID the id of the question
     * @throws IOException possible exception
     */
    private void rateProcessing(long chatID, String language, String subject, int questionID, boolean isLike) throws IOException {
        File file = new File(USERS);
        List<User> usersList = objectMapper.readValue(file, new TypeReference<List<User>>() {});
        String json = new String(Files.readAllBytes(Paths.get(USERS)), StandardCharsets.UTF_8);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        String jsonPath = "$.[?(@.idUser == '" + chatID + "')].ratedQuestions[?(@.language == '" +
                language + "' && @.subject == '" + subject + "' && @.id == '" + questionID + "')]." +
                (isLike ? "like" : "muscle");
        Object userRateStatus = JsonPath.read(document, jsonPath);
        String value = userRateStatus.toString();
        switch (value) {
            case "[1]": {
                String source = String.format(QUESTIONS, language, subject);
                changeQuestionRate(false, file, document, jsonPath, source, questionID, isLike);
                break;
            }
            case "[0]": {
                String source = String.format(QUESTIONS, language, subject);
                changeQuestionRate(true, file, document, jsonPath, source, questionID, isLike);
                break;
            }
            case "[]":
                usersList.stream()
                        .filter(user -> user.getIdUser() == chatID)
                        .findAny()
                        .ifPresent(user -> user.getRatedQuestions().add(new Question(questionID, language, subject,
                                Math.abs(Boolean.compare(isLike, true)), Boolean.compare(isLike, false))));
                    try {
                        rate(true, String.format(QUESTIONS, language, subject), questionID, isLike);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                objectMapper.writeValue(file, usersList);
                log.info("question {} up + registered", isLike ? "like" : "muscle");
                break;
        }
    }

    private void changeQuestionRate(boolean rateUp, File file, Object document, String jsonPath,
                                    String source, int questionID, boolean isLike) {
        try {
            JsonPath.parse(document).set(jsonPath, Boolean.compare(rateUp, false));
            FileWriter writer = new FileWriter(file);
            writer.write(document.toString());
            writer.close();
            rate(rateUp, source, questionID, isLike);
            log.info("question {} up", isLike ? "like" : "muscle");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Main rate analyzer, FOR QUESTION SECTION
     * @param rateUp rate up or down
     * @param questionID the id of the question
     * @param isLike like or muscle
     * @throws IOException possible exception
     */
    private void rate(boolean rateUp, String source, int questionID, boolean isLike) throws IOException {
        log.info("rate started");
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
