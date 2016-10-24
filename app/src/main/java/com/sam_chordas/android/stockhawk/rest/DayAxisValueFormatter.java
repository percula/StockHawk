package com.sam_chordas.android.stockhawk.rest;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Loosely based on https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/custom/DayAxisValueFormatter.java
 * Modified to account for localities and input format of date
 */
public class DayAxisValueFormatter implements IAxisValueFormatter
{

    private LineChart chart;

    public DayAxisValueFormatter(LineChart chart) {
        this.chart = chart;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int year = (int) (value / 1000);
        int days = (int) (value % 1000);
        int month = determineMonth(days);
        int dayOfMonth = determineDayOfMonth(days, month + 12 * (year - 2016));

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        Date date = calendar.getTime();

        SimpleDateFormat abbreviatedDate = new SimpleDateFormat("MMM d", Locale.getDefault());

        return abbreviatedDate.format(date);
    }

    private int getDaysForMonth(int month) {

        // month is 0-based

        if (month == 1) {
            int x400 = month % 400;
            if (x400 < 0)
            {
                x400 = -x400;
            }
            boolean is29 = (month % 4) == 0 && x400 != 100 && x400 != 200 && x400 != 300;

            return is29 ? 29 : 28;
        }

        if (month == 3 || month == 5 || month == 8 || month == 10)
            return 30;
        else
            return 31;
    }

    private int determineMonth(int dayOfYear) {

        int month = -1;
        int days = 0;

        while (days < dayOfYear) {
            month = month + 1;

            if (month >= 12)
                month = 0;

            days += getDaysForMonth(month);
        }

        return Math.max(month, 0);
    }

    private int determineDayOfMonth(int days, int month) {

        int count = 0;
        int daysForMonths = 0;

        while (count < month) {

            daysForMonths += getDaysForMonth(count % 12);
            count++;
        }

        return days - daysForMonths;
    }


    @Override
    public int getDecimalDigits() {
        return 0;
    }
}