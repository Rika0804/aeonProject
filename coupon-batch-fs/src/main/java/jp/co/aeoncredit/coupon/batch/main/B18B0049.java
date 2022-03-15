package jp.co.aeoncredit.coupon.batch.main;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetOutputDTO;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.dao.custom.SiteStatisticsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.SiteStatistics;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

@Named("B18B0049")
@Dependent
public class B18B0049 extends BatchFSApiCalloutBase {
	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0049.getBatchId();

	/** バッチNAME */
	private static final String BATCH_NAME = BatchInfo.B18B0049.getBatchName();

	/**503の時*/
	private static final String MAINTENANCE_VALUE = "2";

	/** ログ */
	private Logger myLog = getLogger();

	/** アプリ内Msg一覧取得API 成功時のステータスコード */
	private static final List<Integer> API_SUCCESS_RETURN_CODE = List.of(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

	/** メッセージ共通 */
	private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0049.getBatchId());

	/**iphone*/
	private static final String IPHONE = "iphone";

	/**android*/
	private static final String ANDROID = "android";

	@Inject
	private SiteStatisticsDAOCustomize sitestatisticsDAO;

	/** URL */
	private String proUrl;

	/** リトライ回数 */
	private int retryCount;

	/**リトライ時スリープ時間(ミリ秒)*/
	private int sleepTime;

