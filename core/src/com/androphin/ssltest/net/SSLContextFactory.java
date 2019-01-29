package com.androphin.ssltest.net;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class SSLContextFactory {

    private static SSLContextFactory instance = null;

    private SSLContextFactory(){}

    public static SSLContextFactory getInstance(){
        if(instance == null) {
            instance = new SSLContextFactory();
        }
        return instance;
    }

    public SSLContext makeContext() throws Exception{
        //KeyManagerFactory defaultKMFactory = KeyManagerFactory.getInstance("X509");
        //KeyManagerFactory defaultKMFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        //KeyManager[] keyManagers = defaultKMFactory.getKeyManagers();

        //FileHandle caFile = new FileHandle("Allchain.crt"); //Order: Root -> Intermediate -> Server
        FileHandle caFile = new FileHandle("cachain.crt"); //Order: Root -> Intermediate
        //FileHandle caFile = new FileHandle("Testchain.crt"); //Order: Intermediate -> Root
        //FileHandle caFile = new FileHandle("TestAllchain.crt"); //Order: Server -> Intermediate -> Root
        //FileHandle caFile = new FileHandle("CAroot.crt"); //Root
        //FileHandle caFile = new FileHandle("CAintermediate.crt"); //Intermediate
        //FileHandle caFile = new FileHandle("server.crt"); //Server
        final KeyStore trustStore = loadPEMTrustStore( caFile.readString() );
        TrustManager[] trustManagers = {new CustomTrustManager(trustStore)};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        //sslContext.init(keyManagers, trustManagers, null);
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }

    private KeyStore loadPEMTrustStore(String certificateString) throws Exception{
        byte[] der = loadPemCertificate(new ByteArrayInputStream(certificateString.getBytes()));
        ByteArrayInputStream derInputStream = new ByteArrayInputStream(der);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(derInputStream);
        String alias = cert.getSubjectX500Principal().getName();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry(alias, cert);

        return trustStore;
    }


    byte[] loadPemCertificate(InputStream certificateStream) throws IOException {
        byte[] der = null;
        BufferedReader br = null;
        try {
            StringBuilder buf = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(certificateStream));

            String line = br.readLine();
            while (line != null){
                if(!line.startsWith("--")){
                    buf.append(line);
                }
                line = br.readLine();
            }
            String pem = buf.toString();
            //uses Base64 of android package: android.util.Base64
            //der = Base64.decode(pem, Base64.DEFAULT);

            //use LibGDX util instead
            der = Base64Coder.decode(pem);

        } finally {
            if(br != null){
                br.close();
            }
        }
        return der;
    }
}
