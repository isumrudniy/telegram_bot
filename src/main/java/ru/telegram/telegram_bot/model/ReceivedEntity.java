package ru.telegram.telegram_bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceivedEntity {
    private Long userId;
    private Long chatId;
    private String userName;
    private String receivedMessage;
}