	/**タイムアウト期間(秒
	 * )*/
	private int timeoutDuration;

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BatchInfo.B18B0049.getBatchId());

	/**(3.1.1)の取得結果の全件数 *iPhone*/
	private int allCountI = 0;

	/**push_enabled"=trueの人数 *iPhone*/
	private int pushCountI = 0;

	/**location_enabled=trueの人数 *iPhone*/
	private int gpsCountI = 0;

	/**(3.1.1)の取得結果の全件数 *Android*/
	private int allCountA = 0;

	/**push_enabled"=trueの人数 *Android*/
	private int pushCountA = 0;

	/**location_enabled=trueの人数 *Android*/
	private int gpsCountA = 0;

	/**
	 * バッチの起動時メイン処理
	 * 
	 */
	@Override
	public String process() {

		String returnCode = ProcessResult.FAILURE.getValue();

		/**処理開始メッセージを出力する*/
		myLog.info(
				batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BatchInfo.B18B0049.getBatchName()));

		/**プロパティファイルから情報を取得する。*/
		readProperties();

		AuthTokenResult authTokenResult = getAuthToken(BatchInfo.B18B0049.getBatchId());

		if (authTokenResult == AuthTokenResult.SUCCESS) {
			/**FS API連携を行う*/
			String resultI = cooperateFSAPI(IPHONE);

			if (ProcessResult.SUCCESS.getValue().equals(resultI)) {
				/**FS API連携を行う*/
				String resultA = cooperateFSAPI(ANDROID);

				if (ProcessResult.SUCCESS.getValue().equals(resultA)) {
					/**(4)サイト統計テーブル登録を行う*/
					returnCode = registStatisticalTable();

				} else if (MAINTENANCE_VALUE.equals(resultA)) {
					returnCode = ProcessResult.SUCCESS.getValue();
				}
			} else if (MAINTENANCE_VALUE.equals(resultI)) {
				returnCode = ProcessResult.SUCCESS.getValue();
			}
		} else if (authTokenResult == AuthTokenResult.MAINTENANCE) {
			returnCode = ProcessResult.SUCCESS.getValue();
		}

		/**(5)処理終了メッセージを出力する。*/
		myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BatchInfo.B18B0049.getBatchName(),
				ProcessResult.SUCCESS.getValue().equals(returnCode)));
		return setExitStatus(returnCode);
	}

	/**
	 * プロパティファイルを読み込む
	 */
	private void readProperties() {
		Properties pro = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0049.getBatchId());

		proUrl = pro.getProperty("fs.site.statistics.get.batch.api.url");

		retryCount = Integer.parseInt(pro.getProperty("fs.site.statistics.get.batch.retry.count"));

		sleepTime = Integer.parseInt(pro.getProperty("fs.site.statistics.get.retry.sleep.time"));

		timeoutDuration = Integer.parseInt(pro.getProperty("fs.site.statistics.get.timeout.duration"));

	}

	/**
	 * サイト統計テーブル登録
	 */
	private String registStatisticalTable() {

		String returnCode = ProcessResult.FAILURE.getValue();
		
		/**entityを作成する*/
		SiteStatistics entity = new SiteStatistics();

		try {

			//トランザクションの開始
			transactionBegin(BatchInfo.B18B0049.getBatchId());

			/**1.取得日時*/
			entity.setAcquisitionDatetime(DateUtils.now());

			/**2.登録者数合計*/
			entity.setTotalRegisterdPerson(allCountI + allCountA);

			/**3.Push通知許諾者数合計*/
			entity.setTotalPushEnabled(pushCountI + pushCountA);

			/**4.GPS許諾者数合計*/
			entity.setTotalGpsEnabled(gpsCountI + gpsCountA);

			/**5.iPhone登録者数合計*/
			entity.setTotalIphone(allCountI);

			/**6.iPhonePush通知許諾者数*/
			entity.setTotalIphonePushEnabled(pushCountI);

			/**7.iPhoneGPS許諾者数*/
			entity.setTotalIphoneGpsEnabled(gpsCountI);

			/**8.Android登録者数*/
			entity.setTotalAndroid(allCountA);

			/**9.AndroidPush通知許諾者数*/
			entity.setTotalAndroidPushEnabled(pushCountA);

			/**10.AndroidGPS許諾者数*/
			entity.setTotalAndroidGpsEnabled(gpsCountA);

			/**11.作成者ID*/
			entity.setCreateUserId(BatchInfo.B18B0049.getBatchId());

			/**13.更新者ID*/
			entity.setUpdateUserId(BatchInfo.B18B0049.getBatchId());

			/**15.削除フラグ*/
			entity.setDeleteFlag("0");

			/**INSERTを行う*/
			sitestatisticsDAO.insert(entity);

			/**トランザクションのコミット*/
			transactionCommit(BatchInfo.B18B0049.getBatchId());

			returnCode = ProcessResult.SUCCESS.getValue();

			return returnCode;

		} catch (Exception e) {
		    myLog.error(e.getMessage(), e);
			transactionRollback(BatchInfo.B18B0049.getBatchId());
			return returnCode;
		}
	}

	/**
	 * FS API発行＆後処理
	 * @param device 端末(iphone、android)
	 * @return 0:成功、1:失敗、2:メンテナンス
	 */
	private String cooperateFSAPI(String device) {
	    // export log
	    myLog.info("cooperateFSAPI.device：" + device);
		
	    String returnCode = ProcessResult.FAILURE.getValue();

		ObjectMapper mapper = new ObjectMapper();

		RegisterInfoListGetInputDTO inputDTO = new RegisterInfoListGetInputDTO();

		String path = String.format("%s%s/", proUrl, device);

		String[] name = { "push_enabled", "location_enabled" };
		inputDTO.setFields(name);
		inputDTO.setFlat(false);
		inputDTO.setCount(100000);

		int page = 1;
		int pages = 0;

		try {
			do {
				inputDTO.setPage(page);
				String param = mapper.writeValueAsString(inputDTO);

				FsApiCallResponse fsApiResponse = fsApiPost(path, param, device);

				if (fsApiResponse == null) {
					return returnCode;
				} else {

					FsApiCallResult fsApiResult = fsApiResponse.getFsApiCallResult();

					if (fsApiResult == FsApiCallResult.SUCCESS) {

						HttpResponse<String> response = fsApiResponse.getResponse();

						RegisterInfoListGetOutputDTO outputDTO = mapper.readValue(response.body(),
								RegisterInfoListGetOutputDTO.class);
						page = outputDTO.getPage();
						pages = outputDTO.getPages();

						// 総件数(1ページ目の時だけ設定)
						if (page == 1) {
							if (device.equals(IPHONE)) {
								allCountI = outputDTO.getTotalResults();
							} else {
								allCountA = outputDTO.getTotalResults();
							}
						}
						
						countup(device, outputDTO.getResult());

						returnCode = ProcessResult.SUCCESS.getValue();

						//503か429の時（メンテナンスを入れるケース）
					} else if (fsApiResult == FsApiCallResult.TOO_MANY_REQUEST ||
							fsApiResult == FsApiCallResult.FANSHIP_MAINTENANCE) {
						returnCode = MAINTENANCE_VALUE;
						break;

					} else if (fsApiResult != FsApiCallResult.SUCCESS) {
						returnCode = ProcessResult.FAILURE.getValue();
						break;
					}
					page++;
				}

			} while (page <= pages);

		} catch (Exception e) {
		    myLog.error(e.getMessage(), e);
			// エラー
			myLog.error(errorMessage(device, e));
			return returnCode;
		}
		return returnCode;
	}

	/**
	 * 件数カウント
	 * @param device 端末(iphone、android)
	 * @param result 登録者情報
	 */
	private void countup(String device, List<List<Boolean>> result) {
		/** push_enabled==trueの件数 */
		for (int j = 0; j < result.size(); j++) {
			if (Boolean.TRUE.equals(result.get(j).get(0))) {
				if (device.equals(IPHONE)) {
					pushCountI++;
				} else {
					pushCountA++;
				}
			}
			/** location_enabled==trueの件数*/
			if (Boolean.TRUE.equals(result.get(j).get(1))) {
				if (device.equals(IPHONE)) {
					gpsCountI++;
				} else {
					gpsCountA++;
				}
			}
		}
	}

	/**
	 * エラーメッセージを生成
	 * @param device
	 * @param e
	 * @return
	 */
	private String errorMessage(String device, Exception e) {
		String errorMessage = "";
		errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
				BATCH_NAME, "なし",
				String.format("デバイス =%s,エラーコード =%s,エラーメッセージ =%s", device, e.getClass().getName(),
						e.getMessage()));

		return batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMessage);

	}

	/**
	 * レスポンスを取得する
	 * @param path APIパス
	 * @param param パラメータ
	 * @return レスポンス
	 * @throws IOException
	 */
	private FsApiCallResponse fsApiPost(String path, String param, String device) throws Exception {
		String url = this.reverseProxyUrl + path;

		FsApiCallResponse result = callFanshipApi(BATCH_ID, BATCH_NAME, url, param, HttpMethodType.POST,
				ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION_POPINFOLOGIN, API_SUCCESS_RETURN_CODE,
				retryCount, sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);

		return result;
	}

}
