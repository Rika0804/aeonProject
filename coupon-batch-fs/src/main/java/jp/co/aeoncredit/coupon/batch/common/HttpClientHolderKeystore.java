package jp.co.aeoncredit.coupon.batch.common;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

/**
*  HttpClientインスタンスホルダ（キーストアあり） - HttpClientは初回呼出し時に動的に初期化される
*/
public final class HttpClientHolderKeystore {
	/** クライアント用のキーストアパス */
	private static String MY_TRUSTED_KEYSTORE;
	/** キーストアパスワード */
	private static char[] MY_TRUSTED_KEYSTORE_PASS;
	/** タイムアウト */
	private static int TIME_OUT;

	/**
	 * logger
	 */
	private Logger logger = LoggerFactory.getInstance().getLogger(HttpClientHolderKeystore.class);

	/**
	*  singleton insntance lazy initializer
	*/
	private static class InstanceHolder {
		private static HttpClientHolderKeystore instance;
		static {
			try {
				instance = new HttpClientHolderKeystore();
			} catch (GeneralSecurityException | IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	* HttpClientHolderインスタンスの取得
	* @return HttpClientHolderインスタンス
	*/
	public static final HttpClientHolderKeystore getInstance() {
		return InstanceHolder.instance;
	}

	/**
	* HttpClientインスタンスの取得
	* @param connectionTimeout 接続タイムアウト
	* @param keystorePath キーストアパス
	* @param keystorePass キーストアパスワード
	* @return HttpClientインスタンス
	*/
	public static final HttpClient getClient(int connectionTimeout, String keystorePath, String keystorePass) {

		// クライアント用のキーストアパス
		MY_TRUSTED_KEYSTORE = keystorePath;
		// キーストアパスワード
		MY_TRUSTED_KEYSTORE_PASS = keystorePass.toCharArray();
		// タイムアウト
		TIME_OUT = connectionTimeout;

		return getInstance().get();
	}

	/** HTTP Client */
	private volatile HttpClient client;
	/** スレッドプール */
	private ExecutorService clientExecutor;

	protected HttpClientHolderKeystore() throws GeneralSecurityException, IOException {
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

		logger.debug("【HttpClientHolderKeystore.create()】キーストアパス : " + MY_TRUSTED_KEYSTORE);
		logger.debug("【HttpClientHolderKeystore.create()】キーストアパスワード : " + MY_TRUSTED_KEYSTORE_PASS);

		// 信頼する client key store を用いて Trust Manager を初期化
		KeyStore ks = KeyStore.getInstance(Paths.get(MY_TRUSTED_KEYSTORE).toFile(), MY_TRUSTED_KEYSTORE_PASS);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
		tmf.init(ks);

		// 作成された Trust Manager を紐付けた SSL context を初期化
		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(null, tmf.getTrustManagers(), null);

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
				.sslContext(ssl)
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
