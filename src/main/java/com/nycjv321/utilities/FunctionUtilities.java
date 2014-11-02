package com.nycjv321.utilities;

import java.util.function.Consumer;

/**
 * Created by jvelasquez on 4/18/15.
 */
public class FunctionUtilities {

    /**
     * Benchmarks a method in milliseconds
     * @param object
     * @return the number of milliseconds it takes to run a method.
     */
    public static double benchmark(Consumer<Object> object) {
        long time = System.currentTimeMillis();
        object.accept(object);
        return (System.currentTimeMillis() - time) / 1000.0;
    }
}
