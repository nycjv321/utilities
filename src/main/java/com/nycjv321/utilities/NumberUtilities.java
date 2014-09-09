package com.nycjv321.utilities;

import java.util.Random;

/**
 * Created by Javier on 9/7/2014.
 */
public class NumberUtilities {
    public static int getRandom(Random random, int min, int max) {
        return min + random.nextInt(max);
    }

    public static boolean isValid(String number) {
        try {
            Float.valueOf(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
