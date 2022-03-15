package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFsAppTestDeliveryRequestBodyCreator;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.EventCouponAppIntroTestCreateOutputDTO;
import jp.co.aeoncredit.coupon.constants.AppMessageType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.JsonType;
import jp.co.aeoncredit.coupon.dao.custom.AppMsgTestDeliveryDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponTestDeliveryDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.AppMsgTestDelivery;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.CouponTestDelivery;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstMerchantCategory;
import jp.co.aeoncredit.coupon.entity.MstStore;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSアプリ内メッセージテスト配信バッチ
 */
@Named("B18B0071")
@Dependent
public class B18B0071 extends BatchFSApiCalloutBase {

	/** エラーメッセージ用 */
	private static final String FANHIP_API_NAME = "イベントクーポン（アプリ導入）作成テスト";

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0071.getBatchId();

	/** バッチ名 */
	private static final String BATCH_NAME = BatchInfo.B18B0071.getBatchName();

	/** 正常HTTPステータスコード */
	private static final int SUCCESS_HTTP_STATUS = 200;

	/** 正常終了_戻り値 */
	protected static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	protected static final String FAIL_RETURN_VALUE = "1";

	/** 実行後の戻り値 */
	private ProcessResult returnCode = ProcessResult.FAILURE;

	/** アプリスキーム */
	private String appScheme;

	/** ログインAPIのURL */
	private String apiUrl;

	/** FS API失敗時のAPI実行リトライ回数 */
	protected int retryCount;

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected int sleepTime;

	/** FS API発行時のタイムアウト期間 */
	protected int timeoutDuration;

	/** メッセージフォーマット */
	private static final String ERROR_TARGET_MSG_COUPON = "エラー対象：アプリ内メッセージ種別=0、クーポンID = %s";
	private static final String ERROR_TARGET_MSG_APP_MESSAGE = "エラー対象：アプリ内メッセージ種別=1、アプリ内メッセージID = %s";

	/** FS API失敗時のAPI実行リトライ回数 */
	protected static final String RETRY_COUNT = "fs.app.msg.test.delivery.retry.count";

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected static final String RETRY_SLEEP_TIME = "fs.app.msg.test.delivery.retry.sleep.time";

	/** FS API発行時のタイムアウト期間 */
	protected static final String TIMEOUT_DURATION = "fs.app.msg.test.delivery.timeout.duration";

	/** ログインAPIのURL */
	protected static final String API_URL = "fs.app.msg.test.delivery.api.url";

	/** error数のカウント */
	private int processCount;

	/** error総数 */
	private int errorCount;

	/** skipした総数 */
	private int skipCount;

	private String tableName;

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** プロパティファイル共通 */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	private static final String COUPON_PROCESS = "アプリイベントクーポン取得,";
	private static final String APP_MSG_PROCESS = "追加アプリ内Msg取得,";
	private static final String APP_MSG_PROCESS_COUPON = "追加アプリ内Msg取得, クーポン不存在";

	private static final String APP_EVENT_COUPON = "アプリイベントクーポン";
	private static final String APP_MESSAGE_TEST = "追加アプリ内Msg";

	private static boolean transactionFlug = false;

	/** FSクーポンUUID */
	protected String fsCouponUuid;

	/** FSアプリ内メッセージUUID */
	protected Long deliveryID;

	/** SQL カラム名 */
	private static final String COUPONS = "COUPONS";
	private static final String COUPONS_ADDITION = "COUPONS_ADDITION";
	private static final String APP_MESSAGE = "APP_MESSAGE";
	private static final String COUPONS_ID = "couponsID";
	private static final String APP_MESSAGE_ID = "appMessegeId";
	private static final String FS_TEST_DELIVERY_STATUS = "fsTestDeliveryStatus";
	private static final String FS_TEST_DELIVERY_STATUS_2 = "fsTestDeliveryStatus2";
	private static final String COUPON_TYPE = "couponType";
	private static final String APP_MESSAGE_TYPE = "appMessegeType";
	private static final String DELETE_FLAG = "deleteFlag";
	private static final String UPDATE_USER_ID = "updateUserId";
	private static final String UPDATE_DATE = "updateDate";
	private static final String PRODUCT_JSON_TYPE = "productJsonType";
	private static final String TEXT_JSON_TYPE = "textJsonType";
	private static final String LINK_JSON_TYPE = "linkJsonType";
	private static final String BARCODE_JSON_TYPE = "barcodeJsonType";
	private static final String COUPON_INCENT_ID = "couponIncentId";

	/** SQL NAME */
	protected static final String SQL_SELECT_COUPONS = "selectCoupons";
	protected static final String SQL_SELECT_COUPONS_2 = "selectCoupons2";
	protected static final String SQL_SELECT_APP_MESSAGES = "selectAppMesseges";
	protected static final String SQL_SELECT_COUPON_IMAGES = "selectCouponImages";
	protected static final String SQL_SELECT_TEST_APP_MESSAGES = "selectTestAppMesseges";
	protected static final String SQL_SELECT_FS_API_JSON = "selectFsApiJson";
	protected static final String SQL_SELECT_FS_API_JSON_2 = "selectFsApiJson2";
	protected static final String SQL_SELECT_COUPON_INCENTS = "selectCouponIncents";
	protected static final String SQL_UPDATE_COUPONS = "updateCoupons";
	protected static final String SQL_UPDATE_APP_MESSAGES = "updateAppMesseges";
	protected static final String SQL_SEQ_COUPON_TEST_DELIVERY_ID = "seqCouponTestDeliveryId";
	protected static final String SQL_SEQ_APP_MSG_TEST_DELIVERY_ID = "seqAppMsgTestDeliveryId";

