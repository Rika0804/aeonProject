package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
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
import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponTestDeliveryRequestBodyCreator;
import jp.co.aeoncredit.coupon.batch.common.BatchFsCouponTestDeliveryRequestBodyCreator.ProcessKbn;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.dto.CouponTestDeliveryApiOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponTestDeliveryOutputDTO;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationType;
import jp.co.aeoncredit.coupon.dao.custom.CouponImagesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponIncentsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponTestDeliveryDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationTestDeliveryDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.CouponTestDelivery;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.PushNotificationTestDelivery;
import jp.co.aeoncredit.coupon.entity.PushNotifications;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSクーポンテスト配信バッチ
 */
@Named("B18B0070")
@Dependent
public class B18B0070 extends BatchFSApiCalloutBase {

	/** FSクーポンテスト配信処理結果 */
	private enum FsCouponTestDeliveryResult {
		/** 成功 */
		SUCCESS,
		/** APIエラー(正常終了) */
		API_ERROR_SUCCESS,
		/** APIエラー(異常終了) */
		API_ERROR_FAIL,
		/** クーポンUUID(Push配信ID)取得エラー */
		UUID_ERROR,
		/** DBエラー */
		DB_ERROR
	}

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0070.getBatchId();

	/** バッチネーム*/
	private static final String BATCH_NAME = BatchInfo.B18B0070.getBatchName();

	/** 正常終了_戻り値 */
	private static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	private static final String FAIL_RETURN_VALUE = "1";

	/** API正常終了_ステータス */
	private static final String API_STATUS_OK = "OK";

	/** その他テスト配信APIURL */
	private String apiUrl = "";

	/** データ件数 */
	protected int readCount = 0;

	/** 成功件数 */
	protected int successCount = 0;

	/** エラー件数 */
	protected int failCount = 0;

	/** スキップ件数 */
	protected int skipCount = 0;

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

	/** 処理中止時の戻り値 */
	private String returnValue;

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	/** FS連携時のリクエストBODY作成 */
	BatchFsCouponTestDeliveryRequestBodyCreator creator;

	private ObjectMapper mapper = new ObjectMapper();

	/** カラム名共通 */
	private static final String COLUMN_COUPON_ID = "couponId";
	private static final String COLUMN_DELETE_FLAG = "deleteFlag";

	/** 処理名 */
	private static final String COUPON_TEST_DELIVERY_PROCESS_MSG = "クーポンテスト配信処理";
	private static final String ADDITIONAL_PUSH_PROCESS_MSG = "追加Push通知テスト配信処理";
	private static final String COUPON_TEST_DELIVERY_MSG = "クーポンテスト配信";
	private static final String ADDITIONAL_PUSH_MSG = "追加Push通知テスト配信";

	/** テーブル名 */
	private static final String COUPONS_TBL_MSG = "クーポンテーブル";
	private static final String PUSH_NOTIFICATIONS_MSG = "Push通知テーブル";

	/** 開始・終了メッセージ */
	private static final String TEST_DELIVERY_START_MSG = "クーポンテスト配信 - 開始";
	private static final String TEST_DELIVERY_END_MSG = "クーポンテスト配信 - 終了";
	private static final String ADDITIONAL_PUSH_START_MSG = "追加Push通知テスト配信 - 開始";
	private static final String ADDITIONAL_PUSH_END_MSG = "追加Push通知テスト配信 - 終了";

	/** メッセージフォーマット */
	private static final String START_COUPON_MSG = "処理対象：クーポンID = %s";
	private static final String START_PUSH_TYPE_MSG = "処理対象：Push通知ID = %s";
	private static final String SKIP_COUPON_MSG = "処理中断スキップ対象：クーポンID = %s";
	private static final String SKIP_PUSH_TYPE_MSG = "処理中断スキップ対象：Push通知ID = %s";
	private static final String FINISH_COUPON_MSG = "処理正常終了：クーポンID = %s";
	private static final String FINISH_PUSH_TYPE_MSG = "処理正常終了：Push通知ID = %s";
	private static final String ERROR_TARGET_COUPON_MSG = "エラー対象：クーポンID = %s";
	private static final String ERROR_TARGET_PUSH_MSG = "エラー対象：Push通知ID = %s";
	private static final String ERROR_TARGET_COUPON_DETAIL_MSG = "エラー対象：クーポンID = %s, エラーコード = %s, 開発者向けメッセージ = %s, ユーザー向けメッセージ = %s";
	private static final String ERROR_TARGET_PUSH_DETAIL_MSG = "エラー対象：Push通知ID = %s, エラーコード = %s, 開発者向けメッセージ = %s, ユーザー向けメッセージ = %s";
	private static final String FS_COUPON_TEST_DELIVERY_MSG = "その他クーポンテスト配信";
	private static final String COUPON_ID_MSG = "クーポンID = %s";
	private static final String COUPONS_COUPON_ID_MSG = "クーポンテーブル クーポンID = %s";
	private static final String COUPON_ID_PUSH_TYPE_MSG = "クーポンID = %s, Push通知種別 = 0:クーポン配信";
	private static final String PUSH_ID_TYPE_MSG = "Push通知ID = %s, Push通知種別 = 1:追加Push通知";
	private static final String API_RESPONSE_STATUS_NG_MSG = "レスポンスボディ 実行結果 = %s, エラーコード = %s, 開発者向けメッセージ = %s, ユーザー向けメッセージ = %s, ";
	private static final String API_REQUEST_BODY_MSG = "リクエストボディ生成失敗, ";
	private static final String API_DESERIALIZE_ERROR = "レスポンスのデシリアライズ失敗";
	private static final String API_COUPON_UUID_ERROR = "クーポンUUID取得エラー, クーポンUUID = %s, ";
	private static final String API_PUSH_ID_ERROR = "Push配信ID取得エラー, Push配信ID = %s, ";

	@Inject
	protected CouponsDAOCustomize couponsDAO;
	@Inject
	protected CouponImagesDAOCustomize couponImagesDAO;
	@Inject
	protected CouponIncentsDAOCustomize couponIncentsDAO;
	@Inject
	protected PushNotificationsDAOCustomize pushNotificationsDAO;
	@Inject
	protected CouponTestDeliveryDAOCustomize couponTestDeliveryDAO;
	@Inject
	protected PushNotificationTestDeliveryDAOCustomize pushNotificationTestDeliveryDAO;

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

