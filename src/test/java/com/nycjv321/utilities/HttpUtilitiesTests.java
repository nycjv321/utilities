package com.nycjv321.utilities;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by Javier on 8/31/2014.
 */
public class HttpUtilitiesTests {

    @Test
    public void test200Head() {
        assertEquals(HttpUtilities.head("http://www.google.com").getStatusLine().getStatusCode(), 200);
    }

    @Test
    public void testGet() {
        assertTrue(!Strings.isNullOrEmpty(HttpUtilities.get("http://www.google.com")));
    }

    @Test
    public void testBuildParameters() {
        Map<String, String> parameters = ImmutableMap.of(
                "keyOne", "valueOne", "keyTwo", "valueTwo", "keyThree", "valueThree"
        );
        assertEquals(HttpUtilities.buildParameters(parameters), "?keyOne=valueOne&keyTwo=valueTwo&keyThree=valueThree");
    }

}
