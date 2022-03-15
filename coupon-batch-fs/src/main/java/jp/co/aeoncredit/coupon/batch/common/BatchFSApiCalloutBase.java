package jp.co.aeoncredit.coupon.batch.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.dto.LoginInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.LoginOutputDTO;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.ServiceType;
import jp.co.aeoncredit.coupon.dao.custom.ExternalApiUserDAOCustomize;
import jp.co.aeoncredit.coupon.entity.ExternalApiUser;
import jp.co.aeoncredit.coupon.entity.ExternalApiUserPK;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

public class BatchFSApiCalloutBase extends BatchDBAccessBase {
	/** 認証トークン取得の戻り値 */
	protected enum AuthTokenResult {
		SUCCESS, FAILURE, MAINTENANCE
	}

	/** 統合API URL */
	protected final String integrationUrl;
	/** リバースプロキシ URL */
	protected final String reverseProxyUrl;
	/** エンドユーザ用API URL */
	protected final String userApiUrl;

	/** 認証トークン */
	protected String authToken;

	/** ユーザエージェント */
	protected String userAgent;

	/** ユーザエージェント（疑似ログイン） */
	protected String userAgentBatchApi;

	/** バッチID */
	private static final String BATCH_ID = "B18BC001";

	/** エラーメッセージ用 */
	private static final String FANHIP_API = "FANSHIP API";

	/** ログインAPIのURL */
	private static final String AUTH_TOKEN_URL = "fs.get.auth.token.url";
	/** FS API 失敗時のAPI実行リトライ回数 */
	private static final String RETRY_COUNT = "fs.get.auth.token.retry.count";
	/** FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒) */
	private static final String SLEEP_TIME = "fs.get.auth.token.retry.sleep.time";
	/** FS API発行時のリクエストタイムアウト期間(秒) */
	private static final String REQUEST_TIMEOUT_DURATION = "fs.get.auth.token.request.timeout.duration";
	/** FS API発行時の接続タイムアウト期間(秒) */
	private static final String CONNECTION_TIMEOUT_DURATION = "fs.get.auth.token.connection.timeout.duration";
	/** 認証トークンの使用可能回数 */
	private static final String COUNT_LIMIT = "fs.get.auth.token.count.limit";
	/** 認証トークンの使用可能秒数 */
	private static final String TIME_LIMIT = "fs.get.auth.token.time.limit";
	/** ユーザエージェント */
	private static final String USER_AGENT = "fs.get.auth.token.user.agent";
	/** ユーザエージェント（疑似ログイン） */
	private static final String USER_AGENT_BATCH_API = "fs.get.auth.token.batchapi.user.agent";
	/** レスポンスタイムワーニングメッセージ時間 */
	private static final String WARNING_MESSAGE_DURATION = "fs.get.auth.token.batchapi.request.timeout.warning.message.duration";
	/** レスポンスタイムワーニングメッセージ対象 */
	private static final String WARNING_MESSAGE_TARGET = "fs.get.auth.token.batchapi.request.timeout.warning.message.target";
	/** APIリクエストデータログ非表示対象 */
	private static final String REQUEST_LOG_HIDE = "fs.get.auth.token.batchapi.request.log.hide";
	/** APIレスポンスデータログ非表示対象 */
	private static final String RESPONSE_LOG_HIDE = "fs.get.auth.token.batchapi.response.log.hide";

	/** ログ */
	private Logger logger = getLogger();

	/** 外部API発行ユーザーテーブルDAO */
	@Inject
	private ExternalApiUserDAOCustomize dao;

	/** ログインAPIのURL */
	private final String authTokenUrl;
	/** ユーザID */
	private final String userId;
	/** リトライ回数 */
	private final int retryCount;
	/** スリープ時間 */
	private final int sleepTime;
	/** リクエストタイムアウト */
	private final int requestTimeoutDuration;
	/** 接続タイムアウト */
	private final int connectionTimeoutDuration;
	/** 認証トークンの使用可能回数 */
	private final int countLimit;
	/** 認証トークンの使用可能秒数 */
	private final int timeLimit;
	/** レスポンスタイムワーニングメッセージ時間 */
	private final int warningMessageDuration;
	/** レスポンスタイムワーニングメッセージ対象 */
	private final String warningMessageTarget;
	/** APIリクエストデータログ非表示対象 */
	private final String requestLogHide;
	/** APIレスポンスデータログ非表示対象 */
	private final String responseLogHide;

	/** FANSHIPの統合API KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
	private final String integrationKeystoreFlag;
	/** FANSHIPのリバースプロキシAPI KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
	private final String reverseProxyKeystoreFlag;
	/** FANSHIPのエンドユーザ用API KeyStore利用フラグ(0:KeyStore未利用　1:KeyStore利用) */
	private final String endUserKeystoreFlag;
	/** クライアント用のキーストアパス */
	private final String keystorePath;
	/** キーストアパスワード */
	private final String keystorePass;

	/** KeyStore利用 */
	private static final String KEYSTORE_USED = "1";

	private BatchLogger batchLogger;
	private ObjectMapper mapper = new ObjectMapper();

	/** HttpClient */
	private HttpClient httpClient;

	/** HttpClient(キーストアあり) */
	private HttpClient httpClientKeystore;

