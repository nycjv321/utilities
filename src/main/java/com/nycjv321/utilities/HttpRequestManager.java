package com.nycjv321.utilities;


import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Consumer;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Created by Javier L. Velasquez on 2/4/15.
 */
public class HttpRequestManager {
    public final int OK_RESPONSE = 200;
    private final Logger logger = getLogger(HttpRequestManager.class);
    private final Requests.Timeouts timeouts;
    private CredentialsProvider credentialsProvider;
    private boolean validateResponse = true;

    private HttpRequestManager(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
        this();
        setCredentials(userName, password, authenticationMethod);
    }

    private HttpRequestManager(String userName, String password) {
        this(userName, password, AUTHENTICATION_METHOD.USERNAME_PASSWORD);
    }

    private HttpRequestManager() {
        timeouts = Requests.Timeouts.getDefault();
    }

    public static HttpRequestManager create() {
        return new HttpRequestManager();
    }

    public synchronized static void unchecked(HttpRequestManager http, Consumer<Consumer<?>> h) {
        boolean originalState = http.validateResponse;
        http.validateResponse = false;
        h.accept(h);
        http.validateResponse = originalState;
    }

    /**
     * Set the credentials used to authenticate requests
     *
     * @param userName
     * @param password
     * @param authenticationMethod
     */
    private void setCredentials(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                createCredentials(userName, password, authenticationMethod)
        );
    }

    private Credentials createCredentials(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
        final String credentialsString = String.format("%s:%s", userName, password);
        switch (authenticationMethod) {
            case USERNAME_PASSWORD:
                return new UsernamePasswordCredentials(credentialsString);
            case NT:
                return new NTCredentials(credentialsString);
            default:
                throw new IllegalArgumentException("Invalid Authentication Method Provided");
        }
    }

    private boolean hasCredentialsProvider() {
        return Objects.nonNull(credentialsProvider);
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

    private CloseableHttpClient createHttpClient() {
        if (hasCredentialsProvider()) {
            return HttpClients.
                    custom().
                    setDefaultCredentialsProvider(credentialsProvider).
                    build();
        } else {
            SSLContextBuilder builder = new SSLContextBuilder();
            try {
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                throw new RuntimeException(e);
            }
            SSLConnectionSocketFactory sslsf;
            try {
                sslsf = new SSLConnectionSocketFactory(
                        builder.build());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
            HttpClientBuilder client = HttpClients.custom().setSSLSocketFactory(
                    sslsf);
            client.setDefaultCredentialsProvider(credentialsProvider);
            return client.build();
        }
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
            throw new HttpException(String.format("%s returned an empty or null response", url));
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
            throw new HttpException(String.format("Error creating document of %s. See: %s", url, e));
        }
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
            HttpEntity entity = response.getEntity();
            // Attempt to create an input stream of the content and create its string representation as UTF8
            try (InputStream content = entity.getContent()) {
                return IOUtils.toString(content, "UTF-8").trim();
            } finally {
                try {
                    EntityUtils.consume(entity);
                } catch (IOException e) {
                    if (e.getMessage().contains("Unauthorized")) {
                        throw new UnAuthorizedException(String.format("Request was not authorized by server. See: %s", e));
                    }
                    logger.error(e);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
        return "";
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
        throw new HttpException(String.format("Could not get %s", url));
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
                throw new HttpException(String.format("Got %s for %s", response.getStatusLine(), url));
            }
        }
    }

    public void validateResponses(boolean validateResponses) {
        validateResponse = validateResponses;
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
            throw new HttpException(String.format("%s did not contain an original HttpResponse", closeableHttpResponse));
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

    public enum AUTHENTICATION_METHOD {
        USERNAME_PASSWORD, NT
    }

    private enum METHOD {
        HEAD, GET, POST, PUT
    }

    public static class Requests {
        public static class Timeouts {

            private static final Timeouts timeouts;

            static {
                timeouts = new Timeouts();
                timeouts.setConnectionRequestTimeout(10000);
                timeouts.setConnectTimeout(10000);
                timeouts.setSocketTimeout(10000);
            }

            private int socketTimeout;
            private int connectTimeout;
            private int connectionRequestTimeout;

            public static Timeouts getDefault() {
                return timeouts;
            }

            public int getConnectTimeout() {
                return connectTimeout;
            }

            public void setConnectTimeout(int connectTimeout) {
                this.connectTimeout = connectTimeout;
            }

            public int getConnectionRequestTimeout() {
                return connectionRequestTimeout;
            }

            public void setConnectionRequestTimeout(int connectionRequestTimeout) {
                this.connectionRequestTimeout = connectionRequestTimeout;
            }

            public int getSocketTimeout() {
                return socketTimeout;
            }

            public void setSocketTimeout(int socketTimeout) {
                this.socketTimeout = socketTimeout;
            }


        }
    }

    /**
     * This unchecked exception represents all http-related exceptions encountered in this class.
     */
    public final static class HttpException extends RuntimeException {
        public HttpException(String message) {
            super(message);
        }
    }

    /**
     * This exception is thrown when the request was not authorized
     */
    private class UnAuthorizedException extends RuntimeException {
        private UnAuthorizedException(String message) {
            super(message);
        }
    }

}