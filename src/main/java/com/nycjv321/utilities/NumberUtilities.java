package com.nycjv321.utilities;

import com.google.common.primitives.Ints;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Holds utility methods used to interact with Numbers
 * Created by Javier on 9/7/2014.
 */
public final class NumberUtilities {

    /**
     * Returns a randomly generated number between the provided minimum and maximum
     * @param random a random number generator
     * @param min the minimum value that the random number can represent
     * @param max the maximum value that the random number can represent
     * @return
     */
    public static int getRandom(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Checks to see if a string represents a valid number
     * @param string a string value that could potentially represents a number
     * @return a boolean indicating if the string actually represented a number
     */
    public static boolean isValid(String string) {
        try {
            Float aFloat = Float.valueOf(string);
            assert aFloat != null;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Takes a string and takes each possible number and stores them in an int array
     * @param string
     * @return
     */
    public static int[] convertStringToIntegerArray(String string) {
        int[] numbers = new int[string.length()];
        for (int i = 0; i < string.length(); ++i) {
            String substring = string.substring(i, i + 1);
            if (isValid(substring))
                numbers[i] = Integer.parseInt(substring);
        }
        return numbers;
    }

    /**
     * Takes an array of integers and return the summation
     * @param numbers an array of integers
     * @return a summation of the given array
     */
    public static int summarizeIntegerArray(int[] numbers) {
        int total = 0;
        for (int number : numbers) {
            total += number;
        }
        return total;
    }


    /**
     * Return a shuffled list of integers.
     * @param maxExclusive the upper bound (exclusive) for the range
     * @return a list of shuffled values 0 - ({@code maxExclusive} - 1)
     */
    public static List<Integer> getShuffled(int maxExclusive) {
        final List<Integer> numbers = Ints.asList(IntStream.range(0, maxExclusive).toArray());
        Collections.shuffle(numbers);
        return numbers;
    }
}
