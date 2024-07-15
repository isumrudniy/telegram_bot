package ru.telegram.telegram_bot.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class CurrencyModel {
    private String cur_ID;
    private LocalDateTime date;
    private String cur_Abbreviation;
    private Integer cur_Scale;
    private String cur_Name;
    private Double cur_OfficialRate;
}
