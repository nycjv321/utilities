package com.nycjv321.utilities;

import com.google.common.collect.ImmutableMap;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Consumer;

import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testng.Assert.assertEquals;

public class HttpRequestManagerTest {
    private ClientAndProxy proxy;
    private ClientAndServer mockServer;
    private HttpRequestManager defaultRequestManager;

    @BeforeClass
    public void beforeClass() throws Exception {
        mockServer = startClientAndServer(1080);
        proxy = startClientAndProxy(1090);
        defaultRequestManager = HttpRequestManager.create();
    }

    @AfterClass
    public void afterClass() throws Exception {
        proxy.stop();
        getMockServer().stop();
    }

    @Test
    public void get() throws Exception {
        String body = "Body Content";
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(body).withStatusCode(200)
                ),
                t -> assertEquals(getHttpRequestManager().get("http://127.0.0.1:1080/"), body));
    }

    private Document toDocument(String string) {
        try {
            return new SAXBuilder().build(new StringReader(string));
        } catch (JDOMException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private String toString(Document document) {
        return new XMLOutputter().outputString(document);
    }

    @Test(dependsOnMethods = "get")
    public void getDocument() throws Exception {
        Document body = toDocument("<root><children><child><id>0</id></child></children></root>");
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(toString(body)).withStatusCode(200)
                ),
                t -> assertEquals(toString(getHttpRequestManager().getDocument("http://127.0.0.1:1080/")), toString(body)));
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
                        json = getHttpRequestManager().getJSON("http://127.0.0.1:1080/");
                        assertEquals(json.toString(), jsonObject.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test(expectedExceptions = HttpRequestManager.HttpException.class, dependsOnMethods = "unchecked")
    public void checked() {
        String body = "Body Content";
        test(getMockServer(),
                interaction -> getMockServer().when(
                        request().withMethod("GET").withPath("/")
                ).respond(
                        response().withBody(body).withStatusCode(400)
                ),
                t -> assertEquals(
                        getHttpRequestManager()
                                .getResponse("http://127.0.0.1:1080/")
                                .getStatusLine()
                                .getStatusCode(),
                        400
                )
        );
    }


    @Test(dependsOnMethods = "getJson")
    public void unchecked() {
        HttpRequestManager.unchecked(getHttpRequestManager(), h -> {
            String body = "Body Content";
            test(getMockServer(),
                    interaction -> getMockServer().when(
                            request().withMethod("GET").withPath("/")
                    ).respond(
                            response().withBody(body).withStatusCode(400)
                    ),
                    t -> assertEquals(getHttpRequestManager().getResponse("http://127.0.0.1:1080/").getStatusLine().getStatusCode(), 400));

        });
    }

    private HttpRequestManager getHttpRequestManager() {
        return defaultRequestManager;
    }

    public <T extends HttpRequestManagerTest> void test(ClientAndServer mockServer, Consumer<T> mockedInteraction, Consumer<T> test) {
        mockedInteraction.accept((T) this);
        test.accept((T) this);
        mockServer.reset();
    }

    public ClientAndServer getMockServer() {
        return mockServer;
    }
}