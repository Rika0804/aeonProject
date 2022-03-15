package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

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
import jp.co.aeoncredit.coupon.batch.dto.GetAppMsgOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.GetAppMsgOutputDTOConditionTriggerPeriod;
import jp.co.aeoncredit.coupon.constants.AppMessageDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.dao.custom.AppMessagesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.AppMsgStatisticsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.AppMsgStatistics;
import jp.co.aeoncredit.coupon.entity.Coupons;

@Named("B18B0052")
@Dependent
public class B18B0052 extends BatchFSApiCalloutBase {

	/** API名 */
	private static final String FANSHIP_API_NAME = "アプリ内Msg一覧取得";

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0052.getBatchId();

	/** バッチNAME */
	private static final String BATCH_NAME = BatchInfo.B18B0052.getBatchName();

	/** 正常終了_戻り値 */
	private static final String SUCCESS_RETURN_VALUE = "0";
	/** 異常終了_戻り値 */
	private static final String FAIL_RETURN_VALUE = "1";

	/** アプリ内Msg一覧取得API 成功時のステータスコード */
	private static final List<Integer> API_SUCCESS_RETURN_CODE = List.of(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

	/** ログ */
	private Logger logger = getLogger();

	/** メッセージ共通 */
	private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** URL */
	private String url;

	/** リトライ回数 */
	private int retryCount;

	/** リトライ時スリープ時間(ミリ秒) */
	private int sleepTime;

	/** タイムアウト期間(秒) */
	private int timeoutDuration;

	/** リクエストパラメータ カウント */
	private static final String REQUEST_COUNT = "100";
	/** リクエストパラメータ ページ */
	private static final String REQUEST_PAGE = "1";

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);
	/** 設定ファイル変数 count */
	private static final String TEMP_EXP_COUNT = "{count}";
	/** 設定ファイル変数 page */
	private static final String TEMP_EXP_PAGE = "{page}";

	@Inject
	private AppMsgStatisticsDAOCustomize appMsgStatisticsDAO;
	@Inject
	protected AppMessagesDAOCustomize appMsgDAO;
	@Inject
	protected CouponsDAOCustomize couponsDAO;

	/** JSON変換用マッパー */
	ObjectMapper mapper = new ObjectMapper();

	/** バリデータ */
	private Validator validator;

	/** スキップ件数 */
	private int skips;

	/**
	 * バッチの起動時メイン処理
	 * 
	 * @throws Exception スローされた例外
	 */

