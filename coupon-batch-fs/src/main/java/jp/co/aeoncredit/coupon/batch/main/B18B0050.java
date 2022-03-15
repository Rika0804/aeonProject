package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.dao.DAOParameter;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.ReferListCouponResourseOutputDTO;
import jp.co.aeoncredit.coupon.constants.FsCouponType;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.dao.custom.CouponStatisticsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.CouponStatistics;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

@Named("B18B0050")
@Dependent
public class B18B0050 extends BatchFSApiCalloutBase {
	
	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0050.getBatchId();

	/** バッチNAME */
	private static final String BATCH_NAME = BatchInfo.B18B0050.getBatchName();

	/**503の時*/
	private static final String MAINTENANCE_VALUE = "2";

	private static final String X_TOTAL_COUNT = "X-Total-Count";
	
	/**ページ数*/
	private static final int PAGE_SIZE = 100;

	/** ログ */
	private final Logger myLog = getLogger();

	/** FSクーポン一覧取得API 成功時のステータスコード */
	private static final List<Integer> API_SUCCESS_RETURN_CODE = List.of(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

	/** メッセージ共通 */
	private final BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0050.getBatchId());

	@Inject
	private CouponStatisticsDAOCustomize couponStatisticsDAO;
	/** URL */
	private String url;

	/** リトライ回数 */
	private int retryCount;

	/**リトライ時スリープ時間(ミリ秒)*/
	private int sleepTime;

