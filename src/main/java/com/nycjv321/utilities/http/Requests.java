package com.nycjv321.utilities.http;

/**
 * Created by fedora on 11/18/15.
 */
public class Requests {
    public static class Timeouts {
        private static final Timeouts defaultTimeouts;

        static {
            defaultTimeouts = new Timeouts();
            defaultTimeouts.setConnectionRequestTimeout(10000);
            defaultTimeouts.setConnectTimeout(10000);
            defaultTimeouts.setSocketTimeout(10000);
        }

        private int socketTimeout;
        private int connectTimeout;
        private int connectionRequestTimeout;

        public static Timeouts getDefault() {
            return defaultTimeouts;
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