	@Override
	public String process() {

		// 処理開始メッセージを出力する
		logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

		// プロパティファイル読み込み
		readProperties();

		// バリデータを取得
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();

		// アプリ内Msg一覧取得主処理
		String result = processGetAppMsgList();

		// 処理終了メッセージを出力する
		logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
				SUCCESS_RETURN_VALUE.equals(result)));

		return setExitStatus(result);
	}

	/**
	 * アプリ内Msg一覧取得主処理
	 */
	private String processGetAppMsgList() {

		// #(2)認証トークン取得処理
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);
		if (authTokenResult == AuthTokenResult.FAILURE) {
			return FAIL_RETURN_VALUE;
		} else if (authTokenResult == AuthTokenResult.MAINTENANCE) {
			return SUCCESS_RETURN_VALUE;
		}

		// FS API連携
		GetAppMsgResult apiResult = new GetAppMsgResult();
		try {
			apiResult = getAppMsgList();

			// 成功時以外は結果に応じた戻り値を返却
			if (apiResult.getResult() == FsApiCallResult.FANSHIP_MAINTENANCE
					|| apiResult.getResult() == FsApiCallResult.TOO_MANY_REQUEST) {
				return SUCCESS_RETURN_VALUE;
			} else if (apiResult.getResult() != FsApiCallResult.SUCCESS) {
				return FAIL_RETURN_VALUE;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return FAIL_RETURN_VALUE;
		}

		// (3.2)アプリ内メッセージ統計テーブルに登録
		int success = 0; // 成功件数
		int error = 0;
		String result = SUCCESS_RETURN_VALUE;
		transactionBegin(BATCH_ID);
		try {

			for (GetAppMsgOutputDTO dto : apiResult.getDtoList()) {
				logger.info("processGetAppMsgList.deliveryId: " + dto.getDeliveryId());
				// FS APIからの戻り値チェック
				if (!isCorrectResponse(dto)) {
					logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB929, "FS APIの戻り値", FANSHIP_API_NAME));
					return FAIL_RETURN_VALUE;
				}
				// アプリ内Msg統計テーブルに登録
				boolean insertResult = insertAppMsgStatistics(dto);
				if (insertResult) {
					success++;
				} else {
					skips++;
				}
			}

			transactionCommit(BATCH_ID);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			result = FAIL_RETURN_VALUE;
			error++;
			transactionRollback(BATCH_ID);
		}

		// (4)処理件数をログに出力する。
		logger.info(
				batchLogger.createMsg(BusinessMessageCode.B18MB005, BATCH_NAME, apiResult.getDtoList().size(), success,
						error, skips, ""));
		return result;
	}

	/**
	 * FS APIの戻り値が正しいことをチェックする
	 * @param dto FS APIの戻り値をセットしたDTO
	 * @return
	 */
	private boolean isCorrectResponse(GetAppMsgOutputDTO dto) {
		//log info
		logger.info("isCorrectResponse.deliveryId: " + dto.getDeliveryId());
		// バリデーションチェック
		Set<ConstraintViolation<GetAppMsgOutputDTO>> errors = validator.validate(dto);

		// 結果を返却
		if (errors.isEmpty()) {
			return true;
		} else {
			for (ConstraintViolation<GetAppMsgOutputDTO> error : errors) {
				logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB929, error.getPropertyPath(),
						error.getMessage()));
			}
			return false;
		}
	}

	/**
	 * アプリ内Msg統計テーブルに登録する
	 * 
	 * @param dto API実行結果のDTO
	 * @return
	 */
	private boolean insertAppMsgStatistics(GetAppMsgOutputDTO dto) {
		//log info
		logger.info("insertAppMsgStatistics.deliveryId: " + dto.getDeliveryId());
		// (3.2.1)【アプリ内メッセージテーブル】を取得する。
		Optional<AppMessages> appMsgs = getAppMessages(dto.getDeliveryId());
		
		if (appMsgs.isEmpty()) {
			String msg = "FSアプリ内MsgUUID:" + dto.getDeliveryId();
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB926, "アプリ内Msgテーブル", msg));
			return false;
		}

		AppMessages appMsg = appMsgs.get();

		// (3.2.2)【クーポンテーブル】を取得する。
		Optional<Coupons> coupons = getCoupons(appMsg.getCouponId());
		if (coupons.isEmpty()) {
			String msg = "クーポンID:" + appMsg.getCouponId();
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB926, "クーポンテーブル", msg));
			return false;
		}

		Coupons coupon = coupons.get();

		// (3.2.3)【アプリ内メッセージ統計テーブル】に登録する。
		AppMsgStatistics appMsgStatistics = createAppMsgStatistics(dto, appMsg, coupon);
		appMsgStatisticsDAO.insert(appMsgStatistics);

		return true;
	}
	
	/**
	 * クーポンテーブルのレコードを取得する
	 * 
	 * @param couponId クーポンID
	 * @return
	 */
	private Optional<Coupons> getCoupons(Long couponId) {

		//log info
		logger.info("getCoupons.couponId: " + couponId);
		DAOParameter dp = new DAOParameter();
		dp.set("couponId", ConvertUtility.longToString(couponId));
		dp.set("deleteFlag", DeleteFlag.NOT_DELETED.getValue());
		List<Coupons> result = couponsDAO.find(dp, null, false, null);

		// 0件の場合
		if (result.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(result.get(0));
		}
	}

	/**
	 * アプリ内Msgテーブルのレコードを取得する
	 * @param deliveryId アプリ内Msg一覧取得APIのレスポンスBody.deliveryId
	 * @return 取得したアプリ内Msgのリスト
	 */
	private Optional<AppMessages> getAppMessages(String deliveryId) {
		//log info
		logger.info("getAppMessages.deliveryId: " + deliveryId);
		DAOParameter dp = new DAOParameter();
		dp.set("fsAppMessageUuid", deliveryId);
		dp.set("deleteFlag", DeleteFlag.NOT_DELETED.getValue());

		List<AppMessages> result = appMsgDAO.find(dp, null, false, null);

		// 0件の場合ログ出力
		if (result.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(result.get(0));

		}
	}

	/**
	 * アプリ内Msg統計テーブルに登録するエンティティを作成する
	 * 
	 * @param dto    API実行結果
	 * @param appMsg アプリ内Msgテーブルのレコード
	 * @param coupon クーポンテーブルのレコード
	 * @return 作成したエンティティ
	 */
	private AppMsgStatistics createAppMsgStatistics(GetAppMsgOutputDTO dto, AppMessages appMsg, Coupons coupon) {
		//log info
		logger.info("createAppMsgStatistics.appMessageId: " + appMsg.getAppMessageId());
		logger.info("createAppMsgStatistics.couponId: " + coupon.getCouponId());
		Timestamp now = DateUtils.now();

		AppMsgStatistics appMsgStatistics = new AppMsgStatistics();
		appMsgStatistics.setAcquisitionDatetime(DateUtils.now());
		appMsgStatistics.setFsCouponUuid(coupon.getFsCouponUuid());
		appMsgStatistics.setCouponId(coupon.getCouponId());
		appMsgStatistics.setFsAppMessageUuid(ConvertUtility.stringToLong(dto.getDeliveryId()));
		appMsgStatistics.setAppMessageId(appMsg.getAppMessageId());
		appMsgStatistics.setAppMessageType(appMsg.getAppMessageType());
		appMsgStatistics
				.setAppMessageStatus(AppMessageDeliveryStatus.convertFromFsApiStatus(dto.getStatus()).getValue());
		appMsgStatistics.setSendTimeFrom(getStart(dto));
		appMsgStatistics.setSendTimeTo(getEnd(dto));
		if (dto.getCondition().getTrigger().getEventName() != null) {
			appMsgStatistics.setTriggerEventName(dto.getCondition().getTrigger().getEventName().get(0));
		}
		appMsgStatistics.setCumulativeSend(dto.getMessage().get(0).getTotalDeliveryCount());
		appMsgStatistics.setCumulativeTap(dto.getMessage().get(0).getButtons().get(0).getTotalTapCount());
		appMsgStatistics.setCreateUserId(BATCH_ID);
		appMsgStatistics.setCreateDate(now);
		appMsgStatistics.setUpdateUserId(BATCH_ID);
		appMsgStatistics.setUpdateDate(now);
		appMsgStatistics.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());
		return appMsgStatistics;
	}

	/**
	 * 配信期間（開始）のうち、最も過去のものを取得し返却する
	 * 
	 * @param dto API実行結果のDTO
	 * @return
	 */
	private Timestamp getStart(GetAppMsgOutputDTO dto) {
		GetAppMsgOutputDTOConditionTriggerPeriod[] periods = dto.getCondition().getTrigger().getPeriod();
		return Arrays.asList(periods).stream().map(GetAppMsgOutputDTOConditionTriggerPeriod::getStart)
				.min(Timestamp::compareTo).orElseThrow(NoSuchElementException::new);
	}

	/**
	 * 配信期間（終了）のうち、最も未来のものを取得し返却する
	 * 
	 * @param dto API実行結果のDTO
	 * @return
	 */
	private Timestamp getEnd(GetAppMsgOutputDTO dto) {
		GetAppMsgOutputDTOConditionTriggerPeriod[] periods = dto.getCondition().getTrigger().getPeriod();
		return Arrays.asList(periods).stream().map(GetAppMsgOutputDTOConditionTriggerPeriod::getEnd)
				.max(Timestamp::compareTo).orElseThrow(NoSuchElementException::new);
	}

	/**
	 * プロパティファイルを読み込む。
	 */

	private void readProperties() {
		Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);
		url = this.integrationUrl + pro.getProperty("fs.app.msg.list.get.batch.api.url")
				.replace(TEMP_EXP_COUNT, REQUEST_COUNT).replace(TEMP_EXP_PAGE, REQUEST_PAGE);
		retryCount = Integer.parseInt(pro.getProperty("fs.app.msg.list.get.batch.retry.count"));
		sleepTime = Integer.parseInt(pro.getProperty("fs.app.msg.list.get.batch.retry.sleep.time"));
		timeoutDuration = Integer.parseInt(pro.getProperty("fs.app.msg.list.get.batch.timeout.duration"));
	}

	/**
	 * (3.1)FS API連携 <br>
	 * アプリ内Msg一覧取得APIの呼び出しを行う <br>
	 * <code>GetAppMsgResult</code>を返却する <br>
	 * 成功時（<code>FsApiCallResult</code>がSUCCESS）以外は<code>GetAppMsgResult.dtoList</code>が空のリストの状態で返却される
	 * @return GetAppMsgResult
	 */

	public GetAppMsgResult getAppMsgList() {

		// 処理結果格納用
		GetAppMsgResult result = new GetAppMsgResult();

		int page = 1;
		// 繰り返し処理初回（1ページ目）
		FsApiCallResponse response = callFsApi(url, page);
		// 処理結果判定
		if (response == null) {
			throw new AssertionError();
		}
		FsApiCallResult fsApiResult = response.getFsApiCallResult();
		result.setResult(fsApiResult);
		// 成功時以外は結果コードのみ返却
		if (fsApiResult != FsApiCallResult.SUCCESS) {
			return result;
		}

		// 次ページ取得
		List<String> links = response.getResponse().headers().allValues("Link");
		String nextUrl = (links.isEmpty()) ? "" : links.get(0);
		List<GetAppMsgOutputDTO> resultList = setResponseToDto(response.getResponse());

		// 繰り返し処理二回目以降（2ページ目以降）
		// 前回実行したAPIのレスポンスヘッダのLink項目に含まれる次ページデータのURLからデータを取得する
		page++;
		while (!nextUrl.isBlank()) {
			response = callFsApi(nextUrl, page);

			// 処理結果判定
			if (response == null) {
				throw new AssertionError();
			}
			fsApiResult = response.getFsApiCallResult();
			result.setResult(fsApiResult);
			// 成功時以外は結果コードのみ返却
			if (fsApiResult != FsApiCallResult.SUCCESS) {
				result.setDtoList(new ArrayList<>());
				return result;
			}

			// 次ページ取得
			links = response.getResponse().headers().allValues("Link");
			nextUrl = links.isEmpty() ? "" : links.get(0);
			resultList.addAll(setResponseToDto(response.getResponse()));
			page++;
		}

		result.setDtoList(resultList);
		return result;

	}

	/**
	 * FS APIから返却されたレスポンスをDTOに格納する
	 * 
	 * @param response FS APIから返却されたレスポンス
	 * @return
	 */
	private List<GetAppMsgOutputDTO> setResponseToDto(HttpResponse<String> response) {

		try {
			return new ArrayList<>(Arrays.asList(mapper.readValue(response.body(), GetAppMsgOutputDTO[].class)));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IllegalArgumentException();
		}
	}

	/**
	 * FANSHIP APIをPOSTで呼び出す(リトライあり)
	 * 
	 * @param url APIのURL
	 */
	protected FsApiCallResponse callFsApi(String url, int page) {
		//log info
		logger.info("callFsApi.url: " + url);
		logger.info("callFsApi.page: " + page);

		// FS API 実行
		FsApiCallResponse result = callFanshipApi(BATCH_ID, FANSHIP_API_NAME, url, "", HttpMethodType.GET,
				ContentType.APPLICATION_JSON,
				TokenHeaderType.AUTHORIZATION, API_SUCCESS_RETURN_CODE, retryCount, sleepTime, timeoutDuration,
				RetryKbn.SERVER_ERROR_TIMEOUT);

		// ログ出力
		outputFsApiLog(result, page);

		return result;

	}

	// FS API の結果をログ出力する
	private void outputFsApiLog(FsApiCallResponse result, int page) {
		//log info
		logger.info("outputFsApiLog.page" + page);
		if (result.getFsApiCallResult() != FsApiCallResult.SUCCESS) {
			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB931, "FS API 連携", "ページ：" + page));
		}
	}

	/**
	 * アプリ内Msg一覧取得APIの取得結果格納用クラス
	 * @author m-omori
	 *
	 */
	private static class GetAppMsgResult {

		/** FS API 実行結果 */
		private FsApiCallResult result;

		/** 取得したアプリ内MsgのDAOのリスト*/
		private List<GetAppMsgOutputDTO> dtoList = new ArrayList<>();

		public FsApiCallResult getResult() {
			return result;
		}

		public void setResult(FsApiCallResult result) {
			this.result = result;
		}

		public List<GetAppMsgOutputDTO> getDtoList() {
			return dtoList;
		}

		public void setDtoList(List<GetAppMsgOutputDTO> dtoList) {
			this.dtoList = dtoList;
		}
	}
}
