package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.dao.DAOParameter;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponRequestBodyCreator;
import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponRequestBodyCreator.CreateKbn;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.common.BatchQrCodeListCreateor;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.dto.CouponRegisterApiOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponRegisterOutputDTO;
import jp.co.aeoncredit.coupon.constants.AppMessageType;
import jp.co.aeoncredit.coupon.constants.ContentsType;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.DeliveryTarget;
import jp.co.aeoncredit.coupon.constants.FsApiType;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationType;
import jp.co.aeoncredit.coupon.dao.custom.AppMessageSendPeriodsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.AppMessagesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponImagesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponIncentsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstCmsContentsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstEventDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstSensorDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationSendPeriodsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.SensorRelationDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessageSendPeriods;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstCmsContents;
import jp.co.aeoncredit.coupon.entity.MstEvent;
import jp.co.aeoncredit.coupon.entity.MstSensor;
import jp.co.aeoncredit.coupon.entity.PushNotificationSendPeriods;
import jp.co.aeoncredit.coupon.entity.PushNotifications;
import jp.co.aeoncredit.coupon.entity.SensorRelation;
import jp.co.aeoncredit.coupon.lib.cms.CmsApiException;
import jp.co.aeoncredit.coupon.lib.cms.CmsImageRegister;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSクーポン登録・更新・削除バッチ
 */
@Named("B18B0011")
@Dependent
public class B18B0011 extends BatchFSApiCalloutBase {

	/** FSクーポン登録処理結果 */
	private enum FsCouponRegisterResult {
		/** 成功 */
		SUCCESS,
		/** APIエラー(正常終了) */
		API_ERROR_SUCCESS,
		/** APIエラー(異常終了) */
		API_ERROR_FAIL,
		/** クーポンUUID(配信ID、Push配信ID)取得エラー ※異常終了 */
		UUID_ERROR,
		/** B18SC002 CMS画像登録API呼び出し共通処理異常終了 ※継続 */
		CMS_ERROR,
		/** DBエラー ※異常終了 */
		DB_ERROR,
		/** 処置中断 */
		STOP
	}

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0011.getBatchId();

	/** バッチネーム*/
	private static final String BATCH_NAME = BatchInfo.B18B0011.getBatchName();

	/** 正常終了_戻り値 */
	private static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	private static final String FAIL_RETURN_VALUE = "1";

	/** API正常終了_ステータス */
	private static final String API_STATUS_OK = "OK";

	/** 新規登録APIURL(マス、ターゲット、パスポート) */
	private String couponRegisterApiUrl = "";

	/** アプリイベントクーポン作成APIURL */
	private String eventCouponRegisterApiUrl = "";

	/** センサーイベントクーポン作成APIURL */
	private String sensorCouponRegisterApiUrl = "";

	/** クーポン更新APIURL */
	private String couponUpdateApiUrl = "";

	/** クーポン削除APIURL */
	private String couponDeleteApiUrl = "";

	/** データ件数(新規作成) */
	protected int readRegisterCount = 0;

	/** 成功件数(新規作成) */
	protected int successRegisterCount = 0;

	/** エラー件数(新規作成) */
	protected int failRegisterCount = 0;

	/** スキップ件数(新規作成) */
	protected int skipRegisterCount = 0;

	/** データ件数(更新) */
	protected int readUpdateCount = 0;

	/** 成功件数(更新) */
	protected int successUpdateCount = 0;

	/** エラー件数(更新) */
	protected int failUpdateCount = 0;

	/** スキップ件数(更新) */
	protected int skipUpdateCount = 0;

	/** データ件数(削除) */
	protected int readDeleteCount = 0;

	/** 成功件数(削除) */
	protected int successDeleteCount = 0;

	/** エラー件数(削除) */
	protected int failDeleteCount = 0;

	/** スキップ件数(削除) */
	protected int skipDeleteCount = 0;

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** リトライ回数 */
	private int retryCount;

	/** リトライ時スリープ時間(ミリ秒) */
	private int sleepTime;

	/** タイムアウト期間(秒) */
	private int timeoutDuration;

	/** 処理中止終了フラグ(trueの場合、以降の処理を行わない) */
	private boolean stopFlg = false;

	/** 戻り値 */
	private String returnValue;

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	/** FS連携時のリクエストBODY作成 */
	private BatchFsCouponRequestBodyCreator creator;

	private ObjectMapper mapper = new ObjectMapper();

	/** カラム名共通 */
	private static final String COLUMN_COUPON_ID = "couponId";
	private static final String COLUMN_DELETE_FLAG = "deleteFlag";
	private static final String COLUMN_FS_API_TYPE = "fsApiType";

	/** 開始・終了メッセージ */
	private static final String REGISTER_START_MSG = "クーポン新規登録 - 開始";
	private static final String REGISTER_END_MSG = "クーポン新規登録 - 終了";
	private static final String UPDATE_START_MSG = "クーポン更新 - 開始";
	private static final String UPDATE_END_MSG = "クーポン更新 - 終了";
	private static final String DELETE_START_MSG = "クーポン削除 - 開始";
	private static final String DELETE_END_MSG = "クーポン削除 - 終了";

	/** 共通処理名 */
	private static final String B18BC002_NAME = "B18BC002_QRコードリスト作成";
	private static final String B18SC002_NAME = "B18SC002 CMS画像登録API呼び出し共通処理";

	/** メッセージフォーマット */
	private static final String START_COUPON_ID_MSG = "処理対象(%s)：クーポンID = %s";
	private static final String SKIP_COUPON_ID_MSG = "処理中断スキップ対象(%s)：クーポンID = %s";
	private static final String ERROR_TARGET_MSG = "エラー対象：クーポンID = %s";
	private static final String FINISH_MSG = "処理正常終了(%s)：クーポンID ＝ %s";
	private static final String ERROR_TARGET_DETAIL_MSG = "エラー対象：エラーコード = %s、開発者向けメッセージ = %s、ユーザー向けメッセージ = %s、クーポンID = %s";
	private static final String FS_COUPON_MSG = "FSクーポン";
	private static final String COUPON_ID_MSG = "クーポンID = %s";
	private static final String COUPON_ID_ERROR_INFO_MSG = "クーポンID = %s, エラー内容 = %s";
	private static final String API_REQUEST_BODY_MSG = "リクエストボディ生成失敗、クーポンID = %s";
	private static final String API_RESPONSE_STATUS_NG_MSG = "レスポンスボディ 実行結果 = %s、エラーコード = %s、開発者向けメッセージ = %s、ユーザー向けメッセージ = %s、クーポンID = %s";
	private static final String API_COUPON_UUID_ERROR = "クーポンUUID取得エラー、クーポンID = %s、クーポンUUID = %s";
	private static final String API_DELIVERY_ID_ERROR = "配信ID取得エラー、クーポンID = %s、配信ID = %s";
	private static final String API_PUSH_ID_ERROR = "Push配信ID取得エラー、クーポンID = %s、Push配信ID = %s";
	private static final String API_DESERIALIZE_ERROR = "レスポンスのデシリアライズ失敗";

	/** API連携その他情報 */
	private static final String API_OTHERS_NOT_QR_LIST = "QRコードリストが存在しません";

	/** テーブル取得処理名 */
	private static final String FIND_COUPONS_MSG = "クーポンテーブル取得";

	/** 処理区分名 */
	private static final String REGISTER_NAME = "新規";
	private static final String UPDATE_STATUS_NAME = "更新(公開ステータス)";
	private static final String UPDATE_NAME = "更新";
	private static final String DELETE_NAME = "削除";

	/** CMSAPIパラメータ */
	private static final String CMS_API_PROSSSSING_TYPE_U = "U";
	private static final String CMS_API_EXPIRATION_DATE = "20991231";
	private static final String CMS_API_APPLICATION_JSON = "application/json";

	@Inject
	protected CouponsDAOCustomize couponsDAO;
	@Inject
	protected CouponImagesDAOCustomize couponImagesDAO;
	@Inject
	protected CouponIncentsDAOCustomize couponIncentsDAO;
	@Inject
	protected AppMessagesDAOCustomize appMessagesDAO;
	@Inject
	protected AppMessageSendPeriodsDAOCustomize appMessageSendPeriodsDAO;
	@Inject
	protected MstEventDAOCustomize mstEventDAO;
	@Inject
	protected PushNotificationsDAOCustomize pushNotificationsDAO;
	@Inject
	protected PushNotificationSendPeriodsDAOCustomize pushNotificationSendPeriodsDAO;
	@Inject
	protected SensorRelationDAOCustomize sensorRelationDAO;
	@Inject
	protected MstSensorDAOCustomize mstSensorDAO;
	@Inject
	protected MstCmsContentsDAOCustomize mstCmsContentsDAO;

