package com.nycjv321.utilities;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Javier L. Velasquez (nycjv321@gmail.com) on 8/30/2014.
 */
public class HttpUtilities {
    private static final Logger logger = LogManager.getLogger(HttpUtilities.class
            .getName());

    /**
     * Create a URI from a string
     * @param string a string representing a URI
     * @return a URI representing the string
     */
    public static URI createURI(String string) {
        try {
            return new URI(string);
        } catch (URISyntaxException e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * Perform a HEAD request on a URI
     * @param url a URI to request
     * @return the string representing the content returned from the HEAD request
     */
    public static HttpResponse head(URI url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpHead httpHead = new HttpHead(url);
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpHead);
            logger.debug(String.format("Got %s response when requesting %s", response.getStatusLine(), url.toString()));
            return response;
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * Perform a GET request on a URI
     * @param url a URI to request
     * @return the string representing the content returned from the GET request
     */
    public static String get(URI url) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            logger.debug(String.format("Got %s response when requesting %s", response.getStatusLine(), url.toString()));
            HttpEntity entity = response.getEntity();

            String output = IOUtils.toString(entity.getContent(), "UTF-8");
            EntityUtils.consume(entity);
            return output;
        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return "";
    }

    /**
     * Add a series of headers to a POST request
     * @param httpPost a HttpPost object
     * @param headers a series of headers to add to the HttpPost request
     */
    private static void addHeaderToPost(HttpPost httpPost, Header... headers) {
        for (int i = 0; i < headers.length; ++i) {
            httpPost.addHeader(headers[i]);
        }
    }

    /**
     * Create a string entity from the given string
     * @param string a string to convert to a string entity
     * @return a string entity representing the given strnig
     */
    public static StringEntity createStringEntity(String string) {
        try {
            return new StringEntity(string);
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
        }
        try {
            return new StringEntity("");
        } catch (UnsupportedEncodingException e) {}
        return null;
    }

    /**
     * Perform a POST request
     * @param uri a uri to post to
     * @param httpEntity a http entity to include in the request
     * @param headers a list of headers to include in the request
     * @return the return of the request
     */
    public static String post(URI uri, HttpEntity httpEntity, Header... headers) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(uri);

        addHeaderToPost(httpPost, headers);

        httpPost.setEntity(httpEntity);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            logger.debug(String.format("Got %s response when requesting %s", response.getStatusLine(), uri.toString()));
            HttpEntity entity = response.getEntity();

            String output = IOUtils.toString(entity.getContent(), "UTF-8");
            EntityUtils.consume(entity);
            return output;
        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                if (response != null)
                    response.close();
            } catch (IOException e) {
                System.out.println(e);

            }
        }
        return "";
    }


    /**
     * Create a Http Header
     * @param name the key representing the header entry identifier
     * @param value the value used to describe the key
     * @return the Header representing the name value pair
     */
    public static Header createHeader(String name, String value) {
        return new BasicHeader(name, value);
    }
}
