package com.nycjv321.utilities.http;

import org.testng.annotations.Test;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testng.Assert.assertEquals;

/**
 * Created by fedora on 11/18/15.
 */
public class SimpleHttpClientBuilderTest extends AbstractSimpleHttpClientTest {

    @Test
    public void testBuild() {
        simpleHttpClient = SimpleHttpClientBuilder.create().build();
        String body = "Body Content";
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(body).withStatusCode(200)
                ),
                t -> assertEquals(getSimpleHttpClient().get("http://127.0.0.1:1080/"), body));
    }

}