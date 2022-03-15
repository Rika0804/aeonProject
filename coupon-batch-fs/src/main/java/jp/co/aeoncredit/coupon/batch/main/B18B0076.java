package jp.co.aeoncredit.coupon.batch.main;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsApiUri;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.CancelPushNotificationOutputErrorDTO;
import jp.co.aeoncredit.coupon.batch.exception.FsApiFailedException;
import jp.co.aeoncredit.coupon.batch.exception.FsMaintenanceException;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.properties.PushNotificationsProps;
import jp.co.aeoncredit.coupon.entity.PushNotifications;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSPush通知配信停止バッチ
 * 
 * @author nguyenphuongnga
 * @version 1.0
 */
@Named("B18B0076")
@Dependent
public class B18B0076 extends BatchFSApiCalloutBase {

	/** バッチID */
	private static final String BATCH_ID = "B18B0076";

	/** SQL NAME get Information of Push Notifications List */
	private static final String SQL_GET_INFO_PUSH_NOTIFICATIONS_LIST = "getInfoPushNotificationsList";

	/** SQL NAME update Fs Stop Status */
	private static final String SQL_UPDATE_FS_STOP_STATUS = "updateFsStopStatus";

	/** FS API Function message value */
	private static final String FS_API_NAME = "Push通知配信停止";

	/** FS API name value in count message */
	private static final String COUNT_MESSAGE_FS_NAME = "Push通知配信停止処理";

	/** FS API RESPONSE KEY WORD STATUS */
	private static final String STATUS_RESPONSE_FROM_FS_API = "status";
	
	/** FS API失敗時にstatusキーに入る値 */
	private static final String STATUS_VALUE_FROM_FS_API_NG = "NG";
	
	/** FS API Push通知が配信期間を過ぎている場合のエラーコード */
	private static final String EXPIRED_ERROR_CODE_FROM_FS_API = "E35020";

	/** メッセージフォーマット（UUID = %s） */
	private static final String MSG_FORM_UUID = "UUID = %s";

	/** メッセージフォーマット（処理対象：UUID = %s） */
	private static final String MSG_FORM_START_TARGET = "処理対象：UUID = %s";

	/** メッセージフォーマット（処理中断スキップ対象：UUID = %s） */
	private static final String MSG_FORM_SKIP_TARGET = "処理中断スキップ対象：UUID = %s";

	/** メッセージフォーマット（エラー対象：UUID = %s） */
	private static final String MSG_FORM_ERROR_TARGET = "エラー対象：UUID = %s";

	/** メッセージ（取得不可） */
	private static final String MSG_UNACQUIRED = "取得不可";

	/** メッセージ（レスポンスのstatusがNG） */
	private static final String MSG_API_STATUS_NG = "レスポンスのstatusがNG エラー対象：UUID = %s";

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	/** ログ */
	private Logger myLog = getLogger();

	/** ObjectMapper */
	private ObjectMapper mapper = new ObjectMapper();

