package com.nycjv321.utilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.logging.log4j.LogManager.getLogger;


/**
 * Created by Javier L. Velasquez on 2/4/15.
 */
public class HttpUtilities {
    public static final int OK_RESPONSE = 200;
    private static final Logger logger = getLogger(HttpUtilities.class);
    private static CredentialsProvider credentialsProvider;
    private static boolean VERIFY_RESPONSE_VALID = true;

    /**
     * Set the credentials used to authenticate requests (using simple username, password authentication)
     *
     * @param userName
     * @param password
     */
    public static void setCredentials(String userName, String password) {
        setCredentials(userName, password, AUTHENTICATION_METHOD.USERNAME_PASSWORD);
    }

    /**
     * Set the credentials used to authenticate requests
     *
     * @param userName
     * @param password
     * @param authenticationMethod
     */
    public static void setCredentials(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                createCredentials(userName, password, authenticationMethod)
        );
    }

    private static Credentials createCredentials(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
        final String credentialsString = String.format("%s:%s", userName, password);
        switch (authenticationMethod) {
            case USERNAME_PASSWORD:
                return new UsernamePasswordCredentials(credentialsString);
            case NT:
                return new NTCredentials(credentialsString);
            default:
                throw new IllegalArgumentException(String.format("Invalid Authentication Method Provided"));
        }
    }

    public static void disableCredentials() {
        credentialsProvider.clear();
    }

    private static boolean hasCredentialsProvider() {
        return Objects.nonNull(credentialsProvider);
    }

    @SuppressWarnings("unchecked")
    private static <T extends HttpRequestBase> T create(METHOD method, String url) {
        switch (method) {
            case HEAD:
                return (T) new HttpHead(url);
            case GET:
                return (T) new HttpGet(url);
            case POST:
                return (T) new HttpPost(url);
            case PUT:
                return (T) new HttpPut(url);

        }
        throw new IllegalArgumentException(String.format("%s was invalid Http Method", method));
    }

    private static CloseableHttpClient createHttpClient() {
        if (hasCredentialsProvider()) {
            return HttpClients.
                    custom().
                    setDefaultCredentialsProvider(credentialsProvider).
                    build();
        } else {
            return HttpClients.createDefault();
        }
    }

    /**
     * Perform a HTTP GET and convert the content body into a JSON Object
     *
     * @param url a url to GET
     * @return a JSON Object representing the content of the URL
     * @throws org.json.JSONException
     */
    public static JSONObject getJSON(String url) throws org.json.JSONException {
        return new JSONObject(get(url));
    }

    /**
     * Create an XML Document based provided url resource
     *
     * @param url
     * @return
     */
    public static Document getDocument(String url) {
        SAXBuilder jdomBuilder = new SAXBuilder();
        try (StringReader characterStream = new StringReader(HttpUtilities.get(url))) {
            return jdomBuilder.build(characterStream);
        } catch (JDOMException | IOException e) {
            throw new IllegalStateException(String.format("Error creating document of %s. See: %s", url, e));
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
    public static HttpResponse post(String url, String body, Header... headers) {
        final File randomFileInTemp = FileUtilities.getRandomFileInTemp();
        try {
            FileUtils.write(randomFileInTemp, body);
            return post(url, randomFileInTemp, headers);
        } catch (IOException e) {
            return getEmptyStatusLine();
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
    public static HttpResponse post(String url, File file, Header... headers) {
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
            return getEmptyStatusLine();
        }
    }

    /**
     * Performs a HTTP GET and return the HTTP Response content in string format. <p> Each request is recorded to the current logger in the following format:
     * "GET ${URL}: ${STATUS_CODE}"
     *
     * @param url a url to get from
     * @return a string representation of the contents of the HTTP Response
     */
    public static String get(String url, Header ... headers) {
        CloseableHttpClient httpClient = createHttpClient();
        HttpRequestBase httpGet = create(METHOD.GET, url);
        // Perform a HTTP GET
        httpGet.setHeaders(headers);
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
                        throw new IllegalStateException(String.format("Request was not authorized by server. See: %s", e));
                    }
                    logger.error(e);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
        return "";
    }


    /**
     * If {@code VERIFY_RESPONSE_VALID} and the response is a 401 throw a {@code UnAuthorizedException}
     *
     * @param response a response to check
     * @param url      a url that was requested to generate the response
     */
    private static void validateResponse(HttpResponse response, String url) {
        if (VERIFY_RESPONSE_VALID) {
            if (response.getStatusLine().getStatusCode() == 401) {
                throw new UnAuthorizedException(String.format("Got %s for %s", response.getStatusLine(), url));
            }
        }
    }

    /**
     * This exception is thrown when the request was not authorized
     */
    private static class UnAuthorizedException extends RuntimeException {
        private UnAuthorizedException(String message) {
            super(message);
        }
    }

    /**
     * Performs a HTTP HEAD and return the HTTP Status Line. <p> Each request is recorded to the current logger in the following format:
     * "HEAD ${URL}: ${STATUS_CODE}"
     *
     * @param url a url to HEAD to
     * @return a string representation of the contents of the HTTP Status Code
     */
    public static HttpResponse head(String url) {
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
        return getEmptyStatusLine();
    }

    /**
     * See {@code HttpUtilities#head(String url}
     *
     * @param uri
     * @return
     */
    public static HttpResponse head(URI uri) {
        return head(uri.toString());
    }

    /**
     * @return an empty status line
     */
    private static BasicHttpResponse getEmptyStatusLine() {
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
    public static HttpResponse put(String url, File file, Header... headers) {
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
            return getEmptyStatusLine();
        }
    }

    /**
     * Extract the original Http Response to get the response headers and other goodies.
     *
     * @param closeableHttpResponse
     * @return
     */
    private static HttpResponse getOriginal(CloseableHttpResponse closeableHttpResponse) {
        try {
            final Field original = closeableHttpResponse.getClass().getDeclaredField("original");
            original.setAccessible(true);
            return (HttpResponse) original.get(closeableHttpResponse);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(String.format("%s did not contain an original HttpResponse", closeableHttpResponse));
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
    public static HttpResponse put(String url, String body, Header... headers) {
        final File randomFileInTemp = FileUtilities.getRandomFileInTemp();
        try {
            FileUtils.write(randomFileInTemp, body);
            return put(url, randomFileInTemp, headers);
        } catch (IOException e) {
            return getEmptyStatusLine();
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

    /**
     * Create string that represents the parameters to a URL resource
     * @param parameters
     * @return
     */
    public static String buildParameters(Map<String, String> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        Set<String> strings = parameters.keySet();
        String parametersString = "?";
        for (Iterator<String> iterator = strings.iterator(); iterator.hasNext(); ) {
            String string = iterator.next();
            parametersString = parametersString + string + "=" + parameters.get(string);
            if (iterator.hasNext()) {
                parametersString = parametersString + "&";
            }
        }
        return parametersString;
    }


}