	/** 処理区分 */
	private enum ProcessKbn {
		/** 新規作成 */
		REGISTER,
		/** 更新 */
		UPDATE,
		/** 削除 */
		DELETE,
		/** 新規登録後の公開ステータス更新(FSAPI登録成功後のステータス更新時に使用される) */
		REGISTER_UPDATE;
	}

	/**
	 * バッチの起動メイン処理
	 * 
	 * @throws Exception スローされた例外
	 * @return 0：正常；1：異常；
	 */
	@Override
	public String process() throws Exception {

		// (1)処理開始メッセージを出力する。
		logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

		// プロパティファイルを読み込む。
		readProperties();

		// #1 FSクーポン登録・更新・削除
		String returnResult = fsCouponProcess();

		// 終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				SUCCESS_RETURN_VALUE.equals(returnResult)));

		// 戻り値を返却する。
		return setExitStatus(returnResult);

	}

	/**
	 * プロパティファイルを読み込む
	 */
	private void readProperties() {
		Properties properties = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		// 新規登録APIURL(マス、ターゲット、パスポート)
		couponRegisterApiUrl = properties.getProperty("fs.coupon.cud.batch.coupon.create.api.url");
		// アプリイベントクーポン作成APIURL
		eventCouponRegisterApiUrl = properties.getProperty("fs.coupon.cud.batch.event.coupon.create.api.url");
		// センサーイベントクーポン作成APIURL */
		sensorCouponRegisterApiUrl = properties.getProperty("fs.coupon.cud.batch.sensor.coupon.create.api.url");
		// クーポン更新APIURL
		couponUpdateApiUrl = properties.getProperty("fs.coupon.cud.batch.coupon.update.api.url");
		// クーポン削除APIURL
		couponDeleteApiUrl = properties.getProperty("fs.coupon.cud.batch.coupon.delete.api.url");
		// FS API 失敗時のAPI実行リトライ回数
		retryCount = Integer.parseInt(properties.getProperty("fs.coupon.cud.batch.retry.count"));
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		sleepTime = Integer.parseInt(properties.getProperty("fs.coupon.cud.batch.retry.sleep.time"));
		// FS API発行時のタイムアウト期間(秒)
		timeoutDuration = Integer.parseInt(properties.getProperty("fs.coupon.cud.batch.timeout.duration"));

	}

