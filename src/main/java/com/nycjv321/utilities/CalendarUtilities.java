package com.nycjv321.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by jvelasquez on 4/15/15.
 */
public class CalendarUtilities {

    private final String dateFormat;

    public CalendarUtilities(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Calendar getCalendar(int month, int day, int year) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.MONTH, month - 1);
        instance.set(Calendar.DAY_OF_MONTH, day);
        instance.set(Calendar.YEAR, year);
        return instance;
    }

    public String getFormattedCalendar(int month, int day, int year) {
        return getFormattedCalendar(getCalendar(month, day, year));
    }

    public String getFormattedCalendar(Calendar instance) {
        return new SimpleDateFormat(dateFormat).format(instance.getTime());
    }
}
