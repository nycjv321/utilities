package com.nycjv321.utilities;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Created by jvelasquez on 5/9/15.
 */
public class FunctionUtilitiesTests {

    @Test(invocationCount = 100)
    public void testBenchmark() {
        class BenchmarkTest extends FunctionUtilities {
            public double go() {
                return benchmark(doSomething -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        double result = new BenchmarkTest().go();
        assertTrue(result < 0.02 && result > 0, String.format("Expected Benchmark to take around .01 ms. Actual is %s", result));
    }

}
