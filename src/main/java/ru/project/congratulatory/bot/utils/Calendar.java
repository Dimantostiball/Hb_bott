package ru.project.congratulatory.bot.utils;

import java.util.List;
import java.util.stream.Collectors;

public class Calendar {
    public static final String JANUARY = "Январь";
    public static final String FEBRUARY = "Февраль";
    public static final String MARCH = "Март";
    public static final String APRIL = "Апрель";
    public static final String MAY = "Май";
    public static final String JUNE = "Июнь";
    public static final String JULY = "Июль";
    public static final String AUGUST = "Август";
    public static final String SEPTEMBER = "Сентябрь";
    public static final String OCTOBER = "Октябрь";
    public static final String NOVEMBER = "Ноябрь";
    public static final String DECEMBER = "Декабрь";

    public static int getCountDays(String month) {
        switch (month) {
            case "Январь":
            case "Июль":
            case "Май":
            case "Март":
            case "Август":
            case "Октябрь":
            case "Декабрь":
                return 31;
            case "Февраль":
                return 28;
            case "Апрель":
            case "Июнь":
            case "Сентябрь":
            case "Ноябрь":
                return 30;
            default:
                return 1;
        }
    }

    public static List<String> getAllMonth() {
        return List.of(JANUARY, FEBRUARY, MARCH, APRIL, MAY,
                 JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER);
    }

    public static String getNumberOfMonth(String month) {
        switch (month) {
            case "Январь":
                return "01";
            case "Февраль":
                return "02";
            case "Март":
                return "03";
            case "Апрель":
                return "04";
            case "Май":
                return "05";
            case "Июнь":
                return "06";
            case "Июль":
                return "07";
            case "Август":
                return "08";
            case "Сентябрь":
                return "09";
            case "Октябрь":
                return "10";
            case "Ноябрь":
                return "11";
            case "Декабрь":
                return "12";
            default:
                return "1";
        }
    }
}
