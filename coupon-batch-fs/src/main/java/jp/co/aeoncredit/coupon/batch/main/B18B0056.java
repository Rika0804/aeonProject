package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryRegisterInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryRegisterInputDTOContent;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryRegisterOutputDTO;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.DeliveryType;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationStatus;
import jp.co.aeoncredit.coupon.constants.PushNotificationType;
//Push通知統計テーブル
//クーポンテーブル
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
//Push通知テーブル
import jp.co.aeoncredit.coupon.dao.custom.PushNotificationsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.PushNotifications;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSPush通知発行バッチ
 */
@Named("B18B0056")
@Dependent
public class B18B0056 extends BatchFSApiCalloutBase {

	/** エラーメッセージ用 */
	private static final String FANHIP_API_NAME = "配信登録";

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0056.getBatchId();

	/** バッチ名 */
	private static final String BATCH_NAME = BatchInfo.B18B0056.getBatchName();

	/** 正常HTTPステータスコード */
	private static final int SUCCESS_HTTP_STATUS = 200;

	private static final String DATA_FORMAT_YYYYMMDD = "yyyy-MM-dd";

	private static final String DATA_FORMAT_HHMMSS = "HH:mm:ss";

	private static final String DATA_FORMAT_YYYYMMDD_HHMMSS = "yyyy-MM-dd HH:mm:ss";

	/** 正常終了_戻り値 */
	protected static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	protected static final String FAIL_RETURN_VALUE = "1";

	/** HTTPステータスコード */
	protected static final int HTTP_STATUS_401 = 401;

	/** 実行後の戻り値 */
	private ProcessResult returnCode = ProcessResult.SUCCESS;

	/** アプリスキーム */
	private String appScheme;

	/** FS API失敗時のAPI実行リトライ回数 */
	protected int retryCount;

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected int sleepTime;

	/** FS API発行時のタイムアウト期間 */
	protected int timeoutDuration;

	/** ログインAPIのURL */
	private String apiUrl;

	/** FS API失敗時のAPI実行リトライ回数 */
	protected static final String RETRY_COUNT = "fs.add.batch.push.notification.add.retry.count";

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected static final String RETRY_SLEEP_TIME = "fs.add.batch.push.notification.add.retry.sleep.time";

	/** FS API発行時のタイムアウト期間 */
	protected static final String TIMEOUT_DURATION = "fs.add.batch.push.notification.add.timeout.duration";

	/** ログインAPIのURL */
	protected static final String API_URL = "fs.add.batch.push.notification.add.api.url";

	protected static final String REQUEST_BODY_URL = "://jp.popinfo.coupon/?uuid=%s&destinationPage=/fscoupon/detail&action=open";

	/** error数のカウント */
	private int processCount;

	/** error総数 */
	private int errorCount;

	/** skipした総数 */
	private int skipCount;

	/** ログ */
	protected Logger logger = getLogger();

	/** システム日付 */
	protected Date today;

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** プロパティファイル共通 */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	private static final String FS_DELIVERY_STATUS = "fsDeliveryStatus";
	private static final String FS_DELIVERY_STATUS_2 = "fsDeliveryStatus2";
	private static final String PUSH_NOTIFICATION_TYPE = "pushNotificationType";
	private static final String PUSH_NOTIFICATION_STATUS = "pushNotificationStatus";
	private static final String DELETE_FLAG = "deleteFlag";
	private static final String PUSH_NOTIFICATION_UUID = "fsPushNotificationUuid";
	private static final String UPDATE_USER_ID = "updateUserId";
	private static final String UPDATE_DATE = "updateDate";
	private static final String PUSH_NOTIFICATION_ID = "pushNotificationId";
	/** SQL NAME */
	protected static final String SQL_SELECT_PUSHNOTIFICATIONS = "selectPushNotifictions";
	protected static final String SQL_UPDATE_PUSHNOTIFICATIONS = "updatePushNotifictions";
	protected static final String SQL_END_UPDATE_PUSHNOTIFICATIONS = "endupdatePushNotifictions";
	@Inject
	protected PushNotificationsDAOCustomize pushNotificationsDAO;