	/**タイムアウト期間(秒)*/
	private int timeoutDuration;

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BatchInfo.B18B0050.getBatchId());

	/**処理対象件数*/
	private int allCoupons;

	/**処理成功件数*/
	private int successCoupons;

	/**処理失敗件数*/
	private int failCoupons;

	/**処理スキップ件数*/
	private int skips;

	/**ObjectMapper*/
	private ObjectMapper mapper = new ObjectMapper();

	@Inject
	protected CouponsDAOCustomize couponsDAO;

	/**
	 * バッチの起動時メイン処理
	 */
	@Override
	public String process() {

		//認証トークン
		AuthTokenResult authTokenResult;

		String returnCode = null;
		//処理開始メッセージを出力する
		myLog.info(
				batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BatchInfo.B18B0050.getBatchName()));

		//プロパティファイルから情報を取得する。
		readProperties();

		//認証トークン取得処理

		authTokenResult = getAuthToken(BatchInfo.B18B0050.getBatchId());

		if (authTokenResult == AuthTokenResult.SUCCESS) {

			//FS API連携を行う
			String result = cooperateFSAPI();

			if (result.equals(ProcessResult.SUCCESS.getValue())) {

				try {
					if (allCoupons != skips) {
						//トランザクションのコミット
						transactionCommit(BatchInfo.B18B0050.getBatchId());
					}
					//登録に成功した件数
					successCoupons = allCoupons - skips - failCoupons;

					returnCode = ProcessResult.SUCCESS.getValue();

					//処理件数をログに出力
					myLog.info(finishMessage());

				} catch (RuntimeException e) {
				    myLog.error(e.getMessage(), e);
					//クーポン統計テーブルへの登録に失敗した数
					failCoupons++;
					transactionRollback(BatchInfo.B18B0050.getBatchId());
					myLog.info(finishMessage());

					returnCode = ProcessResult.FAILURE.getValue();
				}

			} else if (result.equals(MAINTENANCE_VALUE)) {
				returnCode = ProcessResult.SUCCESS.getValue();

			} else {
				returnCode = ProcessResult.FAILURE.getValue();
			}

		} else if (authTokenResult == AuthTokenResult.MAINTENANCE) {
			returnCode = ProcessResult.SUCCESS.getValue();

		} else if (authTokenResult == AuthTokenResult.FAILURE) {
			returnCode = ProcessResult.FAILURE.getValue();
		}

		myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BatchInfo.B18B0050.getBatchName(),
				ProcessResult.SUCCESS.getValue().equals(returnCode)));
		return setExitStatus(returnCode);
	}

	/**
	 * プロパティファイルを読み込む
	 */
	private void readProperties() {
		Properties pro = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0050.getBatchId());

		url = pro.getProperty("fs.coupon.list.get.batch.api.url");

		retryCount = Integer.parseInt(pro.getProperty("fs.coupon.list.get.batch.retry.count"));

		sleepTime = Integer.parseInt(pro.getProperty("fs.coupon.list.get.batch.retry.sleep.time"));

		timeoutDuration = Integer.parseInt(pro.getProperty("fs.coupon.list.get.batch.timeout.duration"));

	}

	/**
	 * クーポン統計テーブルに登録
	 * @param outputDto
	 */
	private void couponStatisticsTable(ReferListCouponResourseOutputDTO[] dtoList) {

		Coupons coupons;

		for (ReferListCouponResourseOutputDTO outputDTO : dtoList) {
			//クーポン情報の総数をカウント
			allCoupons++;

			DAOParameter couponParam = new DAOParameter();

			//entityを作成する
			CouponStatistics entity = new CouponStatistics();

			//uuidが一致するデータをクーポンテーブルから取得
			couponParam.set("fsCouponUuid", outputDTO.getUuid());
			Optional<Coupons> couponsList = couponsDAO.findOne(couponParam, null);

			//ない時は処理をスキップ 
			if (couponsList.isEmpty()) {
				//スキップした数
				skips++;
				myLog.info(dbEmptyError(outputDTO.getUuid()));

			} else {
				//couponListからクーポンを取り出す
				coupons = couponsList.get();

				if ((allCoupons - skips) == 1) {
					//トランザクションの開始
					transactionBegin(BatchInfo.B18B0050.getBatchId());
				}
				//取得日時
				entity.setAcquisitionDatetime(DateUtils.now());

				//FSクーポンUUID
				entity.setFsCouponUuid(outputDTO.getUuid());

				//クーポンID
				entity.setCouponId(coupons.getCouponId());

				//クーポン種別
				entity.setCouponType(coupons.getCouponType());

				//FSクーポン種別
				if (outputDTO.isIsDistributable()) {
					entity.setFsCouponType(FsCouponType.LIMITED_COUPON.getValue());
				} else {
					entity.setFsCouponType(FsCouponType.NORMAL_COUPON.getValue());
				}
				//クーポン表示期間_開始
				entity.setDisplaydateFrom(Timestamp.valueOf(outputDTO.getVisibleStartAt()));
				
				//クーポン表示期間_終了
				entity.setDisplaydateTo(Timestamp.valueOf(outputDTO.getVisibleEndAt()));

				//クーポン全体配信上限枚数
				entity.setTotalDistributableCount(outputDTO.getTotalUsableCount());

				//FSクーポン公開フラグ
				if (outputDTO.isIsOpen()) {
					entity.setOpenFlag("1");
				} else {
					entity.setOpenFlag("0");
				}

				//配信数
				entity.setCounterDistributed(outputDTO.getCounterDistributed());

				//お気に入り登録数
				entity.setCounterFavorite(outputDTO.getCounterFavorite());

				//利用数
				entity.setCounterUsed(outputDTO.getCounterUsed());

				//作成者ID
				entity.setCreateUserId(BatchInfo.B18B0050.getBatchId());

				//作成日
				entity.setCreateDate(Timestamp.valueOf(outputDTO.getCreatedAt()));

				//更新者ID
				entity.setUpdateUserId(BatchInfo.B18B0050.getBatchId());

				//更新日
				entity.setUpdateDate(Timestamp.valueOf(outputDTO.getUpdatedAt()));

				//削除フラグ
				entity.setDeleteFlag("0");

				//INSERTを行う
				couponStatisticsDAO.insert(entity);
			}
		}
	}

	/**
	 * テーブルの取得結果が0件の場合
	 * 
	 */
	private String dbEmptyError(String uuid) {
		String msg = "クーポンテーブルのレコードが取得できませんでした(FSクーポンUUID:[" + uuid + "])";
		return batchLogger.createMsg(BusinessMessageCode.B18MB926.toString(), msg);
	}

	
	/**
	 * エラーメッセージを生成
	 * @param httpStatusCode
	 * @param dto
	 * @param e
	 * @return batchLogger.createMsg(BusinessMessageCode.B18MB002.toString(), errorMessage)
	 */
	private String errorMessage(Integer httpStatusCode, int pageNum, ReferListCouponResourseOutputDTO[] dtoList,
			Exception e) {
		String errorMessage = "";
				errorMessage = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
						BATCH_NAME, "なし",
						String.format("ページ=%s, %s : %s", pageNum, e.getClass().getName(), e.getMessage()));
			
		return batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMessage);

	}

	/**
	 * 全てのPush通知情報の処理が完了した時
	 * 
	 */
	private String finishMessage() {
		String msg = "クーポン一覧取得処理が完了しました。（処理対象件数:[" + allCoupons + "],処理成功件数:[" + successCoupons + "],処理失敗件数:["
				+ failCoupons + "],処理スキップ件数:[" + skips + "],)";
		return batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg);
	}

	/**
	 * (3.1)FS API連携
	 * @return returnCode(HTTPステータスが503の場合は、MAINTENANCE_VALUEを返す。それ以外はProcessResultを返す）
	 */

	public String cooperateFSAPI() {

		String returnCode = ProcessResult.FAILURE.getValue();
		//ページ数
		int pageNum = 0;
		//APIの件数
		int apiNum = 0;
		long totalCount = 0;

		try {

			do {
				apiNum += PAGE_SIZE;

				pageNum++;
				FsApiCallResponse fsApiResponse = fsApiGet(pageNum);

				if (fsApiResponse == null) {
					return returnCode;
				} else {
					FsApiCallResult fsApiResult = fsApiResponse.getFsApiCallResult();

					if (fsApiResult == FsApiCallResult.SUCCESS) {
						HttpResponse<String> response = fsApiResponse.getResponse();
						ReferListCouponResourseOutputDTO[] dtoList = mapper.readValue(response.body(),
								ReferListCouponResourseOutputDTO[].class);
						try {
							//レスポンスヘッダの「X-Total-Count」(総数）を取得する
							OptionalLong totalCountHeader = response.headers().firstValueAsLong(X_TOTAL_COUNT);
							if (totalCountHeader.isPresent()) {
								totalCount = totalCountHeader.getAsLong();
						
							} else {
								returnCode = ProcessResult.FAILURE.getValue();
								break;
							}
						} catch (NumberFormatException e) {
						    myLog.error(e.getMessage(), e);
							throw new NumberFormatException();
						}

						//クーポン統計テーブルに登録
						couponStatisticsTable(dtoList);
						returnCode = ProcessResult.SUCCESS.getValue();

						//503か429の時（メンテナンスを入れるケース）
					} else if (fsApiResult == FsApiCallResult.TOO_MANY_REQUEST
							|| fsApiResult == FsApiCallResult.FANSHIP_MAINTENANCE) {
						returnCode = MAINTENANCE_VALUE;
						break;
					}else {
						returnCode = ProcessResult.FAILURE.getValue();
						break;
					}
				}

				//レスポンスヘッダの「X-Total-Count」(総数)に達するまで繰り返す
			} while (apiNum < totalCount);

		} catch (Exception e) {
			myLog.error(errorMessage(null, pageNum, null, e));
			returnCode = ProcessResult.FAILURE.getValue();
		}
		return returnCode;
	}

	/**
	 * FANSHIP APIをGETで呼び出す(リトライあり)
	 * @param jobid
	 * @return response
	 * @throws Exception
	 */

	private FsApiCallResponse fsApiGet(int pageNum) throws Exception {

		String uri = this.reverseProxyUrl + url + "?page_size=" + PAGE_SIZE + "&sort=-id&page=" + pageNum;
		FsApiCallResponse result = callFanshipApi(BATCH_ID, BATCH_NAME, uri, "", HttpMethodType.GET,
				ContentType.APPLICATION_JSON,TokenHeaderType.X_POPINFO_MAPI_TOKEN, API_SUCCESS_RETURN_CODE, retryCount, sleepTime, timeoutDuration,
				RetryKbn.SERVER_ERROR_TIMEOUT);

		return result;
	}

}
