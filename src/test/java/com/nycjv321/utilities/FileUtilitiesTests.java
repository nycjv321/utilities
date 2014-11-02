package com.nycjv321.utilities;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

import static com.nycjv321.utilities.FileUtilities.savePropertiesToTemp;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by jvelasquez on 5/9/15.
 */
public class FileUtilitiesTests {

    @Test
    public void testGetRandomFileInTemp() {
        File randomFileInTemp = FileUtilities.getRandomFileInTemp();

        String actual = randomFileInTemp.getParent();
        String expected = System.getProperty("java.io.tmpdir");

        // some OSs don't end the path with a slash. Clean up the output here
        if (!expected.endsWith("/")) {
            expected = expected + "/";
        }

        if (!actual.endsWith("/")) {
            actual = actual + "/";
        }

        assertEquals(actual, expected);
    }

    @Test
    public void testSavePropertiesToTemp() {
        Properties testProperties = new Properties();
        testProperties.put("testKey", "testValue");
        File file = null;
        try {
            file = savePropertiesToTemp(testProperties);
            assertTrue(file.exists());
            Properties readProperties = new Properties();
            try (FileInputStream inputStream = openInputStream(file)) {
                readProperties.load(inputStream);
                assertEquals(readProperties.getProperty("testKey"), "testValue");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (Objects.nonNull(file)) {
                file.deleteOnExit();
            }
        }
    }

}