	@Inject
	protected CouponTestDeliveryDAOCustomize couponTestDeliveryDAO;

	@Inject
	protected AppMsgTestDeliveryDAOCustomize appMsgTestDeliveryDAO;

	/** JSON作成用のオブジェクトマッパー */
	private ObjectMapper mapper = new ObjectMapper();

	/** FS連携時のリクエストBODY作成 */
	private BatchFsAppTestDeliveryRequestBodyCreator creator;

	/**
	 * バッチの起動メイン処理
	 * 
	 */
	@Override
	public String process() throws Exception {

		// プロパティファイルの読み込み
		readProperties();

		// 起動メッセージを出力する。
		logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

		// FSアプリ内メッセージテスト配信バッチ処理の開始
		fsAppMsgTestDeliveryProcess();

		// 処理終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				SUCCESS_RETURN_VALUE.equals(returnCode.getValue())));

		return setExitStatus(returnCode.getValue());
	}

	/**
	 * プロパティファイルを読み込む
	 */
	protected void readProperties() {

		Properties properties = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		// アプリスキーム(環境変数)
		this.appScheme = System.getenv(Constants.ENV_FS_APP_SCHEME);

		// ログインAPIのURL
		this.apiUrl = properties.getProperty(API_URL);

		// FS API失敗時のAPI実行リトライ回数
		this.retryCount = Integer.parseInt(properties.getProperty(RETRY_COUNT));

		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		this.sleepTime = Integer.parseInt(properties.getProperty(RETRY_SLEEP_TIME));

		// FS API発行時のタイムアウト期間(秒)
		this.timeoutDuration = Integer.parseInt(properties.getProperty(TIMEOUT_DURATION));
	}

