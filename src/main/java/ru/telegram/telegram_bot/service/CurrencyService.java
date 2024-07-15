package ru.telegram.telegram_bot.service;

import com.google.gson.Gson;
import org.json.JSONObject;
import ru.telegram.telegram_bot.model.Currency;
import ru.telegram.telegram_bot.model.CurrencyModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class CurrencyService {

    public static String getCurrencyRate(String message, CurrencyModel model) throws IOException, ParseException, URISyntaxException {
        URI uri = new URI("https://www.cbr-xml-daily.ru/daily_json.js");
        Scanner scanner = new Scanner((InputStream) uri.toURL().getContent());
        String result = "";

        while (scanner.hasNext()) {
            result += scanner.nextLine();
        }

        JSONObject currenciesData = new JSONObject(result)
                .getJSONObject("Valute");

        List<Currency> currencies = currenciesData.keySet()
                .stream()
                .filter(cur -> cur.equals(message))
                .map((currency) ->
                        new Gson().fromJson(
                                currenciesData.getJSONObject(currency)
                                        .toString(),
                                Currency.class
                        )
                )
                .toList();

        if (currencies.isEmpty()) {
            throw new IOException();
        }

        currencies.stream()
                .forEach(cur -> {
                    model.setCur_ID(cur.getID());
                    model.setCur_Abbreviation(cur.getCharCode());
                    model.setCur_Scale(cur.getNominal());
                    model.setDate(LocalDateTime.now());
                    model.setCur_OfficialRate(cur.getValue());
                });

        return "Official rate of RUB to " + model.getCur_Abbreviation() + "\n" +
                "on the date: " + getFormatDate(model) + "\n" +
                "is: " + model.getCur_OfficialRate() + " RUB per " + model.getCur_Scale()
                + " " + model.getCur_Abbreviation();
    }

    private static String getFormatDate(CurrencyModel model) {
        return model.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

}
