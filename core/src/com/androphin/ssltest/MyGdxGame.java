package com.androphin.ssltest;

import com.androphin.ssltest.net.SSLContextFactory;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.net.HttpRequestBuilder;

import javax.net.ssl.SSLContext;

public class MyGdxGame extends ApplicationAdapter {

	private GameConstants constants;

	private SSLContext sslContext;
	
	@Override
	public void create () {

		try{
			sslContext = SSLContextFactory.getInstance().makeContext();
		}catch (Exception ex){
			System.out.println("SSLContext: "+ex);
		}

		//Bind SSLcontext to standard libgdx Net implementation
		//HOW?
		//Complete own ClientSocketImpl with HttpsUrlConnection?

		//TESTS
		//normal request (uses default android/desktop-jvm TrustStore)
		HttpRequestBuilder builder = new HttpRequestBuilder();
		builder.newRequest();
		builder.method("GET");
		builder.followRedirects(true);
		builder.url("https://twitter.com");
		Net.HttpRequest normalRequest = builder.build();

		//self-signed cert server request (use custom truststore)
		builder.newRequest();
		builder.method("POST");
		builder.followRedirects(false);
		builder.url("https://localhost:9443");
		Net.HttpRequest serverRequest = builder.build();

		//default listener
		Net.HttpResponseListener responseListenerNormal = new Net.HttpResponseListener() {
			@Override
			public void handleHttpResponse(Net.HttpResponse httpResponse) {
				System.out.print( "Normal request: "+httpResponse.getStatus().getStatusCode()+"\n" );
			}
			@Override
			public void failed(Throwable t) {
				System.out.print("Normal error: "+t+"\n");
			}
			@Override
			public void cancelled() {
				System.out.print("Normal request canceled"+"\n");
			}
		};
		Net.HttpResponseListener responseListenerAppserver = new Net.HttpResponseListener() {
			@Override
			public void handleHttpResponse(Net.HttpResponse httpResponse) {
				System.out.print( "Appserver request: "+httpResponse.getStatus().getStatusCode()+"\n" );
			}
			@Override
			public void failed(Throwable t) {
				System.out.print("Appserver error: "+t+"\n");
			}
			@Override
			public void cancelled() {
				System.out.print("Appserver request canceled"+"\n");
			}
		};

		//fire the requests
		Gdx.net.sendHttpRequest( normalRequest, responseListenerNormal );
		Gdx.net.sendHttpRequest( serverRequest, responseListenerAppserver );
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}
	
	@Override
	public void dispose () {
	}
}
