package jp.co.aeoncredit.coupon.batch.main;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOButtons;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOCondition;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOMessage;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOPeriod;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOTarget;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstInputDTOTrigger;
import jp.co.aeoncredit.coupon.batch.dto.CreateAppMstOutputDTO;
import jp.co.aeoncredit.coupon.constants.AppMessageStatus;
import jp.co.aeoncredit.coupon.constants.AppMessageType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.MessageType;
import jp.co.aeoncredit.coupon.dao.custom.AppMessageSendPeriodsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.AppMessagesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstEventDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessageSendPeriods;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.MstEvent;

/**
 * FSアプリ内メッセージ発行バッチ
 */
@Named("B18B0060")
@Dependent
public class B18B0060 extends BatchFSApiCalloutBase {

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0060.getBatchId();

	/** バッチNAME */
	private static final String BATCH_NAME = BatchInfo.B18B0060.getBatchName();

	/** 正常終了_戻り値 */
	protected static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	protected static final String FAIL_RETURN_VALUE = "1";

	/** HTTPステータスコード */
	protected static final int HTTP_STATUS_401 = 401;

	/** アプリスキーム */
	private String appScheme;

	/** FS API失敗時のAPI実行リトライ回数 */
	protected int retryCount;

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected int sleepTime;

	/** FS API発行時のタイムアウト期間 */
	protected int timeoutDuration;

	/** APIのURL */
	private String apiUrl;

	/** APIのURL */
	protected static final String API_URL = "fs.delivery.batch.app.message.delivery.api.url";

	/** FS API失敗時のAPI実行リトライ回数 */
	protected static final String RETRY_COUNT = "fs.delivery.batch.app.message.retry.count";

	/** FS API失敗時のAPI実行リトライ時スリープ時間 */
	protected static final String RETRY_SLEEP_TIME = "fs.delivery.batch.app.message.retry.sleep.time";

	/** FS API発行時のタイムアウト期間 */
	protected static final String TIMEOUT_DURATION = "fs.delivery.batch.app.message.timeout.duration";

	/** success総数 */
	private int successCount;

	/** error総数 */
	private int errorCount;

	/** skip総数 */
	private int skipCount;

	/** yyyy-MM-dd'T'HH:mm:ss.SSSXXX形式*/
	private static final String DATA_FORMAT_YYYYMMDDHHMMSSXXX = "yyyy-MM-dd'T'HH:mm:ssXXX";

	/** ログ */
	protected Logger logger = getLogger();

	/** システム日付 */
	protected Date today;

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** ObjectMapper */
	private ObjectMapper mapper = new ObjectMapper();

	/** プロパティファイル共通 */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	private static final String FS_DELIVERY_STATUS = "fsDeliveryStatus";
	private static final String APP_MESSAGE_TYPE = "appMessageType";
	private static final String APP_MESSAGE_STATUS = "appMessageStatus";
	private static final String DELETE_FLAG = "deleteFlag";
	private static final String APP_MESSAGE_UUID = "fsAppMessageUuid";
	private static final String UPDATE_USER_ID = "updateUserId";
	private static final String UPDATE_DATE = "updateDate";
	private static final String APP_MESSAGE_ID = "appMessageId";
	/** SQL NAME */
	protected static final String SQL_SELECT_APPMESSAGES = "selectAppMessages";
	protected static final String SQL_SELECT_APPMESSAGES_DISTINCT = "selectAppMessagesDistinct";
	protected static final String SQL_UPDATE_APPMESSAGES = "updateAppMessages";
	protected static final String SQL_END_UPDATE_APPMESSAGES = "endupdateAppMessages";

	private static final String COLUMN_APP_MESSAGE_ID = "appMessageId";
	private static final String COLUMN_DELETE_FLAG = "deleteFlag";
	private static final String COLUMN_EVENT_ID = "eventId";
	private static final String COLUMN_COUPON_ID = "couponId";

