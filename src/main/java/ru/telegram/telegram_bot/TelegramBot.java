package ru.telegram.telegram_bot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegram.telegram_bot.config.BotConfig;
import ru.telegram.telegram_bot.model.CurrencyModel;
import ru.telegram.telegram_bot.model.UserState;
import ru.telegram.telegram_bot.service.CurrencyService;
import ru.telegram.telegram_bot.service.components.Buttons;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.telegram.telegram_bot.service.components.BotCommands.HELP_TEXT;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    public void onUpdateReceived(@NotNull Update update) {
        long chatId = 0;
        long userId = 0; //это нам понадобится позже
        String userName = null;
        String receivedMessage;

        //если получено сообщение текстом
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            userId = update.getMessage().getFrom().getId();
            userName = update.getMessage().getFrom().getFirstName();

            if (update.getMessage().hasText()) {
                receivedMessage = update.getMessage().getText();
                botAnswerUtils(receivedMessage, userId, chatId, userName);
            }

            //если нажата одна из кнопок бота
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            userId = update.getCallbackQuery().getFrom().getId();
            userName = update.getCallbackQuery().getFrom().getFirstName();
            String callbackData = update.getCallbackQuery().getData();

            switch (callbackData) {
                case "/rate":
                    UserState userState = userStates.computeIfAbsent(userId, k -> new UserState());
                    userState.setCurrentState("AWAITING_FEEDBACK");
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("Enter the required currency in the format: USD");

                    try {
                        execute(message);
                        log.info(userName + " Reply sent");
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                    break;
                default:
                    botAnswerUtils(callbackData, userId, chatId, userName);
                    break;
            }
        }

    }

    private void botAnswerUtils(String receivedMessage, long userId, long chatId, String userName) {
        switch (receivedMessage) {
            case "/start":
                startBot(chatId, userName);
                break;
            case "/help":
                sendHelpText(chatId, HELP_TEXT);
                break;
            case "/rate":
                UserState userState = userStates.computeIfAbsent(userId, k -> new UserState());
                userState.setCurrentState("AWAITING_FEEDBACK");
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Enter the required currency in the format: USD");

                try {
                    execute(message);
                    log.info(userName + " Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            default:
                sendAnswerText(userId, chatId, receivedMessage);
                break;
        }
    }

    private void startBot(long chatId, String userName) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Hi, " + userName + ", nice to meet you!" + "\n" +
                "Enter the currency whose official exchange rate" + "\n" +
                "you want to know in relation to RUB." + "\n" +
                "For example: USD");
        message.setReplyMarkup(Buttons.inlineMarkup());

        try {
            execute(message);
            log.info(userName + " Reply sent");
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void sendAnswerText(long userId, long chatId, String receivedMessage) {
        CurrencyModel currencyModel = new CurrencyModel();
        String currency = "";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        UserState userState = userStates.get(userId);

        try {
            if (userState != null) {
                message.setText(CurrencyService.getCurrencyRate(receivedMessage.toUpperCase(), currencyModel));
                userStates.remove(userId);

                try {
                    execute(message);
                    log.info(userId + " Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }

            } else {
                sendHelpText(chatId, HELP_TEXT);
            }

        } catch (IOException e) {
            message.setText("We have not found such a currency." + "\n" +
                    "Enter the currency whose official exchange rate" + "\n" +
                    "you want to know in relation to RUB." + "\n" +
                    "For example: USD");
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse date");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendHelpText(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
            log.info(chatId + " Reply sent");
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

}
