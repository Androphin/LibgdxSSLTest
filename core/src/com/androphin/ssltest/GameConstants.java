package com.androphin.ssltest;

public class GameConstants {
    public static final String caCertFilename = "cachain.csr"; //chained certificates PEM encoded (order: Root -> Intermediate [-> Server])
    public static final String clientKeystoreFilename = "";
    public static final String clientKeystorePassword = "";

    public static final String serverHostname = "localhost";
    public static final int serverPort        = 9443;
}
