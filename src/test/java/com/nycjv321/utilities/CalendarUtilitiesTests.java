package com.nycjv321.utilities;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by jvelasquez on 5/9/15.
 */
public class CalendarUtilitiesTests {
    private CalendarUtilities calendarUtilities;
    private String dateFormat = "MM/dd/yyyy";

    @BeforeClass
    public void beforeClass() {
        calendarUtilities = new CalendarUtilities(dateFormat);
    }

    @Test
    public void testGetFormattedCalendar() {
        String formattedCalendar;
        formattedCalendar = calendarUtilities.getFormattedCalendar(1, 1, 2015);
        assertEquals(formattedCalendar, "01/01/2015");
        formattedCalendar = calendarUtilities.getFormattedCalendar(4, 1, 2015);
        assertEquals(formattedCalendar, "04/01/2015");
        formattedCalendar = calendarUtilities.getFormattedCalendar(1, 30, 2015);
        assertEquals(formattedCalendar, "01/30/2015");
        formattedCalendar = calendarUtilities.getFormattedCalendar(1, 30, 1970);
        assertEquals(formattedCalendar, "01/30/1970");
        formattedCalendar = calendarUtilities.getFormattedCalendar(12, 5, 2050);
        assertEquals(formattedCalendar, "12/05/2050");
    }

    @Test
    public void testFormatCalendar() {
        Calendar formattedCalendar = calendarUtilities.getCalendar(1, 1, 2015);
        assertTrue(calendarUtilities.getFormattedCalendar(formattedCalendar).matches("^[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}$"));
    }
}
