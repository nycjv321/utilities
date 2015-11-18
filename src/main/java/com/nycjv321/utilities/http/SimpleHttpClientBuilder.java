package com.nycjv321.utilities.http;

import com.nycjv321.utilities.Builder;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created by fedora on 11/18/15.
 */
public class SimpleHttpClientBuilder extends Builder<SimpleHttpClient, SimpleHttpClientBuilder> {
    private CredentialsProvider credentialsProvider;
    private Supplier<CloseableHttpClient> httpClientSupplier;
    private Requests.Timeouts timeouts;

    private SimpleHttpClientBuilder() {
        httpClientSupplier = createHttpClient();
    }

    public static SimpleHttpClientBuilder create() {
        return new SimpleHttpClientBuilder();
    }

    public SimpleHttpClientBuilder credentialProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return getThis();
    }

    public SimpleHttpClientBuilder credentialProvider(CredentialsProviderBuilder credentialsProvider) {
        return credentialProvider(credentialsProvider.build());
    }

    public SimpleHttpClientBuilder timeouts(Requests.Timeouts timeouts) {
        this.timeouts = timeouts;
        return getThis();
    }


    @Override
    public SimpleHttpClient build() {
        if (Objects.nonNull(timeouts)) {
            return new SimpleHttpClient(httpClientSupplier, timeouts);
        } else {
            return new SimpleHttpClient(httpClientSupplier, Requests.Timeouts.getDefault());
        }
    }

    private boolean hasCredentialsProvider() {
        return Objects.nonNull(credentialsProvider);
    }


    private Supplier<CloseableHttpClient> createHttpClient() {
        return () -> {
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
        };
    }

    @Override
    protected SimpleHttpClientBuilder getThis() {
        return this;
    }
}
