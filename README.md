# LibgdxSSLTest
Using SSL/TLS self-signed certificates from your own CA to secure the connection between server and client in LibGDX. (with client authentication)

Requirements:
- Webserver running with your self-signed server certificate installed and client authentication enabled
- File of your own CAs to trust (ca-chain.cert), which includes your Root CA and Intermediate CA, and maybe, but not necessarily, server certificate
- Client PKCS#12 keystore file (clientkeystore.p12), which includes the client certificate and client keyfile (client.cert, client.key)

Useful sources:
- Creating your own CA and certificates https://jamielinux.com/docs/openssl-certificate-authority/
- General aspects https://nelenkov.blogspot.com/2011/12/using-custom-certificate-trust-store-on.html
- https://chariotsolutions.com/blog/post/https-with-client-certificates-on/
- https://www.codeproject.com/articles/826045/android-security-implementation-of-self-signed-ssl

Keep in mind:
- PKCS#12 keystore export/import password may needs to be the same as the one used for the client.key (if the client.key is password protected)
- Order of the certificates in CA chain matters
