package com.androphin.ssltest.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

public class NetJavaImplCopy {

    private SSLContext sslContext;

    static class HttpClientResponse implements Net.HttpResponse {
        private final HttpsURLConnection connection;
        private HttpStatus status;

        public HttpClientResponse (HttpsURLConnection connection) throws IOException {
            this.connection = connection;
            try {
                this.status = new HttpStatus(connection.getResponseCode());
            } catch (IOException e) {
                this.status = new HttpStatus(-1);
            }
        }

        @Override
        public byte[] getResult () {
            InputStream input = getInputStream();

            // If the response does not contain any content, input will be null.
            if (input == null) {
                return StreamUtils.EMPTY_BYTES;
            }

            try {
                return StreamUtils.copyStreamToByteArray(input, connection.getContentLength());
            } catch (IOException e) {
                return StreamUtils.EMPTY_BYTES;
            } finally {
                StreamUtils.closeQuietly(input);
            }
        }

        @Override
        public String getResultAsString () {
            InputStream input = getInputStream();

            // If the response does not contain any content, input will be null.
            if (input == null) {
                return "";
            }

            try {
                return StreamUtils.copyStreamToString(input, connection.getContentLength());
            } catch (IOException e) {
                return "";
            } finally {
                StreamUtils.closeQuietly(input);
            }
        }

        @Override
        public InputStream getResultAsStream () {
            return getInputStream();
        }

        @Override
        public HttpStatus getStatus () {
            return status;
        }

        @Override
        public String getHeader (String name) {
            return connection.getHeaderField(name);
        }

        @Override
        public Map<String, List<String>> getHeaders () {
            return connection.getHeaderFields();
        }

        private InputStream getInputStream () {
            try {
                return connection.getInputStream();
            } catch (IOException e) {
                return connection.getErrorStream();
            }
        }
    }

    private final ExecutorService executorService;
    final ObjectMap<Net.HttpRequest, HttpsURLConnection> connections;
    final ObjectMap<Net.HttpRequest, Net.HttpResponseListener> listeners;

    public NetJavaImplCopy () {
        this(Integer.MAX_VALUE);
    }

    public NetJavaImplCopy (int maxThreads) {
        executorService = new ThreadPoolExecutor(0, maxThreads,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "NetThread");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        connections = new ObjectMap<HttpRequest, HttpsURLConnection>();
        listeners = new ObjectMap<HttpRequest, HttpResponseListener>();
    }

    public void sendHttpRequest (final HttpRequest httpRequest, final HttpResponseListener httpResponseListener) {
        if (httpRequest.getUrl() == null) {
            httpResponseListener.failed(new GdxRuntimeException("can't process a HTTP request without URL set"));
            return;
        }

        try {
            final String method = httpRequest.getMethod();
            URL url;

            if (method.equalsIgnoreCase(HttpMethods.GET)) {
                String queryString = "";
                String value = httpRequest.getContent();
                if (value != null && !"".equals(value)) queryString = "?" + value;
                url = new URL(httpRequest.getUrl() + queryString);
            } else {
                url = new URL(httpRequest.getUrl());
            }

            final HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
            if(connection instanceof HttpsURLConnection){
                try{
                    sslContext = SSLContextFactory.getInstance().makeContext();
                }catch (Exception ex){
                    System.out.println("SSLContext: "+ex);
                }
                ((HttpsURLConnection)connection).setSSLSocketFactory( sslContext.getSocketFactory() );
                //setHostnameVerifier ?

            }
            // should be enabled to upload data.
            final boolean doingOutPut = method.equalsIgnoreCase(HttpMethods.POST) || method.equalsIgnoreCase(HttpMethods.PUT);
            connection.setDoOutput(doingOutPut);
            connection.setDoInput(true);
            connection.setRequestMethod(method);

            HttpURLConnection.setFollowRedirects(httpRequest.getFollowRedirects());

            putIntoConnectionsAndListeners(httpRequest, httpResponseListener, connection);

            // Headers get set regardless of the method
            for (Map.Entry<String, String> header : httpRequest.getHeaders().entrySet())
                connection.addRequestProperty(header.getKey(), header.getValue());

            // Set Timeouts
            connection.setConnectTimeout(httpRequest.getTimeOut());
            connection.setReadTimeout(httpRequest.getTimeOut());

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Set the content for POST and PUT (GET has the information embedded in the URL)
                        if (doingOutPut) {
                            // we probably need to use the content as stream here instead of using it as a string.
                            String contentAsString = httpRequest.getContent();
                            if (contentAsString != null) {
                                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                                try {
                                    writer.write(contentAsString);
                                } finally {
                                    StreamUtils.closeQuietly(writer);
                                }
                            } else {
                                InputStream contentAsStream = httpRequest.getContentStream();
                                if (contentAsStream != null) {
                                    OutputStream os = connection.getOutputStream();
                                    try {
                                        StreamUtils.copyStream(contentAsStream, os);
                                    } finally {
                                        StreamUtils.closeQuietly(os);
                                    }
                                }
                            }
                        }

                        connection.connect();

                        final HttpClientResponse clientResponse = new HttpClientResponse(connection);
                        try {
                            HttpResponseListener listener = getFromListeners(httpRequest);

                            if (listener != null) {
                                listener.handleHttpResponse(clientResponse);
                            }
                            removeFromConnectionsAndListeners(httpRequest);
                        } finally {
                            connection.disconnect();
                        }
                    } catch (final Exception e) {
                        connection.disconnect();
                        try {
                            httpResponseListener.failed(e);
                        } finally {
                            removeFromConnectionsAndListeners(httpRequest);
                        }
                    }
                }
            });
        } catch (Exception e) {
            try {
                httpResponseListener.failed(e);
            } finally {
                removeFromConnectionsAndListeners(httpRequest);
            }
            return;
        }
    }

    public void cancelHttpRequest (HttpRequest httpRequest) {
        HttpResponseListener httpResponseListener = getFromListeners(httpRequest);

        if (httpResponseListener != null) {
            httpResponseListener.cancelled();
            removeFromConnectionsAndListeners(httpRequest);
        }
    }

    synchronized void removeFromConnectionsAndListeners (final HttpRequest httpRequest) {
        connections.remove(httpRequest);
        listeners.remove(httpRequest);
    }

    synchronized void putIntoConnectionsAndListeners (final HttpRequest httpRequest,
                                                      final HttpResponseListener httpResponseListener, final HttpsURLConnection connection) {
        connections.put(httpRequest, connection);
        listeners.put(httpRequest, httpResponseListener);
    }

    synchronized HttpResponseListener getFromListeners (HttpRequest httpRequest) {
        HttpResponseListener httpResponseListener = listeners.get(httpRequest);
        return httpResponseListener;
    }
}