	/** FS API呼出結果 */
	protected enum FsApiCallResult {
		/** 成功 */
		SUCCESS,
		/** FANSHIPメンテナンス(503)  */
		FANSHIP_MAINTENANCE,
		/** 認証エラー(401) */
		AUTHENTICATION_ERROR,
		/** リクエストが多すぎる(429) */
		TOO_MANY_REQUEST,
		/** クライアントエラー(4xx/401,429以外) */
		CLIENT_ERROR,
		/** サーバエラー(5xx/503以外) */
		SERVER_ERROR,
		/** タイムアウト(HttpTimeoutException) */
		TIMEOUT,
		/** その他エラー */
		OTHERS_ERROR
	}

	/** リトライ区分 */
	protected enum RetryKbn {
		/** サーバエラー「5xx」（503以外） */
		SERVER_ERROR,
		/** タイムアウト */
		TIMEOUT,
		/** サーバエラー「5xx」（503以外）とタイムアウト */
		SERVER_ERROR_TIMEOUT,
		/** リトライなし */
		NONE
	}

	/** HTTPメソッド */
	protected enum HttpMethodType {
		/** POST */
		POST,
		/** POST：認証ヘッダなし（ログインAPI、IDリンク操作（削除）、IDリンク操作（紐付けを行う）用） */
		POST_NOT_AUTH,
		/** POST：認証ヘッダなし、疑似ログイン用User-Agent（疑似ログインAPI用） */
		POST_PSEUDO_LOGIN,
		/** PATCH */
		PATCH,
		/** DELETE */
		DELETE,
		/** GET */
		GET,
		/** PUT */
		PUT;
	}

	/** Content-Type */
	protected enum ContentType {
		/** application/json */
		APPLICATION_JSON,
		/** application/json;charset=UTF-8 */
		APPLICATION_JSON_CHARSET,
		/** 未設定(DELETEの場合) */
		NONE;
	}

	/** 認証トークンヘッダ */
	protected enum TokenHeaderType {
		/** Authorization */
		AUTHORIZATION,
		/** X-POPINFO-MAPI-TOKEN */
		X_POPINFO_MAPI_TOKEN,
		/** Authorization:[SP]PopinfoLogin[SP]auth=[AUTH_TOKEN] */
		AUTHORIZATION_POPINFOLOGIN,
		/** 未設定(ログインAPI、擬似ログイン（ID Link）API、IDリンク操作（削除）API、IDリンク操作（紐付けを行う）API) */
		NONE;
	}

	/** 再認証リトライ区分 */
	protected enum ReAuthRetryKbn {
		/** 初回 */
		NONE,
		/** リトライ(DBからトークン再取得) */
		RETRY_DB,
		/** リトライ(FSからトークン再取得) */
		RETRY_FS
	}

	/**
	 * コンストラクタ
	 */
	public BatchFSApiCalloutBase() {
		// プロパティファイルから情報を取得する。
		BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);
		Properties properties = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		// ログインAPIのURL
		this.authTokenUrl = properties.getProperty(AUTH_TOKEN_URL);
		// リトライ回数
		this.retryCount = Integer.parseInt(properties.getProperty(RETRY_COUNT));
		// スリープ時間
		this.sleepTime = Integer.parseInt(properties.getProperty(SLEEP_TIME));
		// リクエストタイムアウト
		this.requestTimeoutDuration = Integer.parseInt(properties.getProperty(REQUEST_TIMEOUT_DURATION));
		// 接続タイムアウト
		this.connectionTimeoutDuration = Integer.parseInt(properties.getProperty(CONNECTION_TIMEOUT_DURATION));
		// 認証トークンの使用可能回数
		this.countLimit = Integer.parseInt(properties.getProperty(COUNT_LIMIT));
		// 認証トークンの使用可能秒数
		this.timeLimit = Integer.parseInt(properties.getProperty(TIME_LIMIT));
		// ユーザーエージェント
		String userAgentFormat = properties.getProperty(USER_AGENT);
		// ユーザーエージェント（疑似ログイン）
		String userAgentBatchApiFormat = properties.getProperty(USER_AGENT_BATCH_API);
		// レスポンスタイムワーニングメッセージ時間
		this.warningMessageDuration = Integer.parseInt(properties.getProperty(WARNING_MESSAGE_DURATION));
		//レスポンスタイムワーニングメッセージ対象
		this.warningMessageTarget = properties.getProperty(WARNING_MESSAGE_TARGET);
		// APIリクエストデータログ非表示対象
		this.requestLogHide = properties.getProperty(REQUEST_LOG_HIDE);
		// APIレスポンスデータログ非表示対象
		this.responseLogHide = properties.getProperty(RESPONSE_LOG_HIDE);

		// 環境変数から情報を取得する。
		// 統合API URL
		this.integrationUrl = System.getenv(Constants.ENV_FS_INTEGRATION_URL);
		// リバースプロキシ URL
		this.reverseProxyUrl = System.getenv(Constants.ENV_FS_REVERSE_PROXY_URL);
		// エンドユーザ用API URL
		this.userApiUrl = System.getenv(Constants.ENV_FS_END_USER_API_URL);
		// ユーザID
		this.userId = System.getenv(Constants.ENV_FS_LOGIN_API_USER_ID);
		// FANSHIPの統合API KeyStore利用フラグ
		this.integrationKeystoreFlag = System.getenv(Constants.ENV_FS_INTEGRATION_KEYSTORE_FLAG);
		// FANSHIPのリバースプロキシAPI KeyStore利用フラグ
		this.reverseProxyKeystoreFlag = System.getenv(Constants.ENV_FS_REVERSE_PROXY_KEYSTORE_FLAG);
		// FANSHIPのエンドユーザ用API KeyStore利用フラグ
		this.endUserKeystoreFlag = System.getenv(Constants.ENV_FS_END_USER_API_KEYSTORE_FLAG);
		// クライアント用のキーストアパス
		this.keystorePath = System.getenv(Constants.ENV_FS_KEYSTORE_PATH);
		// キーストアパスワード
		this.keystorePass = System.getenv(Constants.ENV_FS_KEYSTORE_PSWD);

