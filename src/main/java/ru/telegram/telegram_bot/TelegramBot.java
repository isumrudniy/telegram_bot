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
import ru.telegram.telegram_bot.model.ReceivedEntity;
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

        //если получено сообщение текстом
        if (update.hasMessage()) {
            var receivedEntity = ReceivedEntity.builder()
                    .chatId(update.getMessage().getChatId())
                    .userId(update.getMessage().getFrom().getId())
                    .userName(update.getMessage().getFrom().getFirstName())
                    .build();

            if (update.getMessage().hasText()) {
                receivedEntity.setReceivedMessage(update.getMessage().getText());
                botAnswerUtils(receivedEntity);
            }

            //если нажата одна из кнопок бота
        } else if (update.hasCallbackQuery()) {
            var receivedEntity = ReceivedEntity.builder()
                    .chatId(update.getCallbackQuery().getMessage().getChatId())
                    .userId(update.getCallbackQuery().getFrom().getId())
                    .userName(update.getCallbackQuery().getFrom().getFirstName())
                    .receivedMessage(update.getCallbackQuery().getData())
                    .build();

            switch (receivedEntity.getReceivedMessage()) {
                case "/rate":
                    UserState userState = userStates.computeIfAbsent(receivedEntity.getUserId(), k -> new UserState());
                    userState.setCurrentState("AWAITING_FEEDBACK");

                    var message = SendMessage.builder()
                            .chatId(receivedEntity.getChatId())
                            .text("Enter the required currency in the format: USD")
                            .replyMarkup(Buttons.inlineMarkup())
                            .build();

                    try {
                        execute(message);
                        log.info(receivedEntity.getUserId() + " Reply sent");
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                    }
                    break;
                default:
                    break;
            }
        }

    }

    private void botAnswerUtils(ReceivedEntity receivedEntity) {
        switch (receivedEntity.getReceivedMessage()) {
            case "/start":
                startBot(receivedEntity.getChatId(), receivedEntity.getUserName());
                break;
            case "/help":
                sendHelpText(receivedEntity.getChatId(), HELP_TEXT);
                break;
            case "/rate":
                UserState userState = userStates.computeIfAbsent(receivedEntity.getUserId(), k -> new UserState());
                userState.setCurrentState("AWAITING_FEEDBACK");

                var message = SendMessage.builder()
                        .chatId(receivedEntity.getChatId())
                        .text("Enter the required currency in the format: USD")
                        .replyMarkup(Buttons.inlineMarkup())
                        .build();
                try {
                    execute(message);
                    log.info(receivedEntity.getUserId() + " Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }
            default:
                sendAnswerText(receivedEntity);
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

    private void sendAnswerText(ReceivedEntity receivedEntity) {
        CurrencyModel currencyModel = new CurrencyModel();
        String currency = "";
        SendMessage message = new SendMessage();
        message.setChatId(receivedEntity.getChatId());
        UserState userState = userStates.get(receivedEntity.getUserId());

        try {
            if (userState != null) {
                message.setText(CurrencyService.getCurrencyRate(receivedEntity.getReceivedMessage().toUpperCase(), currencyModel));
                userStates.remove(receivedEntity.getUserId());

                try {
                    execute(message);
                    log.info(receivedEntity.getUserId() + " Reply sent");
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                }

            } else {
                sendHelpText(receivedEntity.getChatId(), HELP_TEXT);
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
