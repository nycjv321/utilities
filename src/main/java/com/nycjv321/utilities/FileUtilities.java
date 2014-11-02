package com.nycjv321.utilities;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.apache.commons.io.FileUtils.openOutputStream;

/**
 * Created by jvelasquez on 4/14/15.
 */
public class FileUtilities {
    public static File getRandomFileInTemp() {
        String temp = System.getProperty("java.io.tmpdir");
        if (!temp.endsWith("/")) {
            temp = temp + "/";
        }
        return new File(temp + RandomStringUtils.randomAlphabetic(15));
    }

    public static File savePropertiesToTemp(Properties properties) {
        File file = getRandomFileInTemp();
        try (FileOutputStream fileOutputStream = openOutputStream(file)) {
            properties.store(fileOutputStream, "Automation Generated Properties");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

}