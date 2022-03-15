package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDeliveryPlansCreator;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.dto.CouponDeliveryApiInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.CouponDeliveryApiOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.CouponDeliveryCouponsOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.DeliveryTargetDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsCouponDeliveryMaDataOutputDTO;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeliveryBatchStatus;
import jp.co.aeoncredit.coupon.constants.DeliverySaveMethod;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.SequenceType;
import jp.co.aeoncredit.coupon.dao.custom.CouponSequenceCountDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.DeliveryTargetDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUsersDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstAppUsersDAOCustomize;
import jp.co.aeoncredit.coupon.entity.CouponSequenceCount;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSクーポン配信バッチ
 */
@Named("B18B0012")
@Dependent
public class B18B0012 extends BatchFSApiCalloutBase {

	/** FSクーポン配信処理結果 */
	private enum FsCouponDeliveryResult {
		/** 成功 */
		SUCCESS,
		/** DBエラー ※異常終了 */
		DB_ERROR,
		/** レスポンスBODYエラー ※異常終了 */
		RESPONSE_ERROR
	}

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0012.getBatchId();

	/** バッチネーム*/
	private static final String BATCH_NAME = BatchInfo.B18B0012.getBatchName();

	/** 正常終了_戻り値 */
	private static final String SUCCESS_RETURN_VALUE = "0";

	/** 異常終了_戻り値 */
	private static final String FAIL_RETURN_VALUE = "1";

	/** システム日付をyyyyMMddHHmmssSSS形式*/
	private static final DateTimeFormatter dtfYyyymmddhhmmsssss = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	/** システム日付をyyyyMMddHHmmss形式*/
	private static final DateTimeFormatter dtfYyyymmddhhmmss = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	/** 限定クーポン配布APIのURL */
	private String apiUrl = "";

	/** データ件数(枝番単位) */
	protected int readCount = 0;

	/** 成功件数(枝番単位) */
	protected int successCount = 0;

	/** エラー件数(枝番単位) */
	protected int failCount = 0;

	/** スキップ件数(枝番単位) */
	protected int skipCount = 0;

	/** データ件数(FS枝番単位) */
	protected int readFsCount = 0;

	/** 成功件数(FS枝番単位) */
	protected int successFsCount = 0;

	/** エラー件数(FS枝番単位) */
	protected int failFsCount = 0;

	/** スキップ件数(FS枝番単位) */
	protected int skipFsCount = 0;

	/** ログ */
	protected Logger logger = getLogger();

	/** メッセージ共通 */
	private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** ファイル共通 */
	protected BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

	/** リトライ回数 */
	private int retryCount;

	/**リトライ時スリープ時間(ミリ秒)*/
	private int sleepTime;

	/**タイムアウト期間(秒)*/
	private int timeoutDuration;

	/**MA用配信結果ファイル格納先ディレクトリ*/
	private String fileDirectory;

	/**MA用配信結果ファイル名*/
	private String fileName;

	/**MAクーポン配信結果対象取得件数*/
	private int maDeliveryTargetCount;

	/**「配布上限枚数」エラー判定文言*/
	private String deliveryLimitWord;

	/** プロパティファイル共通 */
	private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	/** 配信予定・依頼リスト出力 */
	private BatchDeliveryPlansCreator b18B0009;

	private ObjectMapper mapper = new ObjectMapper();

	/** 共通メッセージ */
	private static final String API_ERR_MSG = "限定クーポン配布";
	private static final String BATCH_ERR_MSG = "配信予定・依頼リスト出力バッチ";

	/** メッセージフォーマット */
	private static final String START_COUPON_MSG = "処理対象：クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String SKIP_COUPON_MSG = "処理中断スキップ対象：クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String ERROR_TARGET_MSG = "エラー対象：クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String FS_USER_COUNT_MSG = "FS枝番[%s]内のFSユーザ件数：%s";
	private static final String COUPON_ID_COMMON_INSIDE_ID_MSG = "クーポンID = %s、枝番 = %s、FS枝番 = %s、内部共通ID = %s";
	private static final String COUPON_ID_BRANCH_ID_MSG = "クーポンID = %s、枝番 = %s";
	private static final String COUPON_ID_BRANCH_ID_FS_BRANCH_ID_MSG = "クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String API_REQUEST_BODY_MSG = "リクエストボディ生成失敗、クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String API_DESERIALIZE_ERROR = "レスポンスのデシリアライズ失敗、クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String API_RESPONSE_ERROR = "配信結果情報がありません、クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String DELIVERY_LIMIT_MSG = "%s、クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String DELIVERY_LIMIT_AFTER_MSG = "配布上限枚数に達しているため、FS連携は行わず「9:FS連携失敗」に更新します。、クーポンID = %s、枝番 = %s、FS枝番 = %s";
	private static final String SEQUENCE_MSG = "シーケンス種別 = 1";
	private static final String NOT_FINISH_MSG = "FSクーポンユーザテーブルに「3:FS連携済み」、「9:FS連携失敗」以外が存在します。、クーポンID = %s、枝番 = %s";

	/** 配布上限枚数エラーステータス */
	private static final int DELIVERY_LIMIT_ERROR_STATUS = 400;

	/** フェッチする必要のある行数 */
	private static final int FETCH_SIZE = 1000;

	@Inject
	protected FsCouponUsersDAOCustomize fsCouponUsersDAO;
	@Inject
	protected MstAppUsersDAOCustomize mstAppUsersDAO;
	@Inject
	protected CouponsDAOCustomize couponsDAO;
	@Inject
	protected DeliveryTargetDAOCustomize deliveryTargetDAO;
	@Inject
	protected CouponSequenceCountDAOCustomize couponSequenceCountDAO;

	/** 配信結果区分(配信成功、配信失敗、結果に存在しない) */
	private enum DeliveryResultKBN {
		SUCCESS, ERROR, NOT_EXIST;
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

		// #1 FSクーポン配信バッチ
		String returnResult = fsCouponDeliveryProcess();

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

		// 限定クーポン配布APIのURL
		apiUrl = properties.getProperty("fs.coupon.delivery.batch.api.url");
		// FS API 失敗時のAPI実行リトライ回数
		retryCount = Integer.parseInt(properties.getProperty("fs.coupon.delivery.batch.retry.count"));
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		sleepTime = Integer.parseInt(properties.getProperty("fs.coupon.delivery.batch.retry.sleep.time"));
		// FS API発行時のタイムアウト期間(秒)
		timeoutDuration = Integer.parseInt(properties.getProperty("fs.coupon.delivery.batch.timeout.duration"));
		// MA用配信結果ファイル格納先ディレクトリ
		fileDirectory = properties.getProperty("fs.coupon.delivery.batch.result.file.directory");
		// MA用配信結果ファイル名
		fileName = properties.getProperty("fs.coupon.delivery.batch.result.file.name");
		// MAクーポン配信結果対象取得件数
		maDeliveryTargetCount = Integer
				.parseInt(properties.getProperty("fs.coupon.delivery.batch.ma.delivery.target.count"));
		//「配布上限枚数」エラー判定文言
		deliveryLimitWord = properties.getProperty("fs.coupon.delivery.batch.delivery.limit.word");

	}

