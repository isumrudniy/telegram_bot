package ru.telegram.telegram_bot.service.components;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

public interface BotCommands {
    List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "start bot"),
            new BotCommand("/rate", "rate now"),
            new BotCommand("/help", "bot info")
    );

    String HELP_TEXT = "This bot allows you to find out the current rate and perform conservation. " +
            "The following commands are available to you:\n\n" +
            "/start - start the bot\n" +
            "/rate - rate now\n" +
            "/help - help menu";
}
