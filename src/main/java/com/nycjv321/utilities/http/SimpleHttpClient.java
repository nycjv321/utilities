package com.nycjv321.utilities.http;

import com.google.common.base.Strings;
import com.nycjv321.utilities.FileUtilities;
import com.nycjv321.utilities.http.exceptions.UnAuthorizedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Created by fedora on 11/18/15.
 */
public class SimpleHttpClient {
    private final Logger logger = getLogger(SimpleHttpClient.class);
    private final Requests.Timeouts timeouts;
    private boolean validateResponse = true;
    private final Supplier<CloseableHttpClient> httpClientSupplier;

    SimpleHttpClient(Supplier<CloseableHttpClient> httpClientSupplier, Requests.Timeouts timeouts) {
        this.timeouts = timeouts;
        this.httpClientSupplier = httpClientSupplier;
    }

    private SimpleHttpClient() {
        httpClientSupplier = null;
        timeouts = null;
    }

    public synchronized static void unchecked(SimpleHttpClient http, Consumer<Consumer<?>> h) {
        boolean originalState = http.validateResponse;
        http.validateResponse = false;
        h.accept(h);
        http.validateResponse = originalState;
    }

    @SuppressWarnings("unchecked")
    private <T extends HttpRequestBase> T create(METHOD method, String url) {
        HttpRequestBase httpRequest;
        switch (method) {
            case HEAD:
                httpRequest = new HttpHead(url);
                break;
            case GET:
                httpRequest = new HttpGet(url);
                break;
            case POST:
                httpRequest = new HttpPost(url);
                break;
            case PUT:
                httpRequest = new HttpPut(url);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s was invalid Http Method", method));
        }

        configureRequestTimeouts(httpRequest);
        return (T) httpRequest;
    }

    private void configureRequestTimeouts(HttpRequestBase request) {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(timeouts.getSocketTimeout())
                .setConnectTimeout(timeouts.getConnectTimeout())
                .setConnectionRequestTimeout(timeouts.getConnectionRequestTimeout())
                .build();
        request.setConfig(config);
    }



    /**
     * Perform a HTTP GET and convert the response body into a JSON Object. <p>
     * Will throw an {@code IllegalStateException} if the response is null or empty
     *
     * @param url a url to GET
     * @return a JSON Object representing the content of the URL
     * @throws JSONException
     */
    public JSONObject getJSON(String url) throws JSONException {
        final String responseBody = get(url);
        if (Strings.isNullOrEmpty(responseBody)) {
            throw new com.nycjv321.utilities.http.exceptions.HttpException(String.format("%s returned an empty or null response", url));
        }
        return new JSONObject(responseBody);
    }

    /**
     * Create an XML Document based provided url resource
     *
     * @param url
     * @return
     */
    public Document getDocument(String url) {
        SAXBuilder jdomBuilder = new SAXBuilder();
        try (StringReader characterStream = new StringReader(get(url))) {
            return jdomBuilder.build(characterStream);
        } catch (JDOMException | IOException e) {
            throw new com.nycjv321.utilities.http.exceptions.HttpException(String.format("Error creating document of %s. See: %s", url, e));
        }
    }

    public HttpResponse post(String url, Header... headers) {
            return post(url, "", headers);
    }

        /**
         * Post a String and return the Response Status Line. <p> Each request is recorded to the current logger in the following format:
         * "POSTing ${FILE} to ${URL}: ${STATUS_LINE}"
         *
         * @param url     a url to POST
         * @param body    a string representing the request body
         * @param headers any headers to associate with the request
         * @return a string representation of the contents of the HTTP Response
         */
    public HttpResponse post(String url, String body, Header... headers) {
        final File randomFileInTemp = FileUtilities.getRandomFileInTemp();
        try {
            FileUtils.write(randomFileInTemp, body);
            return post(url, randomFileInTemp, headers);
        } catch (IOException e) {
            return getEmptyResponse();
        } finally {
            randomFileInTemp.deleteOnExit();
        }
    }

    /**
     * Post a file and return the Response Status Line. <p> Each request is recorded to the current logger in the following format:
     * "POSTing ${FILE} to ${URL}: ${STATUS_LINE}"
     *
     * @param url     a url to POST to
     * @param file    a file representing the post entity
     * @param headers any headers to associate with the request
     * @return a string representation of the contents of the HTTP Response
     */
    public HttpResponse post(String url, File file, Header... headers) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpPost httppost = create(METHOD.POST, url);