	/**
	 * #1 FSクーポン配信
	 * 
	 * @return 0：正常；1：異常；
	 * @throws SQLException 
	 */
	private String fsCouponDeliveryProcess() throws SQLException {

		// (2)認証トークン取得
		// 【B18BC001_認証トークン取得】を実行する。
		AuthTokenResult authTokenResult = getAuthToken(BATCH_ID);

		switch (authTokenResult) {
		case SUCCESS:
			// (2.1a) 【B18BC001_認証トークン取得】の戻り値が「成功（enumで定義）」の場合 は、処理を継続する。
			return fsCouponDelivery();

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
	 * クーポン配信
	 * 
	 * @return 0：正常；1：異常；
	 * @throws SQLException 
	 */
	private String fsCouponDelivery() throws SQLException {

		// 全体処理中止フラグ(trueの場合、以降の全ての処理を行わない)
		boolean stopAllFlg = false;

		// 戻り値
		String returnValue = SUCCESS_RETURN_VALUE;

		// (3.1)クーポンを取得
		List<CouponDeliveryCouponsOutputDTO> couponInfoList = findCouponIdAll();
		if (couponInfoList.isEmpty()) {
			// レコードが取得できなかった場合、戻り値に"0"を設定し、ログに出力後、処理終了する。

			// メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
					"FSクーポン配信", "クーポン取得");

			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));

			return returnValue;

		}

