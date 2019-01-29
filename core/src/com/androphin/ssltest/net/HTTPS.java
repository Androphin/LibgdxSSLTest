package com.androphin.ssltest.net;
/*
    Keystore = Client-Cert + Client private key
    Truststore = eigener CA vertrauen

    Keystore erstellen und PKCS12 reinladen
    CustomTrustStore für die Zertifikate
    Beides dem SSLContext übergeben


    Unklar
    2 eigenständige Verbindungen/Sockets nutzen?
        1. Appserver Verbindung für Gamelogic
        2. public trusted (twitter.com etc.)

    Fragen
    Server Zertifikat mit in die CA-chain? -> Optional
    Reihenfolge? Root -> Intermediate -> Server oder andersrum?
    CA-chain als extra .pem encodierte Datei oder mit im ClientKeystore.p12? -> eher extra zum keystore
    Muss ClientKeystore.p12 export passwort das gleiche sein wie der Client private key damit Java die DB öffnen kann? -> abhängig von Anwendung; besser ja

     */

public class HTTPS {

}