		// #1 FSクーポンテスト配信
		String returnResult = fsCouponTestDeliveryProcess();

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

		// その他テスト配信APIURL
		apiUrl = properties.getProperty("fs.coupon.test.delivery.batch.api.url");
		// FS API 失敗時のAPI実行リトライ回数
		retryCount = Integer.parseInt(properties.getProperty("fs.coupon.test.delivery.batch.retry.count"));
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		sleepTime = Integer.parseInt(properties.getProperty("fs.coupon.test.delivery.batch.retry.sleep.time"));
		// FS API発行時のタイムアウト期間(秒)
		timeoutDuration = Integer.parseInt(properties.getProperty("fs.coupon.test.delivery.batch.timeout.duration"));

	}

	/**
	 * #1 FSクーポンテスト配信
	 * 
	 * @return 0：正常；1：異常；
	 */
	private String fsCouponTestDeliveryProcess() {

		// (2)認証トークン取得
		// 【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		switch (authTokenResult) {
		case SUCCESS:
			// (2.1a) 【B18BC001_認証トークン取得】の戻り値が「成功（enumで定義）」の場合 は、処理を継続する。

			// 正常終了
			returnValue = SUCCESS_RETURN_VALUE;

			// クーポンテスト配信
			logger.info(batchLogger.createMsg(null, TEST_DELIVERY_START_MSG));
			couponTestDelivery();
			logger.info(batchLogger.createMsg(null, TEST_DELIVERY_END_MSG));
			if (stopFlg) {
				// 処理中断フラグがtrueの場合
				return returnValue;
			}

			// 追加Push通知テスト配信
			logger.info(batchLogger.createMsg(null, ADDITIONAL_PUSH_START_MSG));
			additionalPushNotification();
			logger.info(batchLogger.createMsg(null, ADDITIONAL_PUSH_END_MSG));

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
	 * クーポンテスト配信
	 * 
	 */
	private void couponTestDelivery() {

		// (3)クーポン取得
		// (3.1)下記の条件で【クーポンテーブル】を取得する。
		List<FsCouponTestDeliveryOutputDTO> couponInfoList = getCouponInfoListTestDelivery();
		if (couponInfoList.isEmpty()) {
			// 取得結果が0件の場合、ログを出力し、(5)の処理を行う。

			// メッセージを出力
			writeRecordNotExistLog(COUPON_TEST_DELIVERY_MSG, COUPONS_TBL_MSG, false);

			return;
		}

		// データ件数カウント
		readCount = couponInfoList.size();

		// (3.2)取得した全てのクーポンについて、FSテスト配信状況を「FS連携中」に更新する。
		boolean updateResult = updateCouponsRun(couponInfoList);
		if (!updateResult) {
			// 更新失敗の場合

			// 処理中止終了フラグ
			stopFlg = true;

			// 異常終了
			returnValue = FAIL_RETURN_VALUE;

			return;
		}

		// (4)クーポンテスト配信
		// (3)で取得した【クーポンテーブル】レコード分(4.1)～(4.4)を繰り返す（クーポンID単位）
		for (FsCouponTestDeliveryOutputDTO couponInfo : couponInfoList) {
			logger.info("couponTestDelivery.couponInfo.couponId: " + couponInfo.getCouponId());

			// 処理中断フラグがtrueの場合
			if (stopFlg) {

				// 処理中断スキップメッセージ
				writeSkipTargetLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY);

				// 以降のクーポンIDは、FSテスト配信状況を「1:FS連携待ち」に更新
				updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.WAITING);

				// スキップ件数カウント
				skipCount++;

				continue;
			}

			// 処理対象メッセージ
			writeProcessTargetLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY);

			// (4.1)クーポン付加情報取得　※取得結果が0件でも何もせず処理を継続する。
			findRelatedInformation(couponInfo, ProcessKbn.TEST_DELIVERY);

			// (4.2)FS API連携
			FsApiCallResponse fsApiCallResponse = callFsApi(couponInfo, ProcessKbn.TEST_DELIVERY);

			// レスポンスBodyを取得
			CouponTestDeliveryApiOutputDTO outputDTO = getResponseBody(fsApiCallResponse.getResponse());

			String code = "";
			String developerMessage = "";
			String userMessage = "";
			if (outputDTO.getError() != null) {
				code = outputDTO.getError().getCode();
				developerMessage = outputDTO.getError().getDeveloperMessage();
				userMessage = outputDTO.getError().getUserMessage();
			}

			// 成功の場合
			FsCouponTestDeliveryResult fsCouponTestDeliveryResult = null;
			if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {

				// APIレスポンスステータス結果判定
				if (API_STATUS_OK.equals(outputDTO.getStatus())) {

					// クーポンテスト配信の結果処理
					fsCouponTestDeliveryResult = testDeliveryApiResultProcess(ProcessKbn.TEST_DELIVERY,
							couponInfo, outputDTO, fsApiCallResponse.getResponse().statusCode());
				} else {
					// HTTPステータスコードは成功だが、ステータスが「OK」以外の場合

					// メッセージを出力
					writeApiErrorLog(fsApiCallResponse.getResponse().statusCode(),
							String.format(API_RESPONSE_STATUS_NG_MSG, outputDTO.getStatus(), code, developerMessage,
									userMessage),
							couponInfo, ProcessKbn.TEST_DELIVERY, null);

					// APIエラー(異常終了) 
					fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_FAIL;

				}

			} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
					|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
				// FANSHIPメンテナンス(503)の場合
				// リクエストが多すぎる(429)の場合

				// メッセージを出力
				writeErrorTargetDetailLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY, code, developerMessage,
						userMessage);

				// APIエラー(正常終了) 
				fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_SUCCESS;

			} else {
				// 認証エラー(401)の場合
				// クライアントエラー(4xx/401,429以外)の場合
				// サーバエラー(5xx/503以外)の場合
				// タイムアウト(HttpTimeoutException) の場合
				// その他エラーの場合

				// メッセージを出力
				writeErrorTargetDetailLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY, code, developerMessage,
						userMessage);

				// APIエラー(異常終了) 
				fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_FAIL;
			}

			// FSテスト配信状況更新
			if (!FsCouponTestDeliveryResult.SUCCESS.equals(fsCouponTestDeliveryResult)) {
				// 成功ではない場合
				boolean updateStatusResult = updateFsTestDeliveryStatus(couponInfo, ProcessKbn.TEST_DELIVERY,
						fsCouponTestDeliveryResult);
				if (!updateStatusResult) {
					// メッセージを出力
					writeErrorTargetLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY);

					fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.DB_ERROR;
				}
			} else {
				// 成功の場合

				// 処理正常終了メッセージ
				writeFinishTargetLog(couponInfo.getCouponId(), ProcessKbn.TEST_DELIVERY);
			}

			// 後処理
			afterTreatment(fsCouponTestDeliveryResult);

		}

		// (4.5)処理件数をログに出力する。
		// メッセージを出力（%sが完了しました。(処理対象件数:[%d] , 処理成功件数:[%d], 処理失敗件数:[%d] , 処理スキップ件数:[%d], %s)）
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
				COUPON_TEST_DELIVERY_PROCESS_MSG,
				readCount, successCount, failCount, skipCount, "");

		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg));

	}

	/**
	 * 追加Push通知テスト配信
	 * 
	 */
	private void additionalPushNotification() {

		// カウント初期化
		readCount = 0;
		successCount = 0;
		failCount = 0;
		skipCount = 0;

		// (5)Push通知取得
		// (5.1)下記の条件で【Push通知テーブル】を取得する。
		List<PushNotifications> pushNotificationsList = findPushNotificationsList();
		if (pushNotificationsList.isEmpty()) {
			// 取得結果が0件の場合、ログを出力し、(7)の処理を行う。

			// メッセージを出力
			writeRecordNotExistLog(ADDITIONAL_PUSH_MSG, PUSH_NOTIFICATIONS_MSG, false);

			return;
		}

		// データ件数カウント
		readCount = pushNotificationsList.size();

		// (5.2)取得した全てのPush通知について、FSテスト配信状況を「FS連携中」に更新する。
		boolean updateResult = updatePushNotificationsRun(pushNotificationsList);
		if (!updateResult) {
			// 更新失敗の場合

			// 処理中止終了フラグ
			stopFlg = true;

			// 異常終了
			returnValue = FAIL_RETURN_VALUE;

			return;
		}

		// (6)追加Push通知テスト配信
		// (5)で取得した【Push通知テーブル】レコード分(6.1)～(6.5)を繰り返す（Push通知ID単位）
		for (PushNotifications pushNotifications : pushNotificationsList) {
			logger.info("additionalPushNotification.pushNotifications.pushNotificationId: "
					+ pushNotifications.getPushNotificationId());

			// 処理中断フラグがtrueの場合
			if (stopFlg) {

				// 処理中断スキップメッセージ
				writeSkipTargetLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH);

				// 以降のPush通知IDは、FSテスト配信状況を「1:FS連携待ち」に更新
				updatePushNotifications(pushNotifications, FsDeliveryStatus.WAITING);

				// スキップ件数カウント
				skipCount++;

				continue;
			}

			// 処理対象メッセージ
			writeProcessTargetLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH);

			// (6.1)クーポン取得
			List<FsCouponTestDeliveryOutputDTO> couponInfoList = getCouponInfoListAdditionalPush(
					pushNotifications.getCouponId());
			if (couponInfoList.isEmpty() || couponInfoList.get(0).getCoupons() == null) {
				// クーポンテーブルが取得できない場合、ログを出力し次のPush通知IDへ

				// メッセージを出力
				writeRecordNotExistLog(ADDITIONAL_PUSH_MSG,
						String.format(COUPONS_COUPON_ID_MSG, pushNotifications.getCouponId()),
						true);

				// FSテスト配信状況を「9:FS連携失敗」に更新
				boolean updatePushNotificationsResult = updatePushNotifications(pushNotifications,
						FsDeliveryStatus.FAILURE);
				if (!updatePushNotificationsResult) {
					// 更新失敗の場合

					// メッセージを出力
					writeErrorTargetLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH);

					// 後処理
					afterTreatment(FsCouponTestDeliveryResult.DB_ERROR);
					continue;
				}

				// エラー件数カウント
				failCount++;

				continue;
			}

			// FSクーポンテスト配信バッチ用のクーポン情報DTOにPush通知テーブル追加
			couponInfoList.get(0).setPushNotifications(pushNotifications);

			// (6.2)クーポン付加情報取得　※取得結果が0件の場合でも何もせず処理を継続する。
			findRelatedInformation(couponInfoList.get(0), ProcessKbn.ADDITIONAL_PUSH);

			// (6.3)FS API連携
			FsApiCallResponse fsApiCallResponse = callFsApi(couponInfoList.get(0),
					ProcessKbn.ADDITIONAL_PUSH);

			// レスポンスBodyを取得
			CouponTestDeliveryApiOutputDTO outputDTO = getResponseBody(fsApiCallResponse.getResponse());

			String code = "";
			String developerMessage = "";
			String userMessage = "";
			if (outputDTO.getError() != null) {
				code = outputDTO.getError().getCode();
				developerMessage = outputDTO.getError().getDeveloperMessage();
				userMessage = outputDTO.getError().getUserMessage();
			}

			// 成功の場合
			FsCouponTestDeliveryResult fsCouponTestDeliveryResult = null;
			if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {

				// APIレスポンスステータス結果判定
				if (API_STATUS_OK.equals(outputDTO.getStatus())) {

					// 追加Push通知テスト配信の結果処理
					fsCouponTestDeliveryResult = additionalPushApiResultProcess(ProcessKbn.ADDITIONAL_PUSH,
							couponInfoList.get(0), outputDTO, fsApiCallResponse.getResponse().statusCode());

				} else {
					// HTTPステータスコードは成功だが、ステータスが「OK」以外の場合

					// メッセージを出力
					writeApiErrorLog(fsApiCallResponse.getResponse().statusCode(),
							String.format(API_RESPONSE_STATUS_NG_MSG, outputDTO.getStatus(), code, developerMessage,
									userMessage),
							couponInfoList.get(0), ProcessKbn.ADDITIONAL_PUSH, null);

					// APIエラー(異常終了) 
					fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_FAIL;

				}

			} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
					|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
				// FANSHIPメンテナンス(503)の場合
				// リクエストが多すぎる(429)の場合

				// メッセージを出力
				writeErrorTargetDetailLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH, code,
						developerMessage, userMessage);

				// APIエラー(正常終了) 
				fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_SUCCESS;

			} else {
				// 認証エラー(401)の場合
				// クライアントエラー(4xx/401,429以外)の場合
				// サーバエラー(5xx/503以外)の場合
				// タイムアウト(HttpTimeoutException) の場合
				// その他エラーの場合

				// メッセージを出力
				writeErrorTargetDetailLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH, code,
						developerMessage, userMessage);

				// APIエラー(異常終了) 
				fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.API_ERROR_FAIL;
			}

			// FSテスト配信状況更新
			if (!FsCouponTestDeliveryResult.SUCCESS.equals(fsCouponTestDeliveryResult)) {
				// 成功ではない場合
				boolean updateStatusResult = updateFsTestDeliveryStatus(couponInfoList.get(0),
						ProcessKbn.ADDITIONAL_PUSH, fsCouponTestDeliveryResult);
				if (!updateStatusResult) {
					// メッセージを出力
					writeErrorTargetLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH);

					fsCouponTestDeliveryResult = FsCouponTestDeliveryResult.DB_ERROR;
				}
			} else {
				// 成功の場合

				// 処理正常終了メッセージ
				writeFinishTargetLog(pushNotifications.getPushNotificationId(), ProcessKbn.ADDITIONAL_PUSH);
			}

			// 後処理
			afterTreatment(fsCouponTestDeliveryResult);

		}

		// (6.6)処理件数をログに出力する。
		// メッセージを出力（%sが完了しました。(処理対象件数:[%d] , 処理成功件数:[%d], 処理失敗件数:[%d] , 処理スキップ件数:[%d], %s)）
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
				ADDITIONAL_PUSH_PROCESS_MSG,
				readCount, successCount, failCount, skipCount, "");

		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg));

	}

	/**
	 * 後処理
	 * 
	 * @param fsCouponTestDeliveryResult FSクーポンテスト配信処理結果
	 * 
	 */
	private void afterTreatment(FsCouponTestDeliveryResult fsCouponTestDeliveryResult) {

		if (FsCouponTestDeliveryResult.SUCCESS.equals(fsCouponTestDeliveryResult)) {
			// 正常場合

			// 成功件数カウント
			successCount++;

		} else {

			if (FsCouponTestDeliveryResult.API_ERROR_SUCCESS.equals(fsCouponTestDeliveryResult)) {
				// FANSHIPメンテナンス(503)の場合
				// リクエストが多すぎる(429)の場合

				// スキップ件数カウント
				skipCount++;

			} else if (FsCouponTestDeliveryResult.API_ERROR_FAIL.equals(fsCouponTestDeliveryResult)) {
				// 認証エラー(401)の場合
				// クライアントエラー(4xx/401,429以外)の場合
				// サーバエラー(5xx/503以外)の場合
				// タイムアウト(HttpTimeoutException) の場合
				// その他エラーの場合

				// エラー件数カウント
				failCount++;

				// 異常終了
				returnValue = FAIL_RETURN_VALUE;

			} else {
				// クーポンUUID(Push配信ID)取得エラー
				// DBエラー

				// エラー件数カウント
				failCount++;

				// 異常終了
				returnValue = FAIL_RETURN_VALUE;

				// 処理中止終了フラグ
				stopFlg = true;

			}

		}

	}

	/**
	 * FSテスト配信状況更新（異常時）
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param processKbn 処理区分
	 * @param fsCouponTestDeliveryResult FSクーポンテスト配信処理結果
	 * 
	 * @return true:正常 false:異常
	 */
	private boolean updateFsTestDeliveryStatus(FsCouponTestDeliveryOutputDTO couponInfo, ProcessKbn processKbn,
			FsCouponTestDeliveryResult fsCouponTestDeliveryResult) {

		FsDeliveryStatus fsDeliveryStatus = FsDeliveryStatus.WAITING;
		if (FsCouponTestDeliveryResult.UUID_ERROR.equals(fsCouponTestDeliveryResult)) {
			// クーポンUUID(Push配信ID)取得エラーの場合
			fsDeliveryStatus = FsDeliveryStatus.FAILURE;
		}

		// FSテスト配信状況更新
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// クーポンテスト配信の場合
			return updateCoupons(couponInfo.getCoupons(), fsDeliveryStatus);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// 追加Push通知テスト配信の場合
			return updatePushNotifications(couponInfo.getPushNotifications(), fsDeliveryStatus);

		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 *  クーポン情報を取得（クーポンテーブル、店舗マスタ、加盟店/カテゴリマスタ）（クーポンテスト配信）
	 * 
	 * @return FSクーポン登録・更新・削除用のクーポン情報DTOのリスト
	 */
	private List<FsCouponTestDeliveryOutputDTO> getCouponInfoListTestDelivery() {

		List<FsCouponTestDeliveryOutputDTO> couponInfoList = new ArrayList<>();

		// クーポンの情報を取得
		List<Object[]> objectList = sqlSelect(BATCH_ID, "selectTestDeliveryCoupon");

		// 取得したデータを格納
		for (Object[] row : objectList) {
			FsCouponTestDeliveryOutputDTO couponInfo = new FsCouponTestDeliveryOutputDTO();

			// FSクーポンID
			couponInfo.setCouponId(ConvertUtility.objectToLong(row[0]));
			// FS店舗UUID
			couponInfo.setFsStoreUuid(ConvertUtility.objectToString(row[1]));
			// 加盟店/カテゴリID
			couponInfo.setMerchantCategoryId(ConvertUtility.objectToLong(row[2]));

			// リストに格納
			couponInfoList.add(couponInfo);

		}

		return couponInfoList;

	}

	/**
	 *  クーポン情報を取得（クーポンテーブル、店舗マスタ、加盟店/カテゴリマスタ）（追加Push通知テスト配信）
	 * 
	 * @return FSクーポン登録・更新・削除用のクーポン情報DTOのリスト
	 */
	private List<FsCouponTestDeliveryOutputDTO> getCouponInfoListAdditionalPush(long couponId) {

		List<FsCouponTestDeliveryOutputDTO> couponInfoList = new ArrayList<>();

		// クーポンの情報を取得
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(COLUMN_COUPON_ID, couponId);
		List<Object[]> objectList = sqlSelect(BATCH_ID, "selectAdditionalPush", paramMap);

		// 取得したデータを格納
		for (Object[] row : objectList) {
			FsCouponTestDeliveryOutputDTO couponInfo = new FsCouponTestDeliveryOutputDTO();

			// FSクーポンID
			couponInfo.setCouponId(ConvertUtility.objectToLong(row[0]));
			// FS店舗UUID
			couponInfo.setFsStoreUuid(ConvertUtility.objectToString(row[1]));
			// 加盟店/カテゴリID
			couponInfo.setMerchantCategoryId(ConvertUtility.objectToLong(row[2]));

			// クーポンテーブル取得
			Optional<Coupons> coupons = findCoupons(couponInfo.getCouponId());
			if (!coupons.isPresent()) {
				// クーポンが取得できなかった場合
				continue;
			}
			couponInfo.setCoupons(coupons.get());

			// リストに格納
			couponInfoList.add(couponInfo);

		}

		return couponInfoList;

	}

	/**
	 *  クーポンテーブルのFSテスト配信状況を「2:FS連携中」に更新する。
	 * 
	 * @param couponInfoList FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean updateCouponsRun(List<FsCouponTestDeliveryOutputDTO> couponInfoList) {

		try {
			transactionBegin(BATCH_ID);

			for (FsCouponTestDeliveryOutputDTO couponInfo : couponInfoList) {
				logger.info("updateCouponsRun.couponInfo.couponId: " + couponInfo.getCouponId());

				// クーポンテーブル取得
				Optional<Coupons> coupons = findCoupons(couponInfo.getCouponId());
				if (!coupons.isPresent()) {
					// クーポンが取得できなかった場合

					// メッセージを出力
					writeRecordNotExistLog(COUPON_TEST_DELIVERY_MSG,
							String.format(COUPONS_COUPON_ID_MSG, couponInfo.getCouponId()),
							true);

					transactionRollback(BATCH_ID);

					return false;

				}
				couponInfo.setCoupons(coupons.get());

				// クーポンテーブルを更新
				couponInfo.getCoupons().setFsTestDeliveryStatus(FsDeliveryStatus.DELIVERING.getValue());
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
	 *  Push通知テーブルのFSテスト配信状況を「2:FS連携中」に更新する。
	 * 
	 * @param pushNotificationsList Push通知テーブルリスト
	 * 
	 * @return true:正常 false:異常
	 */
	private boolean updatePushNotificationsRun(List<PushNotifications> pushNotificationsList) {

		try {
			transactionBegin(BATCH_ID);

			for (PushNotifications pushNotifications : pushNotificationsList) {
				logger.info("updatePushNotificationsRun.pushNotifications.pushNotificationId: "
						+ pushNotifications.getPushNotificationId());

				// Push通知テーブルを更新
				pushNotifications.setFsTestDeliveryStatus(FsDeliveryStatus.DELIVERING.getValue());
				pushNotifications.setUpdateUserId(BATCH_ID);
				pushNotifications.setUpdateDate(DateUtils.now());

				pushNotificationsDAO.update(pushNotifications);

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
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param processKbn 処理区分
	 * 
	 */
	private void findRelatedInformation(FsCouponTestDeliveryOutputDTO couponInfo, ProcessKbn processKbn) {

		// (4.1.1)クーポン画像取得
		couponInfo.setCouponImagesList(findCouponImages(couponInfo.getCouponId()));

		// (4.1.2)クーポン特典取得
		couponInfo.setCouponIncentsList(findCouponIncents(couponInfo.getCouponId()));

		// クーポンテスト配信でセンサーイベントクーポンの場合
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)
				&& CouponType.SENSOR_EVENT.getValue().equals(couponInfo.getCoupons().getCouponType())) {

			// (4.1.3)Push通知取得
			List<PushNotifications> pushNotificationsList = findPushNotifications(couponInfo.getCouponId());
			if (!pushNotificationsList.isEmpty()) {
				// データが存在する場合
				couponInfo.setPushNotifications(pushNotificationsList.get(0));
			}

		}

		// (4.1.4)FSAPI用JSON取得
		// 「1:対象商品オブジェクト」のデータ取得
		List<FsApiJson> fsApiJsonList = findFsApiJsonTargetProduct(couponInfo.getCouponId(),
				couponInfo.getCouponIncentsList());
		// 「2:テキストオブジェクト」「3:リンクURLオブジェクト」「4:バーコードオブジェクト」のデータ取得
		List<FsApiJson> fsApiJsonTextLinkList = findFsApiJsonTextLink(couponInfo.getCouponId());
		fsApiJsonList.addAll(fsApiJsonTextLinkList);
		couponInfo.setFsApiJsonList(fsApiJsonList);

	}

	/**
	 * FS API連携を行う
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param processKbn 処理区分
	 * 
	 * @return FS APIレスポンス
	 */
	private FsApiCallResponse callFsApi(FsCouponTestDeliveryOutputDTO couponInfo, ProcessKbn processKbn) {

		FsApiCallResponse fsApiCallResponse = new FsApiCallResponse();

		// APIURL
		String url = integrationUrl + apiUrl;

		// リクエストBody取得(Json)
		String requestBody = getRequestBody(couponInfo, processKbn);
		if (requestBody == null) {
			// リクエストBody生成失敗
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);
			return fsApiCallResponse;
		}

		// 正常HTTPステータスコード取得
		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

		// FANSHIP APIを呼び出す
		return callFanshipApi(
				BATCH_ID,
				FS_COUPON_TEST_DELIVERY_MSG,
				url,
				requestBody,
				HttpMethodType.POST,
				ContentType.APPLICATION_JSON_CHARSET,
				TokenHeaderType.AUTHORIZATION,
				successHttpStatusList,
				retryCount,
				sleepTime,
				timeoutDuration,
				RetryKbn.SERVER_ERROR);

	}

	/**
	 * クーポンテスト配信の結果処理
	 * 
	 * @param processKbn 処理区分
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param その他クーポンテスト配信API OUTPUTDTO
	 * @param statusCode ステータスコード
	 * 
	 * @return FSクーポンテスト配信処理結果
	 */
	private FsCouponTestDeliveryResult testDeliveryApiResultProcess(ProcessKbn processKbn,
			FsCouponTestDeliveryOutputDTO couponInfo, CouponTestDeliveryApiOutputDTO outputDTO, int statusCode) {

		// クーポンUUIDの存在確認
		if (outputDTO.getUuid() == null || outputDTO.getUuid().isEmpty() || outputDTO.getUuid().length() > 36) {
			// メッセージを出力(error)
			writeApiErrorLog(statusCode,
					String.format(API_COUPON_UUID_ERROR, outputDTO.getUuid()),
					couponInfo, processKbn, null);

			return FsCouponTestDeliveryResult.UUID_ERROR;
		}
		// センサーの場合、Push配信IDの存在確認
		if (CouponType.SENSOR_EVENT.getValue().equals(couponInfo.getCoupons().getCouponType())
				&& (outputDTO.getId() == null || String.valueOf(outputDTO.getId()).length() > 18)) {
			// メッセージを出力(error)
			writeApiErrorLog(statusCode,
					String.format(API_PUSH_ID_ERROR, outputDTO.getId()),
					couponInfo, processKbn, null);

			return FsCouponTestDeliveryResult.UUID_ERROR;
		}

		// (4.3)FSテスト配信状況更新
		// (4.3a.1)以下の条件で【クーポンテーブル】を更新する。
		boolean updateResult = updateCoupons(couponInfo.getCoupons(), FsDeliveryStatus.DELIVERED);
		if (!updateResult) {
			// 更新エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getCouponId(), processKbn);

			return FsCouponTestDeliveryResult.DB_ERROR;
		}

		// (4.4)APIの戻り値がAPI成功条件を満たす場合、テスト配信テーブル登録
		// (4.4.1)以下の条件で【クーポンテスト配信テーブル】に登録する。
		boolean insertCouponTestResult = insertCouponTestDelivery(couponInfo, outputDTO, processKbn);
		if (!insertCouponTestResult) {
			// 登録エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getCouponId(), processKbn);

			return FsCouponTestDeliveryResult.DB_ERROR;
		}

		// (4.4.2)クーポン種別が「5:センサーイベントクーポン」の場合、以下の条件で【Push通知テスト配信テーブル】に登録する。
		if (CouponType.SENSOR_EVENT.getValue().equals(couponInfo.getCoupons().getCouponType())) {
			boolean insertPushTestResult = insertPushNotificationTestDelivery(couponInfo, outputDTO, processKbn);
			if (!insertPushTestResult) {
				// 登録エラーの場合

				// メッセージを出力
				writeErrorTargetLog(couponInfo.getCouponId(), processKbn);

				return FsCouponTestDeliveryResult.DB_ERROR;
			}
		}

		return FsCouponTestDeliveryResult.SUCCESS;
	}

	/**
	 * 追加Push通知テスト配信の結果処理
	 * 
	 * @param processKbn 処理区分
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param outputDTO その他クーポンテスト配信API OUTPUTDTO
	 * @param statusCode ステータスコード
	 * 
	 * @return FSクーポンテスト配信処理結果
	 */
	private FsCouponTestDeliveryResult additionalPushApiResultProcess(ProcessKbn processKbn,
			FsCouponTestDeliveryOutputDTO couponInfo, CouponTestDeliveryApiOutputDTO outputDTO, int statusCode) {

		// Push配信IDの存在確認
		if (outputDTO.getId() == null || String.valueOf(outputDTO.getId()).length() > 18) {
			// メッセージを出力(error)
			writeApiErrorLog(statusCode,
					String.format(API_PUSH_ID_ERROR, outputDTO.getId()),
					couponInfo, processKbn, null);

			return FsCouponTestDeliveryResult.UUID_ERROR;
		}

		// (6.4)FSテスト配信状況更新
		// (6.4a.1)以下の条件で【Push通知テーブル】を更新する。
		boolean updateResult = updatePushNotifications(couponInfo.getPushNotifications(), FsDeliveryStatus.DELIVERED);
		if (!updateResult) {
			// 更新エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getPushNotifications().getPushNotificationId(), processKbn);

			return FsCouponTestDeliveryResult.DB_ERROR;
		}

		// (6.5)APIの戻り値がAPI成功条件を満たす場合、テスト配信テーブル登録
		// (6.5.1)以下の条件で【クーポンテスト配信テーブル】に登録する。
		boolean insertCouponTestResult = insertCouponTestDelivery(couponInfo, null, processKbn);
		if (!insertCouponTestResult) {
			// 登録エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getPushNotifications().getPushNotificationId(), processKbn);

			return FsCouponTestDeliveryResult.DB_ERROR;
		}

		// (6.5.2)以下の条件で【Push通知テスト配信テーブル】に登録する。
		boolean insertPushTestResult = insertPushNotificationTestDelivery(couponInfo, outputDTO, processKbn);
		if (!insertPushTestResult) {
			// 登録エラーの場合

			// メッセージを出力
			writeErrorTargetLog(couponInfo.getPushNotifications().getPushNotificationId(), processKbn);

			return FsCouponTestDeliveryResult.DB_ERROR;
		}

		return FsCouponTestDeliveryResult.SUCCESS;
	}

	/**
	 * リクエストBody取得
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param processKbn 処理区分
	 * 
	 * @return リクエストBody
	 */
	private String getRequestBody(FsCouponTestDeliveryOutputDTO couponInfo, ProcessKbn processKbn) {

		try {

			if (creator == null) {
				creator = BatchFsCouponTestDeliveryRequestBodyCreator.getInstance();
			}

			return creator.createRequestBody(couponInfo, processKbn);

		} catch (Exception e) {

			// メッセージを出力(error)
			writeApiErrorLog(null, API_REQUEST_BODY_MSG, couponInfo, processKbn, e);

			return null;

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
	 * クーポンテーブルを更新
	 * 
	 * @param coupons クーポンテーブル
	 * @param fsDeliveryStatus FS連携状況
	 * 
	 * @return true:正常 false:異常
	 */
	private boolean updateCoupons(Coupons coupons, FsDeliveryStatus fsDeliveryStatus) {

		try {
			transactionBegin(BATCH_ID);

			// クーポンテーブルを更新
			coupons.setFsTestDeliveryStatus(fsDeliveryStatus.getValue());
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
	 *  Push通知テーブルを取得
	 * 
	 * @return Push通知テーブルリスト
	 */
	private List<PushNotifications> findPushNotificationsList() {

		// Push通知テーブルを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set("pushNotificationType", PushNotificationType.PROMOTION.getValue());
		daoParam.set("fsTestDeliveryStatus", FsDeliveryStatus.WAITING.getValue());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return pushNotificationsDAO.find(daoParam, null, false, null);

	}

	/**
	 *  Push通知テーブルを更新
	 * 
	 * @param Push通知テーブル
	 * @param fsDeliveryStatus FS連携状況
	 * 
	 * @return true:正常 false:異常
	 */
	private boolean updatePushNotifications(PushNotifications pushNotifications, FsDeliveryStatus fsDeliveryStatus) {

		try {
			transactionBegin(BATCH_ID);

			// Push通知テーブルを更新
			pushNotifications.setFsTestDeliveryStatus(fsDeliveryStatus.getValue());
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
	 *  Push通知テーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return Push通知テーブルリスト
	 */
	private List<PushNotifications> findPushNotifications(Long couponId) {

		// Push通知テーブルを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_COUPON_ID, couponId.toString());
		daoParam.set("pushNotificationType", PushNotificationType.DELIVERY.getValue());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return pushNotificationsDAO.find(daoParam, null, false, null);

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
	 *  クーポンテスト配信IDシーケンス取得
	 * 
	 * @return クーポンテスト配信IDシーケンス
	 * 
	 */
	private long createCouponTestDeliveryIdSequence() {

		Long sequence = null;

		// クーポンテスト配信IDシーケンス取得
		List<Object[]> objectList = sqlSelect(BATCH_ID, "seqCouponTestDeliveryId");

		for (Object row : objectList) {
			sequence = ConvertUtility.objectToLong(row);
		}

		return sequence;

	}

	/**
	 * クーポンテスト配信テーブル登録
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param couponTestDeliveryApiOutputDTO その他クーポンテスト配信API OUTPUTDTO
	 * @param processKbn 処理区分
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean insertCouponTestDelivery(FsCouponTestDeliveryOutputDTO couponInfo,
			CouponTestDeliveryApiOutputDTO couponTestDeliveryApiOutputDTO,
			ProcessKbn processKbn) {

		try {
			transactionBegin(BATCH_ID);

			// 現在日付
			Timestamp nowDate = DateUtils.now();

			// シーケンス取得
			long createCouponTestDeliveryId = createCouponTestDeliveryIdSequence();

			// クーポンテスト配信テーブル登録
			CouponTestDelivery couponTestDelivery = new CouponTestDelivery();
			couponTestDelivery.setCouponTestDeliveryId(createCouponTestDeliveryId);
			if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
				// クーポンテスト配信の場合
				couponTestDelivery.setFsCouponUuid(couponTestDeliveryApiOutputDTO.getUuid());

			} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
				// 追加Push通知テスト配信の場合
				couponTestDelivery.setFsCouponUuid(couponInfo.getCoupons().getFsCouponUuid());

			} else {
				throw new IllegalArgumentException();
			}
			couponTestDelivery.setCouponId(couponInfo.getCouponId());
			couponTestDelivery.setCreateUserId(BATCH_ID);
			couponTestDelivery.setCreateDate(nowDate);
			couponTestDelivery.setUpdateUserId(BATCH_ID);
			couponTestDelivery.setUpdateDate(nowDate);
			couponTestDelivery.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());

			couponTestDeliveryDAO.insert(couponTestDelivery);

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
	 *  Push通知テスト配信IDシーケンス取得
	 * 
	 * @return Push通知テスト配信IDシーケンス
	 * 
	 */
	private long createPushNotificationTestDeliveryIdSequence() {

		Long sequence = null;

		// Push通知テスト配信IDシーケンス取得
		List<Object[]> objectList = sqlSelect(BATCH_ID, "seqPushNotificationTestDeliveryId");

		for (Object row : objectList) {
			sequence = ConvertUtility.objectToLong(row);
		}

		return sequence;

	}

	/**
	 * Push通知テスト配信テーブル登録
	 * 
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param couponTestDeliveryApiOutputDTO その他クーポンテスト配信API OUTPUTDTO
	 * @param processKbn 処理区分
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean insertPushNotificationTestDelivery(FsCouponTestDeliveryOutputDTO couponInfo,
			CouponTestDeliveryApiOutputDTO couponTestDeliveryApiOutputDTO,
			ProcessKbn processKbn) {

		try {
			transactionBegin(BATCH_ID);

			// 現在日付
			Timestamp nowDate = DateUtils.now();

			// シーケンス取得
			long pushNotificationTestDeliveryId = createPushNotificationTestDeliveryIdSequence();

			// Push通知テスト配信テーブル登録
			PushNotificationTestDelivery pushNotificationTestDelivery = new PushNotificationTestDelivery();
			pushNotificationTestDelivery.setPushNotificationTestDeliveryId(pushNotificationTestDeliveryId);
			if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
				// クーポンテスト配信の場合
				pushNotificationTestDelivery.setPushNotificationType(PushNotificationType.DELIVERY.getValue());
				pushNotificationTestDelivery.setFsCouponUuid(couponTestDeliveryApiOutputDTO.getUuid());

			} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
				// 追加Push通知テスト配信の場合
				pushNotificationTestDelivery.setPushNotificationType(PushNotificationType.PROMOTION.getValue());
				pushNotificationTestDelivery.setFsCouponUuid(couponInfo.getCoupons().getFsCouponUuid());
			} else {
				throw new IllegalArgumentException();
			}
			pushNotificationTestDelivery.setFsPushNotificationUuid(couponTestDeliveryApiOutputDTO.getId());
			pushNotificationTestDelivery.setCouponId(couponInfo.getCouponId());
			pushNotificationTestDelivery.setCreateUserId(BATCH_ID);
			pushNotificationTestDelivery.setCreateDate(nowDate);
			pushNotificationTestDelivery.setUpdateUserId(BATCH_ID);
			pushNotificationTestDelivery.setUpdateDate(nowDate);
			pushNotificationTestDelivery.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());

			pushNotificationTestDeliveryDAO.insert(pushNotificationTestDelivery);

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
	 * APIエラーログ出力
	 * 
	 * @param statusCode HTTPステータスコード
	 * @param msg メッセージ
	 * @param couponInfo FSクーポンテスト配信バッチ用のクーポン情報DTO
	 * @param processKbn 処理区分
	 * @param e Exception
	 * 
	 */
	private void writeApiErrorLog(Integer statusCode, String msg, FsCouponTestDeliveryOutputDTO couponInfo,
			ProcessKbn processKbn, Exception e) {

		String msgDetail = "";

		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// クーポンテスト配信の場合

			if (CouponType.SENSOR_EVENT.getValue().equals(couponInfo.getCoupons().getCouponType())) {
				// センサーイベントの場合
				msgDetail = String.format(COUPON_ID_PUSH_TYPE_MSG, couponInfo.getCoupons().getCouponId());

			} else {
				// センサーイベント以外の場合
				msgDetail = String.format(COUPON_ID_MSG, couponInfo.getCoupons().getCouponId());
			}

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// 追加Push通知テスト配信の場合

			msgDetail = String.format(PUSH_ID_TYPE_MSG, couponInfo.getPushNotifications().getPushNotificationId());

		} else {
			throw new IllegalArgumentException();
		}

		// メッセージを出力（%sのAPI連携に失敗しました。（HTTPレスポンスコード ＝「%s」,エラー内容 = 「%s」））
		String errorMsg = String.format(
				BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
				FS_COUPON_TEST_DELIVERY_MSG,
				statusCode,
				msg + msgDetail);

		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMsg), e);

	}

	/**
	 * レスポンスボディ取得
	 * 
	 * @param response HTTPレスポンス
	 * 
	 * @return その他クーポンテスト配信API OUTPUT
	 * 
	 */
	private CouponTestDeliveryApiOutputDTO getResponseBody(HttpResponse<String> response) {

		CouponTestDeliveryApiOutputDTO outputDTO = new CouponTestDeliveryApiOutputDTO();

		try {

			if (response != null && response.body() != null && !response.body().isEmpty()) {
				// レスポンスBodyを取得
				outputDTO = mapper.readValue(response.body(), CouponTestDeliveryApiOutputDTO.class);
			}

			return outputDTO;

		} catch (Exception e) {
			log.info(String.format(API_DESERIALIZE_ERROR), e);

			// エラーの場合、空を返す
			return outputDTO;

		}

	}

	/**
	 *  データ取得エラー
	 *  
	 * @param process 処理
	 * @param detail 詳細
	 * @param errorFlg エラーフラグ
	 * 
	 */
	private void writeRecordNotExistLog(String process, String detail, boolean errorFlg) {

		// メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
				process,
				detail);

		if (errorFlg) {
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));
		} else {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));
		}

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
	 *  @param id クーポンIDまたはPush通知ID
	 *  @param processKbn 処理区分
	 *  
	 */
	private void writeProcessTargetLog(Long id, ProcessKbn processKbn) {

		String msg = "";
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// メッセージを出力（処理対象：クーポンID = %s）
			msg = String.format(START_COUPON_MSG, id);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// メッセージを出力（処理対象：Push通知ID = %s）
			msg = String.format(START_PUSH_TYPE_MSG, id);

		} else {
			throw new IllegalArgumentException();
		}

		logger.info(msg);
	}

	/**
	 *  スキップ対象メッセージ
	 *  
	 *  @param id クーポンIDまたはPush通知ID
	 *  @param processKbn 処理区分
	 *  
	 */
	private void writeSkipTargetLog(Long id, ProcessKbn processKbn) {

		String msg = "";
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// メッセージを出力（処理中断スキップ対象：クーポンID = %s）
			msg = String.format(SKIP_COUPON_MSG, id);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// メッセージを出力（処理中断スキップ対象：Push通知ID = %s）
			msg = String.format(SKIP_PUSH_TYPE_MSG, id);

		} else {
			throw new IllegalArgumentException();
		}

		logger.info(msg);
	}

	/**
	 *  エラー対象メッセージ
	 *  
	 *  @param id クーポンIDまたはPush通知ID
	 *  @param processKbn 処理区分
	 *  
	 */
	private void writeErrorTargetLog(Long id, ProcessKbn processKbn) {

		String msg = "";
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// メッセージを出力（エラー対象：クーポンID ＝ %s）
			msg = String.format(ERROR_TARGET_COUPON_MSG, id);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// メッセージを出力（エラー対象：Push通知ID = %s）
			msg = String.format(ERROR_TARGET_PUSH_MSG, id);

		} else {
			throw new IllegalArgumentException();
		}

		logger.info(msg);
	}

	/**
	 *  処理正常終了メッセージ
	 *  
	 *  @param id クーポンIDまたはPush通知ID
	 *  @param processKbn 処理区分
	 *  
	 */
	private void writeFinishTargetLog(Long id, ProcessKbn processKbn) {

		String msg = "";
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// メッセージを出力（処理正常終了：クーポンID ＝ %s）
			msg = String.format(FINISH_COUPON_MSG, id);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// メッセージを出力（処理正常終了：Push通知ID = %s）
			msg = String.format(FINISH_PUSH_TYPE_MSG, id);

		} else {
			throw new IllegalArgumentException();
		}

		logger.info(msg);
	}

	/**
	 *  エラー対象メッセージ(API詳細メッセージ)
	 *  
	 *  @param id クーポンIDまたはPush通知ID
	 *  @param processKbn 処理区分
	 *  @param code エラーコード
	 *  @param developerMessage 開発者向けメッセージ
	 *  @param userMessage ユーザー向けメッセージ
	 *  
	 */
	private void writeErrorTargetDetailLog(Long id, ProcessKbn processKbn, String code, String developerMessage,
			String userMessage) {

		String msg = "";
		if (ProcessKbn.TEST_DELIVERY.equals(processKbn)) {
			// メッセージを出力（エラー対象：クーポンID = %s, エラーコード = %s, 開発者向けメッセージ = %s, ユーザー向けメッセージ = %s）
			msg = String.format(ERROR_TARGET_COUPON_DETAIL_MSG, id, code, developerMessage, userMessage);

		} else if (ProcessKbn.ADDITIONAL_PUSH.equals(processKbn)) {
			// メッセージを出力（エラー対象：Push通知ID = %s, エラーコード = %s, 開発者向けメッセージ = %s, ユーザー向けメッセージ = %s）
			msg = String.format(ERROR_TARGET_PUSH_DETAIL_MSG, id, code, developerMessage, userMessage);

		} else {
			throw new IllegalArgumentException();
		}

		logger.info(msg);
	}

}