        FileEntity reqEntity = new FileEntity(file);
        reqEntity.setChunked(true);
        httppost.setEntity(reqEntity);
        httppost.setHeaders(headers);
        try (CloseableHttpResponse response = httpClient.execute(httppost)) {
            validateResponse(response, url);
            final StatusLine statusLine = response.getStatusLine();
            logger.debug(String.format("POSTing \"%s\" to %s: %s", file.getAbsoluteFile(), url, statusLine));
            return getOriginal(response);
        } catch (IOException e) {
            logger.error(e);
            return getEmptyResponse();
        }
    }

    private CloseableHttpClient createHttpClient() {
        return httpClientSupplier.get();
    }

    public String get(URL url) {
        return get(url.toString());
    }

    /**
     * Performs a HTTP GET and return the HTTP Response content in string format. <p> Each request is recorded to the current logger in the following format:
     * "GET ${URL}: ${STATUS_CODE}"
     *
     * @param url a url to get from
     * @return a string representation of the contents of the HTTP Response
     */
    public String get(String url) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpRequestBase httpGet = create(METHOD.GET, url);
        // Perform a HTTP GET
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            validateResponse(response, url);
            logger.debug(String.format("GET %s: %s", url, response.getStatusLine()));
            return consume(response);
        } catch (IOException e) {
            logger.error(e);
        }
        return "";
    }

    public static String consume(HttpResponse response) {
        HttpEntity entity = response.getEntity();

        // Attempt to create an input stream of the content and create its string representation as UTF8
        try (InputStream content = entity.getContent()) {
            return IOUtils.toString(content, "UTF-8").trim();
        } catch (IOException e) {
            return "";
        } finally {
            try {
                EntityUtils.consume(entity);
            } catch (IOException e) {
                if (e.getMessage().contains("Unauthorized")) {
                    throw new UnAuthorizedException(String.format("Request was not authorized by server. See: %s", e));
                }
                return "";
            }
        }
    }

    public HttpResponse getResponse(URL url) {
        return getResponse(url.toString());
    }

    public HttpResponse getResponse(String url) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpRequestBase httpGet = create(METHOD.GET, url);
        // Perform a HTTP GET
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            validateResponse(response, url);
            logger.debug(String.format("GET %s: %s", url, response.getStatusLine()));
            return response;
        } catch (IOException e) {
            logger.error(e);
        }
        throw new com.nycjv321.utilities.http.exceptions.HttpException(String.format("Could not get %s", url));
    }

    /**
     * If {@code validateResponse} and the response is a 401 throw a {@code UnAuthorizedException}
     *
     * @param response a response to check
     * @param url      a url that was requested to generate the response
     */
    private void validateResponse(HttpResponse response, String url) {
        if (validateResponse) {
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new com.nycjv321.utilities.http.exceptions.HttpException(String.format("Got %s for %s", response.getStatusLine(), url));
            }
        }
    }

    /**
     * Performs a HTTP HEAD and return the HTTP Status Line. <p> Each request is recorded to the current logger in the following format:
     * "HEAD ${URL}: ${STATUS_CODE}"
     *
     * @param url a url to HEAD to
     * @return a string representation of the contents of the HTTP Status Code
     */
    public HttpResponse head(String url) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpRequestBase httpHead = create(METHOD.HEAD, url);
        // Perform a HTTP HEAD
        try (CloseableHttpResponse response = httpClient.execute(httpHead)) {
            validateResponse(response, url);
            validateResponse(response, url);
            final StatusLine statusLine = response.getStatusLine();
            logger.debug(String.format("HEAD %s: %s", url, statusLine));
            return getOriginal(response);
        } catch (IOException e) {
            logger.error(e);
        }
        return getEmptyResponse();
    }

    /**
     * See {@code HttpUtilities#head(String url}
     *
     * @param uri
     * @return
     */
    public HttpResponse head(URI uri) {
        return head(uri.toString());
    }

    /**
     * @return an empty status line
     */
    private BasicHttpResponse getEmptyResponse() {
        return new BasicHttpResponse(new StatusLine() {
            @Override
            @Nullable
            public ProtocolVersion getProtocolVersion() {
                return null;
            }

            @Override
            public int getStatusCode() {
                return 0;
            }

            @Override
            @Nullable
            public String getReasonPhrase() {
                return null;
            }
        });

    }

    /**
     * Performs a HTTP PUT and return the HTTP Status Line. <p> Each request is recorded to the current logger in the following format:
     * "PUT ${URL}: ${STATUS_CODE}"
     *
     * @param url     a url to PUT to
     * @param file    the request entity
     * @param headers a set of additional headers necessary to the request
     * @return a string representation of the contents of the HTTP Status Code
     */
    public HttpResponse put(String url, File file, Header... headers) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpPut httpPut = create(METHOD.PUT, url);

        FileEntity reqEntity = new FileEntity(file);
        httpPut.setEntity(reqEntity);
        httpPut.setHeaders(headers);

        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            validateResponse(response, url);
            final StatusLine statusLine = response.getStatusLine();
            logger.debug(String.format("PUTing \"%s\" to %s: %s", file.getAbsoluteFile(), url, statusLine));
            return getOriginal(response);
        } catch (IOException e) {
            logger.error(e);
            return getEmptyResponse();
        }
    }

    /**
     * Extract the original Http Response to get the response headers and other goodies.
     *
     * @param closeableHttpResponse
     * @return
     */
    private HttpResponse getOriginal(CloseableHttpResponse closeableHttpResponse) {
        try {
            final Field original = closeableHttpResponse.getClass().getDeclaredField("original");
            original.setAccessible(true);
            return (HttpResponse) original.get(closeableHttpResponse);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return closeableHttpResponse;
        }
    }

    /**
     * Performs a HTTP PUT and return the HTTP Status Line. <p> Each request is recorded to the current logger in the following format:
     * "PUT ${URL}: ${STATUS_CODE}"
     *
     * @param url     a url to PUT to
     * @param body    a strings representing the request entity
     * @param headers a set of additional headers necessary to the request
     * @return a string representation of the contents of the HTTP Status Code
     */
    public HttpResponse put(String url, String body, Header... headers) {
        final File randomFileInTemp = FileUtilities.getRandomFileInTemp();
        try {
            FileUtils.write(randomFileInTemp, body);
            return put(url, randomFileInTemp, headers);
        } catch (IOException e) {
            return getEmptyResponse();
        } finally {
            randomFileInTemp.deleteOnExit();
        }
    }

    private enum METHOD {
        HEAD, GET, POST, PUT
    }

}
