package com.nycjv321.utilities.http;

import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.function.Consumer;

import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * Created by fedora on 11/18/15.
 */
public abstract class AbstractSimpleHttpClientTest {

    private ClientAndProxy proxy;
    private ClientAndServer mockServer;
    protected SimpleHttpClient simpleHttpClient;

    @BeforeClass
    public void beforeClass() throws Exception {
        mockServer = startClientAndServer(1080);
        proxy = startClientAndProxy(1090);
        simpleHttpClient = SimpleHttpClientBuilder.create().build();
    }

    public <T extends AbstractSimpleHttpClientTest> void test(ClientAndServer mockServer, Consumer<T> mockedInteraction, Consumer<T> test) {
        mockedInteraction.accept((T) this);
        test.accept((T) this);
        mockServer.reset();
    }

    protected SimpleHttpClient getSimpleHttpClient() {
        return simpleHttpClient;
    }

    public ClientAndServer getMockServer() {
        return mockServer;
    }

    @AfterClass
    public void afterClass() throws Exception {
        proxy.stop();
        getMockServer().stop();
    }

}