	@Inject
	protected AppMessagesDAOCustomize appMessagesDAO;
	@Inject
	protected MstEventDAOCustomize mstEventDAO;
	@Inject
	protected AppMessageSendPeriodsDAOCustomize appMessageSendPeriodsDAO;
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

		// FSアプリ内メッセージ一覧取得処理の開始
		String returnCode = fsAppMessagesListProcess();

		// 処理終了メッセージを出力する。
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				SUCCESS_RETURN_VALUE.equals(returnCode)));

		return setExitStatus(returnCode);
	}

	/**
	 * プロパティファイルを読み込む
	 */
	protected void readProperties() {
		Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);

		// APIURL
		this.apiUrl = pro.getProperty(API_URL);

		// アプリスキーム(環境変数)
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
	 * @return 実行後の戻り値
	 * @throws Exception 
	 */
	private String fsAppMessagesListProcess() throws Exception {
		String returnCode = SUCCESS_RETURN_VALUE;
		// (2)【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		switch (authTokenResult) {

		// (2.1b.1)戻り値が「503」(FANSHIPメンテナンス)の場合
		case MAINTENANCE:
			break;

		// (2.1b.2) 上記以外の失敗
		case FAILURE:
			returnCode = FAIL_RETURN_VALUE;
			break;

		// (2.1a) 成功した場合
		default:
			returnCode = getAppMessagesProcess();
			break;
		}

		return returnCode;
	}

	/**
	 * (3)処理対象アプリ内メッセージ取得
	 * @return 実行後の戻り値
	 * @throws Exception 
	 */
	private String getAppMessagesProcess() throws Exception {

		String returnCode = SUCCESS_RETURN_VALUE;
		//(3.1)更新対象のアプリ内メッセージを取得する。
		List<Long> appMessageList = getAppMessagesList();

		if (appMessageList.isEmpty()) {
			//(3.1b)取得したレコードが0件の場合、メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
			String msg = "アプリ内メッセージ発行";
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, msg, "").replace(", ", ""));
		} else {
			//(3.2)	FS連携状況を「FS連携中」に更新する。

			for (Long appMessagesId : appMessageList) {
				updateAppMsgDelivering(appMessagesId);
			}

		}

		// (4)FS API 連携
		successCount = 0;
		errorCount = 0;
		skipCount = 0;
		for (Long appMessagesId : appMessageList) {
			try {
				AppMessages appMessages = getAppMessages(appMessagesId);
				List<AppMessageSendPeriods> appMessageSendPeriodsList = getAppMessageSendPeriods(
						appMessages.getAppMessageId());
				Optional<Coupons> coupons = getCoupons(appMessages.getCouponId());
				Optional<MstEvent> mstEvent = getMstEvent(appMessages.getEventId());

				// (4.1)FS API アプリ内Msg作成
				CreateAppMstInputDTO input = createInputDTO(appMessages, appMessageSendPeriodsList, coupons, mstEvent);

				String requestBody = getRequestBody(input);

				// FS API呼出し
				boolean isSucesseApi = fsApiPost(requestBody, appMessages);

				if (isSucesseApi) {
					successCount++;
				} else {
					skipCount++;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				returnCode = FAIL_RETURN_VALUE;
				errorCount++;
			}
		}

		// (5)処理件数をログに出力する。
		logger.info(
				batchLogger.createMsg(BusinessMessageCode.B18MB005, BATCH_NAME.replace("FS ", ""),
						appMessageList.size(), successCount, errorCount, skipCount, ""));
		return returnCode;
	}

	/**
	 * アプリ内メッセージのレコードを更新する
	 * FS連携ステータス：連携待ち
	 * @param appMessagesId ... 更新対象のアプリ内メッセージテーブルのレコード
	 */
	private void updateAppMsgWaiting(Long appMessageId) {
		Map<String, Object> paramMap = new HashMap<>();

		try {
			// トランザクションの開始
			transactionBegin(BATCH_ID);

			paramMap.put(APP_MESSAGE_ID, appMessageId);
			paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.WAITING.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// AppMessagesの更新
			sqlExecute(BATCH_ID, SQL_UPDATE_APPMESSAGES, paramMap);

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(BATCH_ID);

			throw new RuntimeException(e);
		}
	}

	/**
	 * アプリ内メッセージのレコードを更新する
	 * FS連携ステータス：FS連携中
	 * @param appMessagesId ... 更新対象のアプリ内メッセージテーブルのレコード
	 */
	private void updateAppMsgDelivering(Long appMessagesId) {
		Map<String, Object> paramMap = new HashMap<>();

		try {
			// トランザクションの開始
			transactionBegin(BATCH_ID);

			paramMap.put(APP_MESSAGE_ID, appMessagesId);
			paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.DELIVERING.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// AppMessagesの更新
			sqlExecute(BATCH_ID, SQL_UPDATE_APPMESSAGES, paramMap);

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(BATCH_ID);

			throw new RuntimeException(e);
		}

	}

	/**
	 * FS連携成功時アプリ内メッセージのレコードを更新する
	 * 
	 * @param AppMessages ... 更新対象のアプリ内メッセージテーブルのレコード
	 */
	private void updateAppMessagesFromFsApiRsult(Long appMessageId, HttpResponse<String> httpResponses) {
		Map<String, Object> paramMap = new HashMap<>();

		String location = httpResponses.headers().firstValue("Location").orElse("");
		String fsUuid = null;

		if (!location.isEmpty()) {
			fsUuid = getUUID(location);
		}

		try {
			// トランザクションの開始
			transactionBegin(BATCH_ID);

			paramMap.put(APP_MESSAGE_UUID, fsUuid);
			paramMap.put(APP_MESSAGE_ID, appMessageId);
			paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.DELIVERED.getValue());
			paramMap.put(UPDATE_USER_ID, BATCH_ID);
			paramMap.put(UPDATE_DATE, DateUtils.now());
			// AppMessagesの更新
			sqlExecute(BATCH_ID, SQL_END_UPDATE_APPMESSAGES, paramMap);

			// トランザクションのコミット
			transactionCommit(BATCH_ID);

		} catch (Exception e) {
			// トランザクションのロールバック
			transactionRollback(BATCH_ID);

			throw new RuntimeException(e);
		}
	}

	/**
	 * リクエストBody アプリ内メッセージ生成
	 * @param appMessages 
	 * @param appMessageSendPeriodsList 
	 * @param coupons 
	 * @param mstEvent 
	 * 
	 * @return delivery
	 * @throws JsonProcessingException
	 * @throws UnsupportedEncodingException 
	 */
	private CreateAppMstInputDTO createInputDTO(AppMessages appMessages,
			List<AppMessageSendPeriods> appMessageSendPeriodsList, Optional<Coupons> coupons,
			Optional<MstEvent> mstEvent)
			throws JsonProcessingException, UnsupportedEncodingException {

		CreateAppMstInputDTO input = new CreateAppMstInputDTO();

		// メッセージ名
		input.setName(appMessages.getMessageName());
		// 表示順
		input.setPriority(0);
		// コンディション
		input.setCondition(createCondition(appMessages, appMessageSendPeriodsList, mstEvent));
		// メッセージ
		input.setMessage(createMessage(appMessages, coupons));

		return input;
	}

	/**
	 * リクエストBody コンディション部生成
	 * @param appMessages
	 * @param appMessageSendPeriodsList
	 * @param mstEvent 
	 * 
	 * @return condition リクエストbody コンディション部
	 * @throws JsonProcessingException
	 */
	private CreateAppMstInputDTOCondition createCondition(AppMessages appMessages,
			List<AppMessageSendPeriods> appMessageSendPeriodsList, Optional<MstEvent> mstEvent)
			throws JsonProcessingException {

		//condition
		CreateAppMstInputDTOCondition condition = new CreateAppMstInputDTOCondition();

		// トリガー		
		condition.setTrigger(createTrigger(appMessageSendPeriodsList, mstEvent));

		// ターゲット
		if (appMessages.getFsSegmentId() != null
				&& appMessages.getFsSegmentId() != -1) {
			// 【アプリ内メッセージテーブル】.「FSセグメントID」 <> null かつ -1の場合
			condition.setTarget(createTarget(appMessages));
		}

		// 配信する最大回数
		if (appMessages.getTotalMaxSendCount() != 0) {
			condition.setMaxTotalCount(appMessages.getTotalMaxSendCount());
		}

		// 1日に配信する最大回数
		if (appMessages.getDailyMaxSendCount() != 0) {
			condition.setMaxTotalCountPerDay(appMessages.getDailyMaxSendCount());
		}

		// カテゴリ
		condition.setCategory(null);

		return condition;
	}

	/**
	 * リクエストBody トリガー部生成
	 * @param appMessageSendPeriodsList
	 * @param mstEvent 
	 * 
	 * @return trigger リクエストbody トリガー部
	 * @throws JsonProcessingException
	 */
	private CreateAppMstInputDTOTrigger createTrigger(List<AppMessageSendPeriods> appMessageSendPeriodsList,
			Optional<MstEvent> mstEvent) throws JsonProcessingException {
		// trigger
		CreateAppMstInputDTOTrigger trigger = new CreateAppMstInputDTOTrigger();

		// 起動トリガーイベント
		trigger.setEventName(new String[] { mstEvent.get().getEventName() });

		// 配信期間			
		trigger.setPeriod(createAppPeriod(appMessageSendPeriodsList));

		return trigger;
	}

	/**
	 * リクエストBody 配布期間部生成
	 * 
	 * @param appMessageSendPeriodsList
	 * 
	 * @return periodList リクエストbody 配布期間部
	 * @throws JsonProcessingException
	 */
	private List<CreateAppMstInputDTOPeriod> createAppPeriod(List<AppMessageSendPeriods> appMessageSendPeriodsList)
			throws JsonProcessingException {

		// period
		List<CreateAppMstInputDTOPeriod> periodList = new ArrayList<>();

		for (int i = 0; i < appMessageSendPeriodsList.size(); i++) {
			CreateAppMstInputDTOPeriod period = new CreateAppMstInputDTOPeriod();

			// 配信期間(開始日時)
			period.setStart(DateUtils.toString(appMessageSendPeriodsList.get(i).getSendPeriodFrom(),
					DATA_FORMAT_YYYYMMDDHHMMSSXXX));

			// 終了日時
			period.setEnd(
					DateUtils.toString(appMessageSendPeriodsList.get(i).getSendPeriodTo(),
							DATA_FORMAT_YYYYMMDDHHMMSSXXX));

			periodList.add(period);
		}

		return periodList;
	}

	/**
	 * リクエストBody ターゲット部生成
	 * 
	 * @param appMessages
	 * 
	 * @return target リクエストbody ターゲット部
	 * @throws JsonProcessingException
	 */
	private CreateAppMstInputDTOTarget createTarget(AppMessages appMessages) {

		CreateAppMstInputDTOTarget target = new CreateAppMstInputDTOTarget();

		// セグメントID
		target.setSegmentationId(appMessages.getFsSegmentId());

		return target;
	}

	/**
	 * リクエストBody メッセージ部生成
	 * 
	 * @param appMessages
	 * 
	 * @return message リクエストbody メッセージ部
	 * @throws JsonProcessingException
	 * @throws UnsupportedEncodingException 
	 */
	private List<CreateAppMstInputDTOMessage> createMessage(AppMessages appMessages, Optional<Coupons> coupons)
			throws JsonProcessingException, UnsupportedEncodingException {
		List<CreateAppMstInputDTOMessage> messageList = new ArrayList<>();
		CreateAppMstInputDTOMessage message = new CreateAppMstInputDTOMessage();

		// メッセージタイプ
		if (MessageType.POPUP.getValue().equals(appMessages.getMessageType())) {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「0:ポップアップ」

			message.setTemplateId(1);

		} else {
			// 【アプリ内メッセージテーブル】.「メッセージタイプ」が「1:フルスクリーン」のとき

			message.setTemplateId(4);
		}

		// タイトル
		message.setTitle(appMessages.getMessageTitle());

		// 本文
		message.setBody(appMessages.getMessageText());

		// 画像URL
		message.setImageUrl(appMessages.getMessageImageUrl());

		// ボタン配列
		message.setButtons(createButton(appMessages, coupons));

		messageList.add(message);

		return messageList;
	}

	/**
	 * リクエストBody ボタン配列部生成
	 * 
	 * @param appMessages 
	 * 
	 * @return buttonList リクエストbody ボタン配列部
	 * @throws UnsupportedEncodingException 
	 * @throws JsonProcessingException
	 */
	private List<CreateAppMstInputDTOButtons> createButton(AppMessages appMessages, Optional<Coupons> coupons) throws UnsupportedEncodingException {

		List<CreateAppMstInputDTOButtons> buttonList = new ArrayList<>();

		CreateAppMstInputDTOButtons button = new CreateAppMstInputDTOButtons();

		// id
		button.setId("1");

		// ボタン表示文言
		button.setName(appMessages.getButtonDisplayName());

		// 遷移先URL
		String url = "inappmsg://action?url=";
		String urlParam = appScheme + "://jp.popinfo.coupon/?uuid=" + coupons.get().getFsCouponUuid().toString()
				+ "&destinationPage=/fscoupon/detail&action=open";
		button.setUrl(url + URLEncoder.encode(urlParam, "UTF-8"));

		// トラッキングイベント名
		button.setEventName(null);

		buttonList.add(button);

		return buttonList;
	}

	/**
	 * locationからUUIDを取得
	 * 
	 * @param location from returned HTTP response
	 * @return uuid FSアプリ内メッセージUUID
	 */
	private String getUUID(String location) {
		String[] locationSplit = location.split(Constants.SYMBOL_SLASH);
		return locationSplit[locationSplit.length - 1];
	}

	/**
	 * リクエストBody取得
	 * 
	 * @param input リクエストbody生成Input
	 * 
	 * @return リクエストbody
	 * @throws JsonProcessingException 
	 */
	private String getRequestBody(CreateAppMstInputDTO input) throws JsonProcessingException {

		String requestBody = null;
		// リクエストBodyを生成
		requestBody = mapper.writeValueAsString(input);

		return requestBody;

	}

	/**
	 * FANSHIP APIをPOSTで呼び出す
	 * 
	 * @param requestBody リクエストbody
	 * @param appMessages アプリ内メッセージリスト
	 * @return api呼出し結果
	 * @throws Exception
	 */
	private boolean fsApiPost(String requestBody, AppMessages appMessages) {
		String url = integrationUrl + apiUrl;

		// 正常HTTPステータスコード取得
		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(HTTPStatus.HTTP_STATUS_CREATED.getValue());

		// FANSHIP APIを呼び出す
		FsApiCallResponse fsApiCallResponse = callFanshipApi(
				BATCH_ID,
				"アプリ内Msg作成",
				url,
				requestBody,
				HttpMethodType.POST,
				ContentType.APPLICATION_JSON_CHARSET,
				TokenHeaderType.X_POPINFO_MAPI_TOKEN,
				successHttpStatusList,
				retryCount,
				sleepTime,
				timeoutDuration,
				RetryKbn.SERVER_ERROR);

		if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
			// FS連携成功時アプリ内メッセージのレコードを更新する
			updateAppMessagesFromFsApiRsult(appMessages.getAppMessageId(), fsApiCallResponse.getResponse());
			return true;

		} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
				|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
			// HTTPステータスコード：503の場合
			// HTTPステータスコード：429の場合

			// FS連携状況を「1:FS連携待ち」に更新して正常終了
			updateAppMsgWaiting(appMessages.getAppMessageId());

			// メッセージ出力
			apiError(fsApiCallResponse.getResponse(), appMessages.getAppMessageId());

			return true;

		} else {

			// FS連携状況を「1:FS連携待ち」に更新して異常終了
			updateAppMsgWaiting(appMessages.getAppMessageId());

			// メッセージ出力
			apiError(fsApiCallResponse.getResponse(), appMessages.getAppMessageId());

			return false;
		}

	}

	/**
	 * (3.1)アプリ内メッセージを取得する。
	 */
	private List<Long> getAppMessagesList() {

		List<Long> app = new ArrayList<Long>();

		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(FS_DELIVERY_STATUS, FsDeliveryStatus.WAITING.getValue());
		paramMap.put(APP_MESSAGE_TYPE, AppMessageType.PROMOTION.getValue());
		paramMap.put(APP_MESSAGE_STATUS, AppMessageStatus.APPROVED.getValue());
		paramMap.put(DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		// (2)PushNotificationsリスト取得
		List<Object[]> appMessageResult = sqlSelect(BATCH_ID, SQL_SELECT_APPMESSAGES, paramMap);

		for (Object[] appMessageRecord : appMessageResult) {
			app.add(ConvertUtility.objectToLong(appMessageRecord[0]));
		}

		return app;

	}

	/**
	 *  アプリ内メッセージテーブルを取得
	 *  
	 * @param couponId クーポンID
	 * 
	 * @return アプリ内メッセージテーブルリスト
	 */
	private AppMessages getAppMessages(Long appMessageId) {

		AppMessages appMessgesList;

		Optional<AppMessages> appMessgesRecord = appMessagesDAO.findById(appMessageId);

		appMessgesList = appMessgesRecord.orElseThrow();

		return appMessgesList;

	}

	/**
	 *  アプリ内メッセージ配信期間を取得
	 * 
	 * @param appMessageId アプリ内メッセージID
	 * 
	 * @return アプリ内メッセージ配信期間リスト
	 */
	private List<AppMessageSendPeriods> getAppMessageSendPeriods(Long appMessageId) {

		// アプリ内メッセージ配信期間を取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_APP_MESSAGE_ID, appMessageId.toString());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return appMessageSendPeriodsDAO.find(daoParam, null, false, null);

	}

	/**
	 *  クーポンテーブルを取得
	 * 
	 * @param couponId クーポンID
	 * 
	 * @return クーポンテーブル
	 */
	private Optional<Coupons> getCoupons(Long couponId) {

		// クーポンテーブルを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_COUPON_ID, couponId.toString());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return couponsDAO.findById(couponId);

	}

	/**
	 *  イベントマスタを取得
	 * 
	 * @param eventId イベントID
	 * 
	 * @return イベントマスタリスト
	 */
	private Optional<MstEvent> getMstEvent(Long eventId) {

		// イベントマスタを取得
		DAOParameter daoParam = new DAOParameter();
		daoParam.set(COLUMN_EVENT_ID, eventId.toString());
		daoParam.set(COLUMN_DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
		return mstEventDAO.findOne(daoParam, null);

	}

	/**
	 * 配信一覧取得APIでエラー発生時
	 * 
	 * @param httpResponse   HTTPレスポンス
	 * @param appMsgid  アプリ内メッセージID
	 */
	private void apiError(HttpResponse<String> response, Long appMsgid) {

		String infMsg = null;

		String loc = "";
		String type = "";
		String msg = "";
		if (response != null && response.body() != null && response.statusCode() == 422) {
			// バリデーションエラーの場合

			try {
				CreateAppMstOutputDTO outputDTO = mapper.readValue(response.body(), CreateAppMstOutputDTO.class);
				loc = String.join(",", outputDTO.getDetail().get(0).getLoc());
				type = outputDTO.getDetail().get(0).getType();
				msg = outputDTO.getDetail().get(0).getMsg();

			} catch (Exception e) {
				logger.info("レスポンスのデシリアライズ失敗", e);
			}
		}

		infMsg = "エラー対象：アプリ内MsgID = [" + appMsgid + "], エラー対象 = [" + loc + "]"
				+ ", エラー種類 = [" + type + "], エラーメッセージ = [" + msg + "]）";
		logger.info(batchLogger.createMsg("", infMsg));

	}

}
