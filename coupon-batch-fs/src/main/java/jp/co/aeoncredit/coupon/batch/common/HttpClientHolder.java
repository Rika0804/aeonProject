package jp.co.aeoncredit.coupon.batch.common;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLParameters;

/**
*  HttpClientインスタンスホルダ - HttpClientは初回呼出し時に動的に初期化される
*/
public final class HttpClientHolder {
	/** タイムアウト */
	private static int TIME_OUT;

	/**
	*  singleton insntance lazy initializer
	*/
	private static class InstanceHolder {
		private static HttpClientHolder instance;
		static {
			try {
				instance = new HttpClientHolder();
			} catch (GeneralSecurityException | IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	* HttpClientHolderインスタンスの取得
	* @return HttpClientHolderインスタンス
	*/
	public static final HttpClientHolder getInstance() {
		return InstanceHolder.instance;
	}

	/**
	* HttpClientインスタンスの取得
	* @param connectionTimeout 接続タイムアウト
	* @return HttpClientインスタンス
	*/
	public static final HttpClient getClient(int connectionTimeout) {

		// タイムアウト
		TIME_OUT = connectionTimeout;

		return getInstance().get();
	}

	/** HTTP Client */
	private volatile HttpClient client;
	/** スレッドプール */
	private ExecutorService clientExecutor;

	protected HttpClientHolder() throws GeneralSecurityException, IOException {
		create();
	}

	/**
	* HttpClientインスタンスの取得
	* @return HttpClientインスタンス
	* @throws IllegalStateException すでにシャットダウンされている場合
	*/
	public HttpClient get() {
		HttpClient client = this.client;

		if (client == null) {
			throw new IllegalStateException("HttpClient has already been shutdown..");
		}
		return client;
	}

	/** 
	* HTTPクライアントの初期化
	* @throws GeneralSecurityException
	* @throws IOException
	*/
	protected void create() throws GeneralSecurityException, IOException {

		// スレッドプールを初期化
		ExecutorService clientExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				return t;
			}
		});

		// HTTP client を作成
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(TIME_OUT)) // 接続タイムアウト
				.followRedirects(Redirect.NEVER)
				.priority(1)
				.proxy(Builder.NO_PROXY)
				.version(Version.HTTP_1_1)
				.sslParameters(new SSLParameters(null, new String[] { "TLSv1.2" }))
				.executor(clientExecutor)
				.build();

		this.clientExecutor = clientExecutor;
		this.client = client;

		installShutdownHook();

	}

	/**
	* HTTPクライアントをシャットダウンする (一度シャットダウンすると再利用できない)
	*/
	public synchronized void shutdown() {
		this.client = null;
		try {
			clientExecutor.shutdown();
			try {
				clientExecutor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException ignore) {
				/* IGNORE */
			}
			if (!clientExecutor.isTerminated()) {
				clientExecutor.shutdownNow();
			}
		} catch (Exception ignore) {
			/* IGNORE */
		} finally {
			this.clientExecutor = null;
		}
	}

	/**
	* JVM終了時にスレッドプール他を動的にシャットダウンするようにする 
	*/
	private void installShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shutdown()));
	}
}