		logger.debug("【BatchFSApiCalloutBase()】キーストア利用フラグ(ENV取得) : "
				+ "統合API = " + integrationKeystoreFlag
				+ ", リバースプロキシAPI = " + reverseProxyKeystoreFlag
				+ ", エンドユーザ用API = " + endUserKeystoreFlag);
		logger.debug("【BatchFSApiCalloutBase()】キーストアパス(ENV取得) : " + keystorePath);
		logger.debug("【BatchFSApiCalloutBase()】キーストアパスワード(ENV取得) : " + keystorePass);

		// ユーザーエージェント取得
		String os = System.getProperty("os.name");
		String version = System.getProperty("os.version");
		this.userAgent = userAgentFormat.replace("{os}", os).replace("{version}", version);
		this.userAgentBatchApi = userAgentBatchApiFormat.replace("{os}", os).replace("{version}", version);
	}

	/**
	 * 認証トークンを取得する
	 * @param jobid ジョブID
	 * @return  SUCCESS：認証トークン取得成功、FAILURE：認証トークン取得失敗、MAINTENANCE：FANSHIPメンテナンス中
	 */
	protected AuthTokenResult getAuthToken(String jobid) {
		return this.getAuthToken(jobid, false);
	}

	/**
	 * 認証トークンを取得する
	 * @param jobid ジョブID
	 * @param isReauthentication 認証が必要か？
	 * @return SUCCESS：認証トークン取得成功、FAILURE：認証トークン取得失敗、MAINTENANCE：FANSHIPメンテナンス中
	 */
	protected AuthTokenResult getAuthToken(String jobid, boolean isAuthentication) {
		AuthTokenResult result = AuthTokenResult.FAILURE;

		batchLogger = new BatchLogger(jobid);

		// 処理開始メッセージを出力する。
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB001.toString(), "認証トークン取得が開始しました。"));

		try {
			// 外部API発行ユーザーテーブルよりレコード取得
			ExternalApiUser externalApiUser = getExternalApiUser();

			if (externalApiUser.getFsAuthToken() == null || isAuthentication) {
				// リクエストパラメータ取得
				String param = getRequestParam(externalApiUser.getApiPassword());

				// FS API発行
				result = calloutApi(jobid, param, externalApiUser);
			} else {
				this.authToken = externalApiUser.getFsAuthToken();
				result = AuthTokenResult.SUCCESS;
			}
		} catch (Exception e) {
			log.error(createErrorMessage(null, null, e));
		} finally {
			// 処理終了メッセージを出力する。
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB002.toString(), "認証トークン取得が終了しました。"));
		}

		return result;
	}

	/**
	 * FS API発行
	 * @param jobid ジョブID
	 * @param param リクエストパラメータ
	 * @param externalApiUser 外部API発行ユーザーテーブルのレコード
	 * @return SUCCESS：認証トークン取得成功、FAILURE：認証トークン取得失敗、MAINTENANCE：FANSHIPメンテナンス中
	 * @throws IOException
	 */
	private AuthTokenResult calloutApi(String jobid, String param, ExternalApiUser externalApiUser) throws IOException {
		AuthTokenResult result = AuthTokenResult.FAILURE;

		String url = this.reverseProxyUrl + this.authTokenUrl;
		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

		// API発行
		FsApiCallResponse fsApiCallResponse = callFanshipApi(
				jobid, "ログインAPI", url, param, HttpMethodType.POST_NOT_AUTH, ContentType.APPLICATION_JSON,
				TokenHeaderType.NONE, successHttpStatusList, retryCount, sleepTime,
				requestTimeoutDuration, RetryKbn.TIMEOUT);

		if (fsApiCallResponse.fsApiCallResult == FsApiCallResult.SUCCESS) {
			LoginOutputDTO dto = mapper.readValue(fsApiCallResponse.response.body(), LoginOutputDTO.class);
			if (dto.getStatus().equals("OK")) {
				// 認証トークン取得
				this.authToken = dto.getResult().getAuthToken();

				// 外部API発行ユーザーテーブル更新
				updateExternalApiUser(jobid, externalApiUser);

				result = AuthTokenResult.SUCCESS;
			} else {
				log.error(createErrorMessage(fsApiCallResponse.response.statusCode(), dto, null));
			}
		} else if (fsApiCallResponse.fsApiCallResult.equals(FsApiCallResult.FANSHIP_MAINTENANCE)
				|| fsApiCallResponse.fsApiCallResult.equals(FsApiCallResult.TOO_MANY_REQUEST)) {
			result = AuthTokenResult.MAINTENANCE;
		}

		return result;
	}

	/**
	 * リクエストパラメータを取得する
	 * @param パスワード
	 * @return リクエストパラメータ
	 * @throws JsonProcessingException
	 */
	private String getRequestParam(String password) throws JsonProcessingException {
		LoginInputDTO dto = new LoginInputDTO();
		dto.setUserId(this.userId);
		dto.setPassword(password);
		dto.setTimeLimit(this.timeLimit);
		dto.setCountLimit(this.countLimit);

		return mapper.writeValueAsString(dto);
	}

	/**
	 * 外部API発行ユーザーテーブルより条件に一致したレコードを取得する
	 * @param userId ユーザID
	 * @return 外部API発行ユーザーテーブルのレコード
	 */
	private ExternalApiUser getExternalApiUser() {
		ExternalApiUserPK pk = new ExternalApiUserPK();
		pk.setServiceType(ServiceType.FS_API.getValue());
		pk.setApiUserId(this.userId);

		return dao.findById(pk).get();
	}

	/**
	 * 外部API発行ユーザーテーブルを更新する
	 * @param jobid ジョブID
	 * @param externalApiUser 外部API発行ユーザーテーブルのレコード
	 */
	private void updateExternalApiUser(String jobid, ExternalApiUser externalApiUser) {
		try {
			// トランザクションの開始
			transactionBegin(jobid);

			externalApiUser.setFsAuthToken(this.authToken);
			externalApiUser.setUpdateDate(DateUtils.now());
			externalApiUser.setUpdateUserId(jobid);

			dao.update(externalApiUser);

			// トランザクションのコミット
			transactionCommit(jobid);
		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(jobid);

			throw e;
		}
	}

	/**
	 * エラーメッセージを作成する
	 * @param httpStatusCode HTTPステータスコード
	 * @param dto レスポンスDTO
	 * @param e Exception
	 * @return エラーメッセージ
	 */
	private String createErrorMessage(Integer httpStatusCode, LoginOutputDTO dto, Exception e) {
		String errorMessage = "";

		if (httpStatusCode != null) {
			if (dto != null) {
				errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
						FANHIP_API, String.format("HTTPステータスコード =%s, エラーコード = %s, エラーメッセージ = %s",
								httpStatusCode, dto.getError().getCode(), dto.getError().getMessage()));
			} else {
				errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
						FANHIP_API, String.format("HTTPステータスコード =%s", httpStatusCode));
			}
		} else {
			if (e != null) {
				errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
						FANHIP_API, String.format("%s : %s", e.getClass().getName(), e.getMessage()));
			} else {
				errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
						FANHIP_API, "リトライ超過");
			}
		}

		return batchLogger.createMsg(BusinessMessageCode.B18MB907.toString(), errorMessage);
	}

	/**
	 * FANSHIP APIを呼び出し、結果を返却する
	 * 
	 * @param batchId バッチID
	 * @param apiName API(処理)名 ※ログ出力用
	 * @param url APIのURL
	 * @param param パラメータ
	 * @param httpMethodType HTTPメソッド
	 * @param contentType Content-Type
	 * @param tokenHeaderType 認証トークンヘッダ
	 * @param successHttpStatusList 正常HTTPステータスコードリスト(通常3桁の数値、正常値が「2xx」等の場合「2」を設定)
	 * @param apiRetryCount リトライ回数(各バッチのプロパティ値)
	 * @param retrySleepTime リトライ時スリープ時間(各バッチのプロパティ値)
	 * @param requestTimeout リクエストタイムアウト(各バッチのプロパティ値)
	 * @param retryKbn リトライ区分
	 * 
	 * @return FS APIレスポンス
	 */
	public FsApiCallResponse callFanshipApi(
			String batchId,
			String apiName,
			String url,
			String param,
			HttpMethodType httpMethodType,
			ContentType contentType,
			TokenHeaderType tokenHeaderType,
			List<Integer> successHttpStatusList,
			int apiRetryCount,
			int retrySleepTime,
			int requestTimeout,
			RetryKbn retryKbn) {

		// batchLogger生成
		if (batchLogger == null) {
			batchLogger = new BatchLogger(batchId);
		}

		// HttpClient生成
		createHttpClient(url);

		// HttpRequest生成
		HttpRequest request = createHttpRequest(url, param, httpMethodType, contentType, tokenHeaderType,
				requestTimeout);

		// API呼び出し
		FsApiCallResponse fsApiCallResponse = callApi(request, apiName, url, param, successHttpStatusList,
				apiRetryCount, retrySleepTime, retryKbn, ReAuthRetryKbn.NONE, batchId, httpMethodType);

		// 認証エラーの場合(ログインAPI、IDリンク操作（削除）API、IDリンク操作（紐付けを行う）API、疑似ログインAPI以外)
		if (!HttpMethodType.POST_NOT_AUTH.equals(httpMethodType)
				&& !HttpMethodType.POST_PSEUDO_LOGIN.equals(httpMethodType)
				&& FsApiCallResult.AUTHENTICATION_ERROR.equals(fsApiCallResponse.getFsApiCallResult())) {

			// 外部API発行ユーザーテーブルよりレコード取得
			ExternalApiUser externalApiUser = getExternalApiUser();
			if (externalApiUser.getFsAuthToken() != null) {
				// 認証トークンが存在する場合

				// 認証トークンが変更されている場合
				if (!this.authToken.equals(externalApiUser.getFsAuthToken())) {

					// 認証トークン設定
					this.authToken = externalApiUser.getFsAuthToken();

					// HttpRequest生成(再作成)
					request = createHttpRequest(url, param, httpMethodType, contentType, tokenHeaderType,
							requestTimeout);

					// API呼び出し(再実施)
					fsApiCallResponse = callApi(request, apiName, url, param, successHttpStatusList,
							apiRetryCount, retrySleepTime, retryKbn, ReAuthRetryKbn.RETRY_DB, batchId, httpMethodType);

					// 認証エラー以外の場合
					if (!FsApiCallResult.AUTHENTICATION_ERROR.equals(fsApiCallResponse.getFsApiCallResult())) {
						return fsApiCallResponse;
					}
				}
			}

			// DBから再取得した認証トークンでも認証エラーの場合、FSから認証トークンを再取得し再度API連携実施
			AuthTokenResult authTokenResult = getAuthToken(batchId, true);

			if (AuthTokenResult.SUCCESS.equals(authTokenResult)) {
				// 【B18BC001_認証トークン取得】の戻り値が「成功（enumで定義）」の場合 は、再度APIを呼び出す。

				// HttpRequest生成(再々作成)
				request = createHttpRequest(url, param, httpMethodType, contentType, tokenHeaderType,
						requestTimeout);

				// API呼び出し(再々実施)
				fsApiCallResponse = callApi(request, apiName, url, param, successHttpStatusList,
						apiRetryCount, retrySleepTime, retryKbn, ReAuthRetryKbn.RETRY_FS, batchId, httpMethodType);

			} else if (AuthTokenResult.MAINTENANCE.equals(authTokenResult)) {
				// 【B18BC002_認証トークン取得】の戻り値が「メンテナンス（enumで定義）」(FANSHIPメンテナンス)の場合 

				// FANSHIPメンテナンス
				fsApiCallResponse.setFsApiCallResult(FsApiCallResult.FANSHIP_MAINTENANCE);

			} else {
				// 【B18BC002_認証トークン取得】の戻り値が「失敗（enumで定義）」の場合 

				// その他エラー
				fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);

			}

		}

		return fsApiCallResponse;
	}

	/**
	 * API呼び出し
	 * 
	 * @param request HttpRequest
	 * @param apiName API(処理)名 ※ログ出力用
	 * @param url APIのURL
	 * @param param パラメータ
	 * @param successHttpStatusList 正常HTTPステータスコードリスト
	 * @param apiRetryCount リトライ回数
	 * @param retrySleepTime リトライ時スリープ時間
	 * @param retryKbn リトライ区分
	 * @param reAuthRetryKbn 再認証リトライ区分
	 * @param batchId バッチID
	 * @param httpMethodType HTTPメソッド
	 * 
	 * @return FS APIレスポンス
	 */
	private FsApiCallResponse callApi(HttpRequest request, String apiName, String url, String param,
			List<Integer> successHttpStatusList, int apiRetryCount, int retrySleepTime,
			RetryKbn retryKbn, ReAuthRetryKbn reAuthRetryKbn, String batchId, HttpMethodType httpMethodType) {

		FsApiCallResponse fsApiCallResponse = new FsApiCallResponse();
		HttpResponse<String> response = null;

		// リトライ回数分繰り返す
		for (int i = 0; i <= apiRetryCount; i++) {

			// 初期化
			fsApiCallResponse = new FsApiCallResponse();

			try {
				if (i > 0) {
					// リトライ前にスリープ
					Thread.sleep(retrySleepTime);
				}

				// API呼び出しログ出力
				if (this.requestLogHide.contains(batchId)
						|| url.contains(this.authTokenUrl)) {
					// APIリクエストデータログ非表示対象の場合、データが大きいため非表示
					// ログインAPIの場合、ID/パスワードのため非表示
					param = "[非表示]";
				}
				log.info(batchLogger.createMsg(BusinessMessageCode.B18MB010, url, getHttpMethodName(httpMethodType),
						param));
				log.info("request.headers()：" + request.headers().toString());

				// API連携
				LocalDateTime execTimeStart = LocalDateTime.now();
				if (chackKeystoreUsed(url)) {
					// Keystore情報が設定されており、KeyStore利用の場合
					log.debug("HttpClient:キーストア利用");
					response = httpClientKeystore.send(request, BodyHandlers.ofString());

				} else {
					// Keystore情報が設定されていない、または、KeyStore未利用の場合
					log.debug("HttpClient:キーストア未利用");
					response = httpClient.send(request, BodyHandlers.ofString());

				}
				LocalDateTime execTimeEnd = LocalDateTime.now();

				// レスポンスタイムワーニングメッセージ対象の場合
				if (this.warningMessageTarget.contains(batchId) && !url.contains(this.authTokenUrl)) {
					long seconds = ChronoUnit.SECONDS.between(execTimeStart, execTimeEnd);
					if (seconds >= this.warningMessageDuration) {
						// レスポンスタイムワーニングメッセージ時間を超えている場合

						// メッセージを出力（FSAPIレスポンスに%s秒かかりました。必要に応じてレスポンスタイムアウト時間の見直しを行ってください。）
						String msg = String.format(
								BusinessMessage.getMessages(BusinessMessageCode.B18MB102.toString()), seconds);
						logger.warn(batchLogger.createMsg(BusinessMessageCode.B18MB102.toString(), msg));
					}
				}

				// FSAPIレスポンスセット
				fsApiCallResponse.setResponse(response);

				// API結果判定
				boolean retryFlg = checkApiResponse(fsApiCallResponse, successHttpStatusList, apiName, apiRetryCount, i,
						retryKbn, reAuthRetryKbn, batchId, url);
				if (!retryFlg) {
					break;
				}

			} catch (HttpTimeoutException e) {

				if (RetryKbn.TIMEOUT.equals(retryKbn)
						|| RetryKbn.SERVER_ERROR_TIMEOUT.equals(retryKbn)) {
					// リトライ対象の場合

					if (i >= apiRetryCount) {
						// リトライ回数が設定ファイルで指定した値を上回った場合

						// メッセージを出力(error)
						writeLog(apiName, null, Constants.OVER_RETRY_COUNT, true, null);

						// タイムアウト(HttpTimeoutException) 
						fsApiCallResponse.setFsApiCallResult(FsApiCallResult.TIMEOUT);

						break;
					}

					// メッセージを出力(info)
					writeLog(apiName, null, Constants.RETRY_COUNT + String.valueOf(i + 1), false, null);

					// レスポンスログ出力
					writeResponseLog(fsApiCallResponse, batchId, url);

				} else {
					// リトライ対象外の場合

					// メッセージを出力(error)
					writeLog(apiName, null, Constants.TIMEOUT, true, null);

					// タイムアウト(HttpTimeoutException)
					fsApiCallResponse.setFsApiCallResult(FsApiCallResult.TIMEOUT);

					break;
				}

			} catch (Exception e) {

				// メッセージを出力(error)
				writeLog(apiName, null, Constants.OTHERS_ERROR, true, e);

				// その他エラー
				fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);

				break;

			}
		}

		// レスポンスログ出力
		writeResponseLog(fsApiCallResponse, batchId, url);

		return fsApiCallResponse;
	}

	/**
	 * API結果判定
	 * 
	 * @param fsApiResponse FSAPIレスポンス
	 * @param successHttpStatusList 正常HTTPステータスコードリスト
	 * @param apiName API(処理)名
	 * @param apiRetryCount リトライ回数
	 * @param retry 現在のリトライ回数
	 * @param retryKbn リトライ区分
	 * @param reAuthRetryKbn 再認証リトライ区分
	 * @param batchId バッチID
	 * @param url APIのURL
	 * 
	 * @return true:リトライ false:リトライしない
	 */
	private boolean checkApiResponse(FsApiCallResponse fsApiCallResponse, List<Integer> successHttpStatusList,
			String apiName, int apiRetryCount, int retry, RetryKbn retryKbn, ReAuthRetryKbn reAuthRetryKbn,
			String batchId, String url) {

		int statusCode = fsApiCallResponse.getResponse().statusCode();

		if (checkHttpStatus(statusCode, successHttpStatusList)) {
			// 成功のステータスの場合

			// 成功
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.SUCCESS);

			return false;

		} else if (HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue() == statusCode) {
			// HTTPレスポンスコードが「503」(FANSHIPメンテナンス)の場合 

			// メッセージを出力(error)
			writeLog(apiName, statusCode, Constants.FANSHIP_MANTAINENCE, true, null);

			// FANSHIPメンテナンス(503)
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.FANSHIP_MAINTENANCE);

			return false;

		} else if (HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue() == statusCode) {
			// HTTPレスポンスコードが「429」の場合

			// メッセージを出力(error)
			writeLog(apiName, statusCode, Constants.RESTRICTED_REQUEST, true, null);

			// リクエストが多すぎる(429) 
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.TOO_MANY_REQUEST);

			return false;

		} else if (HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue() == statusCode) {
			// HTTPレスポンスコードが「401」(認証エラー)の場合 

			if (ReAuthRetryKbn.NONE.equals(reAuthRetryKbn)) {
				// メッセージを出力(info)
				writeLog(apiName, statusCode, Constants.AUTHORIZED_ERROR, false, null);

			} else if (ReAuthRetryKbn.RETRY_DB.equals(reAuthRetryKbn)) {
				// メッセージを出力(info)
				writeLog(apiName, statusCode, Constants.RE_AUTHORIZED_DB_ERROR, false, null);

			} else if (ReAuthRetryKbn.RETRY_FS.equals(reAuthRetryKbn)) {
				// メッセージを出力(error)
				writeLog(apiName, statusCode, Constants.RE_AUTHORIZED_FS_ERROR, true, null);
			}

			// 認証エラー(401)
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.AUTHENTICATION_ERROR);

			return false;

		} else if (ConvertUtility.integerToString(statusCode).startsWith(Constants.HTTP_STATUS_START_NUMBER_ERROR)) {
			// HTTPレスポンスコードが「5xx」（503以外）の場合

			if (RetryKbn.SERVER_ERROR.equals(retryKbn) || RetryKbn.SERVER_ERROR_TIMEOUT.equals(retryKbn)) {
				// リトライ対象の場合

				if (retry >= apiRetryCount) {
					// リトライ回数が設定ファイルで指定した値を上回った場合

					// メッセージを出力(error)
					writeLog(apiName, statusCode, Constants.OVER_RETRY_COUNT, true, null);

					// サーバエラー(5xx/503以外) 
					fsApiCallResponse.setFsApiCallResult(FsApiCallResult.SERVER_ERROR);

					return false;
				}

				// 設定ファイルで指定したリトライ回数分、再度API連携を行う。リトライ毎にエラーメッセージを出力する。

				// メッセージを出力(info)
				writeLog(apiName, statusCode, Constants.RETRY_COUNT + String.valueOf(retry + 1), false, null);

				// レスポンスログ出力
				writeResponseLog(fsApiCallResponse, batchId, url);

				return true;

			} else {
				// リトライ対象外の場合

				// メッセージを出力(error)
				writeLog(apiName, statusCode, Constants.SERVER_ERROR, true, null);

				// サーバエラー(5xx/503以外) 
				fsApiCallResponse.setFsApiCallResult(FsApiCallResult.SERVER_ERROR);

				return false;

			}

		} else if (ConvertUtility.integerToString(statusCode).startsWith(Constants.HTTP_STATUS_START_NUMBER_ERROR_4)) {
			// HTTPレスポンスコードが「4xx」（401,429以外）の場合

			// メッセージを出力(error)
			writeLog(apiName, statusCode, Constants.CLIENT_ERROR, true, null);

			// クライアントエラー(4xx/401,429以外）
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.CLIENT_ERROR);

			return false;

		} else {
			// 上記以外の場合

			// メッセージを出力(error)
			writeLog(apiName, statusCode, Constants.OTHERS_ERROR, true, null);

			// その他エラー
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);

			return false;
		}

	}

	/**
	 * HTTPステータスが正常かどうか判定
	 * 
	 * @param statusCode HTTPステータス
	 * @param successHttpStatusList 正常HTTPステータスコードリスト
	 * 
	 * @return true:正常 false:異常
	 */
	private boolean checkHttpStatus(int statusCode, List<Integer> successHttpStatusList) {

		for (Integer successHttpStatus : successHttpStatusList) {

			int successLength = String.valueOf(successHttpStatus).length();

			if ((successLength == 3 && statusCode == successHttpStatus)
					|| (successLength < 3 && successLength > 0
							&& String.valueOf(statusCode).startsWith(String.valueOf(successHttpStatus)))) {
				// 正常HTTPステータスコードが3桁で一致する、または、1～2桁で前方一致する場合

				return true;

			}
		}

		return false;
	}

	/**
	 * HttpRequest生成
	 * 
	 * @param url URL
	 * @param param パラメータ
	 * @param httpMethodType HTTPメソッド
	 * @param contentType Content-Type
	 * @param tokenHeaderType 認証トークンヘッダ
	 * @param requestTimeout リクエストタイムアウト
	 * 
	 * @return HttpRequest
	 */
	private HttpRequest createHttpRequest(String url, String param, HttpMethodType httpMethodType,
			ContentType contentType, TokenHeaderType tokenHeaderType, int requestTimeout) {

		if (HttpMethodType.POST.equals(httpMethodType)) {
			// POSTの場合 ※Content-Type、認証ヘッダ、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.POST(BodyPublishers.ofString(param))
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							getTokenHeader(tokenHeaderType), getToken(tokenHeaderType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.POST_NOT_AUTH.equals(httpMethodType)) {
			// POST(ログイン、IDリンク操作（削除）、IDリンク操作（紐付けを行う）)の場合 ※Content-Type、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.POST(BodyPublishers.ofString(param))
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.POST_PSEUDO_LOGIN.equals(httpMethodType)) {
			// POST(疑似ログイン)の場合 ※Content-Type、User-Agent（疑似ログイン）
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.POST(BodyPublishers.ofString(param))
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.PATCH.equals(httpMethodType)) {
			// PATCHの場合 ※Content-Type、認証ヘッダ、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.method(Constants.HTTP_METHOD_PATCH, BodyPublishers.ofString(param))
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							getTokenHeader(tokenHeaderType), getToken(tokenHeaderType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.DELETE.equals(httpMethodType)) {
			// DELETEの場合 ※認証ヘッダ、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.DELETE()
					.headers(getTokenHeader(tokenHeaderType), getToken(tokenHeaderType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.GET.equals(httpMethodType)) {
			// GETの場合 ※Content-Type、認証ヘッダ、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.GET()
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							getTokenHeader(tokenHeaderType), getToken(tokenHeaderType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else if (HttpMethodType.PUT.equals(httpMethodType)) {
			// PUTの場合 ※Content-Type、認証ヘッダ、User-Agent
			return HttpRequest.newBuilder()
					.uri(URI.create(url))
					.PUT(BodyPublishers.noBody())
					.headers(Constants.FS_API_URL_HEADER_CONTENT_TYPE, getContentType(contentType),
							getTokenHeader(tokenHeaderType), getToken(tokenHeaderType),
							Constants.FS_API_URL_HEADER_USER_AGENT, this.userAgent)
					.timeout(Duration.ofSeconds(requestTimeout))
					.build();

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 * Content-Typeの値取得
	 * 
	 * @param contentType Content-Type
	 * 
	 * @return Content-Typeの値
	 */
	private String getContentType(ContentType contentType) {

		if (ContentType.APPLICATION_JSON.equals(contentType)) {
			return Constants.FS_API_URL_HEADER_APP_JSON;

		} else if (ContentType.APPLICATION_JSON_CHARSET.equals(contentType)) {
			return Constants.FS_API_URL_HEADER_APP_JSON_CHARSET;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 認証トークンヘッダの値取得
	 * 
	 * @param tokenHeaderType 認証トークンヘッダ
	 * 
	 * @return 認証トークンヘッダの値
	 */
	private String getTokenHeader(TokenHeaderType tokenHeaderType) {

		if (TokenHeaderType.AUTHORIZATION.equals(tokenHeaderType)
				|| TokenHeaderType.AUTHORIZATION_POPINFOLOGIN.equals(tokenHeaderType)) {
			return Constants.AUTHORIZATION;

		} else if (TokenHeaderType.X_POPINFO_MAPI_TOKEN.equals(tokenHeaderType)) {
			return Constants.X_POPINFO_MAPI_TOKEN;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 認証トークンの値取得
	 * 
	 * @param tokenHeaderType 認証トークンヘッダ
	 * @param token 認証トークン
	 * 
	 * @return 認証トークンの値
	 */
	private String getToken(TokenHeaderType tokenHeaderType) {

		if (TokenHeaderType.AUTHORIZATION.equals(tokenHeaderType)
				|| TokenHeaderType.X_POPINFO_MAPI_TOKEN.equals(tokenHeaderType)) {
			return this.authToken;

		} else if (TokenHeaderType.AUTHORIZATION_POPINFOLOGIN.equals(tokenHeaderType)) {
			return String.format(Constants.AUTHORIZATION_POPINFOLOGIN_FORMAT, this.authToken);

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * HttpClient生成
	 * 
	 * @param url APIのURL
	 */
	private void createHttpClient(String url) {

		if (chackKeystoreUsed(url)) {
			// Keystore情報が設定されており、KeyStore利用の場合
			if (httpClientKeystore == null) {
				httpClientKeystore = HttpClientHolderKeystore.getClient(connectionTimeoutDuration, keystorePath,
						keystorePass);
			}

		} else {
			// Keystore情報が設定されていない、または、KeyStore未利用の場合
			if (httpClient == null) {
				httpClient = HttpClientHolder.getClient(connectionTimeoutDuration);
			}

		}
	}

	/**
	 * Keystore利用チェック
	 * 
	 * @param url APIのURL
	 * 
	 * @return true;KeyStore利用 false:KeyStore未利用
	 */
	private boolean chackKeystoreUsed(String url) {

		if ((url.contains(integrationUrl) && KEYSTORE_USED.equals(integrationKeystoreFlag))
				|| (url.contains(reverseProxyUrl) && KEYSTORE_USED.equals(reverseProxyKeystoreFlag))
				|| (url.contains(userApiUrl) && KEYSTORE_USED.equals(endUserKeystoreFlag))) {
			// 「統合API」「リバースプロキシAPI」「エンドユーザ用API」で対象がKeyStore利用の場合

			// Keystore情報が設定されている場合
			if (chackKeystore()) {
				return true;
			}

		}

		return false;
	}

	/**
	 * Keystore情報チェック
	 * 
	 * @return true;KeyStore情報あり false:KeyStore情報なし
	 */
	private boolean chackKeystore() {

		if (keystorePath == null || keystorePass == null || keystorePath.isEmpty() || keystorePass.isEmpty()) {
			// キーストアパス、キーストアパスワードが設定されていない場合
			return false;
		}

		return true;
	}

	/**
	 * APIエラーログ出力
	 * 
	 * @param apiName API(処理)名
	 * @param statusCode HTTPステータスコード
	 * @param msg メッセージ
	 * @param errorFlg エラーフラグ
	 * @param e Exception
	 * 
	 */
	private void writeLog(String apiName, Integer statusCode, String msg, boolean errorFlg, Exception e) {

		// メッセージを出力（%sのAPI連携に失敗しました。（HTTPレスポンスコード ＝「%s」,エラー内容 = 「%s」））
		String errorMsg = String.format(
				BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
				apiName,
				statusCode,
				msg);

		if (errorFlg) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMsg), e);
		} else {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMsg));
		}

	}

	/**
	 * APIエラーログ出力
	 * 
	 * @param fsApiCallResponse FS APIのレスポンス
	 * @param batchId バッチID
	 * @param url APIのURL
	 * 
	 */
	private void writeResponseLog(FsApiCallResponse fsApiCallResponse, String batchId, String url) {

		if (fsApiCallResponse.getResponse() != null) {

			logger.debug("HTTPステータスコード：" + fsApiCallResponse.getResponse().statusCode());

			boolean responseLogFlg = true;
			if (this.responseLogHide.contains(batchId)
					&& FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())
					&& !url.contains(this.authTokenUrl)) {
				// APIレスポンスデータログ非表示対象の場合、データが大きいため非表示
				responseLogFlg = false;
			}

			if (responseLogFlg) {
				logger.debug("レスポンスボディ：" + fsApiCallResponse.getResponse().body());
			}
		} else {
			logger.debug("HTTPステータスコード：null");
			logger.debug("レスポンスボディ：null");
		}

	}

	/**
	 * HTTPメソッド名取得
	 * 
	 * @param HTTPメソッドタイプ
	 * 
	 * @return HTTPメソッド名
	 * 
	 */
	private String getHttpMethodName(HttpMethodType httpMethodType) {

		return httpMethodType.toString().replace("_NOT_AUTH", "").replace("_PSEUDO_LOGIN", "");

	}

	/**
	 * FS APIのレスポンスを格納するクラス
	 */
	public static class FsApiCallResponse {
		/** FS API呼出結果 */
		private FsApiCallResult fsApiCallResult;
		/** FS APIのレスポンス */
		private HttpResponse<String> response;

		/**
		 * FS API呼出結果を取得する
		 * 
		 * @return FS API呼出結果
		 */
		public FsApiCallResult getFsApiCallResult() {
			return fsApiCallResult;
		}

		/**
		 * FS API呼出結果を設定する
		 * 
		 * @param FS API呼出結果
		 */
		public void setFsApiCallResult(FsApiCallResult fsApiCallResult) {
			this.fsApiCallResult = fsApiCallResult;
		}

		/**
		 * FS APIのレスポンスを取得する
		 * 
		 * @return FS APIのレスポンス
		 */
		public HttpResponse<String> getResponse() {
			return response;
		}

		/**
		 * FS APIのレスポンスを設定する
		 * 
		 * @param FS APIのレスポンス
		 */
		public void setResponse(HttpResponse<String> response) {
			this.response = response;
		}
	}
}