	/**
	 * #1 FSクーポン登録・更新・削除
	 * 
	 * @return 0：正常；1：異常；
	 */
	private String fsCouponProcess() {

		// (2)認証トークン取得
		// 【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		switch (authTokenResult) {
		case SUCCESS:
			// (2.1a) 【B18BC001_認証トークン取得】の戻り値が「成功（enumで定義）」の場合 は、処理を継続する。

			// 戻り値
			returnValue = SUCCESS_RETURN_VALUE;
			
			// (4)クーポン更新
			logger.info(batchLogger.createMsg(null, UPDATE_START_MSG));
			fsCouponRegister(CouponType.MASS, ProcessKbn.UPDATE);
			fsCouponRegister(CouponType.TARGET, ProcessKbn.UPDATE);
			fsCouponRegister(CouponType.PASSPORT, ProcessKbn.UPDATE);
			fsCouponRegister(CouponType.APP_EVENT, ProcessKbn.UPDATE);
			fsCouponRegister(CouponType.SENSOR_EVENT, ProcessKbn.UPDATE);
			// (4.4)クーポンテーブルのレコードの処理件数をログに出力する。
			writeCountLog(ProcessKbn.UPDATE);
			logger.info(batchLogger.createMsg(null, UPDATE_END_MSG));
			if (stopFlg) {
				// 処理中断フラグがtrueの場合
				return returnValue;
			}

			// (3)クーポン登録
			logger.info(batchLogger.createMsg(null, REGISTER_START_MSG));
			fsCouponRegister(CouponType.MASS, ProcessKbn.REGISTER);
			fsCouponRegister(CouponType.TARGET, ProcessKbn.REGISTER);
			fsCouponRegister(CouponType.PASSPORT, ProcessKbn.REGISTER);
			fsCouponRegister(CouponType.APP_EVENT, ProcessKbn.REGISTER);
			fsCouponRegister(CouponType.SENSOR_EVENT, ProcessKbn.REGISTER);
			// (3.3)クーポンテーブルのレコードの処理件数をログに出力する。
			writeCountLog(ProcessKbn.REGISTER);
			logger.info(batchLogger.createMsg(null, REGISTER_END_MSG));
			if (stopFlg) {
				// 処理中断フラグがtrueの場合
				return returnValue;
			}

			// (5)クーポン削除
			logger.info(batchLogger.createMsg(null, DELETE_START_MSG));
			fsCouponRegister(null, ProcessKbn.DELETE);
			// (5.3)クーポンテーブルのレコードの処理件数をログに出力する。
			writeCountLog(ProcessKbn.DELETE);
			logger.info(batchLogger.createMsg(null, DELETE_END_MSG));

			return returnValue;

		case MAINTENANCE:
			// (2.1b.1)【B18BC001_認証トークン取得】の戻り値が「メンテナンス（enumで定義）」(FANSHIPメンテナンス)の場合
			// 戻り値に"0"を設定し、処理を終了する（エラーログは【B18BC001_認証トークン取得】で出力される）。
			return SUCCESS_RETURN_VALUE;

		default:
			// (2.1b.2)【B18BC001_認証トークン取得】の戻り値が「失敗（enumで定義）」の場合

			// (2.1b.2.3)戻り値に"1"を設定し、処理を終了する（エラーログは【B18BC001_認証トークン取得】で出力される）。
			return FAIL_RETURN_VALUE;

		}

	}

	/**
	 * FSクーポン登録/更新/削除
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * 
	 */
	private void fsCouponRegister(CouponType couponType, ProcessKbn processKbn) {

		// 処理中断フラグがtrueの場合
		if (stopFlg) {
			// 以降処理はしない
			return;
		}

		// (3.1)クーポンを取得
		// (4.1)クーポンを取得
		// (5.1)クーポンを取得
		List<FsCouponRegisterOutputDTO> couponInfoList = getCouponInfoList(couponType, processKbn);
		if (couponInfoList.isEmpty()) {
			// データが存在しない場合
			return;
		}

		// データ件数カウント
		countReadCount(processKbn, couponInfoList.size());

		// (3.2.1)クーポン更新
		// (4.2.1)クーポン更新
		// (5.2.1)クーポン更新
		boolean updateResult = updateCouponRun(couponInfoList);
		if (!updateResult) {
			// クーポン取得失敗、または更新失敗の場合
			errorProcess(FsCouponRegisterResult.DB_ERROR, null);
			return;
		}

		// (3.2)FS API連携とクーポン更新
		// (4.2)FS API連携とクーポン更新
		for (FsCouponRegisterOutputDTO couponInfo : couponInfoList) {

			// 処理中断フラグがtrueの場合
			if (stopFlg) {
				// 処理中断スキップメッセージ
				writeSkipTargetLog(couponInfo.getCouponId(), processKbn);

				// 以降のクーポンIDは処理せずに、クーポンテーブルを更新(1:FS連携待ち)
				boolean updateCouponsResult = updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.WAITING, null,
						false, false);
				if (!updateCouponsResult) {
					// メッセージを出力
					writeErrorTargetLog(couponInfo.getCouponId());
				}

				// スキップ件数カウント
				countResultCount(processKbn, FsCouponRegisterResult.STOP);

				continue;
			}

			// 処理対象メッセージ
			writeProcessTargetLog(couponInfo.getCouponId(), processKbn);

			// 関連情報取得
			findRelatedInformation(couponType, processKbn, couponInfo);

			// (3.2.7)FS API連携
			// (4.2.5)FS API連携
			// (5.2.2)FS API連携
			FsApiCallResponse fsApiCallResponse = callFsApi(couponType, processKbn, couponInfo);

			// レスポンスボディ取得
			CouponRegisterApiOutputDTO outputDTO = new CouponRegisterApiOutputDTO();
			if (!ProcessKbn.DELETE.equals(processKbn)) {
				outputDTO = getResponseBody(fsApiCallResponse.getResponse());
			}
			// FS API連携成功フラグ
			boolean statusOkFlg = false;

			if (ProcessKbn.DELETE.equals(processKbn)) {
				// 削除の場合

				if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
					// 削除はレスポンスにステータスはないため、成功の場合OKとする
					statusOkFlg = true;
				}

			} else {
				// 削除以外の場合

				// 成功の場合
				if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {

					// APIレスポンスステータス結果判定
					statusOkFlg = API_STATUS_OK.equals(outputDTO.getStatus());
				}

			}

			// FS API連携(新規/更新/削除)が成功の場合
			if (statusOkFlg) {

				// APIの結果処理
				FsCouponRegisterResult fsCouponRegisterResult = judgeApiResultProcess(couponType, processKbn,
						couponInfo, fsApiCallResponse.getResponse(), outputDTO);

				if (ProcessKbn.REGISTER.equals(processKbn)
						&& FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)) {
					// 新規でAPIの結果処理が成功の場合

					// (3.2.8a.5)公開ステータス更新
					fsApiCallResponse = callFsApi(couponType, ProcessKbn.REGISTER_UPDATE, couponInfo);

					// レスポンスボディ取得
					outputDTO = getResponseBody(fsApiCallResponse.getResponse());
					// FS API連携成功フラグ
					statusOkFlg = false;

					// 公開ステータス更新が成功の場合
					if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {

						// APIレスポンスステータス結果判定
						statusOkFlg = API_STATUS_OK.equals(outputDTO.getStatus());

						// FS API連携(公開ステータス更新)が成功の場合
						if (statusOkFlg) {
							// 成功・エラー・スキップ件数カウント
							countResultCount(processKbn, fsCouponRegisterResult);

							// 処理正常終了メッセージ
							writeFinishTargetLog(couponInfo.getCouponId(), processKbn);

							continue;

						}

					}

				} else {
					// 上記以外の場合

					// 成功・エラー・スキップ件数カウント
					countResultCount(processKbn, fsCouponRegisterResult);

					if (!FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)
							&& !FsCouponRegisterResult.CMS_ERROR.equals(fsCouponRegisterResult)) {
						// 成功、CMSエラー以外の場合

						// エラー処理
						errorProcess(fsCouponRegisterResult, couponInfo.getCoupons());

					}

					if (FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)) {
						// 処理正常終了メッセージ
						writeFinishTargetLog(couponInfo.getCouponId(), processKbn);
					}

					continue;

				}
			}

			// FS API連携が成功ではない場合
			if (!statusOkFlg) {

				String code = "";
				String developerMessage = "";
				String userMessage = "";
				if (outputDTO.getError() != null) {
					code = outputDTO.getError().getCode();
					developerMessage = outputDTO.getError().getDeveloperMessage();
					userMessage = outputDTO.getError().getUserMessage();
				}

				// メッセージを出力
				if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
					// HTTPステータスコードは成功だが、ステータスが「OK」以外の場合
					writeApiErrorLog(fsApiCallResponse.getResponse().statusCode(),
							String.format(API_RESPONSE_STATUS_NG_MSG, outputDTO.getStatus(), code, developerMessage,
									userMessage, couponInfo.getCouponId()),
							processKbn, null);

				} else {
					// 共通処理でAPIエラーメッセージ(B18MB924)を出力しているため、詳細のみ出力
					writeErrorTargetDetailLog(couponInfo.getCouponId(), code, developerMessage, userMessage);
				}

				if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
						|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
					// FANSHIPメンテナンス(503)の場合
					// リクエストが多すぎる(429)の場合

					// 成功・エラー・スキップ件数カウント
					countResultCount(processKbn, FsCouponRegisterResult.API_ERROR_SUCCESS);

					// エラー処理
					errorProcess(FsCouponRegisterResult.API_ERROR_SUCCESS, couponInfo.getCoupons());

				} else {
					// 認証エラー(401)の場合
					// クライアントエラー(4xx/401,429以外)の場合
					// サーバエラー(5xx/503以外)の場合
					// タイムアウト(HttpTimeoutException) の場合
					// その他エラーの場合

					// 成功・エラー・スキップ件数カウント
					countResultCount(processKbn, FsCouponRegisterResult.API_ERROR_FAIL);

					// エラー処理
					errorProcess(FsCouponRegisterResult.API_ERROR_FAIL, couponInfo.getCoupons());

				}
			}

		}
	}

	/**
	 * エラー処理
	 * 
	 * @param fsCouponRegisterResult FSクーポン登録結果 
	 * @param coupons クーポンテーブル
	 * 
	 */
	private void errorProcess(FsCouponRegisterResult fsCouponRegisterResult, Coupons coupons) {

		// FS連携状況更新(1:FS連携待ち)
		if (coupons != null && !FsCouponRegisterResult.DB_ERROR.equals(fsCouponRegisterResult)) {

			boolean updateResult = false;
			if (FsCouponRegisterResult.UUID_ERROR.equals(fsCouponRegisterResult)) {
				// FSAPI正常終了で、クーポンUUID(配信ID、Push配信ID)が設定されていない場合
				updateResult = updateCoupons(coupons, FsDeliveryStatus.FAILURE, null, false, false);
			} else {
				updateResult = updateCoupons(coupons, FsDeliveryStatus.WAITING, null, false, false);
			}

			if (!updateResult) {
				// 更新エラーの場合

				// メッセージを出力
				writeErrorTargetLog(coupons.getCouponId());

				fsCouponRegisterResult = FsCouponRegisterResult.DB_ERROR;
			}
		}

		if (FsCouponRegisterResult.API_ERROR_FAIL.equals(fsCouponRegisterResult)) {
			// 認証エラー(401)の場合
			// クライアントエラー(4xx/401,429以外)の場合
			// サーバエラー(5xx/503以外)の場合
			// タイムアウト(HttpTimeoutException) の場合
			// その他エラーの場合
			
			// 異常終了
			returnValue = FAIL_RETURN_VALUE;

		} else if (FsCouponRegisterResult.UUID_ERROR.equals(fsCouponRegisterResult)
				|| FsCouponRegisterResult.DB_ERROR.equals(fsCouponRegisterResult)) {
			// クーポンUUID(配信ID、Push配信ID)取得エラー
			// DBエラー

			// 処理中断フラグをtrue
			stopFlg = true;

			// 異常終了
			returnValue = FAIL_RETURN_VALUE;

		}

	}

	/**
	 *  クーポン情報を取得（クーポンテーブル、店舗マスタ、加盟店/カテゴリマスタ）
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * 
	 * @return FSクーポン登録・更新・削除用のクーポン情報DTOのリスト
	 */
	private List<FsCouponRegisterOutputDTO> getCouponInfoList(CouponType couponType, ProcessKbn processKbn) {

		List<FsCouponRegisterOutputDTO> fsCouponRegisterOutputDTOList = new ArrayList<>();

		// SQL取得
		String sqlPropertes = getCouponsSql(couponType, processKbn);

		// クーポンの情報を取得
		Map<String, Object> paramMap = new HashMap<>();
		if (!ProcessKbn.DELETE.equals(processKbn)) {
			// 削除以外の場合

			if (CouponType.MASS.equals(couponType)
					|| CouponType.APP_EVENT.equals(couponType)
					|| CouponType.SENSOR_EVENT.equals(couponType)) {
				// マス、アプリイベント、センサーイベントの場合
				paramMap.put("couponType", couponType.getValue());
			}

			if (ProcessKbn.REGISTER.equals(processKbn)) {
				// 新規作成の場合
				paramMap.put(COLUMN_FS_API_TYPE, FsApiType.REGISTER.getValue());
			} else if (ProcessKbn.UPDATE.equals(processKbn)) {
				// 更新の場合
				paramMap.put(COLUMN_FS_API_TYPE, FsApiType.UPDATE.getValue());
			} else {
				throw new IllegalArgumentException();
			}
		}

		List<Object[]> objectList = sqlSelect(BATCH_ID, sqlPropertes, paramMap);

		// 取得したデータを格納
		if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			for (Object row : objectList) {
				FsCouponRegisterOutputDTO couponInfo = new FsCouponRegisterOutputDTO();

				// FSクーポンID
				couponInfo.setCouponId(ConvertUtility.objectToLong(row));

				// リストに格納
				fsCouponRegisterOutputDTOList.add(couponInfo);

			}

		} else {
			// 新規登録・更新の場合
			for (Object[] row : objectList) {
				FsCouponRegisterOutputDTO couponInfo = new FsCouponRegisterOutputDTO();

				// FSクーポンID
				couponInfo.setCouponId(ConvertUtility.objectToLong(row[0]));
				// FS店舗UUID
				couponInfo.setFsStoreUuid(ConvertUtility.objectToString(row[1]));
				// 加盟店/カテゴリID
				couponInfo.setMerchantCategoryId(ConvertUtility.objectToLong(row[2]));

				// リストに格納
				fsCouponRegisterOutputDTOList.add(couponInfo);

			}

		}

		return fsCouponRegisterOutputDTOList;

	}

	/**
	 *  対象SQLを取得
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * 
	 * @return SQL
	 */
	private String getCouponsSql(CouponType couponType, ProcessKbn processKbn) {

		if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return "selectDeleteCoupon";

		} else if (ProcessKbn.REGISTER.equals(processKbn) || ProcessKbn.UPDATE.equals(processKbn)) {

			// 新規登録・更新の場合
			switch (couponType) {
			case MASS:
			case APP_EVENT:
			case SENSOR_EVENT:
				// (3.1.1)マスクーポン
				// (3.1.4)アプリイベントクーポン
				// (3.1.5)センサーイベントクーポン

				// 新規登録の場合
				return "selectRegisterCoupon";

			case TARGET:
				// (3.1.2)ターゲットクーポン
				return "selectRegisterTargetCoupon";

			case PASSPORT:
				// (3.1.3)パスポートクーポン
				return "selectRegisterPassportCoupon";

			default:
				throw new IllegalArgumentException();

			}

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  クーポンテーブルのFS連携状況を「2:FS連携中」に更新
	 * 
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * @param couponInfoList FSクーポン登録・更新・削除用のクーポン情報DTOのリスト
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean updateCouponRun(List<FsCouponRegisterOutputDTO> couponInfoList) {

		try {
			transactionBegin(BATCH_ID);

			// 上記(3.1)で取得した【クーポンテーブル】のすべてのレコードを以下の条件で更新する。
			for (FsCouponRegisterOutputDTO couponInfo : couponInfoList) {

				// クーポンテーブル取得
				Optional<Coupons> coupons = findCoupons(couponInfo.getCouponId());
				if (!coupons.isPresent()) {
					// クーポンが取得できなかった場合

					// メッセージを出力
					writeRecordNotExistLog(FIND_COUPONS_MSG, couponInfo.getCouponId());

					transactionRollback(BATCH_ID);

					return false;

				}
				couponInfo.setCoupons(coupons.get());

				// クーポンテーブルを更新
				couponInfo.getCoupons().setFsDeliveryStatus(FsDeliveryStatus.DELIVERING.getValue());
				couponInfo.getCoupons().setUpdateUserId(BATCH_ID);
				couponInfo.getCoupons().setUpdateDate(DateUtils.now());

				couponsDAO.update(couponInfo.getCoupons());

			}

			transactionCommit(BATCH_ID);

			return true;

		} catch (Exception e) {
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		}

	}

	/**
	 *  関連情報取得
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 */
	private void findRelatedInformation(CouponType couponType, ProcessKbn processKbn,
			FsCouponRegisterOutputDTO coupon) {

		if (ProcessKbn.REGISTER.equals(processKbn) || ProcessKbn.UPDATE.equals(processKbn)) {
			// 新規作成、更新の場合

			// (3.2.2)クーポン画像取得
			// (4.2.2)クーポン画像取得
			coupon.setCouponImagesList(findCouponImages(coupon.getCouponId()));

			// (3.2.3)クーポン特典取得
			// (4.2.3)クーポン特典取得
			coupon.setCouponIncentsList(findCouponIncents(coupon.getCouponId()));

			// ※アプリイベントクーポンの場合
			if (CouponType.APP_EVENT.equals(couponType)) {

				// (3.2.4)アプリ内メッセージ取得
				findRelatedInformationAppEvent(coupon, processKbn);

			}

			// 新規登録の場合
			if (ProcessKbn.REGISTER.equals(processKbn)) {

				// ※センサーイベントクーポンの場合
				if (CouponType.SENSOR_EVENT.equals(couponType)) {

					// (3.2.5)Push通知取得
					findRelatedInformationSensorEvent(coupon);

				}
			}

			// (3.2.6)FSAPI用JSON取得
			// (4.2.4)FSAPI用JSON取得
			// 「1:対象商品オブジェクト」のデータ取得
			List<FsApiJson> fsApiJsonList = findFsApiJsonTargetProduct(coupon.getCouponId(),
					coupon.getCouponIncentsList());
			// 「2:テキストオブジェクト」「3:リンクURLオブジェクト」「4:バーコードオブジェクト」のデータ取得
			List<FsApiJson> fsApiJsonTextLinkList = findFsApiJsonTextLink(coupon.getCouponId());
			fsApiJsonList.addAll(fsApiJsonTextLinkList);
			coupon.setFsApiJsonList(fsApiJsonList);

		} else if (!ProcessKbn.DELETE.equals(processKbn)) {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  関連情報取得（アプリイベント）
	 * 
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * @param processKbn 処理区分(新規作成、更新)
	 * 
	 */
	private void findRelatedInformationAppEvent(FsCouponRegisterOutputDTO coupon, ProcessKbn processKbn) {

		// アプリ内メッセージテーブル取得
		coupon.setAppMessages(findAppMessages(coupon.getCouponId()).get(0));

		// 新規登録の場合のみ
		if (ProcessKbn.REGISTER.equals(processKbn)) {

			// アプリ内メッセージ配信期間取得
			coupon.setAppMessageSendPeriodsList(
					findAppMessageSendPeriods(coupon.getAppMessages().getAppMessageId()));

			// イベントマスタ取得
			coupon.setMstEvent(findMstEvent(coupon.getAppMessages().getEventId()).get());
		}

	}

	/**
	 *  関連情報取得（センサーイベント）
	 * 
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 */
	private void findRelatedInformationSensorEvent(FsCouponRegisterOutputDTO coupon) {

		// Push通知テーブル取得
		coupon.setPushNotifications(findPushNotifications(coupon.getCouponId()).get(0));

		// Push通知配信期間取得
		coupon.setPushNotificationSendPeriodsList(
				findPushNotificationSendPeriods(coupon.getPushNotifications().getPushNotificationId()));

		// センサー関連付けテーブル取得
		List<SensorRelation> sensorRelationList = findSensorRelation(
				coupon.getPushNotifications().getPushNotificationId());

		// センサーマスタ取得
		List<MstSensor> mstSensorAddList = new ArrayList<>();
		for (SensorRelation sensorRelation : sensorRelationList) {
			List<MstSensor> mstSensorList = findMstSensor(sensorRelation.getSensorId());
			mstSensorAddList.addAll(mstSensorList);
		}
		coupon.setMstSensorList(mstSensorAddList);

	}

	/**
	 * FS API連携を行う
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return FS API呼出レスポンス
	 */
	private FsApiCallResponse callFsApi(CouponType couponType, ProcessKbn processKbn,
			FsCouponRegisterOutputDTO couponInfo) {

		FsApiCallResponse fsApiCallResponse = new FsApiCallResponse();

		// APIURL取得
		String apiUrl = getApiUrl(couponType, processKbn, couponInfo.getCoupons().getFsCouponUuid());

		// リクエストBody取得(Json)
		String requestBody = getRequestBody(couponType, processKbn, couponInfo);
		if (requestBody == null) {
			// リクエストBody生成失敗
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);
			return fsApiCallResponse;
		}

		// HTTPメソッド取得
		HttpMethodType httpMethodType = getHttpMethod(processKbn);

		// Content-Type取得
		ContentType contentType = getContentType(processKbn);

		// 認証トークンヘッダ取得
		TokenHeaderType tokenHeaderType = getTokenHeaderType(couponType, processKbn);

		// 正常HTTPステータスコード取得
		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(getSuccessHttpStatus(processKbn));

		// リトライ区分取得
		RetryKbn retryKbn = getRetryKbn(processKbn);

		// FANSHIP APIを呼び出す
		return callFanshipApi(
				BATCH_ID,
				FS_COUPON_MSG + getProcessName(processKbn),
				apiUrl,
				requestBody,
				httpMethodType,
				contentType,
				tokenHeaderType,
				successHttpStatusList,
				retryCount,
				sleepTime,
				timeoutDuration,
				retryKbn);

	}

	/**
	 * APIの結果処理
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * @param response APIのレスポンス
	 * @param outputDTO クーポン新規登録・更新 OUTPUTDTO
	 * 
	 * 
	 * @return FSクーポン登録結果
	 */
	private FsCouponRegisterResult judgeApiResultProcess(CouponType couponType, ProcessKbn processKbn,
			FsCouponRegisterOutputDTO couponInfo, HttpResponse<String> response, CouponRegisterApiOutputDTO outputDTO) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規作成の場合
			return registerApiResultProcess(couponType, couponInfo, response, processKbn, outputDTO);

		} else if (ProcessKbn.UPDATE.equals(processKbn)) {
			// 更新の場合
			return updateApiResultProcess(couponInfo);

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return deleteApiResultProcess(couponInfo);

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 * 新規登録APIの結果処理
	 * 
	 * @param couponType クーポン種別
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * @param response APIのレスポンス
	 * @param processKbn 処理区分(新規作成)
	 * @param outputDTO クーポン新規登録・更新 OUTPUTDTO
	 * 
	 * @return FSクーポン登録結果
	 */
	private FsCouponRegisterResult registerApiResultProcess(CouponType couponType,
			FsCouponRegisterOutputDTO couponInfo, HttpResponse<String> response, ProcessKbn processKbn,
			CouponRegisterApiOutputDTO outputDTO) {

		// クーポンUUID
		String couponUuid = outputDTO.getUuid();
		if (couponUuid == null || couponUuid.isEmpty() || couponUuid.length() > 36) {
			// メッセージを出力(error)
			writeApiErrorLog(response.statusCode(),
					String.format(API_COUPON_UUID_ERROR, couponInfo.getCouponId(), couponUuid),
					processKbn, null);

			return FsCouponRegisterResult.UUID_ERROR;
		}

		// (3.2.8a.1)アプリイベントクーポンの場合のみ、以下の条件で【アプリ内メッセージテーブル】を更新する。
		if (CouponType.APP_EVENT.equals(couponType)) {

			if (outputDTO.getDeliveryId() == null || String.valueOf(outputDTO.getDeliveryId()).length() > 18) {
				// メッセージを出力(error)
				writeApiErrorLog(response.statusCode(),
						String.format(API_DELIVERY_ID_ERROR, couponInfo.getCouponId(), outputDTO.getDeliveryId()),
						processKbn, null);

				return FsCouponRegisterResult.UUID_ERROR;
			}

			boolean updateResult = updateAppMessages(couponInfo.getAppMessages(), outputDTO);
			if (!updateResult) {
				// 更新エラーの場合

				// メッセージを出力
				writeErrorTargetLog(couponInfo.getCouponId());

				return FsCouponRegisterResult.DB_ERROR;
			}
		}

		// (3.2.8a.2)センサーイベントクーポンの場合のみ、以下の条件で【Push通知テーブル】を更新する。
		if (CouponType.SENSOR_EVENT.equals(couponType)) {

			if (outputDTO.getId() == null || String.valueOf(outputDTO.getId()).length() > 18) {
				// メッセージを出力(error)
				writeApiErrorLog(response.statusCode(),
						String.format(API_PUSH_ID_ERROR, couponInfo.getCouponId(), outputDTO.getId()),
						processKbn, null);

				return FsCouponRegisterResult.UUID_ERROR;
			}

			boolean updateResult = updatePushNotifications(couponInfo.getPushNotifications(), outputDTO);
			if (!updateResult) {
				// 更新エラーの場合

				// メッセージを出力
				writeErrorTargetLog(couponInfo.getCouponId());

				return FsCouponRegisterResult.DB_ERROR;
			}
		}

		// (3.2.8a.3)クーポン更新
		boolean updateResult = updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.DELIVERED, couponUuid,
				false, false);
		if (!updateResult) {
			// 更新エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getCouponId());

			return FsCouponRegisterResult.DB_ERROR;
		}

		// クーポンUUID格納
		couponInfo.getCoupons().setFsCouponUuid(couponUuid);

		// (3.2.8a.4)QRコードリスト登録
		return createQrCodeList(couponInfo.getCoupons());

	}

	/**
	 * 更新APIの結果処理
	 * 
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return FSクーポン登録結果
	 */
	private FsCouponRegisterResult updateApiResultProcess(FsCouponRegisterOutputDTO couponInfo) {

		// 以下の条件で【クーポンテーブル】を更新する。
		boolean updateCouponsResult = updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.DELIVERED, null, false,
				false);
		if (!updateCouponsResult) {
			// 更新エラーの場合
			return FsCouponRegisterResult.DB_ERROR;
		}

		// (4.3)QRコードリスト登録
		return createQrCodeList(couponInfo.getCoupons());

	}

	/**
	 * 削除APIの結果処理
	 * 
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return FSクーポン登録結果
	 */
	private FsCouponRegisterResult deleteApiResultProcess(FsCouponRegisterOutputDTO couponInfo) {

		// (5.2.3)クーポン更新
		boolean updateCouponsResult = updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.DELIVERED, null, true,
				false);
		if (!updateCouponsResult) {
			// 更新エラーの場合
			return FsCouponRegisterResult.DB_ERROR;
		}

		return FsCouponRegisterResult.SUCCESS;

	}

	/**
	 * 新規登録・更新用リクエストBody取得
	 * 
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * @param coupon FSクーポン登録・更新・削除用のクーポン情報DTO
	 * 
	 * @return API連携結果区分
	 */
	private String getRequestBody(CouponType couponType, ProcessKbn processKbn, FsCouponRegisterOutputDTO couponInfo) {

		try {
			if (ProcessKbn.REGISTER.equals(processKbn)
					|| ProcessKbn.UPDATE.equals(processKbn)
					|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
				// 新規登録または更新の場合

				// リクエストBodyを生成
				CreateKbn createKbn = CreateKbn.B18B0011_REGISTER;
				if (ProcessKbn.UPDATE.equals(processKbn) || ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
					createKbn = CreateKbn.B18B0011_UPDATE;
				}

				if (creator == null) {
					creator = BatchFsCouponRequestBodyCreator.getInstance();
				}

				return creator.createRequestBody(couponType, createKbn, couponInfo);

			} else if (ProcessKbn.DELETE.equals(processKbn)) {
				// 削除の場合
				return "";

			} else {
				throw new IllegalArgumentException();
			}

		} catch (Exception e) {

			// メッセージを出力(error)
			writeApiErrorLog(null, String.format(API_REQUEST_BODY_MSG, couponInfo.getCouponId()), processKbn, e);

			return null;
		}

	}

	/**
	 *  新規登録・更新APIのURL取得
	 *  
	 * @param couponType クーポン種別
	 * @param processKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * @param fsCouponUuid FSクーポンUUID
	 * 
	 * @return APIURL
	 * 
	 */
	private String getApiUrl(CouponType couponType, ProcessKbn processKbn, String fsCouponUuid) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規登録の場合

			if (CouponType.APP_EVENT.equals(couponType)) {
				// アプリイベントクーポンの場合「
				return integrationUrl + eventCouponRegisterApiUrl;

			} else if (CouponType.SENSOR_EVENT.equals(couponType)) {
				// センサーイベントクーポンの場合
				return integrationUrl + sensorCouponRegisterApiUrl;

			} else if (CouponType.MASS.equals(couponType)
					|| CouponType.TARGET.equals(couponType)
					|| CouponType.PASSPORT.equals(couponType)) {
				// マス、ターゲット、パスポートの場合
				return integrationUrl + couponRegisterApiUrl;

			} else {
				throw new IllegalArgumentException();
			}

		} else if (ProcessKbn.UPDATE.equals(processKbn) || ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
			// 更新の場合
			return integrationUrl + couponUpdateApiUrl.replace("${uuid}", fsCouponUuid);

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return reverseProxyUrl + couponDeleteApiUrl.replace("${uuid}", fsCouponUuid);

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  認証ヘッダ取得
	 *  
	 * @param couponType クーポン種別
	 * @param ProcessKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 * @return 認証トークンヘッダ 
	 * 
	 */
	private TokenHeaderType getTokenHeaderType(CouponType couponType, ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規登録の場合

			if (CouponType.APP_EVENT.equals(couponType)
					|| CouponType.SENSOR_EVENT.equals(couponType)) {
				// アプリイベントクーポン、センサーイベントクーポンの場合「
				return TokenHeaderType.AUTHORIZATION;

			} else if (CouponType.MASS.equals(couponType)
					|| CouponType.TARGET.equals(couponType)
					|| CouponType.PASSPORT.equals(couponType)) {
				// マス、ターゲット、パスポートの場合
				return TokenHeaderType.X_POPINFO_MAPI_TOKEN;

			} else {
				throw new IllegalArgumentException();
			}

		} else if (ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)
				|| ProcessKbn.DELETE.equals(processKbn)) {
			// 更新、削除の場合
			return TokenHeaderType.X_POPINFO_MAPI_TOKEN;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  HTTPメソッド取得
	 *  
	 * @param ProcessKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 * @return APIURL
	 * 
	 */
	private HttpMethodType getHttpMethod(ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規登録の場合
			return HttpMethodType.POST;

		} else if (ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
			// 更新の場合
			return HttpMethodType.PATCH;

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return HttpMethodType.DELETE;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  Content-Type取得
	 *  
	 * @param ProcessKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 * @return APIURL
	 * 
	 */
	private ContentType getContentType(ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規登録の場合
			return ContentType.APPLICATION_JSON_CHARSET;

		} else if (ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
			// 更新の場合
			return ContentType.APPLICATION_JSON_CHARSET;

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return ContentType.NONE;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  正常HTTPステータスコード取得
	 *  
	 * @param ProcessKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 * @return 正常HTTPステータスコード
	 * 
	 */
	private int getSuccessHttpStatus(ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)
				|| ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
			// 新規登録または更新の場合
			return HTTPStatus.HTTP_STATUS_SUCCESS.getValue();

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return HTTPStatus.HTTP_STATUS_DELETED.getValue();

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  リトライ区分取得
	 *  
	 * @param ProcessKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 * @return リトライ区分
	 * 
	 */
	private RetryKbn getRetryKbn(ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規登録の場合
			return RetryKbn.SERVER_ERROR;

		} else if (ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.REGISTER_UPDATE.equals(processKbn)
				|| ProcessKbn.DELETE.equals(processKbn)) {
			// 更新または削除の場合
			return RetryKbn.SERVER_ERROR_TIMEOUT;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  クーポンテーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return クーポンテーブル
	 */
	private Optional<Coupons> findCoupons(Long couponId) {

		// クーポンテーブルを取得
		return couponsDAO.findById(couponId);

	}

	/**
	 *  クーポン画像テーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return クーポン画像テーブルリスト
	 */
	private List<CouponImages> findCouponImages(Long couponId) {

		// クーポン画像テーブルを取得
		return couponImagesDAO.findByCouponId(couponId, false);

	}

	/**
	 *  クーポン特典テーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return クーポン特典テーブルリスト
	 */
	private List<CouponIncents> findCouponIncents(Long couponId) {

		// クーポン特典テーブルを取得
		return couponIncentsDAO.findByCouponId(couponId, false);

	}

	/**
	 *  アプリ内メッセージテーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return アプリ内メッセージテーブルリスト
	 */
	private List<AppMessages> findAppMessages(Long couponId) {

		// アプリ内メッセージテーブルを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_COUPON_ID, String.valueOf(couponId));
		daoParam.set("appMessageType", AppMessageType.DELIVERY.getValue());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return appMessagesDAO.find(daoParam, null, false, null);

	}

	/**
	 *  アプリ内メッセージ配信期間を取得
	 * 
	 * @param appMessageId アプリ内メッセージID
	 * 
	 * @return アプリ内メッセージ配信期間リスト
	 */
	private List<AppMessageSendPeriods> findAppMessageSendPeriods(Long appMessageId) {

		// アプリ内メッセージ配信期間を取得
		return appMessageSendPeriodsDAO.findByAppMessageId(appMessageId, false);

	}

	/**
	 *  イベントマスタを取得
	 * 
	 * @param eventId イベントID
	 * 
	 * @return イベントマスタリスト
	 */
	private Optional<MstEvent> findMstEvent(Long eventId) {

		// イベントマスタを取得
		return mstEventDAO.findByEventId(eventId, false);

	}

	/**
	 *  Push通知テーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return Push通知テーブルリスト
	 */
	private List<PushNotifications> findPushNotifications(Long couponId) {

		// Push通知テーブルを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_COUPON_ID, String.valueOf(couponId));
		daoParam.set("pushNotificationType", PushNotificationType.DELIVERY.getValue());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return pushNotificationsDAO.find(daoParam, null, false, null);

	}

	/**
	 *  Push通知配信期間を取得
	 * 
	 * @param pushNotificationId Push通知ID
	 * 
	 * @return Push通知配信期間リスト
	 */
	private List<PushNotificationSendPeriods> findPushNotificationSendPeriods(Long pushNotificationId) {

		// Push通知配信期間を取得
		return pushNotificationSendPeriodsDAO.findByPushNotificationId(pushNotificationId, false);

	}

	/**
	 *  センサー関連付けテーブルを取得
	 * 
	 * @param pushNotificationId Push通知ID
	 * 
	 * @return センサー関連付けテーブルリスト
	 */
	private List<SensorRelation> findSensorRelation(Long pushNotificationId) {

		// センサー関連付けテーブルを取得
		return sensorRelationDAO.findByPushNotificationId(pushNotificationId, false);

	}

	/**
	 *  センサーマスタを取得
	 * 
	 * @param sensorId センサーID
	 * 
	 * @return センサーマスタリスト
	 */
	private List<MstSensor> findMstSensor(Long sensorId) {

		// センサーマスタを取得
		return mstSensorDAO.findBySensorId(sensorId, false);

	}

	/**
	 *  FSAPI用JSONテーブルを取得(1:対象商品オブジェクト)
	 * 
	 * @param couponId クーポンID
	 * @param couponIncentsList クーポン特典テーブルリスト
	 * 
	 * @return FSAPI用JSONテーブルリスト
	 */
	private List<FsApiJson> findFsApiJsonTargetProduct(Long couponId, List<CouponIncents> couponIncentsList) {

		List<FsApiJson> fsApiJsonList = new ArrayList<>();

		// クーポン特典テーブルのレコード分繰り返す
		for (CouponIncents couponIncents : couponIncentsList) {

			// FSAPI用JSONテーブルを取得
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put(COLUMN_COUPON_ID, couponId.toString());
			paramMap.put("couponIncentId", couponIncents.getCouponIncentId());
			List<Object[]> objectList = sqlSelect(BATCH_ID, "selectFsApiJsonTargetProduct", paramMap);

			// 取得したデータを格納
			for (Object[] row : objectList) {
				FsApiJson fsApiJson = new FsApiJson();

				// JSON種別
				fsApiJson.setJsonType(ConvertUtility.objectToString(row[0]));
				// クーポン特典ID
				fsApiJson.setCouponIncentId(ConvertUtility.objectToLong(row[1]));
				// JSONURL
				fsApiJson.setJsonUrl(ConvertUtility.objectToString(row[2]));

				fsApiJsonList.add(fsApiJson);
			}

		}
		return fsApiJsonList;

	}

	/**
	 *  FSAPI用JSONテーブルを取得(2:テキストオブジェクト、3:リンクURLオブジェクト、4:バーコードオブジェクト)
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return FSAPI用JSONテーブルリスト
	 */
	private List<FsApiJson> findFsApiJsonTextLink(Long couponId) {

		List<FsApiJson> fsApiJsonList = new ArrayList<>();

		// FSAPI用JSONテーブルを取得
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(COLUMN_COUPON_ID, couponId.toString());
		List<Object[]> objectList = sqlSelect(BATCH_ID, "selectFsApiJsonTextLink", paramMap);

		// 取得したデータを格納
		for (Object[] row : objectList) {
			FsApiJson fsApiJson = new FsApiJson();

			// JSON種別
			fsApiJson.setJsonType(ConvertUtility.objectToString(row[0]));
			// クーポン特典ID
			fsApiJson.setCouponIncentId(ConvertUtility.objectToLong(row[1]));
			// JSONURL
			fsApiJson.setJsonUrl(ConvertUtility.objectToString(row[2]));

			fsApiJsonList.add(fsApiJson);
		}

		return fsApiJsonList;

	}

	/**
	 * アプリ内メッセージテーブルを更新
	 * 
	 * @param appMessages アプリ内メッセージテーブル
	 * @param dto クーポン新規登録 OUTPUTDTO
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean updateAppMessages(AppMessages appMessages, CouponRegisterApiOutputDTO dto) {

		try {
			transactionBegin(BATCH_ID);

			// アプリ内メッセージテーブルを更新
			appMessages.setFsAppMessageUuid(dto.getDeliveryId());
			appMessages.setUpdateUserId(BATCH_ID);
			appMessages.setUpdateDate(DateUtils.now());

			appMessagesDAO.update(appMessages);

			transactionCommit(BATCH_ID);

			return true;

		} catch (Exception e) {
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		}

	}

	/**
	 * Push通知テーブルを更新
	 * 
	 * @param pushNotifications Push通知テーブル
	 * @param dto クーポン新規登録 OUTPUTDTO
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean updatePushNotifications(PushNotifications pushNotifications, CouponRegisterApiOutputDTO dto) {

		try {
			transactionBegin(BATCH_ID);

			// Push通知テーブルを更新
			pushNotifications.setFsPushNotificationUuid(dto.getId());
			pushNotifications.setUpdateUserId(BATCH_ID);
			pushNotifications.setUpdateDate(DateUtils.now());

			pushNotificationsDAO.update(pushNotifications);

			transactionCommit(BATCH_ID);

			return true;

		} catch (Exception e) {
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		}

	}

	/**
	 * クーポンテーブルを更新
	 * 
	 * @param coupons クーポンテーブル
	 * @param fsDeliveryStatus FS連携状況
	 * @param uuid クーポンUUID
	 * @param deleteFlg 削除フラグ
	 * @param cmsErrorFlg CMS画像登録エラーフラグ
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean updateCoupons(Coupons coupons, FsDeliveryStatus fsDeliveryStatus, String uuid, boolean deleteFlg,
			boolean cmsErrorFlg) {

		try {
			transactionBegin(BATCH_ID);

			// クーポンテーブルを更新

			// 新規作成でUUIDを登録する場合
			if (uuid != null) {
				coupons.setFsCouponUuid(uuid);
			}
			// FSクーポン削除の場合
			if (deleteFlg) {
				coupons.setDeleteFlag(DeleteFlag.DELETED.getValue());
			}
			// B18BC002_QRコードリスト作成、CMS画像登録APIエラー時
			// または、新規登録かつUUID設定済かつ正常終了(FS連携済み)以外の場合
			if (cmsErrorFlg
					|| (FsApiType.REGISTER.getValue().equals(coupons.getFsApiType())
							&& coupons.getFsCouponUuid() != null
							&& !FsDeliveryStatus.DELIVERED.equals(fsDeliveryStatus))) {
				coupons.setFsApiType(FsApiType.UPDATE.getValue());
			}

			coupons.setFsDeliveryStatus(fsDeliveryStatus.getValue());
			coupons.setUpdateUserId(BATCH_ID);
			coupons.setUpdateDate(DateUtils.now());

			couponsDAO.update(coupons);

			transactionCommit(BATCH_ID);

			return true;

		} catch (Exception e) {
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		}

	}

	/**
	 * QRコードリスト登録
	 * 
	 * @param coupons クーポンテーブル
	 * 
	 * @return FSクーポン登録結果 
	 * 
	 */
	private FsCouponRegisterResult createQrCodeList(Coupons coupons) {

		if (!DeliveryTarget.QRCODE.getValue().equals(coupons.getDeliveryTarget())) {
			// (3.2.8a.4.2)【クーポンテーブル】.「配信対象」が「3:QRコード」以外の場合
			// (4.3b)【クーポンテーブル】.「配信対象」が「3:QRコード」以外の場合

			return FsCouponRegisterResult.SUCCESS;
		}

		// (3.2.8a.4.1)【クーポンテーブル】.「配信対象」が「3:QRコード」の場合
		// (4.3a)【クーポンテーブル】.「配信対象」が「3:QRコード」の場合

		// (3.2.8a.4.1.1)以下の条件で【CMSコンテンツマスタ】を取得する。
		// (4.3a.1)以下の条件で【CMSコンテンツマスタ】を取得する。
		List<MstCmsContents> mstCmsContentsList = findMstCmsContents();

		// (3.2.8a.4.1.2)共通処理「B18BC002_QRコードリスト作成」を呼び出し、JSONを取得する。
		// (4.3a.2)共通処理「B18BC002_QRコードリスト作成」を呼び出し、JSONを取得する。
		BatchQrCodeListCreateor qrCodeListCreateor = new BatchQrCodeListCreateor(this);
		String qrCodeList = qrCodeListCreateor.getQrCodeList();
		if (qrCodeList == null || qrCodeList.isEmpty()) {
			// (3.2.8a.4.1.2a)JSONが取得できなかった場合
			// (4.3a.2a)JSONが取得できなかった場合

			// メッセージを出力（%sでエラーが発生しました。（%s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB931.toString()),
					B18BC002_NAME,
					String.format(COUPON_ID_ERROR_INFO_MSG, coupons.getCouponId(), API_OTHERS_NOT_QR_LIST));

			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg));

			// ステータス更新
			boolean updateResult = updateCoupons(coupons, FsDeliveryStatus.WAITING, null, false, true);
			if (!updateResult) {
				// メッセージを出力
				writeErrorTargetLog(coupons.getCouponId());

				return FsCouponRegisterResult.DB_ERROR;
			}

			return FsCouponRegisterResult.CMS_ERROR;
		}

		// (3.2.8a.4.1.3)「B18SC002 CMS画像登録API呼び出し共通処理」を呼び出し、(3.2.8a.4.1.2)で取得したJSONを登録する。
		// (4.3a.3)「B18SC002 CMS画像登録API呼び出し共通処理」を呼び出し、(4.3a.2)で取得したJSONを登録する。
		try {
			// CMS画像連携共通処理
			CmsImageRegister cmsImageRegister = new CmsImageRegister();
			cmsImageRegister.register(CMS_API_APPLICATION_JSON,
					mstCmsContentsList.get(0).getContentsUrl(),
					qrCodeList.getBytes(),
					CMS_API_PROSSSSING_TYPE_U,
					CMS_API_EXPIRATION_DATE);

		} catch (CmsApiException e) {
			// (3.2.8a.4.1.3b)API失敗時（CmsApiException）
			//    以下の条件で【クーポンテーブル】を更新し、エラーログを出力後、処理(3.2)へ（次のクーポンID）
			// (4.3a.3b)API失敗時（CmsApiException）
			//    以下の条件で【クーポンテーブル】を更新し、エラーログを出力後、処理(4.2)へ（次のクーポンID）

			// メッセージを出力（%sでエラーが発生しました。（%s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB931.toString()),
					B18SC002_NAME,
					String.format(COUPON_ID_ERROR_INFO_MSG, coupons.getCouponId(), e.getErrorMessage()));

			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg));

			// ステータス更新
			boolean updateResult = updateCoupons(coupons, FsDeliveryStatus.WAITING, null, false, true);
			if (!updateResult) {
				// メッセージを出力
				writeErrorTargetLog(coupons.getCouponId());

				return FsCouponRegisterResult.DB_ERROR;
			}

			return FsCouponRegisterResult.CMS_ERROR;
		}

		return FsCouponRegisterResult.SUCCESS;

	}

	/**
	 * CMSコンテンツマスタを取得
	 * 
	 * @return CMSコンテンツマスタリスト
	 * 
	 */
	private List<MstCmsContents> findMstCmsContents() {

		// CMSコンテンツマスタを取得
		return mstCmsContentsDAO.findByContentsType(ContentsType.QR_CODE, false);

	}

	/**
	 * レスポンスボディ取得
	 * 
	 * @param response HTTPレスポンス
	 * 
	 * @return クーポン新規登録 OUTPUT
	 * 
	 */
	private CouponRegisterApiOutputDTO getResponseBody(HttpResponse<String> response) {

		CouponRegisterApiOutputDTO outputDTO = new CouponRegisterApiOutputDTO();

		try {

			if (response != null && response.body() != null && !response.body().isEmpty()) {
				// レスポンスBodyを取得
				outputDTO = mapper.readValue(response.body(),
						CouponRegisterApiOutputDTO.class);
			}

			return outputDTO;

		} catch (Exception e) {
			log.info(String.format(API_DESERIALIZE_ERROR), e);

			// エラーの場合、空を返す
			return outputDTO;

		}

	}

	/**
	 *  データ件数カウント処理
	 *  
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * @param count データ件数
	 * 
	 */
	private void countReadCount(ProcessKbn processKbn, int count) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規作成の場合
			readRegisterCount = readRegisterCount + count;

		} else if (ProcessKbn.UPDATE.equals(processKbn)) {
			// 更新の場合
			readUpdateCount = readUpdateCount + count;

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			readDeleteCount = readDeleteCount + count;

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  成功件数、スキップ件数、エラー件数カウント処理
	 *  
	 * @param processKbn 処理区分(新規作成、更新、削除)
	 * @param fsCouponRegisterResult FSクーポン登録結果
	 * 
	 */
	private void countResultCount(ProcessKbn processKbn, FsCouponRegisterResult fsCouponRegisterResult) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規作成の場合

			if (FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)) {
				// 成功件数(登録)
				successRegisterCount++;

			} else if (FsCouponRegisterResult.API_ERROR_SUCCESS.equals(fsCouponRegisterResult)
					|| FsCouponRegisterResult.STOP.equals(fsCouponRegisterResult)) {
				// スキップ件数(登録)
				skipRegisterCount++;

			} else {
				// エラー件数(登録)
				failRegisterCount++;

			}

		} else if (ProcessKbn.UPDATE.equals(processKbn)) {
			// 更新の場合

			if (FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)) {
				// 成功件数(更新)
				successUpdateCount++;

			} else if (FsCouponRegisterResult.API_ERROR_SUCCESS.equals(fsCouponRegisterResult)
					|| FsCouponRegisterResult.STOP.equals(fsCouponRegisterResult)) {
				// スキップ件数(更新)
				skipUpdateCount++;

			} else {
				// エラー件数(更新)
				failUpdateCount++;

			}

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合

			if (FsCouponRegisterResult.SUCCESS.equals(fsCouponRegisterResult)) {
				// 成功件数(削除)
				successDeleteCount++;

			} else if (FsCouponRegisterResult.API_ERROR_SUCCESS.equals(fsCouponRegisterResult)
					|| FsCouponRegisterResult.STOP.equals(fsCouponRegisterResult)) {
				// スキップ件数(削除)
				skipDeleteCount++;

			} else {
				// エラー件数(削除)
				failDeleteCount++;

			}

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  カウント結果ログ出力
	 *  
	 * @param processKbn 処理区分
	 * @param resultKbn 結果区分(新規作成、更新、削除)
	 * 
	 */
	private void writeCountLog(ProcessKbn processKbn) {

		int readCount = 0;
		int successCount = 0;
		int failCount = 0;
		int skipCount = 0;

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規作成の場合

			readCount = readRegisterCount;
			successCount = successRegisterCount;
			failCount = failRegisterCount;
			skipCount = skipRegisterCount;

		} else if (ProcessKbn.UPDATE.equals(processKbn)) {
			// 更新の場合

			readCount = readUpdateCount;
			successCount = successUpdateCount;
			failCount = failUpdateCount;
			skipCount = skipUpdateCount;

		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合

			readCount = readDeleteCount;
			successCount = successDeleteCount;
			failCount = failDeleteCount;
			skipCount = skipDeleteCount;

		} else {
			throw new IllegalArgumentException();
		}

		// メッセージを出力
		if (readCount == 0) {

			// メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
					FS_COUPON_MSG + getProcessName(processKbn), "クーポン取得");

			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));

		} else {

			// メッセージを出力（%sが完了しました。(処理対象件数:[%d] , 処理成功件数:[%d], 処理失敗件数:[%d] , 処理スキップ件数:[%d], %s)）
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
					FS_COUPON_MSG + getProcessName(processKbn) + "処理",
					readCount, successCount, failCount, skipCount, "");

			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg));

		}

	}

	/**
	 *  処理区分名取得
	 *  
	 * @param processKbn 処理区分
	 * @param resultKbn 結果区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * 
	 */
	private String getProcessName(ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)) {
			// 新規作成の場合
			return REGISTER_NAME;
		} else if (ProcessKbn.REGISTER_UPDATE.equals(processKbn)) {
			// 更新(ステータス)の場合
			return UPDATE_STATUS_NAME;
		} else if (ProcessKbn.UPDATE.equals(processKbn)) {
			// 更新の場合
			return UPDATE_NAME;
		} else if (ProcessKbn.DELETE.equals(processKbn)) {
			// 削除の場合
			return DELETE_NAME;
		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  データ取得エラー共通処理
	 *  
	 * @param process 処理
	 * @param couponId クーポンID
	 * 
	 */
	private void writeRecordNotExistLog(String process, Long couponId) {

		// メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
				process,
				String.format(COUPON_ID_MSG, couponId));

		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));

	}

	/**
	 * APIエラーログ出力
	 * 
	 * @param statusCode HTTPステータスコード
	 * @param msg メッセージ
	 * @param processKbn 処理区分(新規作成、更新、削除、新規登録後の公開ステータス更新)
	 * @param e Exception
	 * 
	 */
	private void writeApiErrorLog(Integer statusCode, String msg, ProcessKbn processKbn, Exception e) {

		String commonMsg = FS_COUPON_MSG + getProcessName(processKbn);

		// メッセージを出力（%sのAPI連携に失敗しました。（HTTPレスポンスコード ＝「%s」,エラー内容 = 「%s」））
		String errorMsg = String.format(
				BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
				commonMsg,
				statusCode,
				msg);

		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMsg), e);

	}

	/**
	 *  DBエラー共通処理
	 *  
	 * @param e Exception
	 */
	private void writeDbErrorLog(Exception e) {

		// メッセージを出力（DBエラーが発生しました。%s）
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), "");
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), msg), e);

	}

	/**
	 *  処理対象メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  @param processKbn 処理区分(新規作成、更新、削除)
	 *  
	 */
	private void writeProcessTargetLog(Long couponId, ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)
				|| ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.DELETE.equals(processKbn)) {

			// メッセージを出力（処理対象(%s)：クーポンID = %s）
			String msg = String.format(START_COUPON_ID_MSG, getProcessName(processKbn), couponId);

			logger.info(msg);

		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 *  スキップ対象メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  @param processKbn 処理区分(新規作成、更新、削除)
	 *  
	 */
	private void writeSkipTargetLog(Long couponId, ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)
				|| ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.DELETE.equals(processKbn)) {

			// メッセージを出力（処理中断スキップ対象(%s)：クーポンID = %s）
			String msg = String.format(SKIP_COUPON_ID_MSG, getProcessName(processKbn), couponId);

			logger.info(msg);

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  エラー対象メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  
	 */
	private void writeErrorTargetLog(Long couponId) {

		// メッセージを出力（エラー対象：クーポンID ＝ %s）
		String msg = String.format(ERROR_TARGET_MSG, couponId);

		logger.info(msg);
	}

	/**
	 *  処理正常終了メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  @param processKbn 処理区分(新規作成、更新、削除)
	 *  
	 */
	private void writeFinishTargetLog(Long couponId, ProcessKbn processKbn) {

		if (ProcessKbn.REGISTER.equals(processKbn)
				|| ProcessKbn.UPDATE.equals(processKbn)
				|| ProcessKbn.DELETE.equals(processKbn)) {

			// メッセージを出力（処理正常終了(%s)：クーポンID ＝ %s）
			String msg = String.format(FINISH_MSG, getProcessName(processKbn), couponId);

			logger.info(msg);

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  エラー対象メッセージ(API詳細メッセージ)
	 *  
	 *  @param couponId クーポンID
	 *  @param code エラーコード
	 *  @param developerMessage 開発者向けメッセージ
	 *  @param userMessage ユーザー向けメッセージ
	 *  
	 */
	private void writeErrorTargetDetailLog(Long couponId, String code, String developerMessage, String userMessage) {

		// メッセージを出力（エラー対象：エラーコード = %s、開発者向けメッセージ = %s、ユーザー向けメッセージ = %s、クーポンID = %s）
		String msg = String.format(ERROR_TARGET_DETAIL_MSG, code, developerMessage, userMessage, couponId);

		logger.info(msg);
	}

}
