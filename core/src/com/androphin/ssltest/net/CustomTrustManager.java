package com.androphin.ssltest.net;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class CustomTrustManager implements X509TrustManager {

    private final X509TrustManager defaultTM;
    private final KeyStore trustStore;

    public CustomTrustManager(KeyStore trustStore) throws NoSuchAlgorithmException, KeyStoreException {
        this.trustStore = trustStore;

        TrustManagerFactory defaultTMFactory = TrustManagerFactory.getInstance("X509");
        defaultTMFactory.init((KeyStore) null);

        TrustManager[] defaultTrustManagers = defaultTMFactory.getTrustManagers();
        defaultTM = (X509TrustManager) defaultTrustManagers[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTM.checkServerTrusted(chain, authType);
        } catch (CertificateException originalException){
            try {
                //check if chain is in the right order ( Root -> Intermediate -> Server Cert)
                X509Certificate[] reorderedChain = reorderCertificateChain(chain);
                CertPathValidator validator = CertPathValidator.getInstance("PKIX");
                CertificateFactory factory = CertificateFactory.getInstance("X509");
                CertPath certPath = factory.generateCertPath( Arrays.asList(reorderedChain) );
                PKIXParameters params = new PKIXParameters(trustStore);
                params.setRevocationEnabled(false);
                validator.validate(certPath,params);
            }catch(Exception ex) {
                throw originalException;
            }
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }


    private X509Certificate[] reorderCertificateChain(X509Certificate[] chain) {

        X509Certificate[] reorderedChain = new X509Certificate[chain.length];
        List<X509Certificate> certificates = Arrays.asList(chain);

        int position = chain.length - 1;
        X509Certificate rootCert = findRootCert(certificates);
        reorderedChain[position] = rootCert;

        X509Certificate cert = rootCert;
        while((cert = findSignedCert(cert, certificates)) != null && position > 0) {
            reorderedChain[--position] = cert;
        }

        return reorderedChain;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the root certificate in a possibly out-of-order certificate chain.
     * @param certificates the certificate change, possibly out-of-order
     * @return the root certificate, if any, that was found in the list of certificates
     */
    private X509Certificate findRootCert(List<X509Certificate> certificates) {
        X509Certificate rootCert = null;

        for(X509Certificate cert : certificates) {
            X509Certificate signer = findSigner(cert, certificates);
            if(signer == null || signer.equals(cert)) { // no signer present, or self-signed
                rootCert = cert;
                break;
            }
        }

        return rootCert;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the first certificate in the list of certificates that is signed by the sigingCert.
     */
    private X509Certificate findSignedCert(X509Certificate signingCert, List<X509Certificate> certificates) {
        X509Certificate signed = null;

        for(X509Certificate cert : certificates) {
            Principal signingCertSubjectDN = signingCert.getSubjectDN();
            Principal certIssuerDN = cert.getIssuerDN();
            if(certIssuerDN.equals(signingCertSubjectDN) && !cert.equals(signingCert)) {
                signed = cert;
                break;
            }
        }

        return signed;
    }

    /**
     * A helper method for certificate re-ordering.
     * Finds the certificate in the list of certificates that signed the signedCert.
     */
    private X509Certificate findSigner(X509Certificate signedCert, List<X509Certificate> certificates) {
        X509Certificate signer = null;

        for(X509Certificate cert : certificates) {
            Principal certSubjectDN = cert.getSubjectDN();
            Principal issuerDN = signedCert.getIssuerDN();
            if(certSubjectDN.equals(issuerDN)) {
                signer = cert;
                break;
            }
        }

        return signer;
    }
}
