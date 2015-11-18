package com.nycjv321.utilities.http;

import com.google.common.collect.ImmutableMap;
import com.nycjv321.utilities.XMLUtilities;
import com.nycjv321.utilities.http.exceptions.HttpException;
import org.jdom2.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.Test;

import static com.nycjv321.utilities.XMLUtilities.toDocument;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testng.Assert.assertEquals;

public class SimpleHttpClientTest extends AbstractSimpleHttpClientTest {

    @Test
    public void get() throws Exception {
        String body = "Body Content";
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(body).withStatusCode(200)
                ),
                t -> assertEquals(getSimpleHttpClient().get("http://127.0.0.1:1080/"), body));
    }



    @Test(dependsOnMethods = "get")
    public void getDocument() throws Exception {
        Document body = toDocument("<root><children><child><id>0</id></child></children></root>");
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(XMLUtilities.toString(body)).withStatusCode(200)
                ),
                t -> assertEquals(XMLUtilities.toString(getSimpleHttpClient().getDocument("http://127.0.0.1:1080/")), XMLUtilities.toString(body)));
    }

    @Test(dependsOnMethods = "getDocument")
    public void getJson() {
        JSONObject jsonObject = new JSONObject(ImmutableMap.of("key1", "value1", "key2", "value2"));
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(jsonObject.toString()).withStatusCode(200)
                ),
                t -> {
                    JSONObject json = null;
                    try {
                        json = getSimpleHttpClient().getJSON("http://127.0.0.1:1080/");
                        assertEquals(json.toString(), jsonObject.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test(expectedExceptions = HttpException.class, dependsOnMethods = "unchecked")
    public void checked() {
        String body = "Body Content";
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(body).withStatusCode(400)
                ),
                t -> assertEquals(
                        getSimpleHttpClient()
                                .getResponse("http://127.0.0.1:1080/")
                                .getStatusLine()
                                .getStatusCode(),
                        400
                )
        );
    }



    @Test(dependsOnMethods = "getJson")
    public void unchecked() {
        SimpleHttpClient.unchecked(getSimpleHttpClient(), h -> {
            String body = "Body Content";
            test(getMockServer(),
                    interaction -> getMockServer().when(
                            request().withMethod("GET").withPath("/")
                    ).respond(
                            response().withBody(body).withStatusCode(400)
                    ),
                    t -> assertEquals(getSimpleHttpClient().getResponse("http://127.0.0.1:1080/").getStatusLine().getStatusCode(), 400));

        });
    }



}