	@Inject
	protected CouponsDAOCustomize couponsDAO;

	/** JSON作成用のオブジェクトマッパー */
	private ObjectMapper mapper = new ObjectMapper();

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
		fsPushNotificationListProcess();

		// 処理終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				SUCCESS_RETURN_VALUE.equals(returnCode.getValue())));

		return setExitStatus(returnCode.getValue());
	}

	/**
	 * プロパティファイルを読み込む
	 */
	protected void readProperties() {

		Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		this.apiUrl = pro.getProperty(API_URL);

		// アプリスキーム
		this.appScheme = System.getenv(Constants.ENV_FS_APP_SCHEME);

		// FS API失敗時のAPI実行リトライ回数
		this.retryCount = Integer.parseInt(pro.getProperty(RETRY_COUNT));

		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		this.sleepTime = Integer.parseInt(pro.getProperty(RETRY_SLEEP_TIME));

		// FS API発行時のタイムアウト期間(秒)
		this.timeoutDuration = Integer.parseInt(pro.getProperty(TIMEOUT_DURATION));
	}

	/**
	 * (2)FS 認証トークン取得
	 */
	private void fsPushNotificationListProcess() {

		// (2)【B18BC001_認証トークン取得】を実行する。

		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);
		switch (authTokenResult) {

		// (2.1b.1)戻り値が「503」(FANSHIPメンテナンス)の場合
		case MAINTENANCE:
			break;

		// (2.1b.2) 上記以外の失敗
		case FAILURE:
			returnCode = ProcessResult.FAILURE;
			break;
		// (2.1a) 成功した場合
		case SUCCESS:
			// (3)処理対象Push通知取得
			getPushNotificationProcesse();
			break;
		}
	}

	/**
	 * (3)処理対象Push通知取得
	 */
	private void getPushNotificationProcesse() {

		int recodeNumber = 0;

		List<PushNotifications> pushNotificationsList = new ArrayList<PushNotifications>();
		List<Coupons> couponsList = new ArrayList<Coupons>();

		// (3.1)Push通知を取得する。
		getPushNotifications(pushNotificationsList, couponsList);

		if (pushNotificationsList.isEmpty()) {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), "処理対象レコードがありません。（Push通知発行）"));
			return;
		}

		processCount = pushNotificationsList.size();
		try {

			// FS連携状況を「FS連携中」に更新する。
			updateDrivingPushNotifications(pushNotificationsList);

			for (PushNotifications pushNotifications : pushNotificationsList) {

				// 処理対象のPush通知IDを出力
				writeProcessTargetLog(pushNotifications.getPushNotificationId());

				// (4)FS API 連携
				apiAlignment(pushNotifications, couponsList.get(recodeNumber));
				recodeNumber++;
			}

		} catch (Exception e) {
			returnCode = ProcessResult.FAILURE;
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			return;

		}
		// (5)処理件数をログに出力する。
		resultInformation();

	}

	/**
	 * (3.1)Push通知を取得する。
	 * 
	 * @param pushNotificationslist ... push通知テーブル
	 * @param couponslist           ... クーポンテーブル
	 */
	private List<Object[]> getPushNotifications(List<PushNotifications> pushNotificationslist,
			List<Coupons> couponslist) {

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.WAITING.getValue());
		paramMap.put(FS_DELIVERY_STATUS_2, FsDeliveryStatus.DELIVERING.getValue());
		paramMap.put(PUSH_NOTIFICATION_TYPE, PushNotificationType.PROMOTION.getValue());
		paramMap.put(PUSH_NOTIFICATION_STATUS, PushNotificationStatus.APPROVED.getValue());
		paramMap.put(DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());

		// (2)PushNotificationsリスト取得
		List<Object[]> pushNotificationsList = sqlSelect(BATCH_ID, SQL_SELECT_PUSHNOTIFICATIONS, paramMap);

		for (Object[] row : pushNotificationsList) {

			PushNotifications pushNotificationsInfo = new PushNotifications();
			Coupons couponsInfo = new Coupons();

			// Push通知ID
			pushNotificationsInfo.setPushNotificationId(ConvertUtility.objectToLong(row[0]));
			// お知らせ本文
			pushNotificationsInfo.setNotificationBody(ConvertUtility.objectToString(row[1]));
			// ボタン名称
			pushNotificationsInfo.setButtonDisplayName(ConvertUtility.objectToString(row[2]));
			// ヘッダ画像
			pushNotificationsInfo.setHeaderImageUrl(ConvertUtility.objectToString(row[3]));
			// Push通知本文
			pushNotificationsInfo.setPushNotificationText(ConvertUtility.objectToString(row[4]));
			// コンテンツ件名
			pushNotificationsInfo.setNotificationTitle(ConvertUtility.objectToString(row[5]));
			// 配信日時
			pushNotificationsInfo.setSendDate(ConvertUtility.objectToTimestamp(row[6]));
			// FSセグメントID
			pushNotificationsInfo.setFsSegmentId(ConvertUtility.objectToLong(row[7]));

			pushNotificationslist.add(pushNotificationsInfo);

			// 表示期間_終了
			couponsInfo.setDisplaydateTo(ConvertUtility.objectToTimestamp(row[8]));
			couponsInfo.setFsCouponUuid(ConvertUtility.objectToString(row[9]));
			couponslist.add(couponsInfo);

		}

		return pushNotificationsList;
	}

	/**
	 * (4)FS API 連携
	 * 
	 * @param pushNotifications ... Push通知テーブル
	 * @param coupons           ... クーポンテーブル
	 */
	private ProcessResult apiAlignment(PushNotifications pushNotifications, Coupons coupons) {

		ProcessResult returnStatus = ProcessResult.FAILURE;
		String script = null;

		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(SUCCESS_HTTP_STATUS);

		// URLを設定
		String url = this.reverseProxyUrl + apiUrl;

		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			script = mapper.writeValueAsString(creaetRequestBody(pushNotifications, coupons));

			// (4.1)FS API Push通知登録
			FsApiCallResponse fsApiResponse = callFanshipApi(BATCH_ID, BATCH_NAME, url, script, HttpMethodType.POST,
					ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION_POPINFOLOGIN, successHttpStatusList,
					retryCount, sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR);

			if (fsApiResponse == null) {
				throw new AssertionError();
			}

			FsApiCallResult fsApiCallResult = fsApiResponse.getFsApiCallResult();

			if (fsApiCallResult != FsApiCallResult.SUCCESS) {
				apiErrorProcessTargetLog(pushNotifications.getPushNotificationId());
			}

			switch (fsApiCallResult) {
			case SUCCESS:
				// 再認証なし or 再認証成功
				HttpResponse<String> httpResponses = fsApiResponse.getResponse();

				DeliveryRegisterOutputDTO outputDTO = mapper.readValue(httpResponses.body(),
						DeliveryRegisterOutputDTO.class);

				if (outputDTO.getStatus().equals("OK")) {
					// (4.1a)配信登録APIの戻り値がAPI成功条件を満たす場合

					// トランザクションの開始
					transactionBegin(BATCH_ID);

					updatePushNotificationsAfter(pushNotifications.getPushNotificationId(),
							FsDeliveryStatus.DELIVERED, outputDTO);

					// トランザクションのコミット
					transactionCommit(BATCH_ID);

					returnStatus = ProcessResult.SUCCESS;
					writeFinishTargetLog(pushNotifications.getPushNotificationId());
				} else {
					// エラーメッセージを出力
					respponseError(httpResponses.statusCode(), pushNotifications.getPushNotificationId(),
							outputDTO.getStatus(), outputDTO.getError().getMessage());
					returnCode = ProcessResult.FAILURE;
					errorCount++;

				}

				break;

			case TIMEOUT:
				// タイムアウト
				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;

			case SERVER_ERROR:
				// リトライ回数超過
				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;

			case FANSHIP_MAINTENANCE:
				// 再認証でメンテナンス
				// メッセージは認証トークン取得処理内で出力
			    apiError(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), pushNotifications.getPushNotificationId());
				errorCount++;
				break;
				
			case TOO_MANY_REQUEST:
                // 再認証でメンテナンス
			    // メッセージは認証トークン取得処理内で出力
			    apiError(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), pushNotifications.getPushNotificationId());
			    errorCount++;
                break;

			case AUTHENTICATION_ERROR:
				// 再認証失敗
				// メッセージは認証トークン取得処理内で出力
				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;

			case CLIENT_ERROR:
				// クライアントエラー(4xx/401,429以外)

				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;

			case OTHERS_ERROR:
				// その他エラー
				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;

			default:
				returnCode = ProcessResult.FAILURE;
				errorCount++;
				break;
			}

		} catch (Exception e) {

			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			returnCode = ProcessResult.FAILURE;
			errorCount++;
		}
		return returnStatus;
	}

	/**
	 * リクエストボディを作成し取得する
	 * 
	 * @param result           ... 更新対象のPush通知テーブルのレコード
	 * @param status...更新ステータス
	 * @throws JsonProcessingException
	 * @return リクエストボディ
	 */
	private DeliveryRegisterInputDTO creaetRequestBody(PushNotifications pushNotifications, Coupons coupons)
			throws JsonProcessingException {

		String url = appScheme + String.format(REQUEST_BODY_URL, coupons.getFsCouponUuid());

		List<String> platforms = new ArrayList<String>();
		platforms.add("iphone");
		platforms.add("android");

		DeliveryRegisterInputDTO input = new DeliveryRegisterInputDTO();

		input.setType("scheduled");
		input.setContentType("text/plain");
		input.setPlatform(platforms);
		input.setPopup(pushNotifications.getPushNotificationText());
		input.setTitle(pushNotifications.getNotificationTitle());
		input.setContent(createContent(pushNotifications, coupons));
		input.setUrl(url);
		input.setCategory("coupon");
		input.setDeliveryType(DeliveryType.AEON_WALLET_APP.getValue());
		input.setSegmentationId(pushNotifications.getFsSegmentId());
		if (pushNotifications.getSendDate() != null) {
			input.setSendTime(
					new SimpleDateFormat(DATA_FORMAT_YYYYMMDD_HHMMSS).format(pushNotifications.getSendDate()));
		} else {
			input.setSendTime(null);
		}

		return input;

	}

	/**
	 * Push通知テーブルのレコードを2:連携中に更新する
	 * 
	 * @param pushNotificationsList ... 更新対象のPush通知テーブルのレコード
	 */
	private void updateDrivingPushNotifications(List<PushNotifications> pushNotificationsList) {

		Map<String, Object> paramMap = new HashMap<>();
		Timestamp updateDay = DateUtils.now();
		try {
			// トランザクションの開始
			transactionBegin(BATCH_ID);
			for (PushNotifications pushNotifications : pushNotificationsList) {
				paramMap.put(PUSH_NOTIFICATION_ID, pushNotifications.getPushNotificationId());
				paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.DELIVERING.getValue());
				paramMap.put(UPDATE_USER_ID, BATCH_ID);
				paramMap.put(UPDATE_DATE, updateDay);

				// PushNotificationsの更新
				sqlExecute(BATCH_ID, SQL_UPDATE_PUSHNOTIFICATIONS, paramMap);
			}

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(BATCH_ID);
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * Push通知テーブルのレコードを3:連携済みに更新する
	 * 
	 * @param result           ... 更新対象のPush通知テーブルのレコード
	 * @param status...更新ステータス
	 * @param outputDTO        ... 配信登録のOutputDTO
	 */
	private void updatePushNotificationsAfter(Long pushNotificationId, FsDeliveryStatus status,
			DeliveryRegisterOutputDTO outputDTO) {

		Map<String, Object> paramMap = new HashMap<>();
		try {
			paramMap.put(PUSH_NOTIFICATION_UUID, outputDTO.getResult().getId().toString());
			paramMap.put(PUSH_NOTIFICATION_ID, pushNotificationId);
			paramMap.put(FS_DELIVERY_STATUS, status.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// PushNotificationsの更新
			sqlExecute(BATCH_ID, SQL_END_UPDATE_PUSHNOTIFICATIONS, paramMap);
		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(BATCH_ID);
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
			throw new RuntimeException(e);
		}
	}

	/**
	 * コンテンツ部分を作成する。
	 * 
	 * @param pushNotifications Push通知テーブル
	 * @param coupons           クーポンテーブル
	 * 
	 * @return コンテンツ
	 * @throws JsonProcessingException
	 */
	private String createContent(PushNotifications pushNotifications, Coupons coupons) throws JsonProcessingException {

		DeliveryRegisterInputDTOContent content = new DeliveryRegisterInputDTOContent();

		// コンテンツ本文
		content.setInformationText(pushNotifications.getNotificationBody());
		// リンクテキスト
		content.setInformationLinkTitle(pushNotifications.getButtonDisplayName());
		// リンクURL
		if (pushNotifications.getHeaderImageUrl() != null) {
			content.setInformationImage(pushNotifications.getHeaderImageUrl().toString());
		} else {
			content.setInformationImage(null);
		}
		if (coupons.getDisplaydateTo() != null) {
			// 表示終了日
			content.setInformationDisplayEndDate(
					new SimpleDateFormat(DATA_FORMAT_YYYYMMDD).format(coupons.getDisplaydateTo()));
			// 表示終了時刻
			content.setInformationDisplayEndTime(
					new SimpleDateFormat(DATA_FORMAT_HHMMSS).format(coupons.getDisplaydateTo()));
		} else {
			// 表示終了日
			content.setInformationDisplayEndDate(null);
			// 表示終了時刻
			content.setInformationDisplayEndTime(null);
		}

		return mapper.writeValueAsString(content);

	}

	/**
	 * 処理件数をログに出力する。
	 * 
	 */
	private void resultInformation() {
		int success = processCount - errorCount - skipCount;
		String msg = "Push通知発行処理が完了しました。(処理対象件数:[" + processCount + "] , 処理成功件数:[" + success + "], 処理失敗件数:["
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

		if (httpStatusCode == HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue()) {
			// 503の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("Push通知ID =%s、エラーメッセージ=「FANSHIPメンテナンス」", uuid));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));
		} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue()) {
			// 429の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("Push通知ID =%s、エラーメッセージ=「リクエストが制限されています。」", uuid));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

		} else if (httpStatusCode == HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue()) {
			// 401の時
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("Push通知ID =%s、エラーメッセージ=「認証エラー」", uuid));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

		} else {
			msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
					httpStatusCode, String.format("Push通知ID =%s", uuid));
			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));
		}

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

		msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()), FANHIP_API_NAME,
				httpStatusCode, String.format("Push通知ID =%s、レスポンスボディ=%s、メッセージ=%s", uuid, responseBody, errorMsg));
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), msg));

	}

	/**
	 * エラー対象メッセージ(API連携時)
	 * 
	 * @param targetPage エラー対象Id
	 * 
	 */
	private void apiErrorProcessTargetLog(Long targetId) {
		String msg = null;

		msg = String.format("エラー対象：Push通知ID = %s", targetId);

		logger.info(msg);

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
     *  処理正常終了メッセージ
     *  
     * @param targetId 処理対象ID
     *  
     */
    private void writeFinishTargetLog(Long targetId) {
        // メッセージを出力（処理正常終了(%s)：クーポンID ＝ %s）
        String msg = String.format("targetId：Push通知ID = %s", targetId);
        logger.info(msg);
    }
}
