package ru.project.congratulatory.bot.sevice;

import com.mysql.cj.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project.congratulatory.bot.config.BotConfig;
import ru.project.congratulatory.bot.model.User;
import ru.project.congratulatory.bot.model.UserRepo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.project.congratulatory.bot.utils.Calendar.*;
import static ru.project.congratulatory.bot.utils.Emoji.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepo userRepo;
    private final BotConfig config;
    Map<Long, String> userData = new HashMap<>();

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList();
        listOfCommands.add(new BotCommand("/start", "начать"));
        listOfCommands.add(new BotCommand("/add_new_date", "добавить дату для напоминания"));
        listOfCommands.add(new BotCommand("/qweqweqwe", "qweqweя"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list {}", e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery() && getAllMonth().contains(update.getCallbackQuery().getData())) {
            String month = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            log.info("Month {}", month);
            log.info("chatId {}", chatId);
            selectDay(chatId, month);
            return;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Месяц:")) {
            String date = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            userData.put(chatId, date);
            log.info("date {}", date);
            log.info("chatId {}", chatId);
            selectDescription(chatId);
            return;
        }
        if (update.hasMessage() && update.getMessage().getText().contains("Описание")) {
            String description = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();
            log.info("description {}", description);
            log.info("chatId {}", chatId);
            saveDateInDb(chatId, userData.get(chatId), userName, description);
            userData.remove(chatId);
            return;
        }
        if (update.hasMessage() && update.getMessage().getText().contains("описание")) {
            String description = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();
            log.info("description {}", description);
            log.info("chatId {}", chatId);
            saveDateInDb(chatId, userData.get(chatId), userName, description);
            userData.remove(chatId);
            return;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Удалить из БД")) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String message = update.getCallbackQuery().getData();

            log.info("Запрос на удаление записи, {}, chatId {}", message, chatId);
            deleteDateFromDb(chatId, message);
            return;
        }
        //TODO Блок ничего полезного не делает. Только для выбора из меню
        //TODO Подумать что отвечать на рандомный текст напысаный в бота от пользователя
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();
            String userName = update.getMessage().getChat().getUserName();
            messageExecutor(message, chatId, userName);
        } else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String userName = update.getCallbackQuery().getMessage().getChat().getUserName();
            String query = update.getCallbackQuery().getData();
            log.info("Query {}", query);
            log.info("chatId {}", chatId);
            callBackExecutor(query, chatId);
        }
    }

    public void messageExecutor(String massage, long chatId, String userName) {
        switch (massage) {
            case "/start":
                startMessageAnswer(chatId, userName);
                break;
            case "/add_new_date":
                selectMonth(chatId);
                break;
            case "/qweqweqwe":
                getAllTodayEventsFromDb();
                break;
            default:
                sendMessage(chatId, String.format("%1$sЧто-то пошло не так%1$s", EMOJI_WARNING));
        }
    }

    public void callBackExecutor(String massage, long chatId) {
        switch (massage) {
            case "добавить день рождения":
            case "добавить памятную дату":
                selectMonth(chatId);
                break;
            case "удалить дату":
                whatDateDeleteQuestion(chatId);
                break;
            case "отблагодарить":
                thankMessageAnswer(chatId);
                break;
            case "связаться":
                contactMessageAnswer(chatId);
                break;
            default:
                sendMessage(chatId, String.format("%1$sЧто-то пошло не так в кол бэк%1$s", EMOJI_WARNING));
        }
    }

    public void startMessageAnswer(long chatId, String userName) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(String.format("%s, выбери то, что тебе нужно:", userName));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        List<InlineKeyboardButton> thirdRow = new ArrayList<>();
        List<InlineKeyboardButton> forthRow = new ArrayList<>();
        List<InlineKeyboardButton> fifthRow = new ArrayList<>();

        InlineKeyboardButton addBirthdayButton = new InlineKeyboardButton();
        InlineKeyboardButton addDateButton = new InlineKeyboardButton();
        InlineKeyboardButton deleteDateButton = new InlineKeyboardButton();
        InlineKeyboardButton giftButton = new InlineKeyboardButton();
        InlineKeyboardButton telephoneButton = new InlineKeyboardButton();

        addBirthdayButton.setText(String.format("%s Добавить день рождения", EMOJI_BIRTHDAY));
        addDateButton.setText(String.format("%s Добавить памятную дату", EMOJI_STAR2));
        deleteDateButton.setText(String.format("%s Удалить напоминание", EMOJI_X));
        giftButton.setText(String.format("%s Отблагодарить разработчика ", EMOJI_CALL_ME_HAND));
        telephoneButton.setText(String.format("%s Связаться с разработчиком", EMOJI_TELEPHONE));

        addBirthdayButton.setCallbackData("добавить день рождения");
        addDateButton.setCallbackData("добавить памятную дату");
        deleteDateButton.setCallbackData("удалить дату");
        giftButton.setCallbackData("отблагодарить");
        telephoneButton.setCallbackData("связаться");

        firstRow.add(addBirthdayButton);
        secondRow.add(addDateButton);
        thirdRow.add(deleteDateButton);
        forthRow.add(giftButton);
        fifthRow.add(telephoneButton);

        rowsInLine.add(firstRow);
        rowsInLine.add(secondRow);
        rowsInLine.add(thirdRow);
        rowsInLine.add(forthRow);
        rowsInLine.add(fifthRow);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить сообщение пользователю с id:{}", chatId);
        }
    }

    public void selectMonth(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выбери месяц: ");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        List<InlineKeyboardButton> thirdRow = new ArrayList<>();

        for (int i = 0; i < getAllMonth().size(); i++) {
            if (i <= 3) {
                InlineKeyboardButton newButton = new InlineKeyboardButton();
                newButton.setText(getAllMonth().get(i));
                newButton.setCallbackData(getAllMonth().get(i));
                firstRow.add(newButton);
            }
            if (i > 3 && i <= 7) {
                InlineKeyboardButton newButton = new InlineKeyboardButton();
                newButton.setText(getAllMonth().get(i));
                newButton.setCallbackData(getAllMonth().get(i));
                secondRow.add(newButton);
            }
            if (i > 7 && i <= 11) {
                InlineKeyboardButton newButton = new InlineKeyboardButton();
                newButton.setText(getAllMonth().get(i));
                newButton.setCallbackData(getAllMonth().get(i));
                thirdRow.add(newButton);
            }
        }

        rowsInLine.add(firstRow);
        rowsInLine.add(secondRow);
        rowsInLine.add(thirdRow);

        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить сообщение пользователю с id:{}", chatId);
        }
    }

    public void selectDay(long chatId, String month) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выбери число: ");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> row = null;

        int daysInMonth = getCountDays(month);
        for (int i = 1; i <= daysInMonth; i++) {
            if (i == 1 || row.size() == 7) {
                row = new ArrayList<>();
            }
            InlineKeyboardButton newButton = new InlineKeyboardButton();
            newButton.setText(String.valueOf(i));

            //Что-бы в бд сохранялось число в формате dd
            if (i < 10) {
                newButton.setCallbackData("Месяц:" + month + " число:0" + i);
            } else {
                newButton.setCallbackData("Месяц:" + month + " число:" + i);
            }
            row.add(newButton);
            if (row.size() == 7 || i == daysInMonth) {
                rowsInLine.add(row);
            }
        }
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить сообщение пользователю с id:{}", chatId);
        }
    }

    public void selectDescription(long chatId) {
        String answer = "Введи описане: \n" +
                "Сообщение должно начинаться словом 'Описание'";

        sendMessage(chatId, answer);
    }

    public void saveDateInDb(long chatId, String data, String userName, String description) {
        String month = data.replaceAll("Месяц:", "").replaceAll(" число.*", "");
        String numberOfMonth = getNumberOfMonth(month);

        String date = numberOfMonth + "." + data.replaceAll(".*:", "");

        String success = String.format("Супер! Я сохранил напоминание %1$s\n" +
                "'%2$s, %3$s' Теперь ты будешь получать от меня уведомление за один день и в день этого события %4$s\n" +
                "Хорошего дня!", EMOJI_WRITING_HAND, date, description, EMOJI_TADA);

        User user = new User();

        user.setChatNum(chatId);
        user.setName(userName);
        user.setDate(date);
        user.setDescription(description);
        userRepo.save(user);
        log.info("В бд сохранилалсь инфа дата: {} чатИд: {}", data, chatId);

        sendMessage(chatId, success);
    }

    public void contactMessageAnswer(long chatId) {
        String answer = String.format("%1$s Telegram для связи https://t.me/DariaEskova %2$s", EMOJI_TELEPHONE, EMOJI_WOMAN_JUDGE);
        sendMessage(chatId, answer);
    }

    public void thankMessageAnswer(long chatId) {
        String answer = String.format("Мы рады, что тебе нравится бот %s \n" +
                "Отблагорить нас можно по ссылке: https://pay.cloudtips.ru/p/9e863c6c", EMOJI_HEART);
        sendMessage(chatId, answer);
    }

    public void whatDateDeleteQuestion(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);

        List<User> list = userRepo.findAllDateByChatNum(chatId);
        if (list.size() == 0) {
            sendMessage.setText(String.format("У тебя нет добавленных событий о которых я могу напомнить ", EMOJI_ROBOT_FACE));
        } else {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                String date = list.get(i).getDate();
                String description = list.get(i).getDescription();
                int id = list.get(i).getId();

                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton button = new InlineKeyboardButton();

                button.setText(String.format("Дата: %s, %s", date, description));
                button.setCallbackData(String.format("Удалить из БД %s", id));

                row.add(button);
                rowsInLine.add(row);
            }

            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить ВОПРОС какую дату удалить. user id:{}", chatId);
        }
    }

    public void deleteDateFromDb(long chatId, String message) {
        int id = Integer.parseInt(message.replace("Удалить из БД ", ""));

        try {
            userRepo.deleteById(id);
            log.warn("Напоминание успешно удалено из календаря. chatId:{}, id:{}", chatId, id);
        } catch (Exception e) {
            String ups = "Упс, что то пошло не так.";
            sendMessage(chatId, ups);
            log.warn("!!!!ЧТО ТО ПОШЛО НЕТ ТАК при удалении записи из бд!!! chatId:{}, id:{}", chatId, id);
        }
        String success = "Напоминание успешно удалено из календаря";
        sendMessage(chatId, success);
    }

    public void sendMessage(long chatId, String answer) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(answer);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.warn("Не удалось отправить сообщение пользователю с id:{}", chatId);
        }
    }


    //--------------------------------НАПОМИНАНИЕ-----------------------------

    public void getAllTodayEventsFromDb() {
        // Определяем паттерн для форматирования даты
        String pattern = "MM.dd"; // Пример паттерна

        // Создаем объект DateTimeFormatter с указанным паттерном
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        // Получаем текущую дату и форматируем ее с помощью DateTimeFormatter
        String today = LocalDate.now().format(formatter);

        List<User> results = userRepo.findAllDateByDate(today);
        System.out.println("reasasREEEEES" + results);
    }
}