	/** メッセージ共通 */
	private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0076.getBatchId());

	/** URL */
	private String proUrl;

	/** API実行リトライ回数 */
	private int apiRetryCount;

	/** API実行リトライ時スリープ時間(ミリ秒) */
	private int apiSleepTime;

	/** タイムアウト期間(秒) */
	private int apiTimeoutDuration;

	/** 処理対象件数 */
	private int targetCount;

	/** 処理成功件数 */
	private int successCount;

	/** 処理スキップ件数 */
	private int skipCount;

	/** 処理失敗件数 */
	private int failCount;

	/**
	 * バッチの起動時メイン処理
	 */
	@Override
	public String process() throws Exception {
		// read file properties
		readProperties();
		// (1) export start message
		myLog.info(
				batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BatchInfo.B18B0076.getBatchName()));
		String returnCode = processBatch();
		// (5) Export record count to table 配信バッチ情報管理
		// Export end message
		myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BatchInfo.B18B0076.getBatchName(),
				ProcessResult.SUCCESS.getValue().equals(returnCode)));
		return setExitStatus(returnCode);
	}

	/**
	 * バッチの処理
	 * 
	 * @return value of batch's processing ("0" or "1")
	 */
	private String processBatch() {
		String returnCode = null;
		try {
			// (2) Get fan ship auth token
			AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);
			// (2.1a) If 【B18BC001_認証トークン取得】's return is SUCCESS
			if (AuthTokenResult.SUCCESS.equals(authTokenResult)) {
				// (3) get processing object notification push
				// (3.1) get record from 【Push通知テーブル】
				List<PushNotifications> pushNotificationsList = getInforPushNotificationsList();
				// (3.1b) If pushNotificationsList is empty
				if (pushNotificationsList.isEmpty()) {
					// export message B18MB006
					myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(),
							String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
									FS_API_NAME, Constants.NO_DETAIL)));
					// set code return = 0, export message end process normally
					returnCode = ProcessResult.SUCCESS.getValue();
					// (3.1a) If pushNotificationsList isn't empty
				} else {
					// (3.2) update FS_STOP_STATUS
					boolean updateSuccess = updateFsStopStatusList(pushNotificationsList,
							FsDeliveryStatus.DELIVERING.getValue());
					targetCount = pushNotificationsList.size();
					if (!updateSuccess) {
						skipCount = pushNotificationsList.size() - 1;
						failCount = 1;
						returnCode = ProcessResult.FAILURE.getValue();
					} else {
						// (4) cooperate FS API
						returnCode = cooperateFSAPI(pushNotificationsList);
					}
					// (4.3) Export log record count Push通知配信停止処理完了時 B18MB005
					String countRecordMessage = String.format(
							BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()), COUNT_MESSAGE_FS_NAME,
							targetCount, successCount, failCount, skipCount, Constants.NO_DETAIL);
					myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), countRecordMessage));
				}
				// (2.1b.1)If 【B18BC001_認証トークン取得】's return is MAINTAIN
			} else if (AuthTokenResult.MAINTENANCE.equals(authTokenResult)) {
				returnCode = ProcessResult.SUCCESS.getValue();
				// (2.1b.2) If 【B18BC001_認証トークン取得】's return is FAILURE
			} else {
				// With records are gotten with the conditions same to (3.1)
				returnCode = ProcessResult.FAILURE.getValue();
			}
		} catch (Exception e) {
		    log.error(e.getMessage(), e);
			// Set "1" to return value at abnormal end
			returnCode = ProcessResult.FAILURE.getValue();
		}
		return returnCode;
	}

	/**
	 * FS API 連携
	 * 
	 * @param pushNotificationsList list of PUSH_NOTIFICATIONS's entity
	 * @return ProcessResult
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or
	 *                              otherwise occupied, and the thread is
	 *                              interrupted, either before or during the
	 *                              activity. Occasionally a method may wish to test
	 *                              whether the current thread has been interrupted,
	 *                              and if so, to immediately throw this exception
	 * @throws IOException          Signals that an I/O exception of some sort has
	 *                              occurred. This class is the general class of
	 *                              exceptions produced by failed or interrupted I/O
	 *                              operations.
	 */
	private String cooperateFSAPI(List<PushNotifications> pushNotificationsList)
			throws IOException, InterruptedException {
		// (4) cooperate FS API
		// For each record from 【Push通知テーブル】 repeat (4.1) to(4.2)
		// (4.1) cooperate FS API
		boolean updateSuccess = false; // true:FS連携状況を更新成功
		int index = 0;
		for (PushNotifications pushNotifications : pushNotificationsList) {
			String uuidStr = ConvertUtility.longToString(pushNotifications.getFsPushNotificationUuid());
			// FSPUSH通知UUID
			try {
				HttpResponse<String> response = processConnectFSAPI(pushNotifications);
				if (response == null) {
					for (int i = index; i < pushNotificationsList.size(); i++) {
						// 未処理分をFS連携待ちに戻す
						updateSuccess = updateFsStopStatus(pushNotificationsList.get(i),
								FsDeliveryStatus.WAITING.getValue());
						if (!updateSuccess) {
							skipCount++;
						} else {
							failCount++;
						}
					}
					return ProcessResult.FAILURE.getValue();
				}
				updateSuccess = updateFsStopStatus(pushNotifications, FsDeliveryStatus.DELIVERED.getValue());
				successCount++;
				index++;
			} catch (FsMaintenanceException e) {
				for (int i = index; i < pushNotificationsList.size(); i++) {
					// 未処理分をFS連携待ちに戻す
					updateSuccess = updateFsStopStatus(pushNotificationsList.get(i),
							FsDeliveryStatus.WAITING.getValue());
					if (!updateSuccess) {
						failCount++;
					} else {
						skipCount++;
					}
				}
				return ProcessResult.SUCCESS.getValue();
			} catch (FsApiFailedException e) {
				for (int i = index; i < pushNotificationsList.size(); i++) {
					// 未処理分をFS連携待ちに戻す
					updateFsStopStatus(pushNotificationsList.get(i), FsDeliveryStatus.WAITING.getValue());
					failCount++;
				}
				return ProcessResult.FAILURE.getValue();
			} catch (Exception e) {
				for (int i = index; i < pushNotificationsList.size(); i++) {
					// 未処理分をFS連携待ちに戻す
					updateFsStopStatus(pushNotificationsList.get(i), FsDeliveryStatus.WAITING.getValue());
					failCount++;
				}
				// ログを出力する
				writeErrorTargetLog(uuidStr, e);
				return ProcessResult.FAILURE.getValue();
			}
		}
		return ProcessResult.SUCCESS.getValue();
	}

	/**
	 * FS公開停止状況を更新 (List)
	 * 
	 * @param pushNotificationsList List of PUSH_NOTIFICATIONS's entity
	 * @param fsStopStatus          FS連携状況
	 * @return trueの場合処理成功
	 */
	private boolean updateFsStopStatusList(List<PushNotifications> pushNotificationsList, String fsStopStatus) {
		boolean isSuccess = false;
		for (PushNotifications pushNotifications : pushNotificationsList) {
			isSuccess = updateFsStopStatus(pushNotifications, fsStopStatus);
		}
		return isSuccess;
	}

	/**
	 * FS公開停止状況を更新
	 * 
	 * @param pushNotifications PUSH_NOTIFICATIONS's entity
	 * @param fsStopStatus      FS公開停止状況
	 * @return trueの場合処理成功
	 */
	private boolean updateFsStopStatus(PushNotifications pushNotifications, String fsStopStatus) {
		try {
			transactionBegin(BATCH_ID);
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put(PushNotificationsProps.FS_STOP_STATUS, fsStopStatus);
			paramMap.put(PushNotificationsProps.UPDATE_USER_ID, BATCH_ID);
			paramMap.put(PushNotificationsProps.UPDATE_DATE, DateUtils.now());
			paramMap.put(PushNotificationsProps.PUSH_NOTIFICATION_ID, pushNotifications.getPushNotificationId());
			sqlExecute(BATCH_ID, SQL_UPDATE_FS_STOP_STATUS, paramMap);
			transactionCommit(BATCH_ID);
			return true;
		} catch (Exception e) {
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(pushNotifications.getFsPushNotificationUuid(), e);
			return false;
		}
	}

	/**
	 * Cooperate FS API
	 * 
	 * @param pushNotifications Push通知テーブル(PUSH_NOTIFICATIONS)のEntity
	 * @throws FsMaintenanceException
	 * @throws FsApiFailedException
	 */
	private HttpResponse<String> processConnectFSAPI(PushNotifications pushNotifications)
			throws FsMaintenanceException, FsApiFailedException {
		// FSPush通知UUID FS_PUSH_NOTIFICATION_UUID
		String fsPushNoticationsUUID;
		fsPushNoticationsUUID = ConvertUtility.longToString(pushNotifications.getFsPushNotificationUuid());
		String uri = proUrl.replace("${id}", fsPushNoticationsUUID);

		// HttpRequestを取得する
		String url = reverseProxyUrl + uri;
		FsApiCallResponse fsApiCallResponse = null;
		try {
			// 正常HTTPステータスコードリストを設定する
			List<Integer> successHttpStatusList = new ArrayList<Integer>();
			successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

			// 処理対象のログを出力する
			writeApiStartLog(fsPushNoticationsUUID);

			// FANSHIP APIを呼び出す
			fsApiCallResponse = callFanshipApi(BATCH_ID, FS_API_NAME, url, Strings.EMPTY, HttpMethodType.POST,
					ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION_POPINFOLOGIN, successHttpStatusList,
					apiRetryCount, apiSleepTime, apiTimeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);

			if (fsApiCallResponse == null || fsApiCallResponse.getFsApiCallResult() == null
					|| fsApiCallResponse.getResponse() == null || fsApiCallResponse.getResponse().body() == null) {

				// API連携失敗時のログを出力する
				writeApiFailLog(null, fsPushNoticationsUUID, null);
				throw new FsApiFailedException();
			} else if (isApiSuccess(fsApiCallResponse, fsPushNoticationsUUID)) {
				// API成功条件を満たす場合
				return fsApiCallResponse.getResponse();
			} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
					|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
				// HTTPステータスコード：429、503の場合

				// 処理中断スキップ対象のログを出力する
				writeApiSkipLog(fsPushNoticationsUUID);
				throw new FsMaintenanceException(fsApiCallResponse.getResponse().statusCode());
			} else {
				// 上記以外の場合

				// エラー対象のログを出力する
				writeApiErrorTargetLog(fsPushNoticationsUUID);
				throw new FsApiFailedException(fsApiCallResponse.getResponse().statusCode());
			}
		} catch (FsMaintenanceException | FsApiFailedException e) {
			// FSメンテナンス中、またはFS API連携失敗のの場合
			throw e;
		} catch (Exception e) {
			// その他例外発生時

			// API連携失敗時のログを出力する
			writeApiFailLog(null, fsPushNoticationsUUID, e);
			return null;
		}
	}

	/**
	 * 以下の条件で【Push通知テーブル】からレコードを取得する
	 * 
	 * @return list of PUSH_NOTIFICATIONS's entity
	 */
	private List<PushNotifications> getInforPushNotificationsList() {
		List<PushNotifications> pushNotificationsList = new ArrayList<>();
		Map<String, Object> paramMap = new HashMap<>();
		List<String> fsDeliveryStatusList = new ArrayList<String>();
		fsDeliveryStatusList.add(FsDeliveryStatus.WAITING.getValue());
		fsDeliveryStatusList.add(FsDeliveryStatus.DELIVERING.getValue());
		paramMap.put(PushNotificationsProps.FS_STOP_STATUS, fsDeliveryStatusList);
		List<Object[]> resultList = sqlSelect(BATCH_ID, SQL_GET_INFO_PUSH_NOTIFICATIONS_LIST, paramMap);

		for (Object[] result : resultList) {
			if (result[1] == null) {
				continue;
			}
			PushNotifications pushNotifications = new PushNotifications();
			pushNotifications.setPushNotificationId(ConvertUtility.objectToLong(result[0]));
			pushNotifications.setFsPushNotificationUuid(ConvertUtility.objectToLong(result[1]));
			pushNotificationsList.add(pushNotifications);
		}
		return pushNotificationsList;
	}

	/**
	 * プロパティファイルを読み込む
	 * 
	 */
	private void readProperties() {
		Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0076.getBatchId());
		// FS API連携(Push通知配信停止)のURL
		this.proUrl = properties.getProperty(FsApiUri.STOP_DELIVERY_PUSH_NOTIFICATION.getValue());
		// FS API 失敗時のAPI実行リトライ回数
		this.apiRetryCount = Integer.parseInt(properties.getProperty("fs.cancel.batch.push.notification.retry.count"));
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		this.apiSleepTime = Integer
				.parseInt(properties.getProperty("fs.cancel.batch.push.notification.retry.sleep.time"));
		// FS API発行時のタイムアウト期間(秒)
		this.apiTimeoutDuration = Integer
				.parseInt(properties.getProperty("fs.cancel.batch.push.notification.timeout.duration"));
	}

	/**
	 * API成功条件を満たすか判定する
	 * 
	 * @param FsApiCallResponse     fsApiCallResponse
	 * @param fsPushNoticationsUUID fan ship Push Notifications UUID
	 * @return API成功条件を満たす場合、true
	 * @throws IOException          Signals that an I/O exception of some sort has
	 *                              occurred. This class is the general class of
	 *                              exceptions produced by failed or interrupted I/O
	 *                              operations.
	 * @throws JsonMappingException Checked exception used to signal fatal problems
	 *                              with mapping of content, distinct from low-level
	 *                              I/O problems (signaled using simple
	 *                              {@link java.io.IOException}s) or data
	 *                              encoding/decoding problems (signaled with
	 *                              {@link com.fasterxml.jackson.core.JsonParseException},
	 *                              {@link com.fasterxml.jackson.core.JsonGenerationException}).
	 * @throws JsonParseException   Exception type for parsing problems, used when
	 *                              non-well-formed content (content that does not
	 *                              conform to JSON syntax as per specification) is
	 *                              encountered.
	 */
	private boolean isApiSuccess(FsApiCallResponse fsApiCallResponse, String fsPushNoticationsUUID)
			throws JsonParseException, JsonMappingException, IOException {
		String status = "";
		Map<String, Object> jsonMap = mapper.readValue(fsApiCallResponse.getResponse().body(),
				new TypeReference<Map<String, Object>>() {
				});
		if (jsonMap.get(STATUS_RESPONSE_FROM_FS_API) != null) {
			status = jsonMap.get(STATUS_RESPONSE_FROM_FS_API).toString();
		}
		if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
			if ("OK".equals(status)) {
				return true;
			} else {
				// エラー時のDTOに変換
				Optional<CancelPushNotificationOutputErrorDTO> errorOpt = convertToErrorDto(fsApiCallResponse.getResponse().body());
				if (errorOpt.isPresent() && isExpired(errorOpt.get())) {
					// Push通知が既に配信期間を過ぎている場合、API成功として処理を行う
					return true;
				} else {
					// API失敗時
					// ログを出力する
					String msg = String.format(MSG_API_STATUS_NG, fsPushNoticationsUUID);
					myLog.info(batchLogger.createMsg("", msg));
					return false;
					
				}
				
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Push通知配信停止API 結果判定
	 * Push通知が配信期間を過ぎている場合のレスポンスか判定する
	 * @param errorDto
	 * @return Push通知が配信期間を過ぎている場合のレスポンスの場合、<code>true</code>
	 */
	private boolean isExpired(CancelPushNotificationOutputErrorDTO errorDto) {
		if (Objects.equals(errorDto.getStatus(), STATUS_VALUE_FROM_FS_API_NG) &&  //
				Objects.equals(errorDto.getError().getCode(), EXPIRED_ERROR_CODE_FROM_FS_API)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * プッシュ通知配信停止API <br>
	 * エラー時のレスポンスをDTOに格納する <br>
	 * DTOに変換失敗した場合、ログを出力しnullを返却する
	 * @param responseBody
	 * @return Optional<StopPushNotificationOutputErrorDTO>
	 */
	private Optional<CancelPushNotificationOutputErrorDTO> convertToErrorDto (String responseBody) {
		try {
			return Optional.of(mapper.readValue(responseBody, CancelPushNotificationOutputErrorDTO.class));
		} catch (IOException e) {
			myLog.error(e.getMessage(), e);
			return Optional.empty();
		}
	}

	/**
	 * DBエラー共通処理
	 * 
	 * @param uuidStr FSアプリ内メッセージUUID
	 * @param e       Exception
	 */
	private void writeDbErrorLog(Long uuidStr, Exception e) {

		// メッセージを出力（DBエラーが発生しました。%s）
		String msg = String.format(MSG_FORM_UUID, ConvertUtility.longToString(uuidStr));
		myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), msg), e);
	}

	/**
	 * 処理対象のログを出力する
	 * 
	 * @param uuidStr FSアプリ内メッセージUUID
	 */
	private void writeApiStartLog(String uuidStr) {

		// メッセージを出力（処理対象：UUID = %s）
		String msg = String.format(MSG_FORM_START_TARGET, uuidStr);
		myLog.info(batchLogger.createMsg("", msg));
	}

	/**
	 * 処理中断スキップ対象のログを出力する
	 * 
	 * @param uuidStr FSアプリ内メッセージUUID
	 */
	private void writeApiSkipLog(String uuidStr) {

		// メッセージを出力（処理中断スキップ対象：UUID = %s）
		String msg = String.format(MSG_FORM_SKIP_TARGET, uuidStr);
		myLog.info(batchLogger.createMsg("", msg));
	}

	/**
	 * エラー対象のログを出力する
	 * 
	 * @param uuidStr FSアプリ内メッセージUUID
	 */
	private void writeApiErrorTargetLog(String uuidStr) {

		// メッセージを出力（エラー対象：UUID = %s）
		String msg = String.format(MSG_FORM_ERROR_TARGET, uuidStr);
		myLog.info(batchLogger.createMsg("", msg));
	}

	/**
	 * API連携失敗時のログを出力する
	 * 
	 * @param statusCode            statusCode
	 * @param fsPushNoticationsUUID fan ship Push Notifications UUID
	 * @param errReason             error reason
	 * @return an error message
	 */
	private void writeApiFailLog(Integer statusCode, String fsPushNoticationsUUID, Exception e) {
		String statusCodeStr = statusCode == null ? MSG_UNACQUIRED : ConvertUtility.integerToString(statusCode);
		String errorInfo = String.format(MSG_FORM_UUID, fsPushNoticationsUUID);
		myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB924, FS_API_NAME, statusCodeStr, errorInfo), e);
	}

	/**
	 * エラー対象のログを出力する
	 * 
	 * @param uuidStr FSアプリ内メッセージUUID
	 * @param e       Exception
	 */
	private void writeErrorTargetLog(String uuidStr, Exception e) {

		// メッセージを出力（エラー対象：UUID = %s）
		String msg = String.format(MSG_FORM_ERROR_TARGET, uuidStr);
		myLog.error(batchLogger.createMsg("", msg), e);
	}
}