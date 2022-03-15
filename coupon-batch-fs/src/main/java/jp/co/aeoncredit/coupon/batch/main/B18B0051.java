package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryListGetInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryListGetOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryListGetOutputDTOResult;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationDeliveryType;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationStatisticsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.PushNotificationStatistics;
import jp.co.aeoncredit.coupon.entity.PushNotifications;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSPush通知一覧取得バッチ
 */
@Named("B18B0051")
@Dependent
public class B18B0051 extends BatchFSApiCalloutBase {

	/** エラーメッセージ用 */
	private static final String FANHIP_API_NAME = "配信一覧取得";

	protected HttpClient httpClient = null;

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0051.getBatchId();

	/** バッチ名 */
	private static final String BATCH_NAME = BatchInfo.B18B0051.getBatchName();

	/** 正常HTTPステータスコード */
	private static final int SUCCESS_HTTP_STATUS = 200;

	protected static final String API_URL = "fs.push.notification.list.get.batch.api.url";

	/** FS API失敗時のAPI実行リトライ回数 */
	protected static final String RETRY_COUNT = "fs.push.notification.list.get.batch.retry.count";

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected static final String RETRY_SLEEP_TIME = "fs.push.notification.list.get.batch.retry.sleep.time";

	/** FS API発行時のタイムアウト期間 */
	protected static final String TIMEOUT_DURATION = "fs.push.notification.list.get.batch.timeout.duration";

	protected static final String API_COUNT = "100";

	/** ログインAPIのURL */
	private String pushNotificationUrl;

	/** FS API 失敗時のAPI実行リトライ回数 */
	private int retryCount;

	/** FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒) */
	private int sleepTime;

	/** FS API発行時のタイムアウト期間(秒) */
	private int timeoutDuration;

	/** error数のカウント */
	private int errorCount = 0;

	/** skip数のカウント */
	private int skipCount = 0;

	/** 処理対象件数 */
	private int allProcces;

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** プロパティファイル共通 */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	@Inject
	protected PushNotificationStatisticsDAOCustomize pushNotificationStatisticsDAO;

	@Inject
	protected PushNotificationsDAOCustomize pushNotificationsDAO;

	@Inject
	protected CouponsDAOCustomize couponsDAO;

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

		// FSPush通知一覧取得処理の開始
		String returnCode = fsPushNotificationListProcess();

		// 処理終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				ProcessResult.SUCCESS.getValue().equals(returnCode)));