		// (3.3)FS API連携とFSクーポンユーザ更新
		// 上記(3.1)で取得した【クーポンテーブル】をレコード分繰り返す（クーポンID、枝番単位）
		for (CouponDeliveryCouponsOutputDTO couponInfo : couponInfoList) {

			// 配布上限枚数エラーフラグ（trueの場合、以降のFS枝番はFS連携せずに「9:FS連携失敗」とする）
			boolean deliveryLimitFlg = false;

			// 一部処理中止フラグ(trueの場合、以降のFS枝番の処理は行わずに次のクーポンを実施)
			boolean stopPartFlg = false;

			// 件数初期化(枝番単位)
			readCount = 0;
			successCount = 0;
			failCount = 0;
			skipCount = 0;

			// ユーザ件数、FS枝番の最大値を取得
			List<int[]> countList = findFsCouponUsersCount(couponInfo.getCouponId(), couponInfo.getBranchId());

			// データ件数カウント(枝番単位)
			readCount = ConvertUtility.objectToInteger(countList.get(0)[0]);

			if (readCount == 0) {
				// レコードが取得できなかった場合、ログを出力し(3.2)の処理へ（次の「クーポンID、枝番」）

				// メッセージを出力（処理対象レコードがありません。（処理：%s, %s））
				String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
						"FSクーポンユーザテーブル取得（FS枝番単位）",
						String.format(COUPON_ID_BRANCH_ID_MSG, couponInfo.getCouponId(), couponInfo.getBranchId()));

				logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));

				continue;
			}

			// FS枝番の最大値を取得
			int fsBranchIdMax = ConvertUtility.objectToInteger(countList.get(0)[1]);

			// 処理残件数(枝番単位)
			int remainingNumber = readCount;

			// (3.3.2)【FSクーポンユーザテーブル】.「FS枝番」の最大値分繰り返す
			for (int i = 1; i <= fsBranchIdMax; i++) {

				// 件数初期化(FS枝番単位)
				readFsCount = 0;
				successFsCount = 0;
				failFsCount = 0;
				skipFsCount = 0;

				// 一部処理中断フラグがtrueの場合
				if (stopPartFlg) {
					// 処理中断スキップメッセージ
					writeSkipTargetLog(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

					continue;
				}

				// 処理対象メッセージ
				writeProcessTargetLog(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

				// (3.3.1)ユーザ取得（FS枝番単位）
				List<DeliveryTargetDTO> fsCouponUsersList = findFsCouponUsersList(couponInfo.getCouponId(),
						couponInfo.getBranchId(), String.valueOf(i));

				// FS枝番内の件数
				readFsCount = fsCouponUsersList.size();

				// 処理対象件数メッセージ
				writeFsUsetCountLog(fsCouponUsersList.size(), String.valueOf(i));

				// 処理残件数
				remainingNumber = remainingNumber - fsCouponUsersList.size();

				// リクエストBody用のUUIDリスト作成
				List<String> uuidList = new ArrayList<>();
				List<DeliveryTargetDTO> targetList = fsCouponUsersList.stream()
						.filter(s -> s.getAwTrackingId() != null).collect(Collectors.toList());
				for (DeliveryTargetDTO deliveryTarget : targetList) {
					if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
						// ターゲットクーポンの場合、アプリユーザマスタの「イオンウォレットトラッキングID」を格納
						uuidList.add(deliveryTarget.getAwTrackingId());

					} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
						// パスポートクーポンの場合、FSクーポンユーザの「CPパスポートID」を格納
						uuidList.add(deliveryTarget.getAcsUserCardCpPassportId());
					}
				}

				// アプリユーザマスタが存在しないFSクーポンユーザテーブル更新（9:FS連携失敗）
				List<DeliveryTargetDTO> notTargetList = fsCouponUsersList.stream()
						.filter(s -> s.getAwTrackingId() == null).collect(Collectors.toList());
				if (!notTargetList.isEmpty()) {
					for (DeliveryTargetDTO deliveryTarget : notTargetList) {
						// メッセージを出力（%sのレコードが取得できませんでした。（%s））
						String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB926.toString()),
								"アプリユーザマスタ",
								String.format(COUPON_ID_COMMON_INSIDE_ID_MSG,
										couponInfo.getCouponId(),
										deliveryTarget.getBranchId(),
										deliveryTarget.getFsBranchId(),
										deliveryTarget.getCommonInsideId()));

						logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB926.toString(), msg));
					}
					// FSクーポンユーザテーブル更新（9:FS連携失敗）
					boolean updateResult = updateFsCouponUsersList(notTargetList);
					if (!updateResult) {
						// DBエラーの場合

						// 処理を中断するため、FS枝番内全件エラーとしカウント(枝番単位)
						failCount = failCount + fsCouponUsersList.size();

						// 処理を中断するため、処理残件数をスキップとしカウント(枝番単位)
						skipCount = skipCount + remainingNumber;

						// 枝番内全件エラーとしカウント(FS枝番単位)
						failFsCount = fsCouponUsersList.size();

						// （FS枝番単位）件数をログに出力する。
						writeFsBranchIdUnitEndMsg(couponInfo.getCouponId(), couponInfo.getBranchId(),
								String.valueOf(i));

						// 異常終了
						stopAllFlg = true;
						stopPartFlg = true;
						returnValue = FAIL_RETURN_VALUE;
						continue;
					}
				}

				if (uuidList.isEmpty()) {
					// アプリユーザマスタの存在するデータが存在しない場合

					// FS枝番内全件エラーとしカウント(枝番単位)
					failCount = failCount + fsCouponUsersList.size();

					// FS枝番内全件エラーとしカウント(FS枝番単位)
					failFsCount = fsCouponUsersList.size();

					// （FS枝番単位）件数をログに出力する。
					writeFsBranchIdUnitEndMsg(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

					// 次のFS枝番へ
					continue;
				}

				// 異常件数格納(アプリユーザマスタ存在しない件数、異常終了時件数ログに利用)
				int fail = fsCouponUsersList.size() - uuidList.size();

				// FS API呼出結果
				FsApiCallResponse fsApiCallResponse = new FsApiCallResponse();

				if (!deliveryLimitFlg) {
					// 対象クーポンで配布上限枚数エラーが発生していない場合

					// (3.3.2.2)FS API連携
					fsApiCallResponse = callFsApi(couponInfo, fsCouponUsersList, uuidList);

					// 配布上限枚数エラー確認
					deliveryLimitFlg = checkDeliveryLimit(fsApiCallResponse.getResponse());
				}

				if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())
						|| deliveryLimitFlg) {
					// 成功、または配布上限枚数エラー場合

					// レスポンスBodyを取得
					CouponDeliveryApiOutputDTO outputDTO = getResponseBody(fsApiCallResponse.getResponse(),
							couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));
					if ((outputDTO == null
							|| (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())
									&& (outputDTO.getSent() == null
											|| outputDTO.getSent().getAndroidUuid() == null
											|| outputDTO.getSent().getAndroidUuid().length == 0)
									&& (outputDTO.getNotSent() == null
											|| outputDTO.getNotSent().getAndroidUuid() == null
											|| outputDTO.getNotSent().getAndroidUuid().length == 0))
							|| (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())
									&& (outputDTO.getSent() == null
											|| outputDTO.getSent().getIosUuid() == null
											|| outputDTO.getSent().getIosUuid().length == 0)
									&& (outputDTO.getNotSent() == null
											|| outputDTO.getNotSent().getIosUuid() == null
											|| outputDTO.getNotSent().getIosUuid().length == 0)))
							&& !deliveryLimitFlg) {
						// 成功でレスポンスBodyが取得できなかった、または結果が設定されていない場合

						// メッセージを出力(error)
						writeApiErrorLog(fsApiCallResponse.getResponse().statusCode(),
								String.format(API_RESPONSE_ERROR, couponInfo.getCouponId(), couponInfo.getBranchId(),
										String.valueOf(i)),
								null);

						// 処理を中断するため、FS枝番内全件エラーとしカウント(枝番単位)
						failCount = failCount + fsCouponUsersList.size();

						// 処理を中断するため、処理残件数をスキップとしカウント(枝番単位)
						skipCount = skipCount + remainingNumber;

						// FS枝番全件エラー件数カウント(FS枝番単位)
						failFsCount = fsCouponUsersList.size();

						// （FS枝番単位）件数をログに出力する。
						writeFsBranchIdUnitEndMsg(couponInfo.getCouponId(), couponInfo.getBranchId(),
								String.valueOf(i));

						// 異常終了
						stopAllFlg = true;
						stopPartFlg = true;
						returnValue = FAIL_RETURN_VALUE;
						continue;
					}

					// 配布上限枚数エラーの場合
					if (deliveryLimitFlg) {

						if (outputDTO != null) {
							// FS連携し、初めて発生した場合
							// メッセージを出力（%s、クーポンID = %s、枝番 = %s、FS枝番 = %s）
							String msg = String.format(DELIVERY_LIMIT_MSG, outputDTO.getError(),
									couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));
							logger.error(msg);

						} else {
							// 配布上限枚数エラーが既に発生している場合
							// メッセージを出力（配布上限枚数に達しているため、FS連携は行わず「9:FS連携失敗」に更新します。、クーポンID = %s、枝番 = %s、FS枝番 = %s）
							String msg = String.format(DELIVERY_LIMIT_AFTER_MSG,
									couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));
							logger.info(msg);

							outputDTO = new CouponDeliveryApiOutputDTO();
						}
					}

					// APIの結果処理
					FsCouponDeliveryResult fsCouponDeliveryResult = apiResultProcess(couponInfo, fsCouponUsersList,
							outputDTO);

					if (!FsCouponDeliveryResult.SUCCESS.equals(fsCouponDeliveryResult)) {
						// APIの結果処理が成功以外の場合（DBエラー）

						// 処理を中断するため、処理残件数をスキップとしカウント(枝番単位)
						skipCount = skipCount + remainingNumber;

						// 異常終了
						stopAllFlg = true;
						stopPartFlg = true;
						returnValue = FAIL_RETURN_VALUE;
					}

				} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
						|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
					// FANSHIPメンテナンス(503)の場合
					// リクエストが多すぎる(429)の場合

					// 共通処理でAPIエラーメッセージ(B18MB924)を出力しているため、詳細のみ出力
					writeErrorTargetLog(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

					// 処理を中断するため、連携対象のFS枝番全件スキップ件数カウント(枝番単位)
					skipCount = skipCount + uuidList.size();

					// 処理を中断するため、処理残件数をスキップとしカウント(枝番単位)
					skipCount = skipCount + remainingNumber;

					// 処理を中断するため、異常件数カウント(枝番単位)
					failCount = failCount + fail;

					// 連携対象のFS枝番全件スキップ件数カウント(FS枝番単位)
					skipFsCount = uuidList.size();

					// 異常件数カウント(FS枝番単位)
					failFsCount = fail;

					// 正常終了(他処理で「FAIL_RETURN_VALUE」となった時に上書きしないようここでは設定しない)
					stopPartFlg = true;

				} else {
					// 認証エラー(401)の場合
					// クライアントエラー(4xx/401,429以外)の場合
					// サーバエラー(5xx/503以外)の場合
					// タイムアウト(HttpTimeoutException) の場合
					// その他エラーの場合

					// 共通処理でAPIエラーメッセージ(B18MB924)を出力しているため、詳細のみ出力
					writeErrorTargetLog(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

					// 処理を中断するため、FS枝番全件エラー件数カウント(枝番単位)
					failCount = failCount + fsCouponUsersList.size();

					// 処理を中断するため、処理残件数をスキップとしカウント(枝番単位)
					skipCount = skipCount + remainingNumber;

					// FS枝番全件エラー件数カウント(FS枝番単位)
					failFsCount = fsCouponUsersList.size();

					// 異常終了
					stopPartFlg = true;
					returnValue = FAIL_RETURN_VALUE;

				}

				// （FS枝番単位）件数をログに出力する。
				writeFsBranchIdUnitEndMsg(couponInfo.getCouponId(), couponInfo.getBranchId(), String.valueOf(i));

			}

			// (3.3.3)配信予定・依頼リスト出力
			boolean b18B0009Result = callB18B0009(couponInfo.getCouponId(), couponInfo.getBranchId());
			if (!b18B0009Result) {
				// 異常終了の場合
				stopAllFlg = true;
				stopPartFlg = true;
				returnValue = FAIL_RETURN_VALUE;
			}

			// (3.3.4)FS API連携を行った件数をログに出力する。
			// メッセージを出力（%sが完了しました。(処理対象件数:[%d] , 処理成功件数:[%d], 処理失敗件数:[%d] , 処理スキップ件数:[%d], %s)）
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
					"FS連携処理(限定クーポン配布API)",
					readCount, successCount, failCount, skipCount,
					String.format(COUPON_ID_BRANCH_ID_MSG, couponInfo.getCouponId(), couponInfo.getBranchId()));

			logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg));

			if (!stopPartFlg) {
				// 正常の場合

				if (!checkFsCouponUsers(couponInfo.getCouponId(), couponInfo.getBranchId())) {
					// 「3:FS連携済み」、「9:FS連携失敗」以外が存在する場合

					// 異常終了
					stopAllFlg = true;
					returnValue = FAIL_RETURN_VALUE;

					// (3.3.6)配信バッチ情報管理テーブル更新(異常)
					updateDeliveryBatchInfoCtl(couponInfo.getCouponId(), couponInfo.getBranchId(),
							DeliveryBatchStatus.FAILURE.getValue());

				} else {
					// 「3:FS連携済み」「9:FS連携失敗」のみの場合

					// (3.3.5)MAクーポン配信結果
					boolean maCouponDeliveryResult = createMaCouponDeliveryResult(couponInfo, couponInfo.getBranchId());
					if (!maCouponDeliveryResult) {
						// 異常終了
						stopAllFlg = true;
						returnValue = FAIL_RETURN_VALUE;
					} else {
						// (3.3.6)配信バッチ情報管理テーブル更新(正常)
						updateDeliveryBatchInfoCtl(couponInfo.getCouponId(), couponInfo.getBranchId(),
								DeliveryBatchStatus.SUCCESS.getValue());
					}

				}
			}
			if (stopAllFlg) {
				// 全体処理中止の場合
				return returnValue;
			}

		}

		return returnValue;

	}

	/**
	 *  処理対象のクーポン情報をすべて取得
	 * 
	 * @return FSクーポン配信バッチ用 クーポン情報OUTPUTDTOリスト
	 * @throws SQLException 
	 */
	private List<CouponDeliveryCouponsOutputDTO> findCouponIdAll() throws SQLException {

		List<CouponDeliveryCouponsOutputDTO> couponInfoList = new ArrayList<>();

		// (3.1.1)ターゲットクーポン
		couponInfoList.addAll(findCoupon("selectTargetCoupon"));

		// (3.1.2)パスポートクーポン
		couponInfoList.addAll(findCoupon("selectPassportCoupon"));

		return couponInfoList;
	}

	/**
	 *  クーポン情報リストを取得
	 * 
	 * @param sqlPropertes 対象SQL
	 * 
	 * @return FSクーポン配信バッチ用 クーポン情報OUTPUTDTOリスト
	 * @throws SQLException 
	 */
	private List<CouponDeliveryCouponsOutputDTO> findCoupon(String sqlPropertes) throws SQLException {
		// クーポンを取得
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
		try {
			getDbConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, sqlPropertes);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();
			List<CouponDeliveryCouponsOutputDTO> couponInfoList = new ArrayList<>();
			// 取得したデータを格納
			while (resultSet.next()) {
				index = 0;
				CouponDeliveryCouponsOutputDTO couponInfo = new CouponDeliveryCouponsOutputDTO();

				// クーポンID
				couponInfo.setCouponId(ConvertUtility.objectToLong(resultSet.getString(++index)));
				// 枝番
				couponInfo.setBranchId(ConvertUtility.objectToString(resultSet.getString(++index)));
				// クーポン種別
				couponInfo.setCouponType(ConvertUtility.objectToString(resultSet.getString(++index)));
				// FSクーポンUUID
				couponInfo.setFsCouponUuid(ConvertUtility.objectToString(resultSet.getString(++index)));
				// クーポン有効期間_開始
				couponInfo.setLimitdateFrom(ConvertUtility.objectToTimestamp(resultSet.getTimestamp(++index)));
				// クーポン有効期間_終了
				couponInfo.setLimitdateTo(ConvertUtility.objectToTimestamp(resultSet.getTimestamp(++index)));
				// クーポン表示期間_開始
				couponInfo.setDisplaydateFrom(ConvertUtility.objectToTimestamp(resultSet.getTimestamp(++index)));
				// クーポン表示期間_終了
				couponInfo.setDisplaydateTo(ConvertUtility.objectToTimestamp(resultSet.getTimestamp(++index)));
				// 配信先登録方法
				couponInfo.setDeliverySaveMethod(ConvertUtility.objectToString(resultSet.getString(++index)));

				couponInfoList.add(couponInfo);
			}
			return couponInfoList;
		} finally {
			closeQuietly(resultSet, preparedStatement);
			closeConnection(BATCH_ID);
		}
	}

	/**
	 * ユーザ件数、FS枝番の最大値を取得
	 * 
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * 
	 * @return FSクーポンユーザテーブルリスト
	 * @throws SQLException 
	 */
	private List<int[]> findFsCouponUsersCount(long couponId, String branchId) throws SQLException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
		try {
			getDbConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, "selectFsCouponUserCount");
			preparedStatement.setLong(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			List<int[]> countList = new ArrayList<>();
			// 取得結果を設定
			while (resultSet.next()) {
				index = 0;
				Integer count = ConvertUtility.objectToInteger(resultSet.getString(++index));
				Integer maxFsBranchId = ConvertUtility.objectToInteger(resultSet.getString(++index));
				int[] row = new int[2];
				row[0] = count != null ? count : 0;
				row[1] = maxFsBranchId != null ? maxFsBranchId : 0;
				countList.add(row);
			}
			if (countList.isEmpty()) {
				countList.add(new int[] { 0, 0 });
			}
			return countList;
		} finally {
			closeQuietly(resultSet, preparedStatement);
			closeConnection(BATCH_ID);
		}

	}

	/**
	 * FSクーポンユーザテーブル取得
	 * 
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * @param fsBranchId FS枝番
	 * 
	 * @return FSクーポンユーザテーブルリスト
	 * @throws SQLException 
	 */
	private List<DeliveryTargetDTO> findFsCouponUsersList(long couponId, String branchId, String fsBranchId)
			throws SQLException {

		// 下記の条件で【FSクーポンユーザテーブル】を取得する。
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
		try {
			getDbConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, "selectFsCouponUserList");
			preparedStatement.setLong(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setString(++index, fsBranchId);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			List<DeliveryTargetDTO> deliveryTargetList = new ArrayList<>();
			// 取得結果を設定
			while (resultSet.next()) {
				index = 0;
				DeliveryTargetDTO deliveryTarget = new DeliveryTargetDTO();
				deliveryTarget.setFsCouponUserId(ConvertUtility.objectToLong(resultSet.getString(++index)));
				deliveryTarget.setCouponId(ConvertUtility.objectToLong(resultSet.getString(++index)));
				deliveryTarget.setBranchId(ConvertUtility.objectToString(resultSet.getString(++index)));
				deliveryTarget.setCommonInsideId(ConvertUtility.objectToString(resultSet.getString(++index)));
				deliveryTarget.setAcsUserCardCpPassportId(ConvertUtility.objectToString(resultSet.getString(++index)));
				deliveryTarget.setFsBranchId(ConvertUtility.objectToString(resultSet.getString(++index)));
				deliveryTarget.setFsDeliveryStatus(ConvertUtility.objectToString(resultSet.getString(++index)));
				deliveryTarget.setAwTrackingId(ConvertUtility.objectToString(resultSet.getString(++index)));

				deliveryTargetList.add(deliveryTarget);
			}
			return deliveryTargetList;
		} finally {
			closeQuietly(resultSet, preparedStatement);
			closeConnection(BATCH_ID);
		}

	}

	/**
	 * FSクーポンユーザテーブル更新（リスト分）
	 * 
	 * @param fsCouponUsersList FSクーポンユーザテーブルリスト
	 * 
	 * @return true:正常 false:異常
	 * @throws SQLException 
	 * 
	 */
	private boolean updateFsCouponUsersList(List<DeliveryTargetDTO> deliveryTargetList) throws SQLException {

		// FSクーポンユーザテーブル更新
		return executeUpdateFsCouponUsersList(deliveryTargetList);
	}

	/**
	 * FSクーポンユーザテーブル更新（リスト分）
	 * 
	 * @param fsCouponUsersList FSクーポンユーザテーブルリスト
	 * 
	 * @return true:正常 false:異常
	 * @throws SQLException 
	 * 
	 */
	private boolean executeUpdateFsCouponUsersList(List<DeliveryTargetDTO> deliveryTargetList) throws SQLException {
		PreparedStatement preparedStatement = null;
		try {
			transactionBeginConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, "updateFsCouponUser");
			for (DeliveryTargetDTO deliveryTarget : deliveryTargetList) {
				int index = 0;
				preparedStatement.setString(++index, deliveryTarget.getFsDeliveryStatus());
				preparedStatement.setLong(++index, deliveryTarget.getFsCouponUserId());
				preparedStatement.addBatch();
			}
			// FSクーポンユーザテーブルを更新
			preparedStatement.executeBatch();
			transactionCommitConnection(BATCH_ID);

			return true;

		} catch (Exception e) {

			transactionRollbackConnection(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		} finally {
			closeQuietly(null, preparedStatement);
		}

	}

	/**
	 * FS API連携を行う
	 * 
	 * @param couponInfo FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
	 * @param fsCouponUsersList FS枝番単位のFSクーポンユーザテーブルリスト
	 * @param uuidList UUIDリスト(イオンウォレットトラッキングIDまたはCPパスポートID)
	 * 
	 * @return FS APIレスポンス
	 */
	private FsApiCallResponse callFsApi(CouponDeliveryCouponsOutputDTO couponInfo,
			List<DeliveryTargetDTO> fsCouponUsersList, List<String> uuidList) {

		FsApiCallResponse fsApiCallResponse = new FsApiCallResponse();

		// クーポンID
		Long couponId = fsCouponUsersList.get(0).getCouponId();
		// 枝番
		String branchId = fsCouponUsersList.get(0).getBranchId();
		// FS枝番
		String fsBranchId = fsCouponUsersList.get(0).getFsBranchId();

		// APIURL
		String url = reverseProxyUrl + apiUrl.replace("${uuid}", couponInfo.getFsCouponUuid());

		// リクエストBody取得(Json)
		String requestBody = getRequestBody(couponInfo.getCouponType(), uuidList, couponId, branchId, fsBranchId);
		if (requestBody == null) {
			// リクエストBody生成失敗
			fsApiCallResponse.setFsApiCallResult(FsApiCallResult.OTHERS_ERROR);
			return fsApiCallResponse;
		}

		// 正常HTTPステータスコード取得(2xx)
		List<Integer> successHttpStatusList = new ArrayList<Integer>();
		successHttpStatusList.add(2);

		// FANSHIP APIを呼び出す
		return callFanshipApi(
				BATCH_ID,
				API_ERR_MSG,
				url,
				requestBody,
				HttpMethodType.POST,
				ContentType.APPLICATION_JSON,
				TokenHeaderType.X_POPINFO_MAPI_TOKEN,
				successHttpStatusList,
				retryCount,
				sleepTime,
				timeoutDuration,
				RetryKbn.SERVER_ERROR);

	}

	/**
	 * リクエストBody取得
	 * 
	 * @param couponType クーポン種別
	 * @param uuidList UUIDリスト
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * @param fsBranchId FS枝番
	 * 
	 * @return API連携結果区分
	 */
	private String getRequestBody(String couponType, List<String> uuidList, Long couponId, String branchId,
			String fsBranchId) {

		try {

			CouponDeliveryApiInputDTO input = new CouponDeliveryApiInputDTO();

			if (CouponType.TARGET.getValue().equals(couponType)) {
				// ターゲットクーポンの場合

				input.setAndroidUuid(new String[0]);
				input.setIosUuid(uuidList.toArray(new String[uuidList.size()]));

			} else if (CouponType.PASSPORT.getValue().equals(couponType)) {
				// パスポートイベントクーポンの場合

				input.setAndroidUuid(uuidList.toArray(new String[uuidList.size()]));
				input.setIosUuid(new String[0]);

			} else {
				throw new IllegalArgumentException();
			}

			// Json形式に変換
			return mapper.writeValueAsString(input);

		} catch (Exception e) {

			// メッセージを出力(error)
			writeApiErrorLog(null, String.format(API_REQUEST_BODY_MSG, couponId, branchId, fsBranchId), e);

			return null;
		}

	}

	/**
	 * APIの結果処理
	 * 
	 * @param couponInfo FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
	 * @param fsCouponUsersList FS枝番単位のFSクーポンユーザテーブルリスト
	 * @param outputDTO 限定クーポン配布API OUTPUTDTO
	 * 
	 * @return FSクーポン配信処理結果
	 * @throws SQLException 
	 */
	private FsCouponDeliveryResult apiResultProcess(CouponDeliveryCouponsOutputDTO couponInfo,
			List<DeliveryTargetDTO> fsCouponUsersList, CouponDeliveryApiOutputDTO outputDTO) throws SQLException {

		FsCouponDeliveryResult fsCouponDeliveryResult = FsCouponDeliveryResult.SUCCESS;

		// (3.3.2.3)FSクーポンユーザ更新
		String[] sendUuid;
		String[] notSendUuid;
		if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
			// ターゲットクーポンの場合
			if (outputDTO.getSent() == null || outputDTO.getSent().getIosUuid() == null) {
				sendUuid = new String[0];
			} else {
				sendUuid = outputDTO.getSent().getIosUuid();
			}
			if (outputDTO.getNotSent() == null || outputDTO.getNotSent().getIosUuid() == null) {
				notSendUuid = new String[0];
			} else {
				notSendUuid = outputDTO.getNotSent().getIosUuid();
			}

		} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
			// パスポートクーポンの場合
			if (outputDTO.getSent() == null || outputDTO.getSent().getAndroidUuid() == null) {
				sendUuid = new String[0];
			} else {
				sendUuid = outputDTO.getSent().getAndroidUuid();
			}
			if (outputDTO.getNotSent() == null || outputDTO.getNotSent().getAndroidUuid() == null) {
				notSendUuid = new String[0];
			} else {
				notSendUuid = outputDTO.getNotSent().getAndroidUuid();
			}

		} else {
			throw new IllegalArgumentException();
		}

		// FS枝番単位のFSクーポンユーザテーブル分繰り返す
		for (DeliveryTargetDTO fsCouponUsers : fsCouponUsersList) {

			// FS連携状況が「FS連携失敗」の場合（アプリユーザマスタ取得できなかった）
			if (FsDeliveryStatus.FAILURE.getValue().equals(fsCouponUsers.getFsDeliveryStatus())) {
				// (FS枝番内)エラー件数カウント
				failFsCount++;
				continue;
			}

			// 配信成功チェック
			DeliveryResultKBN deliveryResultKBN = checkSend(fsCouponUsers, sendUuid, notSendUuid,
					couponInfo.getCouponType());

			if (DeliveryResultKBN.SUCCESS.equals(deliveryResultKBN)) {
				// 配信成功場合

				// ステータス設定（3:FS連携済み）
				fsCouponUsers.setFsDeliveryStatus(FsDeliveryStatus.DELIVERED.getValue());

				// (FS枝番内)正常件数カウント
				successFsCount++;

			} else if (DeliveryResultKBN.ERROR.equals(deliveryResultKBN)
					|| DeliveryResultKBN.NOT_EXIST.equals(deliveryResultKBN)) {
				// 配信失敗または戻り値に存在しない場合（配布上限枚数エラーの場合、FS連携失敗にする）

				// ステータス設定（9:FS連携失敗）
				fsCouponUsers.setFsDeliveryStatus(FsDeliveryStatus.FAILURE.getValue());

				// (FS枝番内)エラー件数カウント
				failFsCount++;

			} else {
				throw new IllegalArgumentException();
			}

		}

		// 【FSクーポンユーザテーブル】を更新する。
		boolean updateResult = updateFsCouponUsersList(fsCouponUsersList);
		if (updateResult) {
			// 正常の場合
			failCount = failCount + failFsCount;
			successCount = successCount + successFsCount;
		} else {
			// 異常の場合、全件エラーカウント
			failCount = failCount + fsCouponUsersList.size();

			// FS枝番単位用
			successFsCount = 0;
			failFsCount = fsCouponUsersList.size();

			fsCouponDeliveryResult = FsCouponDeliveryResult.DB_ERROR;
		}

		return fsCouponDeliveryResult;

	}

	/**
	 * UUIDリスト内に指定のIDが存在するかチェック
	 * 
	 * @param fsCouponUsers FSクーポンユーザテーブル
	 * @param sendUuid 配信成功UUID配列(イオンウォレットトラッキングIDまたはCPパスポートID)
	 * @param notSendUuid 配信失敗UUID配列(イオンウォレットトラッキングIDまたはCPパスポートID)
	 * @param couponType クーポン種別
	 * 
	 * @return 配信結果区分
	 */
	private DeliveryResultKBN checkSend(DeliveryTargetDTO fsCouponUsers, 
			String[] sendUuid, String[] notSendUuid, String couponType) {

		Object id = "";

		if (CouponType.TARGET.getValue().equals(couponType)) {
			// ターゲットクーポンの場合

			// イオンウォレットトラッキングID
			id = fsCouponUsers.getAwTrackingId();

		} else if (CouponType.PASSPORT.getValue().equals(couponType)) {
			// パスポートイベントクーポンの場合

			// CPパスポートID
			id = fsCouponUsers.getAcsUserCardCpPassportId();

		} else {
			throw new IllegalArgumentException();
		}

		// 配信成功UUIDリスト存在確認
		if (Arrays.asList(sendUuid).contains(id)) {
			return DeliveryResultKBN.SUCCESS;
		}

		// 配信失敗UUIDリスト存在確認
		if (Arrays.asList(notSendUuid).contains(id)) {
			return DeliveryResultKBN.ERROR;
		}

		// 成功失敗どちらにも存在しない
		return DeliveryResultKBN.NOT_EXIST;

	}

	/**
	 *  B18B0009_配信予定・依頼リスト出力バッチ実行
	 *  
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * 
	 * @return true:正常 false:異常
	 * 
	 */
	private boolean callB18B0009(long couponId, String branchId) {

		try {
			// (3.3.3)配信予定・依頼リスト出力
			if (b18B0009 == null) {
				b18B0009 = new BatchDeliveryPlansCreator(this, couponsDAO, deliveryTargetDAO);
			}
			String b18B0009Result = b18B0009.process(ConvertUtility.longToString(couponId),
					branchId);

			// 異常終了の場合
			if (FAIL_RETURN_VALUE.equals(b18B0009Result)) {

				// メッセージを出力
				writeb18B0009ErrorLog(couponId, branchId, null);

				// バッチを異常終了させる
				return false;
			}

		} catch (Exception e) {

			// メッセージを出力
			writeb18B0009ErrorLog(couponId, branchId, e);

			// バッチを異常終了させる
			return false;

		}

		return true;

	}

	/**
	 *  MAクーポン配信結果
	 *  
	 * @param couponInfo FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
	 * @param branchId 枝番
	 * 
	 * @return true:正常 false:異常
	 * @throws SQLException 
	 * 
	 */
	private boolean createMaCouponDeliveryResult(CouponDeliveryCouponsOutputDTO couponInfo, String branchId)
			throws SQLException {

		// 配信先登録方法が「3：MA自動連携」ではない場合
		if (!DeliverySaveMethod.MA.getValue().equals(couponInfo.getDeliverySaveMethod())) {
			return true;
		}

		// (3.8.1)シーケンスNoの取得
		Optional<CouponSequenceCount> couponSequenceCount = null;
		try {
			transactionBegin(BATCH_ID);

			// クーポンシーケンス取得テーブル取得と更新（1：MAクーポン配信結果）
			couponSequenceCount = couponSequenceCountDAO
					.findById(SequenceType.MA_COUPON_DELIVERY_RESULTS.getValue(), BATCH_ID);

			transactionCommit(BATCH_ID);

		} catch (Exception e) {

			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(e);

			return false;
		}

		if (!couponSequenceCount.isPresent()) {

			// メッセージを出力（%sのレコードが取得できませんでした。（%s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB926.toString()),
					"クーポンシーケンス取得テーブル",
					SEQUENCE_MSG);

			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB926.toString(), msg));

			return false;
		}

		// (3.3.5a.3)MA用配信結果ファイルの作成
		// (3.3.5a.4)配信結果ファイルの送信
		return createFile(couponInfo, couponSequenceCount.get().getSequenceNo(), branchId);

	}

	/**
	 * FSクーポンユーザテーブルを取得し、 MAクーポン配信結果作成対象か判定
	 * 
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * 
	 * @return true:作成対象 false:作成対象外
	 * @throws SQLException 
	 */
	private boolean checkFsCouponUsers(long couponId, String branchId) throws SQLException {
		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
		try {
			getDbConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, "selectFsCouponUserNotCompletion");
			preparedStatement.setLong(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得結果を設定
			while (resultSet.next()) {
				index = 0;
				Integer countFsCouponUserNotCompletion = ConvertUtility.objectToInteger(resultSet.getString(++index));
				// (3.3.5a)(3.3.1)で取得した【FSクーポンユーザテーブル】の「クーポンID」、「枝番」に紐づく全レコードの「FS連携状況」が			
				//「3:FS連携済み」または「9:FS連携失敗」の場合
				if (countFsCouponUserNotCompletion == null || countFsCouponUserNotCompletion == 0) {
					return true;
				}
			}

			// メッセージを出力（FSクーポンユーザテーブルに「FS連携待ち」が存在します、クーポンID = %s、枝番 = %s）
			String msg = String.format(NOT_FINISH_MSG, couponId, branchId);
			logger.error(msg);

			return false;
		} finally {
			closeQuietly(resultSet, preparedStatement);
			closeConnection(BATCH_ID);
		}

	}

	/**
	 * MA用配信結果ファイルに必要な情報を取得
	 * 
	 * @param couponInfo FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
	 * @param branchId 枝番
	 * @param lowerLimit 下限
	 * @param upperLimit 上限
	 * 
	 * @return MAクーポン配信結果データ取得DTOリスト
	 * @throws SQLException 
	 */
	private List<List<String>> getMaData(CouponDeliveryCouponsOutputDTO couponInfo, String branchId,
			int lowerLimit, int upperLimit) throws SQLException {

		List<List<String>> dataRecodeList = new ArrayList<>();

		String sql;
		if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
			// ターゲットの場合（取得データ：共通内部ID、FS連携状況）
			sql = "selectFsCouponUserMaTarget";
		} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
			// パスポートの場合（取得データ：会員番号、家族CD、共通内部ID、配布結果）
			sql = "selectFsCouponUserMaPassport";
		} else {
			throw new IllegalArgumentException();
		}

		// SQLを実行する。
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int index = 0;
		try {
			getDbConnection(BATCH_ID);
			preparedStatement = prepareStatement(BATCH_ID, sql);
			preparedStatement.setLong(++index, couponInfo.getCouponId());
			preparedStatement.setString(++index, branchId);
			preparedStatement.setLong(++index, couponInfo.getCouponId());
			preparedStatement.setString(++index, branchId);
			preparedStatement.setInt(++index, lowerLimit);
			preparedStatement.setInt(++index, upperLimit);
			preparedStatement.setFetchSize(FETCH_SIZE);
			resultSet = preparedStatement.executeQuery();

			// 取得したデータを格納
			while (resultSet.next()) {
				index = 0;
				FsCouponDeliveryMaDataOutputDTO outputDTO = new FsCouponDeliveryMaDataOutputDTO();

				if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
					// ターゲットの場合（取得データ：共通内部ID、配布結果）
					outputDTO.setCommonInsideId(ConvertUtility.objectToString(resultSet.getString(++index)));
					outputDTO.setDeliveryResult(ConvertUtility.objectToString(resultSet.getString(++index)));

				} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
					// パスポートの場合（取得データ：会員番号、家族CD、共通内部ID、枚数フラグ、配布結果）
					outputDTO.setAcsUserCardId(ConvertUtility.objectToString(resultSet.getString(++index)));
					outputDTO.setAcsUserCardFamilyCd(ConvertUtility.objectToString(resultSet.getString(++index)));
					outputDTO.setCommonInsideId(ConvertUtility.objectToString(resultSet.getString(++index)));
					outputDTO.setCountFlag(ConvertUtility.objectToShort(resultSet.getString(++index)));
					outputDTO.setDeliveryResult(ConvertUtility.objectToString(resultSet.getString(++index)));

				} else {
					throw new IllegalArgumentException();
				}

				// レコード編集
				List<String> bodyFieldList = new ArrayList<>();

				bodyFieldList.add("G"); // レコード種別
				bodyFieldList.add(outputDTO.getCommonInsideId()); // 共通内部ID
				if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
					// ターゲットの場合
					bodyFieldList.add(" ".repeat(12)); // 会員番号
				} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
					// パスポートの場合
					bodyFieldList.add(outputDTO.getAcsUserCardId()); // 会員番号
				} else {
					throw new IllegalArgumentException();
				}
				bodyFieldList.add(outputDTO.getDeliveryResult()); // 配布結果
				if (CouponType.TARGET.getValue().equals(couponInfo.getCouponType())) {
					// ターゲットの場合
					bodyFieldList.add(" "); // 家族区分
					bodyFieldList.add(" ".repeat(2)); // 枚数フラグ
				} else if (CouponType.PASSPORT.getValue().equals(couponInfo.getCouponType())) {
					// パスポートの場合
					bodyFieldList.add(outputDTO.getAcsUserCardFamilyCd()); // 家族区分
					bodyFieldList.add(String.format("%02d", outputDTO.getCountFlag())); // 枚数フラグ
				} else {
					throw new IllegalArgumentException();
				}

				bodyFieldList.add(String.format("%70s", "")); // 余白

				dataRecodeList.add(bodyFieldList);

			}
		} finally {
			closeQuietly(resultSet, preparedStatement);
			closeConnection(BATCH_ID);
		}

		return dataRecodeList;

	}

	/**
	 *  MA用配信結果ファイルの作成
	 * 
	 * @param couponInfo FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
	 * @param sequenceNo シーケンスＮｏ
	 * @param branchId 枝番
	 * 
	 * @return true:成功 false:失敗
	 * @throws SQLException 
	 * 
	 */
	private boolean createFile(CouponDeliveryCouponsOutputDTO couponInfo, String sequenceNo, String branchId)
			throws SQLException {

		// 固定長ファイルを出力する
		boolean result = outputFixedLengthFile(couponInfo, sequenceNo, branchId);

		if (!result) {

			// メッセージを出力（%sでエラーが発生しました。（%s））
			String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB931.toString()),
					"MA用配信結果ファイルの作成",
					String.format(COUPON_ID_BRANCH_ID_MSG, couponInfo.getCouponId(),
							branchId));

			logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg));

			return false;
		}

		return true;

	}

	/**
	 * APIエラーログ出力
	 * 
	 * @param statusCode HTTPステータスコード
	 * @param msg メッセージ
	 * @param e Exception
	 * 
	 * @return true:リトライ false:リトライしない
	 */
	private void writeApiErrorLog(Integer statusCode, String msg, Exception e) {

		// メッセージを出力（%sのAPI連携に失敗しました。（HTTPレスポンスコード ＝「%s」,エラー内容 = 「%s」））
		String errorMsg = String.format(
				BusinessMessage.getMessages(BusinessMessageCode.B18MB924.toString()),
				API_ERR_MSG,
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
	 *  @param branchId 枝番
	 *  @param fsBranchId FS枝番
	 *  
	 */
	private void writeProcessTargetLog(Long couponId, String branchId, String fsBranchId) {

		// メッセージを出力（処理対象：クーポンID = %s、枝番 = %s、FS枝番 = %s）
		String msg = String.format(START_COUPON_MSG, couponId, branchId, fsBranchId);

		logger.info(msg);
	}

	/**
	 *  処理対象件数メッセージ
	 *  
	 *  @param count FS枝番内のユーザ件数
	 *  
	 */
	private void writeFsUsetCountLog(int count, String fsBranchId) {

		// メッセージを出力（FS枝番[%s]内のFSユーザ件数：%s）
		String msg = String.format(FS_USER_COUNT_MSG, fsBranchId, count);

		logger.info(msg);
	}

	/**
	 *  スキップ対象メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  @param branchId 枝番
	 *  @param fsBranchId FS枝番
	 *  
	 */
	private void writeSkipTargetLog(Long couponId, String branchId, String fsBranchId) {

		// メッセージを出力（処理中断スキップ対象：クーポンID = %s、枝番 = %s、FS枝番 = %s）
		String msg = String.format(SKIP_COUPON_MSG, couponId, branchId, fsBranchId);

		logger.info(msg);

	}

	/**
	 *  エラー対象メッセージ
	 *  
	 *  @param couponId クーポンID
	 *  @param branchId 枝番
	 *  @param fsBranchId FS枝番
	 *  
	 */
	private void writeErrorTargetLog(Long couponId, String branchId, String fsBranchId) {

		// メッセージを出力（エラー対象：クーポンID = %s、枝番 = %s、FS枝番 = %s）
		String msg = String.format(ERROR_TARGET_MSG, couponId, branchId, fsBranchId);

		logger.info(msg);
	}

	/**
	 *  配信予定・依頼リスト出力バッチエラーメッセージ
	 *
	 *  @param couponId クーポンID
	 *  @param branchId 枝番
	 *  @param e Exception
	 *  
	 */
	private void writeb18B0009ErrorLog(Long couponId, String branchId, Exception e) {

		// メッセージを出力（%sでエラーが発生しました。（%s））
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB931.toString()),
				BATCH_ERR_MSG,
				String.format(COUPON_ID_BRANCH_ID_MSG, couponId, branchId));

		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931.toString(), msg), e);

	}

	/**
	 *  FS枝番単位の件数をログに出力
	 *
	 *  @param couponId クーポンID
	 *  @param branchId 枝番
	 *  @param fsBranchId FS枝番
	 *  
	 */
	private void writeFsBranchIdUnitEndMsg(Long couponId, String branchId, String fsBranchId) {

		// （FS枝番単位）FS API連携を行った件数をログに出力する。
		// メッセージを出力（%sが完了しました。(処理対象件数:[%d] , 処理成功件数:[%d], 処理失敗件数:[%d] , 処理スキップ件数:[%d], %s)）
		String msg = String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
				"FS連携処理(限定クーポン配布API)[FS枝番単位]",
				readFsCount, successFsCount, failFsCount, skipFsCount,
				String.format(COUPON_ID_BRANCH_ID_FS_BRANCH_ID_MSG, couponId, branchId, fsBranchId));

		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg));

	}

	/**
	 * レスポンスボディ取得
	 * 
	 * @param response HTTPレスポンス
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * @param fsBranchId FS枝番
	 * 
	 * @return 限定クーポン配布API OUTPUT
	 * 
	 */
	private CouponDeliveryApiOutputDTO getResponseBody(HttpResponse<String> response, long couponId, String branchId,
			String fsBranchId) {

		try {

			if (response != null && response.body() != null && !response.body().isEmpty()) {
				// レスポンスBodyを取得
				return mapper.readValue(response.body(),
						CouponDeliveryApiOutputDTO.class);
			}

			return null;

		} catch (Exception e) {
			log.error(String.format(API_DESERIALIZE_ERROR, couponId, branchId, fsBranchId), e);

			// エラーの場合、nullを返す
			return null;

		}

	}

	/**
	 *  配布上限枚数エラー確認
	 *
	 *  @param response レスポンス
	 *  
	 *  @return true:配布上限枚数エラー false:配布上限枚数以外
	 */
	private boolean checkDeliveryLimit(HttpResponse<String> response) {

		if (response != null
				&& DELIVERY_LIMIT_ERROR_STATUS == response.statusCode()
				&& response.body().contains(deliveryLimitWord)) {
			// ステータスが「400」で「配布上限枚数」エラーの場合
			return true;
		}
		return false;
	}

	/**
	 * 配信バッチ情報管理テーブルのステータスを更新
	 * 
	 * @param couponId クーポンID
	 * @param branchId 枝番
	 * @param deliveryBatchStatus ステータス
	 * 
	 * @return true:正常 false:異常
	 * @throws SQLException
	 * 
	 */
	private boolean updateDeliveryBatchInfoCtl(long couponId, String branchId, String deliveryBatchStatus)
			throws SQLException {

		PreparedStatement preparedStatement = null;
		int index = 0;
		try {
			transactionBeginConnection(BATCH_ID);

			preparedStatement = prepareStatement(BATCH_ID, "updateDeliveryBatchInfoCtl");
			preparedStatement.setShort(++index, Short.parseShort(deliveryBatchStatus));
			preparedStatement.setString(++index, BATCH_ID);
			preparedStatement.setTimestamp(++index, DateUtils.now());
			preparedStatement.setLong(++index, couponId);
			preparedStatement.setString(++index, branchId);
			preparedStatement.executeUpdate();
			transactionCommitConnection(BATCH_ID);
			return true;
		} catch (Exception e) {
			transactionRollbackConnection(BATCH_ID);
			// DBエラー共通処理
			writeDbErrorLog(e);
			return false;
		} finally {
			closeQuietly(null, preparedStatement);
		}
	}

	/**
	 * 固定長ファイルを出力するメソッド。
	 * @param deliveryBatchInfoCtl 配信バッチ情報管理テーブル
	 * @param coupons クーポンテーブル
	 * @param sequenceNo シーケンスＮｏ
	 * @return 処理結果
	 * @throws SQLException 
	 * 
	 * */
	private Boolean outputFixedLengthFile(CouponDeliveryCouponsOutputDTO couponInfo, String sequenceNo, String branchId)
			throws SQLException {

		Boolean result = false;

		// ディレクトリの存在確認
		if (Boolean.FALSE.equals(batchFileHandler.existFile(fileDirectory))) {
			// ディレクトリ作成
			File file = new File(fileDirectory);
			file.mkdirs();
		}

		// データ抽出日時
		LocalDateTime nowDate = LocalDateTime.now();
		String dataExtractionDate = nowDate.format(dtfYyyymmddhhmmss);

		// ファイル名
		String createfileName = fileName.replace("{yyyyMMddHHmmssSSS}", nowDate.format(dtfYyyymmddhhmmsssss));

		String output = fileDirectory + createfileName;

		Charset enc = Charset.forName("Shift_JIS");
		int[] headerFixedLengthList = new int[] { 1, 4, 2, 2, 14, 1, 18, 1, 14, 14, 14, 14, 8 };
		int[] bodyFixedLengthList = new int[] { 1, 20, 12, 1, 1, 2, 70 };
		int[] footerFixedLengthList = new int[] { 1, 10, 96 };

		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output))) {

			// 空白の設定
			String space = " ";
			byte[] binSpace = space.getBytes(enc);

			// ヘッダー部出力
			List<String> headerFieldList = new ArrayList<>();
			headerFieldList.add("H"); // レコード種別
			headerFieldList.add(String.format("%4s", sequenceNo).replace(" ", "0")); // シーケンスＮｏ
			headerFieldList.add("CK"); // 送信元システムＩＤ
			headerFieldList.add("MA"); // 送信先システムＩＤ
			headerFieldList.add(dataExtractionDate); // データ抽出日時
			headerFieldList.add("0"); // エラーコード
			headerFieldList.add(String.format("%18d", couponInfo.getCouponId())); // クーポンID
			headerFieldList.add(couponInfo.getCouponType()); // クーポン種別
			headerFieldList.add(couponInfo.getDisplaydateFrom().toLocalDateTime().format(dtfYyyymmddhhmmss)); // クーポン表示期間開始
			headerFieldList.add(couponInfo.getDisplaydateTo().toLocalDateTime().format(dtfYyyymmddhhmmss)); // クーポン表示期間終了
			headerFieldList.add(couponInfo.getLimitdateFrom().toLocalDateTime().format(dtfYyyymmddhhmmss)); // クーポン有効期間開始
			headerFieldList.add(couponInfo.getLimitdateTo().toLocalDateTime().format(dtfYyyymmddhhmmss)); // クーポン有効期間終了
			headerFieldList.add(String.format("%08d", 108)); // レコード長

			// 出力
			writeFile(headerFieldList, bos, binSpace, headerFixedLengthList, enc);

			// データ部出力
			int dateCount = 0;
			int offset = 0;
			int limit = maDeliveryTargetCount;
			// 配信対象者取得下限、上限
			// (3.3a.1.1)下記条件で【配信対象者テーブル】を取得する。
			logger.info("getMaData start");
			List<List<String>> dataRecodeList = getMaData(couponInfo, branchId, offset, limit);
			logger.info("getMaData end");

			do {
				for (int i = 0; i < dataRecodeList.size(); i++) {
					// 出力
					writeFile(dataRecodeList.get(i), bos, binSpace, bodyFixedLengthList, enc);
				}

				// データ件数  
				dateCount = dateCount + dataRecodeList.size();

				// set new offset
				offset += dataRecodeList.size();

				logger.info("getMaData start");
				dataRecodeList = getMaData(couponInfo, branchId, offset, limit);
				logger.info("getMaData end");
			} while (!dataRecodeList.isEmpty());

			dataRecodeList = null;

			// フッター部出力
			List<String> footerFieldList = new ArrayList<>();
			footerFieldList.add("F");
			footerFieldList.add(String.format("%10s", dateCount + 2).replace(" ", "0"));
			footerFieldList.add(String.format("%96s", ""));

			// 出力
			writeFile(footerFieldList, bos, binSpace, footerFixedLengthList, enc);

			logger.info("固定長ファイルを出力しました。：" + output);
			result = true;

		} catch (IOException e) {
			logger.error("固定長ファイルの出力に失敗しました。：" + output);
			e.printStackTrace();
			result = false;
		}

		return result;

	}

	/**
	 * 固定長ファイルを出力するメソッド。
	 * @param output 出力先ファイル名
	 * @param dataList 出力データのリスト
	 * @return 処理結果
	 * @throws SQLException 
	 * @throws IOException 
	 * 
	 * */
	private void writeFile(List<String> fieldList, BufferedOutputStream bos, byte[] binSpace,
			int[] fixedLengthList, Charset enc) throws IOException {

		int spaceByteCount = 0;

		// 出力
		for (int i = 0; i < fieldList.size(); i++) {
			String field = fieldList.get(i);
			//指定された文字コードでバイト配列化
			byte[] binData = field.getBytes(enc);
			//バイト配列の長さ
			int fieldByteLength = binData.length;

			//指定された項目のサイズと、比較
			spaceByteCount = fixedLengthList[i] - fieldByteLength;

			//実際のバイト配列のほうが大きかったら
			if (spaceByteCount < 0) {
				//負の値を足すので、差分を減らす(実際には指定されたバイト数になる)
				fieldByteLength = fieldByteLength + spaceByteCount;
				spaceByteCount = 0;
			}
			// データの出力
			bos.write(binData, 0, fieldByteLength);

			// たりない場合、空白の出力
			for (int count = 0; count < spaceByteCount; count++) {
				bos.write(binSpace);
			}
		}
		// 改行の出力
		bos.write("\r".getBytes(enc));

	}
}
