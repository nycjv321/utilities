package com.nycjv321.utilities;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Created by Javier on 8/31/2014.
 */
public class HttpUtilitiesTests {

    @Test
    public void test200Head() {
        assertEquals(HttpUtilities.head(HttpUtilities.createURI("http://www.google.com")).getStatusLine().getStatusCode(), 200);
    }

    @Test
    public void testGet() {
        assertNotNull(HttpUtilities.get(HttpUtilities.createURI("http://www.google.com")));
    }

}
