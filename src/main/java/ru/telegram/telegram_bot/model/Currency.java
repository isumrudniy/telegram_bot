package ru.telegram.telegram_bot.model;

import lombok.Data;

@Data
public class Currency {
    private String ID;
    private Long NumCode;
    private String CharCode;
    private Integer Nominal;
    private String Name;
    private Double Value;
    private Double Previous;
}