		return setExitStatus(returnCode);
	}

	/**
	 * プロパティファイルを読み込む
	 */
	protected void readProperties() {

		Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		// ログインAPIのURL
		this.pushNotificationUrl = pro.getProperty(API_URL);

		// FS API失敗時のAPI実行リトライ回数
		this.retryCount = Integer.parseInt(pro.getProperty(RETRY_COUNT));

		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		this.sleepTime = Integer.parseInt(pro.getProperty(RETRY_SLEEP_TIME));

		// FS API発行時のタイムアウト期間(秒)
		this.timeoutDuration = Integer.parseInt(pro.getProperty(TIMEOUT_DURATION));

	}

	/**
	 * FSPush通知一覧取得処理の開始
	 */
	private String fsPushNotificationListProcess() {
	  String result = ProcessResult.SUCCESS.getValue();
		// (2)【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		// (2.1b.1)戻り値が「503」(FANSHIPメンテナンス)の場合
		if (authTokenResult == AuthTokenResult.MAINTENANCE) {
		  return result;
		} else if (authTokenResult == AuthTokenResult.FAILURE) {
			// (2.1b.2) 上記以外の失敗
		  result = ProcessResult.FAILURE.getValue();
          return result;
		} else if (authTokenResult == AuthTokenResult.SUCCESS) {

			try {
				// (2.1a) 成功した場合
			  result = apiAlignment();
			} catch (Exception e) {
				logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
				errorCount++;
				result = ProcessResult.FAILURE.getValue();
		        return result;
			}

		}
		return result;
	}

	/**
	 * FS API連携
	 */
	private String apiAlignment() {
	    String returnCode = ProcessResult.SUCCESS.getValue();
		ObjectMapper mapper = new ObjectMapper();
		String script = null;
		DeliveryListGetOutputDTO outputDTO = null;
		DeliveryListGetInputDTO input = new DeliveryListGetInputDTO();

		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(SUCCESS_HTTP_STATUS);

		// URLを設定
		String url = this.reverseProxyUrl + pushNotificationUrl;

		int page = 1;

		input.setCount(API_COUNT);

		try {
			do {

				input.setPage(Integer.toString(page));

				script = mapper.writeValueAsString(input);

				// FS API呼出
				FsApiCallResponse fsApiResponse = callFanshipApi(BATCH_ID, BATCH_NAME, url, script, HttpMethodType.POST,
						ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION_POPINFOLOGIN, successHttpStatusList,
						retryCount, sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);

				if (fsApiResponse == null) {
					throw new AssertionError();
				}
				FsApiCallResult fsApiCallResult = fsApiResponse.getFsApiCallResult();

				if (fsApiCallResult != FsApiCallResult.SUCCESS) {
					apiErrorProcessTargetLog(page);
				}

				switch (fsApiCallResult) {
				case SUCCESS:
					// 再認証なし or 再認証成功
					HttpResponse<String> httpResponses = fsApiResponse.getResponse();

						outputDTO = mapper.readValue(httpResponses.body(), DeliveryListGetOutputDTO.class);

						if (outputDTO.getStatus().equals("OK")) {

							// (3.2)Push通知統計テーブルに登録
							for (int j = 0; j < outputDTO.getResult().size(); j++) {
								insertPushNotificationStatistics(outputDTO.getResult().get(j));
							}
							// pageをカウントアップする。
							page++;

						} else {
							String message = "";
							if (outputDTO.getError() != null) {
								message = outputDTO.getError().getMessage();
							}
							
							// エラーメッセージを出力
							respponseError(httpResponses.statusCode(), page, outputDTO.getStatus(), message);
							returnCode = ProcessResult.FAILURE.getValue();
							errorCount++;
						}

					break;

				case SERVER_ERROR:
					// リトライ回数超過                   
                    returnCode = ProcessResult.FAILURE.getValue();
					
					break;

				case TIMEOUT:
					// タイムアウト
                    returnCode = ProcessResult.FAILURE.getValue();
					
					break;

				case FANSHIP_MAINTENANCE:
					// 再認証でメンテナンス
					// メッセージは認証トークン取得処理内で出力
                    apiError(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), page);
					break;
					
				case TOO_MANY_REQUEST:
				    apiError(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), page);
				    break;

				case AUTHENTICATION_ERROR:
					// 再認証失敗
					// メッセージは認証トークン取得処理内で出力
                    returnCode = ProcessResult.FAILURE.getValue();
					break;

				case CLIENT_ERROR:          
                    returnCode = ProcessResult.FAILURE.getValue();
					break;

				case OTHERS_ERROR:
					// その他エラー
                    returnCode = ProcessResult.FAILURE.getValue();
					break;

				default:
					break;
				}

			} while (outputDTO != null && outputDTO.getHasNextPage());

			if (allProcces - skipCount >= 1) {
				// トランザクションのコミット
				transactionCommit(BATCH_ID);
			}

		} catch (Exception e) {
			if (allProcces - skipCount >= 1) {
				// トランザクションのロールバック
				transactionRollback(BATCH_ID);
			}
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			returnCode = ProcessResult.FAILURE.getValue();

		} finally {
			// 実行結果の出力
			resultInformation();
		}
		return returnCode;
	}

	/**
	 * Push通知統計テーブルに登録する。
	 * 
	 * @param result 配信一覧取得のoutputDTO
	 */
	private void insertPushNotificationStatistics(DeliveryListGetOutputDTOResult result) {

		// 処理件数のカウント
		allProcces++;

		Timestamp insertTime = DateUtils.now();

		// 各インスタンスの作成
		Coupons coupons = new Coupons();
		PushNotificationStatistics pushNotificationStatistics = new PushNotificationStatistics();

		// push通知テーブルからselectする。
		DAOParameter pushNotificationsParam = new DAOParameter();
		pushNotificationsParam.set("fsPushNotificationUuid", result.getId());
		List<PushNotifications> pushNotificationslist = pushNotificationsDAO.find(pushNotificationsParam, null, false,
				null);

		if (pushNotificationslist.isEmpty()) {
			dbEmptyError("push通知テーブル", "FSPush通知UUID", result.getId());
			skipCount++;
			return;
		}
		try {
			if ((allProcces - skipCount) == 1) {
				// トランザクションの開始
				transactionBegin(BATCH_ID);
			}

			for (PushNotifications pushNotifications : pushNotificationslist) {
				// 処理対象を出力
				writeProcessTargetLog(pushNotifications.getPushNotificationId());

				DAOParameter couponsParam = new DAOParameter();
				couponsParam.set("couponId", pushNotifications.getCouponId().toString());
				List<Coupons> couponslist = couponsDAO.find(couponsParam, null, false, null);

				if (couponslist.isEmpty()) {
					dbEmptyError("クーポンテーブル", "クーポンID", pushNotifications.getCouponId().toString());
					skipCount++;
					continue;
				}

				// インスタンスにselectしたレコードを格納する
				coupons = couponslist.get(0);

				String type = result.getType();
				String deliverystatus = result.getStatus();

				// 値をセットする。
				/** 1.取得日時 */
				pushNotificationStatistics.setAcquisitionDatetime(insertTime);
				/** 2.FSクーポンUUID */
				pushNotificationStatistics.setFsCouponUuid(coupons.getFsCouponUuid());
				/** 3.クーポンID */
				pushNotificationStatistics.setCouponId(coupons.getCouponId());
				/** 4.FSPUSH通知UUID */
				pushNotificationStatistics.setFsPushNotificationUuid(ConvertUtility.objectToLong(result.getId()));
				/** 5.PUSH通知ID */
				pushNotificationStatistics.setPushNotificationId(pushNotifications.getPushNotificationId());
				/** 6.PUSH通知種別 */
				pushNotificationStatistics.setPushNotificationType(pushNotifications.getPushNotificationType());

				if (type != null) {
					/** 7.PUSH通知配信タイプ */
					switch (type) {
					case "scheduled":
						pushNotificationStatistics
								.setPushNotificationDeliveryType(PushNotificationDeliveryType.RESERVATION.getValue());
						break;
					case "location":
						pushNotificationStatistics
								.setPushNotificationDeliveryType(PushNotificationDeliveryType.GPS.getValue());
						break;
					case "bluetooth":
						pushNotificationStatistics
								.setPushNotificationDeliveryType(PushNotificationDeliveryType.BLUETOOTH.getValue());
						break;
					}
				}
				if (deliverystatus != null) {
					/** 8.PUSH通知配信状況 */
					switch (deliverystatus) {
					case "finished":
						pushNotificationStatistics
								.setPushNotificationStatus(PushNotificationDeliveryStatus.COMPLETE.getValue());
						break;
					case "canceled":
						pushNotificationStatistics
								.setPushNotificationStatus(PushNotificationDeliveryStatus.CANCEL.getValue());
						break;
					case "delivering":
						pushNotificationStatistics
								.setPushNotificationStatus(PushNotificationDeliveryStatus.DELIVERY.getValue());
						break;
					}
				}
				/** 9.配信日時 */
				pushNotificationStatistics.setSendTime(ConvertUtility.stringToTimestamp(result.getSendTime()));

				if (result.getPeriod() != null) {
					/** 10.配信期間FROM */
					pushNotificationStatistics
							.setSendPeriodFrom(ConvertUtility.stringToTimestamp(result.getPeriod().get(0).getStart()));
					/** 11.配信期間TO */
					pushNotificationStatistics
							.setSendPeriodTo(ConvertUtility.stringToTimestamp(result.getPeriod().get(0).getEnd()));
				}
				if (result.getSent() != null) {
					/** 12.配信数 */
					pushNotificationStatistics
							.setCounterDistributed(ConvertUtility.stringToInteger(result.getSent().getTotal()));
				}
				if (result.getView() != null) {
					/** 13.開封数(お知らせ) */
					pushNotificationStatistics
							.setCounterViewedNotification(ConvertUtility.stringToInteger(result.getView().getTotal()));
				}
				if (result.getOpen() != null) {
					/** 14.開封数(Push) */
					pushNotificationStatistics
							.setCounterOpened(ConvertUtility.stringToInteger(result.getOpen().getTotal()));
				}
				if (result.getClick() != null) {
					/** 15.サイト閲覧数(クーポン) */
					pushNotificationStatistics
							.setCounterViewedSite(ConvertUtility.stringToInteger(result.getClick().getTotal()));
				}
				/** 16.作成者ID */
				pushNotificationStatistics.setCreateUserId(BATCH_ID);
				/** 17.作成日 */
				pushNotificationStatistics.setCreateDate(insertTime);
				/** 18.更新者ID */
				pushNotificationStatistics.setUpdateUserId(BATCH_ID);
				/** 19.更新日 */
				pushNotificationStatistics.setUpdateDate(insertTime);
				/** 20.削除フラグ */
				pushNotificationStatistics.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());

				pushNotificationStatisticsDAO.insert(pushNotificationStatistics);

				// 処理対象を出力
		        logger.info( String.format("処理正常終了：Push通知ID = %s", pushNotifications.getPushNotificationId()));
			}
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			errorCount++;
			throw new RuntimeException(e);
		}
	}

	/**
	 * テーブルの取得結果が0件の時、ログを出力する。
	 * 
	 * @param table テーブル名
	 * @param str   カラム名
	 * @param id    id
	 * 
	 */
	private void dbEmptyError(String table, String str, String id) {
		String msg = table + "のレコードが取得できませんでした(" + str + ":[" + id + "])";
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB926.toString(), msg));
	}

	/**
	 * 配信一覧取得APIでエラー発生時
	 * 
	 * @param code HTTPレスポンスコード名
	 * @param page ページ
	 * 
	 */
	private void apiError(Integer httpStatusCode, int page) {
		String msg = null;

		if (httpStatusCode == HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue()) {
			// 503の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("ページ =%s,エラーメッセージ=「FANSHIPメンテナンス」", page));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

		} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue()) {
			// 429の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("ページ =%s,エラーメッセージ=「リクエストが制限されています。」", page));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

		} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue()) {
			// 401の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("ページ =%s,エラーメッセージ=「認証エラー」", page));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));
		} else {
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("ページ =%s", page));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));
		}
	}

	/**
	 * レスポンス取得でエラー発生時
	 * 
	 * @param code         HTTPレスポンスコード名
	 * @param page         ページ
	 * @param responseBody レスポンスボディ
	 * @param errorMsg     エラーメッセージ
	 * 
	 */
	private void respponseError(Integer httpStatusCode, int page, String responseBody, String errorMsg) {
		String msg = null;

		msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
				httpStatusCode, String.format("ページ =%s、レスポンスボディ=%s、メッセージ=%s", page, responseBody, errorMsg));
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

	}

	/**
	 * 全てのPush通知情報の処理が完了時の処理件数を出力する。
	 * 
	 */
	private void resultInformation() {
		int successCount = allProcces - errorCount - skipCount;
		String msg = "Push通知一覧取得処理が完了しました。(処理対象件数:[" + allProcces + "] , 処理成功件数:[" + successCount + "], 処理失敗件数:["
				+ errorCount + "],処理スキップ件数:[ " + skipCount + "])";
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg));
	}

	/**
	 * 処理対象メッセージ
	 * 
	 * @param targetId 処理対象ID
	 * 
	 */
	private void writeProcessTargetLog(Long targetId) {
		String msg = null;

		msg = String.format("処理対象：Push通知ID = %s", targetId);

		logger.info(msg);

	}

	/**
	 *エラー対象メッセージ(API連携時)
	 * 
	 * @param targetPage エラー対象ページ
	 * 
	 */
	private void apiErrorProcessTargetLog(Integer targetPage) {
		String msg = null;

		msg = String.format("エラー対象：ページ = %s", targetPage);

		logger.info(msg);

	}

}