	/**
	 * メイン処理
	 */
	private void fsAppMsgTestDeliveryProcess() {
		List<MstStore> mstStorelist = new ArrayList<MstStore>();
		List<MstMerchantCategory> mstMerchantCategorylist = new ArrayList<MstMerchantCategory>();
		List<Coupons> couponsResult = new ArrayList<Coupons>();
		List<AppMessages> appMessegeResult = new ArrayList<AppMessages>();
		String returnvalue = ProcessResult.SUCCESS.getValue();

		int recordNumber = 0;

		// (2)【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		switch (authTokenResult) {

		// (2.1b.1)戻り値が「503」(FANSHIPメンテナンス)の場合
		case MAINTENANCE:

			returnCode = ProcessResult.SUCCESS;
			break;

		// (2.1b.2) 上記以外の失敗
		case FAILURE:

			break;
		// (2.1a) 成功した場合
		case SUCCESS:
			// (3.1)下記の条件で【クーポンテーブル】を取得する。
			couponsResult = getCouponTable(COUPONS, mstStorelist, mstMerchantCategorylist, null);
			tableName = COUPONS;
			try {
				if (couponsResult.isEmpty()) {
					// 取得結果が0件の場合、ログを出力
					dbEmptyError(COUPON_PROCESS);

				} else {
					processCount = couponsResult.size();

					// トランザクションの開始
					transactionBegin(BATCH_ID);

					transactionFlug = true;

					// (3.2)取得した全てのクーポンについて、FSテスト配信状況を「FS連携中」に更新する。
					for (Coupons coupons : couponsResult) {
						updateCouponTable(coupons, FsDeliveryStatus.DELIVERING);
					}

					// トランザクションのコミット
					transactionCommit(BATCH_ID);

					transactionFlug = false;

					// (4)アプリイベントクーポンテスト配信
					for (Coupons coupons : couponsResult) {
						logger.info("fsAppMsgTestDeliveryProcess.coupons.couponId: " + coupons.getCouponId());

						// 処理対象のクーポンIDを出力
						writeProcessTargetLog(coupons.getCouponId());

						returnvalue = appEventCouponTestDelivery(coupons, mstStorelist.get(recordNumber),
								mstMerchantCategorylist.get(recordNumber));
						if (ProcessResult.SUCCESS.getValue().equals(returnvalue)) {
							writeProcessFinishLog(coupons.getCouponId());
						}
						recordNumber++;
					}
					// (処件数をログに出力する。
					resultInformation(APP_EVENT_COUPON);

					// Api連携が成功していない場合は処理を終了させる。
					if (!ProcessResult.SUCCESS.getValue().equals(returnvalue)) {
						break;
					}
				}

				tableName = APP_MESSAGE;
				// (5.1)下記の条件で【アプリ内メッセージテーブル】を取得する。
				appMessegeResult = getAppMsgTable(null);
				if (appMessegeResult.isEmpty()) {
					// 取得結果が0件の場合、ログを出力
					dbEmptyError(APP_MSG_PROCESS);
					returnCode = ProcessResult.SUCCESS;
				} else {
					// 変数の初期化
					processCount = appMessegeResult.size();
					skipCount = 0;
					errorCount = 0;

					// トランザクションの開始
					transactionBegin(BATCH_ID);

					transactionFlug = true;

					// (5.2)取得した全てのアプリ内Msgについて、FSテスト配信状況を「FS連携中」に更新する。
					for (AppMessages appMessege : appMessegeResult) {
						updateAppTable(appMessege, FsDeliveryStatus.DELIVERING);
					}

					// トランザクションのコミット
					transactionCommit(BATCH_ID);

					transactionFlug = false;

					// (6)追加アプリ内Msgテスト配信
					for (AppMessages appMessege : appMessegeResult) {
						// 処理対象のアプリ内メッセージIDを出力
						writeProcessTargetLog(appMessege.getAppMessageId());

						returnvalue = AditionCouponTestDelivery(appMessege);
						if (ProcessResult.SUCCESS.getValue().equals(returnvalue)) {
							writeProcessFinishLog(appMessege.getAppMessageId());
						}
					}
					if (ProcessResult.SUCCESS.getValue().equals(returnvalue)) {
						returnCode = ProcessResult.SUCCESS;
					}
					resultInformation(APP_MESSAGE_TEST);
				}
				break;
			} catch (Exception e) {
			    logger.error(e.getMessage(), e);
				if (transactionFlug) {
					// トランザクションのロールバック
					transactionRollback(BATCH_ID);
				}
				break;
			}
		}

	}

	/**
	 * ,(4)アプリイベントクーポンテスト配信
	 * 
	 * @param result              ... 取得したクーポンテーブルのレコード
	 * @param mstStore            ... 店舗マスタテーブル
	 * @param mstMerchantCategory ... 加盟店/カテゴリマスタテーブル
	 */
	private String appEventCouponTestDelivery(Coupons coupon, MstStore mstStore,
			MstMerchantCategory mstMerchantCategory) {

		String result = ProcessResult.FAILURE.getValue();
		try {

			// (4.1.1)クーポン画像取得
			List<CouponImages> couponImage = getCouponImageTable(coupon.getCouponId());
			// (4.1.2)クーポン特典取得
			List<CouponIncents> couponIncent = getCouponIncentsTable(coupon.getCouponId());
			// (4.1.3)アプリ内メッセージ取得
			List<AppMessages> appMessege = getAppMsgTable(coupon.getCouponId());
			// (4.1.4)FSAPI用JSON取得
			List<FsApiJson> fsApiJson = getFsApiJsonTable(coupon.getCouponId(), couponIncent);

			// (4.2)FS API連携
			if (appMessege.isEmpty()) {
				result = apiAlignment(coupon, couponImage, couponIncent, null, fsApiJson, mstStore,
						mstMerchantCategory);
			} else {
				result = apiAlignment(coupon, couponImage, couponIncent, appMessege.get(0), fsApiJson, mstStore,
						mstMerchantCategory);
			}

			// トランザクションの開始
			transactionBegin(BATCH_ID);

			transactionFlug = true;

			if (ProcessResult.SUCCESS.getValue().equals(result)) {

				// (4.3a.1)以下の条件で【クーポンテーブル】を更新する
				updateCouponTable(coupon, FsDeliveryStatus.DELIVERED);
				// (4.4.1)以下の条件で【クーポンテスト配信テーブル】に登録する。
				createCouponTestTable(coupon.getCouponId());
				// (4.4.2)以下の条件で【アプリ内メッセージテスト配信テーブル】に登録する。
				createAppTestTable(coupon.getCouponId());
			} else {
				updateCouponTable(coupon, FsDeliveryStatus.WAITING);
			}

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

			transactionFlug = false;

		} catch (Exception e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * (6)追加アプリ内Msgテスト配信
	 * 
	 * @param result ... 取得したアプリ内メッセージテーブルのレコード
	 */
	private String AditionCouponTestDelivery(AppMessages appMessege) {
		String result = ProcessResult.FAILURE.getValue();
		List<Coupons> couponsResultAddition;

		try {
			List<MstStore> mstStorelist = new ArrayList<MstStore>();
			List<MstMerchantCategory> mstMerchantCategorylist = new ArrayList<MstMerchantCategory>();
			// (6.1)クーポン取得
			couponsResultAddition = getCouponTable(COUPONS_ADDITION, mstStorelist, mstMerchantCategorylist,
					appMessege.getCouponId());

			if (couponsResultAddition.isEmpty()) {
				// 取得結果が0件の場合、ログを出力し、(6.1)へ戻り次のアプリ内Msgに対して処理を行う。
				dbEmptyError(APP_MSG_PROCESS_COUPON);

				// トランザクションの開始
				transactionBegin(BATCH_ID);

				transactionFlug = true;

				updateAppTable(appMessege, FsDeliveryStatus.WAITING);

				// トランザクションのコミット
				transactionCommit(BATCH_ID);

				result = ProcessResult.SUCCESS.getValue();

				transactionFlug = false;
				skipCount++;
				return result;
			}
			// (6.2.1)クーポン画像取得
			List<CouponImages> couponImage = getCouponImageTable(appMessege.getCouponId());
			// (6.2.2)クーポン特典取得
			List<CouponIncents> couponIncent = getCouponIncentsTable(appMessege.getCouponId());
			// (6.2.3)FSAPI用JSON取得
			List<FsApiJson> fsApiJson = getFsApiJsonTable(appMessege.getCouponId(), couponIncent);

			// (6.3)FS API連携
			result = apiAlignment(couponsResultAddition.get(0), couponImage, couponIncent, appMessege, fsApiJson,
					mstStorelist.get(0), mstMerchantCategorylist.get(0));

			// トランザクションの開始
			transactionBegin(BATCH_ID);

			transactionFlug = true;

			if (ProcessResult.SUCCESS.getValue().equals(result)) {

				// (6.4)FSテスト配信状況更新
				updateAppTable(appMessege, FsDeliveryStatus.DELIVERED);
				// (6.5.1)以下の内容で【クーポンテスト配信テーブル】に登録する。
				createCouponTestTable(appMessege.getCouponId());
				// (6.5.2)以下の内容で【アプリ内メッセージテスト配信テーブル】に登録する。。
				createAppTestTable(appMessege.getCouponId());

			} else {
				updateAppTable(appMessege, FsDeliveryStatus.WAITING);
			}

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

			transactionFlug = false;

		} catch (Exception e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * FS API 連携
	 * 
	 * @param coupon              ... 前処理で取得したcouponテーブルのレコード
	 * @param couponImage         ... クーポン画像テーブル
	 * @param couponIncent        ... クーポン特典テーブル
	 * @param appMessege          ... アプリ内メッセージテーブル
	 * @param fsApiJson           ... FSAPI用JSONテーブル
	 * @param mstStore            ... 店舗マスタテーブル
	 * @param mstMerchantCategory ... 加盟店/カテゴリテーブル
	 * @return 0:成功、1:失敗、2:メンテナンス
	 */
	private String apiAlignment(Coupons coupon, List<CouponImages> couponImage, List<CouponIncents> couponIncent,
			AppMessages appMessege, List<FsApiJson> fsApiJson, MstStore mstStore,
			MstMerchantCategory mstMerchantCategory) {

		String returnStatus = ProcessResult.FAILURE.getValue();
		fsCouponUuid = null;

		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(SUCCESS_HTTP_STATUS);

		// URLを設定
		String url = this.integrationUrl + apiUrl;

		try {

			if (creator == null) {
				creator = BatchFsAppTestDeliveryRequestBodyCreator.getInstance();
			}

			String script = creator.createRequestBody(coupon, couponImage, couponIncent, appMessege, fsApiJson,
					mstStore, mstMerchantCategory, appScheme);

			// FS API呼出
			FsApiCallResponse fsApiResponse = callFanshipApi(BATCH_ID, BATCH_NAME, url, script, HttpMethodType.POST,
					ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION, successHttpStatusList, retryCount,
					sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR);
			if (fsApiResponse == null) {
				throw new AssertionError();
			}

			FsApiCallResult fsApiCallResult = fsApiResponse.getFsApiCallResult();

			// API連携に失敗した場合はエラー対象を出力する
			if (fsApiCallResult != FsApiCallResult.SUCCESS) {
				if (tableName.equals(COUPONS)) {
					outputFsApiLog(coupon.getCouponId());
				} else if (tableName.equals(APP_MESSAGE)) {
					outputFsApiLog(appMessege.getAppMessageId());
				}
			}

			// 再認証なし or 再認証成功
			HttpResponse<String> httpResponses = fsApiResponse.getResponse();
			switch (fsApiCallResult) {
			case SUCCESS:
				EventCouponAppIntroTestCreateOutputDTO outputDTO = mapper.readValue(httpResponses.body(),
						EventCouponAppIntroTestCreateOutputDTO.class);
				// (4.3)FSテスト配信状況更新
				if (outputDTO.getStatus().equals("OK")) {

					fsCouponUuid = outputDTO.getUuid();
					deliveryID = outputDTO.getDeliveryId();

					returnStatus = ProcessResult.SUCCESS.getValue();

				} else {
					String message = "";
					if (outputDTO.getError() != null) {
						message = outputDTO.getError().getMessage();
					}

					if (tableName.equals(COUPONS)) {
						respponseError(httpResponses.statusCode(), coupon.getCouponId(), outputDTO.getStatus(),
								message);
					} else if (tableName.equals(APP_MESSAGE)) {
						respponseError(httpResponses.statusCode(), appMessege.getCouponId(), outputDTO.getStatus(),
								message);
					}
					errorCount++;
				}

				break;

			case FANSHIP_MAINTENANCE:
				// 再認証でメンテナンス
				// メッセージは認証トークン取得処理内で出力
				if (tableName.equals(COUPONS)) {
					apiError(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), coupon.getCouponId());
					returnCode = ProcessResult.SUCCESS;
				} else if (tableName.equals(APP_MESSAGE)) {
					apiError(httpResponses.statusCode(), appMessege.getCouponId());
				}
				skipCount++;
				break;

			default:
				if (tableName.equals(COUPONS)) {
					apiError(httpResponses.statusCode(), coupon.getCouponId());
				} else if (tableName.equals(APP_MESSAGE)) {
					apiError(httpResponses.statusCode(), appMessege.getCouponId());
				}
				errorCount++;
				break;

			}

		} catch (Exception e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			errorCount++;
		}

		return returnStatus;

	}

	/**
	 * クーポンテーブルを取得する。
	 * 
	 * @param table                   ... テーブル名
	 * @param mstStorelist            ... 店舗マスタテーブル
	 * @param mstMerchantCategorylist ... 加盟店/カテゴリマスタテーブル
	 * @param id                      ... クーポンID
	 * @return クーポンテーブル
	 */
	private List<Coupons> getCouponTable(String table, List<MstStore> mstStorelist,
			List<MstMerchantCategory> mstMerchantCategorylist, Long id) {

		List<Object[]> list = new ArrayList<Object[]>();
		Map<String, Object> paramMap = new HashMap<>();

		List<Coupons> couponlist = new ArrayList<Coupons>();

		// 【クーポンテーブル】を取得する。
		if (table.equals(COUPONS)) {
			paramMap.put(FS_TEST_DELIVERY_STATUS, FsDeliveryStatus.WAITING.getValue());
			paramMap.put(FS_TEST_DELIVERY_STATUS_2, FsDeliveryStatus.DELIVERING.getValue());
			paramMap.put(COUPON_TYPE, CouponType.APP_EVENT.getValue());
			paramMap.put(DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());

			list = sqlSelect(BATCH_ID, SQL_SELECT_COUPONS, paramMap);
		} else if (table.equals(COUPONS_ADDITION)) {
			paramMap.put(COUPONS_ID, id);
			list = sqlSelect(BATCH_ID, SQL_SELECT_COUPONS_2, paramMap);
		}
		for (Object[] row : list) {
			Coupons couponinfo = new Coupons();
			MstStore mstStoreinfo = new MstStore();
			MstMerchantCategory mstMerchantCategory = new MstMerchantCategory();

			couponinfo.setCouponId(ConvertUtility.objectToLong(row[0]));
			couponinfo.setCouponName(ConvertUtility.objectToString(row[1]));
			couponinfo.setCouponAvailableNumber(ConvertUtility.objectToShort(row[2]));
			couponinfo.setUserDistributableCount(ConvertUtility.objectToShort(row[3]));
			couponinfo.setTotalDistributableCount(ConvertUtility.objectToInteger(row[4]));
			couponinfo.setDisplaydateFrom(ConvertUtility.objectToTimestamp(row[5]));
			couponinfo.setDisplaydateTo(ConvertUtility.objectToTimestamp(row[6]));
			couponinfo.setLimitdateFrom(ConvertUtility.objectToTimestamp(row[7]));
			couponinfo.setLimitdateTo(ConvertUtility.objectToTimestamp(row[8]));
			couponinfo.setCouponUseType(ConvertUtility.objectToString(row[9]));
			couponinfo.setPromotionCode(ConvertUtility.objectToString(row[10]));
			couponinfo.setMerchantName(ConvertUtility.objectToString(row[11]));
			couponinfo.setCouponUsingText(ConvertUtility.objectToString(row[12]));
			couponinfo.setCouponBarcodeNo(ConvertUtility.objectToString(row[13]));
			couponinfo.setBarcodeType(ConvertUtility.objectToString(row[14]));
			couponinfo.setDeliveryTypeTab(ConvertUtility.objectToString(row[15]));
			couponinfo.setCouponUseUnit(ConvertUtility.objectToString(row[16]));
			couponinfo.setBackColor(ConvertUtility.objectToString(row[17]));
			couponinfo.setPromotionLinkUrlTitle(ConvertUtility.objectToString(row[18]));
			couponinfo.setPromotionLinkUrl(ConvertUtility.objectToString(row[19]));
			couponinfo.setIncentiveSummaryText(ConvertUtility.objectToString(row[20]));
			couponinfo.setIncentiveSummaryType(ConvertUtility.objectToString(row[21]));
			couponinfo.setFsCouponUuid(ConvertUtility.objectToString(row[22]));
			couponinfo.setCouponType(ConvertUtility.objectToString(row[25]));
			couponinfo.setCouponUseLimitFlag(ConvertUtility.objectToString(row[26]));
			couponlist.add(couponinfo);

			mstStoreinfo.setFsStoreUuid(ConvertUtility.objectToString(row[23]));
			mstStorelist.add(mstStoreinfo);

			mstMerchantCategory.setMerchantCategoryId(ConvertUtility.objectToLong(row[24]));
			mstMerchantCategorylist.add(mstMerchantCategory);

		}

		return couponlist;
	}

	/**
	 * クーポン画像テーブルを取得する。
	 * 
	 * @param id ... クーポンID
	 * @return クーポン画像テーブル
	 */
	private List<CouponImages> getCouponImageTable(Long id) {

		List<Object[]> list = new ArrayList<Object[]>();
		Map<String, Object> paramMap = new HashMap<>();

		List<CouponImages> couponImagelist = new ArrayList<CouponImages>();

		paramMap.put(COUPONS_ID, id);
		list = sqlSelect(BATCH_ID, SQL_SELECT_COUPON_IMAGES, paramMap);

		for (Object[] row : list) {
			CouponImages couponImageinfo = new CouponImages();
			couponImageinfo.setCouponImageType(ConvertUtility.objectToString(row[0]));
			couponImageinfo.setCouponImageUrl(ConvertUtility.objectToString(row[1]));
			couponImagelist.add(couponImageinfo);
		}

		return couponImagelist;
	}

	/**
	 * クーポン特典テーブルを取得する。
	 * 
	 * @param id ... クーポンID
	 * @return クーポン特典テーブル
	 */
	private List<CouponIncents> getCouponIncentsTable(Long id) {

		List<Object[]> list = new ArrayList<Object[]>();
		Map<String, Object> paramMap = new HashMap<>();

		List<CouponIncents> couponIncentlist = new ArrayList<CouponIncents>();

		paramMap.put(COUPONS_ID, id);
		list = sqlSelect(BATCH_ID, SQL_SELECT_COUPON_INCENTS, paramMap);

		for (Object[] row : list) {
			CouponIncents couponIncentinfo = new CouponIncents();
			couponIncentinfo.setCouponIncentId(ConvertUtility.objectToLong(row[0]));
			couponIncentinfo.setIncentiveText(ConvertUtility.objectToString(row[1]));
			couponIncentinfo.setIncentiveType(ConvertUtility.objectToString(row[2]));
			couponIncentlist.add(couponIncentinfo);
		}

		return couponIncentlist;
	}

	/**
	 * アプリ内メッセージテーブルを取得する。
	 * 
	 * @param id    ... クーポンID
	 * @param table ...テーブル名
	 * @return アプリ内メッセージテーブル
	 */
	private List<AppMessages> getAppMsgTable(Long id) {

		List<Object[]> list = new ArrayList<Object[]>();
		Map<String, Object> paramMap = new HashMap<>();

		List<AppMessages> appMsgList = new ArrayList<AppMessages>();
		if (tableName.equals(APP_MESSAGE)) {
			paramMap.put(FS_TEST_DELIVERY_STATUS, FsDeliveryStatus.WAITING.getValue());
			paramMap.put(FS_TEST_DELIVERY_STATUS_2, FsDeliveryStatus.DELIVERING.getValue());
			paramMap.put(APP_MESSAGE_TYPE, AppMessageType.PROMOTION.getValue());
			paramMap.put(DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());

			list = sqlSelect(BATCH_ID, SQL_SELECT_APP_MESSAGES, paramMap);
		} else if (tableName.equals(COUPONS)) {
			paramMap.put(COUPONS_ID, id);
			list = sqlSelect(BATCH_ID, SQL_SELECT_TEST_APP_MESSAGES, paramMap);
		}
		for (Object[] row : list) {
			AppMessages appMsgInfo = new AppMessages();
			appMsgInfo.setCouponId(ConvertUtility.objectToLong(row[0]));
			appMsgInfo.setAppMessageId(ConvertUtility.objectToLong(row[1]));
			appMsgInfo.setMessageName(ConvertUtility.objectToString(row[2]));
			appMsgInfo.setMessageType(ConvertUtility.objectToString(row[3]));
			appMsgInfo.setMessageFormat(ConvertUtility.objectToString(row[4]));
			appMsgInfo.setMessageTitle(ConvertUtility.objectToString(row[5]));
			appMsgInfo.setMessageText(ConvertUtility.objectToString(row[6]));
			appMsgInfo.setMessageImageUrl(ConvertUtility.objectToString(row[7]));
			appMsgInfo.setButtonDisplayName(ConvertUtility.objectToString(row[8]));
			appMsgList.add(appMsgInfo);
		}

		return appMsgList;
	}

	/**
	 * FSAPI用JSONテーブルを取得する。
	 * 
	 * @param id               ... クーポンID
	 * @param couponIncentlist .. クーポン特典テーブル
	 * @return FSAPI用JSONテーブル
	 */
	private List<FsApiJson> getFsApiJsonTable(Long id, List<CouponIncents> couponIncentlist) {

		List<Object[]> list = new ArrayList<Object[]>();
		Map<String, Object> paramMap = new HashMap<>();

		List<FsApiJson> fsApiJsonList = new ArrayList<FsApiJson>();

		// クーポン特典テーブルのレコード分繰り返す

		List<Object[]> incentlist = new ArrayList<Object[]>();

		if (tableName.equals(COUPONS)) {
			for (CouponIncents incents : couponIncentlist) {
				paramMap = new HashMap<>();
				paramMap.put(COUPONS_ID, id);
				paramMap.put(COUPON_INCENT_ID, incents.getCouponIncentId());
				paramMap.put(PRODUCT_JSON_TYPE, JsonType.PRODUCT.getValue());
				paramMap.put(TEXT_JSON_TYPE, JsonType.TEXT.getValue());
				paramMap.put(LINK_JSON_TYPE, JsonType.LINK.getValue());
				paramMap.put(BARCODE_JSON_TYPE, JsonType.BARCODE.getValue());
				incentlist = sqlSelect(BATCH_ID, SQL_SELECT_FS_API_JSON, paramMap);
			}
		} else if (tableName.equals(APP_MESSAGE)) {
			paramMap.put(COUPONS_ID, id);
			incentlist = sqlSelect(BATCH_ID, SQL_SELECT_FS_API_JSON_2, paramMap);
		}

		if (incentlist.isEmpty() == false) {
			for (Object[] incentresult : incentlist) {
				list.add(incentresult);
			}
		}

		for (Object[] row : list) {
			FsApiJson fsApiJsonInfo = new FsApiJson();
			fsApiJsonInfo.setJsonType(ConvertUtility.objectToString(row[0]));
			fsApiJsonInfo.setJsonUrl(ConvertUtility.objectToString(row[1]));
			fsApiJsonInfo.setCouponIncentId(ConvertUtility.objectToLong(row[2]));
			fsApiJsonList.add(fsApiJsonInfo);
		}

		return fsApiJsonList;
	}

	/**
	 * クーポンテーブルを更新する
	 * 
	 * @param coupon ... 更新対象のレコード
	 * @param status ... 更新ステータス
	 */
	private void updateCouponTable(Coupons coupon, FsDeliveryStatus status) {

		Map<String, Object> paramMap = new HashMap<>();
		try {

			paramMap.put(COUPONS_ID, coupon.getCouponId());
			paramMap.put(FS_TEST_DELIVERY_STATUS, status.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// クーポンテーブルの更新
			sqlExecute(BATCH_ID, SQL_UPDATE_COUPONS, paramMap);

		} catch (Exception e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * アプリ内メッセージテーブルを更新する
	 * 
	 * @param appMessages ... 更新対象のレコード
	 * @param status      ... 更新ステータス
	 */
	private void updateAppTable(AppMessages appMessages, FsDeliveryStatus status) {

		Map<String, Object> paramMap = new HashMap<>();
		try {
			paramMap.put(APP_MESSAGE_ID, appMessages.getAppMessageId());
			paramMap.put(FS_TEST_DELIVERY_STATUS, status.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// アプリ内メッセージテーブルの更新
			sqlExecute(BATCH_ID, SQL_UPDATE_APP_MESSAGES, paramMap);

		} catch (Exception e) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * クーポンテスト配信テーブルに登録する
	 * 
	 * @param couponId ... クーポンID
	 */
	private void createCouponTestTable(Long couponId) {
		try {
			Timestamp insertTime = DateUtils.now();

			// 各インスタンスの作成
			CouponTestDelivery couponTestDelivery = new CouponTestDelivery();

			long couponTestDeliveryId = createCouponTestDeliveryIdSequence();
			couponTestDelivery.setCouponTestDeliveryId(couponTestDeliveryId);
			couponTestDelivery.setFsCouponUuid(fsCouponUuid);
			couponTestDelivery.setCouponId(couponId);
			couponTestDelivery.setCreateUserId(BATCH_ID);
			couponTestDelivery.setCreateDate(insertTime);
			couponTestDelivery.setUpdateUserId(BATCH_ID);
			couponTestDelivery.setUpdateDate(insertTime);
			couponTestDelivery.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());

			couponTestDeliveryDAO.insert(couponTestDelivery);

		} catch (Exception e) {
		    logger.error(e.getMessage(), e);

			throw new RuntimeException(e);
		}
	}

	/**
	 * アプリ内メッセージテスト配信テーブルに登録する
	 * 
	 * @param couponId ... クーポンID
	 */
	private void createAppTestTable(Long couponId) {
		try {

			Timestamp insertTime = DateUtils.now();
			// 各インスタンスの作成
			AppMsgTestDelivery appMsgTestDelivery = new AppMsgTestDelivery();

			long appMsgTestDeliveryId = createAppMsgTestDeliveryIdSequence();
			appMsgTestDelivery.setAppMsgTestDeliveryId(appMsgTestDeliveryId);
			if (tableName.equals(COUPONS)) {
				appMsgTestDelivery.setAppMessageType(AppMessageType.DELIVERY.getValue());
			} else if (tableName.equals(APP_MESSAGE)) {
				appMsgTestDelivery.setAppMessageType(AppMessageType.PROMOTION.getValue());
			}
			appMsgTestDelivery.setFsAppMessageUuid(deliveryID);
			appMsgTestDelivery.setFsCouponUuid(fsCouponUuid);
			appMsgTestDelivery.setCouponId(couponId);
			appMsgTestDelivery.setCreateUserId(BATCH_ID);
			appMsgTestDelivery.setCreateDate(insertTime);
			appMsgTestDelivery.setUpdateUserId(BATCH_ID);
			appMsgTestDelivery.setUpdateDate(insertTime);
			appMsgTestDelivery.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());

			appMsgTestDeliveryDAO.insert(appMsgTestDelivery);

		} catch (Exception e) {
		    logger.error(e.getMessage(), e);

			throw new RuntimeException(e);
		}
	}

	/**
	 * クーポンテスト配信IDシーケンス取得
	 * 
	 * @return クーポンテスト配信IDシーケンス
	 * 
	 */
	private long createCouponTestDeliveryIdSequence() {

		Long sequence = null;

		// クーポンテスト配信IDシーケンス取得
		List<Object[]> objectList = sqlSelect(BATCH_ID, SQL_SEQ_COUPON_TEST_DELIVERY_ID);

		for (Object row : objectList) {
			sequence = ConvertUtility.objectToLong(row);
		}

		return sequence;

	}

	/**
	 * アプリ内メッセージテスト配信IDシーケンス取得
	 * 
	 * @return アプリ内メッセージテスト配信IDシーケンス
	 * 
	 */
	private long createAppMsgTestDeliveryIdSequence() {

		Long sequence = null;

		// アプリ内メッセージテスト配信IDシーケンス取得
		List<Object[]> objectList = sqlSelect(BATCH_ID, SQL_SEQ_APP_MSG_TEST_DELIVERY_ID);

		for (Object row : objectList) {
			sequence = ConvertUtility.objectToLong(row);
		}

		return sequence;

	}

	/**
	 * 処理件数をログに出力する。
	 * 
	 */
	private void resultInformation(String name) {
		int success = processCount - errorCount - skipCount;
		String msg = name + "テスト配信が完了しました。(処理対象件数:[" + processCount + "] , 処理成功件数:[" + success + "], 処理失敗件数:["
				+ errorCount + "] , 処理スキップ件数:[" + skipCount + "],)";
		logger.info(batchLogger.createMsg("B18MB005", msg));
	}

	/**
	 * 配信一覧取得APIでエラー発生時
	 * 
	 * @param code HTTPレスポンスコード名
	 * @param uuid uuid
	 */
	private void apiError(Integer httpStatusCode, Long uuid) {

		String msg = null;
		if (tableName.equals(COUPONS)) {
			if (httpStatusCode != null) {
				// メンテナンス時のエラー
				if (httpStatusCode == HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue()) {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, "なし",
							String.format("アプリ内メッセージ種別=0、クーポンID =%s、エラーメッセージ=「FANSHIPメンテナンス」", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));

				} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue()) {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, "なし", String.format("アプリ内メッセージ種別=0、クーポンID =%s、エラーメッセージ=「認証エラー」", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));

				} else {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, httpStatusCode, String.format("アプリ内メッセージ種別=0、クーポンID =%s", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));

				}
			}
		} else if (tableName.equals(APP_MESSAGE)) {
			if (httpStatusCode != null) {
				// メンテナンス時のエラー
				if (httpStatusCode == HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue()) {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, "なし",
							String.format("アプリ内メッセージ種別=1、アプリ内メッセージID =%s、エラーメッセージ=「FANSHIPメンテナンス」", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));

				} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue()) {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, "なし",
							String.format("アプリ内メッセージ種別=1、アプリ内メッセージID =%s、エラーメッセージ=「認証エラー」", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));
				} else {
					msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
							FANHIP_API_NAME, httpStatusCode, String.format("アプリ内メッセージ種別=1、アプリ内メッセージID =%s", uuid));
					logger.error(batchLogger.createMsg("B18MB924", msg));
				}
			}
		}
	}

	/**
	 * テーブルの取得結果が0件の場合
	 * 
	 * @param table テーブル名
	 * @param str   カラム名
	 * @param id    id
	 * 
	 */
	private void dbEmptyError(String name) {
		String msg = "処理対象のレコードがありません(処理：" + name + ")";
		logger.info(batchLogger.createMsg("B18MB926", msg));
	}

	/**
	 * レスポンス取得でエラー発生時
	 * 
	 * @param code         HTTPレスポンスコード名
	 * @param uuid         uuid
	 * @param responseBody レスポンスボディ
	 * @param errorMsg     エラーメッセージ
	 * 
	 */
	private void respponseError(Integer httpStatusCode, Long uuid, String responseBody, String errorMsg) {

		String msg = null;
		if (tableName.equals(COUPONS)) {
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode,
					String.format("アプリ内メッセージ種別=0、クーポンID =%s、レスポンスボディ=%s、メッセージ=%s", uuid, responseBody, errorMsg));
		} else if (tableName.equals(APP_MESSAGE)) {
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode,
					String.format("アプリ内メッセージ種別=1、アプリ内メッセージID =%s、レスポンスボディ=%s、メッセージ=%s", uuid, responseBody, errorMsg));
		}

		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

	}

	/**
	 * 処理対象メッセージ
	 * 
	 * @param targetId 処理対象ID
	 * 
	 */
	private void writeProcessTargetLog(Long targetId) {
		String msg = null;

		if (tableName.equals(COUPONS)) {
			msg = String.format("処理対象：クーポンID = %s", targetId);
		} else if (tableName.equals(APP_MESSAGE)) {
			msg = String.format("処理対象：アプリ内メッセージID = %s", targetId);
		}

		logger.info(msg);

	}
	
	/**
	 * 処理正常終了メッセージ
	 * 
	 * @param targetId 処理対象ID
	 * 
	 */
	private void writeProcessFinishLog(Long targetId) {
		String msg = null;

		if (tableName.equals(COUPONS)) {
			msg = String.format("処理正常終了：クーポンID = %s", targetId);
		} else if (tableName.equals(APP_MESSAGE)) {
			msg = String.format("処理正常終了：アプリ内メッセージID = %s", targetId);
		}

		logger.info(msg);
	}

	/**
	 * エラー対象メッセージ(API連携失敗時)
	 * 
	 * @param targetId 対象ID
	 * 
	 */
	private void outputFsApiLog(Long targetId) {

		String msg = null;

		// メッセージを出力（エラー対象：アプリ内メッセージ種別、クーポンID = %s）
		if (tableName.equals(COUPONS)) {
			msg = String.format(ERROR_TARGET_MSG_COUPON, targetId);
		} else if (tableName.equals(APP_MESSAGE)) {
			msg = String.format(ERROR_TARGET_MSG_APP_MESSAGE, targetId);
		}

		logger.info(msg);
	}

}
