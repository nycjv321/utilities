package com.nycjv321.utilities.http;

import com.nycjv321.utilities.Builder;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

/**
 * Created by fedora on 11/18/15.
 */
public class CredentialsProviderBuilder extends Builder<CredentialsProvider, CredentialsProviderBuilder> {
    private CredentialsProvider credentialsProvider;

    public static CredentialsProviderBuilder BasicUserNamePasswordBuilder(String userName, String password) {
        CredentialsProviderBuilder builder = new CredentialsProviderBuilder();
        builder.credentialsProvider = new BasicCredentialsProvider();
        builder.credentialsProvider.setCredentials(
                AuthScope.ANY,
                createCredentials(userName, password, AUTHENTICATION_METHOD.USERNAME_PASSWORD)
        );
        return builder;
    }

    private enum AUTHENTICATION_METHOD {
        USERNAME_PASSWORD, NT
    }

    private static Credentials createCredentials(String userName, String password, AUTHENTICATION_METHOD authenticationMethod) {
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

    @Override
    protected CredentialsProviderBuilder getThis() {
        return this;
    }
}
