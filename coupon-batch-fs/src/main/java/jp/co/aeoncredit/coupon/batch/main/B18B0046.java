package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.FsTestDeliveryUsersInfoDTO;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetInputDTOFilter;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetOutputDTOForFlatIsTrue;
import jp.co.aeoncredit.coupon.batch.dto.TestDeviceAddOrRemoveInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.TestDeviceAddOrRemoveOutputDTO;
import jp.co.aeoncredit.coupon.batch.exception.FsApiFailedException;
import jp.co.aeoncredit.coupon.batch.exception.FsMaintenanceException;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.OsType;
import jp.co.aeoncredit.coupon.constants.properties.MstAppUsersProps;
import jp.co.aeoncredit.coupon.constants.properties.MstTestDeliveryUsersProps;
import jp.co.aeoncredit.coupon.dao.custom.MstTestDeliveryUsersDAOCustomize;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * FSテスト端末登録・削除バッチ
 */
@Named("B18B0046")
@Dependent
public class B18B0046 extends BatchFSApiCalloutBase {

	/** バッチID */
	private static final String BATCH_ID = BatchInfo.B18B0046.getBatchId();

	/** バッチネーム */
	private static final String BATCH_NAME = BatchInfo.B18B0046.getBatchName();

	/** レスポンスのstatus_OK */
	private static final String RES_STATUS_OK = "OK";

	/** レスポンスのstatus_NG */
	private static final String RES_STATUS_NG = "NG";

	/** エラーコード_E60104 */
	private static final String ERROR_CODE_E60104 = "E60104";

	/** エラーコード_E60105 */
	private static final String ERROR_CODE_E60105 = "E60105";

	/** API名_登録者情報一覧取得 */
	private static final String API_NAME_REGISTER_INFO_LIST_GET = "登録者情報一覧取得";

	/** API名_テスト端末登録・解除 */
	private static final String API_NAME_TEST_DEVICE_ADD_REMOVE = "テスト端末登録・解除";

	/** メッセージフォーマット（処理対象：共通内部ID = %s） */
	private static final String MSG_FORM_START_TARGET = "処理対象：共通内部ID = %s";

	/** メッセージフォーマット（処理中断スキップ対象：共通内部ID = %s） */
	private static final String MSG_FORM_SKIP_TARGET = "処理中断スキップ対象：共通内部ID = %s";

	/** メッセージフォーマット（エラー対象：共通内部ID = %s） */
	private static final String MSG_FORM_ERROR_TARGET = "エラー対象：共通内部ID = %s";

	/** メッセージフォーマット（処理正常終了：共通内部ID = %s） */
	private static final String FINISH_MSG = "処理正常終了：共通内部ID = %s";

	/** メッセージフォーマット（共通内部ID ＝ %s） */
	private static final String MSG_FORM_COMMON_INSIDE_ID = "共通内部ID ＝ %s";

	/** メッセージ（FSテスト端末登録） */
	private static final String MSG_FS_TEST_DEVICE_ADD = "FSテスト端末登録";

	/** メッセージ（FSテスト端末削除） */
	private static final String MSG_FS_TEST_DEVICE_REMOVE = "FSテスト端末削除";

	/** メッセージ（取得不可） */
	private static final String MSG_UNACQUIRED = "取得不可";

	/** メッセージ（詳細なし） */
	private static final String MSG_NO_DETAILS = "詳細なし";

	/** メッセージ（アプリユーザマスタ） */
	private static final String MSG_MST_APP_USERS = "アプリユーザマスタ";

	/** メッセージ（popinfo ID） */
	private static final String MSG_POPINFO_ID = "popinfo ID";

	/** メッセージ（NULL） */
	private static final String MSG_NULL = "NULL";

	/** 処理対象件数(登録) */
	private int addReadCount = 0;

	/** 処理成功件数(登録) */
	private int addSuccessCount = 0;

	/** 処理失敗件数(登録) */
	private int addFailCount = 0;

	/** 処理スキップ件数(登録) */
	private int addSkipCount = 0;

	/** 処理対象件数(削除) */
	private int removeReadCount = 0;

	/** 処理成功件数(削除) */
	private int removeSuccessCount = 0;

	/** 処理失敗件数(削除) */
	private int removeFailCount = 0;

	/** 処理スキップ件数(削除) */
	private int removeSkipCount = 0;

	/** 処理継続フラグ */
	private boolean processContinueFlg;

	/** 登録者情報一覧取得APIのURL */
	private String registerInfoListGetUrl;

	/** テスト端末登録・解除API(登録)のURL */
	private String testDeviceAddUrl;

	/** テスト端末登録・解除API(削除)のURL */
	private String testDeviceRemoveUrl;

	/** API実行リトライ回数 */
	private int apiRetryCount;

	/** API実行リトライ時スリープ時間(ミリ秒) */
	private int apiSleepTime;

	/** タイムアウト期間(秒) */
	private int apiTimeoutDuration;

	/** 戻り値 */
	private String returnValue;
	
	/** ログ */
	protected Logger logger = getLogger();

	/** ObjectMapper */
	private ObjectMapper mapper = new ObjectMapper();

	/** メッセージ共通 */
	protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

	/** プロパティファイル共通 */
	protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

	/** ファイル共通 */
	protected BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

	/** テスト配信ユーザマスタ（MST_TEST_DELIVERY_USERS）Entityのカスタマイズ用DAOクラス */
	@Inject
	private MstTestDeliveryUsersDAOCustomize mstTestDeliveryUsersDAOCustomize;

	/** 処理区分(テスト配信ユーザ登録、テスト配信ユーザ削除) */
	private enum ProcessKbn {
		TEST_DELIVERY_USER_ADD, TEST_DELIVERY_USER_REMOVE;
	}

	/** API区分(登録者情報一覧取得、テスト端末登録、テスト端末解除) */
	private enum ApiKbn {
		GET, ADD, REMOVE;
	}

	/**
	 * バッチの起動メイン処理
	 * 
	 * @throws Exception スローされた例外
	 */
	@Override
	public String process() throws Exception {

		// プロパティファイルの読み込み
		readProperties();

		// 起動メッセージを出力する。
		logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

		// FSテスト端末登録・削除処理の開始
		String returnCode = executeFsTestDeviceAddAndRemove();

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

		// テスト端末登録・解除APIのURL(登録)
		this.testDeviceAddUrl = pro.getProperty("fs.test.device.add.remove.batch.add.api.url");
		// テスト端末登録・解除APIのURL(削除)
		this.testDeviceRemoveUrl = pro.getProperty("fs.test.device.add.remove.batch.remove.api.url");
		// 登録者情報一覧取得APIのURL
		this.registerInfoListGetUrl = pro.getProperty("fs.test.device.add.remove.batch.get.api.url");
		// FS API 失敗時のAPI実行リトライ回数
		this.apiRetryCount = Integer.parseInt(pro.getProperty("fs.test.device.add.remove.batch.retry.count"));
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		this.apiSleepTime = Integer.parseInt(pro.getProperty("fs.test.device.add.remove.batch.retry.sleep.time"));
		// FS API発行時のタイムアウト期間(秒)
		this.apiTimeoutDuration = Integer.parseInt(pro.getProperty("fs.test.device.add.remove.batch.timeout.duration"));
	}

	/**
	 * FSテスト端末登録・削除処理を実行する
	 */
	private String executeFsTestDeviceAddAndRemove() {

		// (2)【B18BC001_認証トークン取得】を実行する。
		try {
			processAuthToken();
		} catch (FsApiFailedException e) {
		    logger.error(e.getMessage(), e);
			// 【B18BC001_認証トークン取得】の戻り値が「失敗（enumで定義）」の場合
			return ProcessResult.FAILURE.getValue();
		} catch (FsMaintenanceException e) {
		    logger.error(e.getMessage(), e);
			// 【B18BC001_認証トークン取得】の戻り値が「メンテナンス（enumで定義）」(FANSHIPメンテナンス)の場合
			return ProcessResult.SUCCESS.getValue();
		}
		return testDeliveryUser();
	}

	/**
	 * テスト配信ユーザの登録/削除する。
	 * 
	 * @return テスト配信ユーザマスタ
	 */
	private String testDeliveryUser() {
		processContinueFlg = true; // 処理継続フラグ

		returnValue = ProcessResult.SUCCESS.getValue();
		
		// FSテスト端末登録処理
		testDeliveryUserAdd();

		// テスト端末登録の件数をログに出力する
		writeCountLog(ProcessKbn.TEST_DELIVERY_USER_ADD);

		if (processContinueFlg) {
			// FSテスト端末削除処理
			testDeliveryUserRemove();

			// テスト端末削除の件数をログに出力する
			writeCountLog(ProcessKbn.TEST_DELIVERY_USER_REMOVE);
		}

		return returnValue;
	}

	/**
	 * テスト配信ユーザ登録を行う。
	 * 
	 */
	private void testDeliveryUserAdd() {
		ProcessKbn processKbn = ProcessKbn.TEST_DELIVERY_USER_ADD; // 処理区分
		String commonInsideId = "";
		boolean updateSuccess = false; // true:FS連携状況を更新成功
		try {

			// テスト配信ユーザを取得する
			List<FsTestDeliveryUsersInfoDTO> testDeliveryUsersInfoList = findTargetOfFsApiTestDeviceAdd(
					DeleteFlag.NOT_DELETED, DeleteFlag.NOT_DELETED);

			if (CollectionUtils.isEmpty(testDeliveryUsersInfoList)) {
				// テスト配信ユーザが存在しない場合、ログを出力し(4)テスト配信ユーザ削除処理へ。

				writeNoRecordLog(processKbn);
				return;
			}
			// テスト配信ユーザが存在する場合、処理を継続する。
			for (FsTestDeliveryUsersInfoDTO testDeliveryUsersInfo : testDeliveryUsersInfoList) {
			    logger.info("testDeliveryUserAdd.testDeliveryUsersInfo.commonInsideId：" + testDeliveryUsersInfo.getCommonInsideId());
			    logger.info("testDeliveryUserAdd.testDeliveryUsersInfo.osType：" + testDeliveryUsersInfo.getOsType());
			    logger.info("testDeliveryUserAdd.testDeliveryUsersInfo.popInfoIdTest：" + testDeliveryUsersInfo.getPopInfoIdTest());
		        
				// 処理対象件数をカウントする
				addReadCount = addReadCount + 1;

				// 共通内部ID
				commonInsideId = testDeliveryUsersInfo.getCommonInsideId();

				// 処理対象のログを出力する
				writeApiStartLog(commonInsideId);

				// テスト配信ユーザマスタを更新する(2:FS連携中)
				updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.DELIVERING,
						null, commonInsideId, FsDeliveryStatus.WAITING);
				if (!updateSuccess) {
					countResult(processKbn, 3);
					returnValue = ProcessResult.FAILURE.getValue();
					processContinueFlg = false;
					return;
				}

				// アプリユーザマスタ.popinfo IDがNULLの場合
				if (testDeliveryUsersInfo.getPopInfoIdApp() == null) {

					// テスト配信ユーザを更新する(9:FS連携失敗)
					updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.FAILURE,
							"", commonInsideId, FsDeliveryStatus.DELIVERING);
					if (!updateSuccess) {
						countResult(processKbn, 3);
						returnValue = ProcessResult.FAILURE.getValue();
						processContinueFlg = false;
						return;
					}

					// エラーログを出力する
					writePopinfoIdNull(testDeliveryUsersInfo.getCommonInsideId());

					// 処理件数をカウントする
					countResult(processKbn, 3);
					continue; // 次のループへ
					
				} else {

					try {
						// 登録者情報一覧取得APIを呼び出す
						HttpResponse<String>  resFsApiRegisterInfoListGet = callFsApiRegisterInfoListGet(
								testDeliveryUsersInfo.getCommonInsideId(), testDeliveryUsersInfo.getPopInfoIdTest(),
								testDeliveryUsersInfo.getOsType());
					
						if (resFsApiRegisterInfoListGet == null) {
							// 登録者情報一覧取得処理に失敗した場合

							// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
							updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING, 
									"", commonInsideId, FsDeliveryStatus.DELIVERING);

							// 失敗件数をカウントする
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							processContinueFlg = false;
							return;
						}

						// レスポンスBodyからDTOに詰めかえる
						RegisterInfoListGetOutputDTOForFlatIsTrue FsApiRegisterInfoListGet = mapper
								.readValue(resFsApiRegisterInfoListGet.body(), RegisterInfoListGetOutputDTOForFlatIsTrue.class);

						if (FsApiRegisterInfoListGet.getTotalResults() > 0) {
							// 登録者情報一覧取得APIの戻り値が「total_results > 0」(登録済み)の場合

							// テスト端末削除を行う
							HttpResponse<String> resFsApiTestDeviceRemove = callFsApiTestDeviceAddOrRemove(
										testDeliveryUsersInfo, processKbn, ApiKbn.REMOVE);
								
							if (resFsApiTestDeviceRemove == null) {
								// テスト端末削除処理に失敗した場合

								// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
								updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
										"", commonInsideId, FsDeliveryStatus.DELIVERING);

								// 失敗件数をカウントする
								countResult(processKbn, 3);
								returnValue = ProcessResult.FAILURE.getValue();
								processContinueFlg = false;
								return;
							}
						}
						// popinfo ID更新を更新する
						boolean updatePopinfoIdSuccess = updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus(
								testDeliveryUsersInfo.getPopInfoIdApp(), testDeliveryUsersInfo.getCommonInsideId(),
								FsDeliveryStatus.DELIVERING, DeleteFlag.NOT_DELETED);
						if (!updatePopinfoIdSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							processContinueFlg = false;
							return;
						}

						// テスト端末登録を行う
						HttpResponse<String> resFsApiTestDeviceAdd = callFsApiTestDeviceAddOrRemove(testDeliveryUsersInfo,
									processKbn, ApiKbn.ADD);
						
						if (resFsApiTestDeviceAdd == null) {
							// テスト端末登録処理に失敗した場合

							// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
							updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING, 
									"", commonInsideId, FsDeliveryStatus.DELIVERING);

							// 失敗件数をカウントする
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							processContinueFlg = false;
							return;
						}

						// DTOに変換する
						TestDeviceAddOrRemoveOutputDTO outputDTO = mapper.readValue(resFsApiTestDeviceAdd.body(),
								TestDeviceAddOrRemoveOutputDTO.class);

						if (RES_STATUS_NG.equals(outputDTO.getStatus())
								&& outputDTO.getError() != null
								&& ERROR_CODE_E60105.equals(outputDTO.getError().getCode())) {
							// 以下の条件で【テスト配信ユーザマスタ】を更新、業務エラーフラグをONにし、エラーログを出力後、処理(3.2)へ（次の共通内部ID）

							// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
							updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
									BusinessMessage.getMessages(BusinessMessageCode.B18MB933.toString()), 
									commonInsideId, FsDeliveryStatus.DELIVERING);
							if (!updateSuccess) {
								countResult(processKbn, 3);
								returnValue = ProcessResult.FAILURE.getValue();
								processContinueFlg = false;
								return;
							}

							// 処理件数をカウントする
							countResult(processKbn, 3);

							// ワーニングのログを出力する(プッシュ通知を受信しない設定になっています。)
							writeE60105Log(resFsApiTestDeviceAdd.statusCode(), commonInsideId);	

						} else {
							// 【テスト配信ユーザマスタ】を更新する（3:FS連携済み）
							updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.DELIVERED,
									"", commonInsideId, FsDeliveryStatus.DELIVERING);
							if (!updateSuccess) {
								countResult(processKbn, 3);
								returnValue = ProcessResult.FAILURE.getValue();
								processContinueFlg = false;
								return;
							}

							// 処理件数をカウントする
							countResult(processKbn, 1);
							
							// 処理正常終了のログを出力する
							writeFinishTargetLog(commonInsideId);
							
						}
						
					} catch (FsMaintenanceException e) {
						// FSメンテナンス中の場合

						// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
						updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
								"", commonInsideId, FsDeliveryStatus.DELIVERING);
						if (!updateSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							processContinueFlg = false;
							return;
						}
						
						// スキップ件数をカウントする
						countResult(processKbn, 2);
						
					} catch (FsApiFailedException e) {
						
						// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
						updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
								"", commonInsideId, FsDeliveryStatus.DELIVERING);
						if (!updateSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							processContinueFlg = false;
							return;
						}
						
						// 失敗件数をカウントする
						countResult(processKbn, 3);
						returnValue = ProcessResult.FAILURE.getValue();
					}
					
				}
			}

		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// FS API連携失敗、またはその他の例外発生の場合

			// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
			updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
					"", commonInsideId, FsDeliveryStatus.DELIVERING);

			// スキップ件数と失敗件数をカウントする
			countResult(processKbn, 3);
			returnValue = ProcessResult.FAILURE.getValue();
			processContinueFlg = false;
		}
	}

	/**
	 * テスト配信ユーザ削除を行う。
	 * 
	 */
	private void testDeliveryUserRemove() {
		ProcessKbn processKbn = ProcessKbn.TEST_DELIVERY_USER_REMOVE; // 処理区分
		String commonInsideId = "";
		boolean updateSuccess = false; // true:FS連携状況を更新成功
		try {
			// テスト配信ユーザを取得する
			List<FsTestDeliveryUsersInfoDTO> testDeliveryUsersInfoList = findTargetOfFsApiTestDeviceAdd(
					DeleteFlag.DELETED, DeleteFlag.NOT_DELETED);

			if (CollectionUtils.isEmpty(testDeliveryUsersInfoList)) {
				// テスト配信ユーザが存在しない場合、ログを出力し処理終了。

				writeNoRecordLog(processKbn);
				return;
			}
			// テスト配信ユーザが存在する場合、処理を継続する。
			for (FsTestDeliveryUsersInfoDTO testDeliveryUsersInfo : testDeliveryUsersInfoList) {
			    logger.info("testDeliveryUserRemove.testDeliveryUsersInfo.commonInsideId：" + testDeliveryUsersInfo.getCommonInsideId());
                logger.info("testDeliveryUserRemove.testDeliveryUsersInfo.osType：" + testDeliveryUsersInfo.getOsType());
                logger.info("testDeliveryUserRemove.testDeliveryUsersInfo.popInfoIdTest：" + testDeliveryUsersInfo.getPopInfoIdTest());
              
				// 処理対象件数をカウントする
				removeReadCount = removeReadCount + 1;

				// 共通内部ID
				commonInsideId = testDeliveryUsersInfo.getCommonInsideId();

				// 処理対象のログを出力する
				writeApiStartLog(commonInsideId);
				
				// テスト配信ユーザマスタを更新する(2:FS連携中)
				updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.DELIVERING,
						"", testDeliveryUsersInfo.getCommonInsideId(), FsDeliveryStatus.WAITING);
				if (!updateSuccess) {
					countResult(processKbn, 3);
					returnValue = ProcessResult.FAILURE.getValue();
					return;
				}

				// アプリユーザマスタ.popinfo IDがNULLの場合
				if (testDeliveryUsersInfo.getPopInfoIdApp() == null) {

					// テスト配信ユーザを更新する(9:FS連携失敗)
					updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.FAILURE,
							"", commonInsideId, FsDeliveryStatus.DELIVERING);
					if (!updateSuccess) {
						countResult(processKbn, 3);
						returnValue = ProcessResult.FAILURE.getValue();
						return;
					}

					// エラーログを出力する
					writePopinfoIdNull(testDeliveryUsersInfo.getCommonInsideId());

					// 処理件数をカウントする
					countResult(processKbn, 3);

					continue; // 次のループへ
					
				} else {

					try {
						// テスト端末削除を行う
						HttpResponse<String> resFsApiTestDeviceRemove = callFsApiTestDeviceAddOrRemove(
								testDeliveryUsersInfo, processKbn, ApiKbn.REMOVE);
						
						if (resFsApiTestDeviceRemove == null) {
							// テスト端末削除処理に失敗した場合

							// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
							updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING, 
									"", commonInsideId, FsDeliveryStatus.DELIVERING);

							// 失敗件数をカウントする
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							return;
						}

						// 【テスト配信ユーザマスタ】を更新する（3:FS連携済み）
						updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.DELIVERED,
								"", commonInsideId, FsDeliveryStatus.DELIVERING);
						if (!updateSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							return;
						}

						// 処理件数をカウントする
						countResult(processKbn, 1);
						
						// 処理正常終了のログを出力する
						writeFinishTargetLog(commonInsideId);
						
					} catch (FsMaintenanceException e) {
						// FSメンテナンス中の場合

						// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
						updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
								"", commonInsideId, FsDeliveryStatus.DELIVERING);
						if (!updateSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							return;
						}
						
						// スキップ件数をカウントする
						countResult(processKbn, 2);

					} catch (FsApiFailedException e) {
						
						// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
						updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
								"", commonInsideId, FsDeliveryStatus.DELIVERING);
						if (!updateSuccess) {
							countResult(processKbn, 3);
							returnValue = ProcessResult.FAILURE.getValue();
							return;
						}
						
						// 失敗件数をカウントする
						countResult(processKbn, 3);
						returnValue = ProcessResult.FAILURE.getValue();
					}
					
				}
			}
			
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// FS API連携失敗、またはその他の例外発生の場合

			// 【テスト配信ユーザマスタ】を更新し、エラーログを出力する（1:FS連携待ち）
			updateSuccess = updateMstTestDeliveryUsersFsDeliveryStatus(processKbn, FsDeliveryStatus.WAITING,
					"", commonInsideId, FsDeliveryStatus.DELIVERING);

			// スキップ件数と失敗件数をカウントする
			countResult(processKbn, 3);
			returnValue = ProcessResult.FAILURE.getValue();
			return;	
		}
	}

	/**
	 * 登録者情報一覧取得APIを呼び出す
	 * 
	 * @param commonInsideId 共通内部ID
	 * @param popinfoId      popinfo ID
	 * @param osType         OS区分
	 * @return HttpResponse
	 * @throws FsMaintenanceException
	 * @throws FsApiFailedException
	 */
	private HttpResponse<String> callFsApiRegisterInfoListGet(String commonInsideId, String popinfoId, String osType)
			throws FsMaintenanceException, FsApiFailedException {
		ProcessKbn processKbn = ProcessKbn.TEST_DELIVERY_USER_ADD; // 処理区分
		ApiKbn apiKbn = ApiKbn.GET; // API区分
		try {

			// リクエストパラメータを設定する
			RegisterInfoListGetInputDTO inputDTO = new RegisterInfoListGetInputDTO();
			String[] name = { "popinfo_id" };
			inputDTO.setFields(name);
			inputDTO.setFlat(true);
			List<RegisterInfoListGetInputDTOFilter> inputFilterList = new ArrayList<RegisterInfoListGetInputDTOFilter>();
			RegisterInfoListGetInputDTOFilter inputFilter = new RegisterInfoListGetInputDTOFilter();
			inputFilter.setPopinfoId(popinfoId);
			inputFilter.setTestSend(true);
			inputFilterList.add(inputFilter);
			inputDTO.setFilter(inputFilterList);
			inputDTO.setPage(1);
			String param = mapper.writeValueAsString(inputDTO);

			// APIパスを設定する
			String path = String.format("%s%s/", registerInfoListGetUrl, OsType.convertToDefinition(osType));

			// FS APIを呼び出す
			HttpResponse<String> response = callApi(path, param, processKbn, apiKbn, commonInsideId);
			return response;
		} catch (FsMaintenanceException | FsApiFailedException e) {
			// FSメンテナンス中、またはFS API連携失敗の場合
			throw e;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// API連携失敗時のログを出力する
			writeApiFailLog(null, apiKbn, commonInsideId, e);
			return null;
		}
	}

	/**
	 * テスト端末登録・解除APIを呼び出す
	 * 
	 * @param testDeliveryUsersInfo FSテスト端末登録・解除用のテスト配信ユーザ情報DTO
	 * @param processKbn            処理区分
	 * @param apiKbn                API区分
	 * @return HttpResponse
	 * @throws FsMaintenanceException
	 * @throws FsApiFailedException
	 */
	private HttpResponse<String> callFsApiTestDeviceAddOrRemove(FsTestDeliveryUsersInfoDTO testDeliveryUsersInfo,
			ProcessKbn processKbn, ApiKbn apiKbn) throws FsMaintenanceException, FsApiFailedException {
		boolean isAdd = ApiKbn.ADD.equals(apiKbn); // API区分
		String commonInsideId = testDeliveryUsersInfo.getCommonInsideId(); // 共通内部ID
		try {
			// リクエストパラメータを設定する
			TestDeviceAddOrRemoveInputDTO inputDTO = new TestDeviceAddOrRemoveInputDTO();
			inputDTO.setDeviceType(OsType.convertToDefinition(testDeliveryUsersInfo.getOsType()));
			inputDTO.setPopinfoId(
					isAdd ? testDeliveryUsersInfo.getPopInfoIdApp() : testDeliveryUsersInfo.getPopInfoIdTest());
			String param = mapper.writeValueAsString(inputDTO);

			// APIパスを設定する
			String path = isAdd ? testDeviceAddUrl : testDeviceRemoveUrl;

			// FS APIを呼び出す
			HttpResponse<String> response = callApi(path, param, processKbn, apiKbn, commonInsideId);
			return response;
		} catch (FsMaintenanceException | FsApiFailedException e) {
			// FSメンテナンス中、またはFS API連携失敗のの場合
			throw e;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// API連携失敗時のログを出力する
			writeApiFailLog(null, apiKbn, commonInsideId, e);
			return null;
		}
	}

	/**
	 * FS API連携を行う
	 * 
	 * @param path           APIパス
	 * @param processKbn     処理区分
	 * @param apiKbn         API区分
	 * @param commonInsideId 共通内部ID
	 * @return FS APIのレスポンス
	 * @throws FsMaintenanceException
	 * @throws FsApiFailedException
	 */
	private HttpResponse<String> callApi(String path, String param, ProcessKbn processKbn, ApiKbn apiKbn,
			String commonInsideId) throws FsMaintenanceException, FsApiFailedException {
		FsApiCallResponse fsApiCallResponse = null;
		String apiName = ApiKbn.GET.equals(apiKbn) ? API_NAME_REGISTER_INFO_LIST_GET : API_NAME_TEST_DEVICE_ADD_REMOVE;
		
		try {
			// 正常HTTPステータスコードリストを設定する
			List<Integer> successHttpStatusList = new ArrayList<Integer>();
			successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

			// FANSHIP APIを呼び出す
			String url = this.reverseProxyUrl + path;
			fsApiCallResponse = callFanshipApi(BATCH_ID, apiName, url, param, HttpMethodType.POST,
					ContentType.APPLICATION_JSON_CHARSET, TokenHeaderType.AUTHORIZATION_POPINFOLOGIN,
					successHttpStatusList, apiRetryCount, apiSleepTime, apiTimeoutDuration,
					RetryKbn.SERVER_ERROR_TIMEOUT);

			if (fsApiCallResponse == null || fsApiCallResponse.getFsApiCallResult() == null
					|| fsApiCallResponse.getResponse() == null) {

				// API連携失敗時のログを出力する
				writeApiFailLog(null, apiKbn, commonInsideId, null);
				throw new FsApiFailedException();
			} else if (isApiSuccess(fsApiCallResponse.getResponse(), processKbn, apiKbn)) {
				// API成功条件を満たす場合
				return fsApiCallResponse.getResponse();
				
			} else if (isApiE60105(fsApiCallResponse.getResponse(), processKbn, apiKbn)) {
				// E60105(プッシュ通知を受信しない設定)の場合
				
				return fsApiCallResponse.getResponse();
				
			} else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
					|| FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
				// HTTPステータスコード：429、503の場合

				// 処理中断スキップ対象のログを出力する
				writeApiSkipLog(commonInsideId);
				throw new FsMaintenanceException(fsApiCallResponse.getResponse().statusCode());
			} else {
				// 上記以外の場合

				// エラー対象のログを出力する
				writeApiErrorTargetLog(commonInsideId);
				throw new FsApiFailedException(fsApiCallResponse.getResponse().statusCode());
			}
		} catch (FsMaintenanceException | FsApiFailedException e) {
			// FSメンテナンス中、またはFS API連携失敗のの場合
			throw e;
		} catch (Exception e) {
		    log.error(e.getMessage(), e);
			// その他例外発生時

			// API連携失敗時のログを出力する
			writeApiFailLog(null, apiKbn, commonInsideId, e);
			return null;
		}
	}

	/**
	 * API成功条件を満たすかどうか
	 * 
	 * @param response   HttpResponse<String>
	 * @param processKbn 処理区分
	 * @param apiKbn     API区分
	 * @return API成功条件を満たすかどうか(満たす：true、満たさない：false)
	 * @throws Exception
	 */
	private boolean isApiSuccess(HttpResponse<String> response, ProcessKbn processKbn, ApiKbn apiKbn) throws Exception {
		if (response.body() == null || response.body().isEmpty()) {
			return false;
		}
		
		int statusCode = response.statusCode();
		// ステータスを取得する
        String status = getStatus(response, apiKbn);

		if (ProcessKbn.TEST_DELIVERY_USER_REMOVE.equals(processKbn)) {
			// DTOに変換する
			TestDeviceAddOrRemoveOutputDTO outputDTO = mapper.readValue(response.body(),
					TestDeviceAddOrRemoveOutputDTO.class);

			// HTTPステータスコードが「200」かつレスポンスBody.statusが「OK」
			// または、status「NG」でcodeが「E60104」(指定されたpopinfo_id のユーザーは存在しません。)の場合
			return (HTTPStatus.HTTP_STATUS_SUCCESS.getValue() == statusCode
					&& RES_STATUS_OK.equals(outputDTO.getStatus()))
					|| (RES_STATUS_NG.equals(outputDTO.getStatus()) && outputDTO.getError() != null
							&& ERROR_CODE_E60104.equals(outputDTO.getError().getCode()));
		} else {
			// HTTPステータスコードが「200」かつレスポンスBody.statusが「OK」の場合
			return HTTPStatus.HTTP_STATUS_SUCCESS.getValue() == statusCode && RES_STATUS_OK.equals(status);
		}
	}
	
	/**
	 * E60105(プッシュ通知を受信しない設定)かどうか判定
	 * 
	 * @param response   HttpResponse<String>
	 * @param processKbn 処理区分
	 * @param apiKbn     API区分
	 * @return E60105：true、E60105以外：false
	 * @throws Exception
	 */
	private boolean isApiE60105(HttpResponse<String> response, ProcessKbn processKbn, ApiKbn apiKbn) throws Exception {
		if (response.body() == null || response.body().isEmpty() 
				|| ProcessKbn.TEST_DELIVERY_USER_REMOVE.equals(processKbn) 
				|| !ApiKbn.ADD.equals(apiKbn)) {
			return false;
		}
		
		int statusCode = response.statusCode();
		
		// DTOに変換する
		TestDeviceAddOrRemoveOutputDTO outputDTO = mapper.readValue(response.body(),
				TestDeviceAddOrRemoveOutputDTO.class);
        
		// HTTPステータスコードが「200」かつ、status「NG」でcodeが「E60105」(プッシュ通知を受信しない設定)の場合
		return (HTTPStatus.HTTP_STATUS_SUCCESS.getValue() == statusCode
				&& RES_STATUS_NG.equals(outputDTO.getStatus())
				&& outputDTO.getError() != null
				&& ERROR_CODE_E60105.equals(outputDTO.getError().getCode()));
		
	}

	/**
	 * 処理対象件数、処理成功件数、処理失敗件数、処理スキップ件数をカウントする
	 * 
	 * @param processKbn 処理区分
	 * @param mode       API処理結果区分(1:正常終了、2:スキップ、3:異常終了)
	 */
	private void countResult(ProcessKbn processKbn, int mode) {
		if (ProcessKbn.TEST_DELIVERY_USER_ADD.equals(processKbn)) {
			// テスト配信ユーザ登録処理の場合

			if (mode == 1) {
				// 正常終了の場合

				// 成功件数カウント
				addSuccessCount = addSuccessCount + 1;

			} else if (mode == 2) {
				// FSメンテナンスの場合

				// スキップ件数カウント
				addSkipCount = addSkipCount + 1;
			} else {
				// 異常終了の場合

				// エラー件数カウント
				addFailCount = addFailCount + 1;
			}

		} else {
			// テスト配信ユーザ削除処理の場合

			if (mode == 1) {
				// 正常終了の場合

				// 成功件数カウント
				removeSuccessCount = removeSuccessCount + 1;

			} else if (mode == 2) {
				// FSメンテナンスの場合

				// スキップ件数カウント
				removeSkipCount = removeSkipCount + 1;
			} else {
				// 異常終了の場合

				// エラー件数カウント
				removeFailCount = removeFailCount + 1;
			}
		}
	}

	/**
	 * テスト配信ユーザ情報を取得する。
	 * 
	 * @param deleteFlagTest テスト配信ユーザマスタ.削除フラグ
	 * @param deleteFlagApp  アプリユーザマスタ.削除フラグ
	 * @return テスト配信ユーザ情報リスト
	 */
	private List<FsTestDeliveryUsersInfoDTO> findTargetOfFsApiTestDeviceAdd(DeleteFlag deleteFlagTest,
			DeleteFlag deleteFlagApp) {
		List<FsTestDeliveryUsersInfoDTO> outputDtoList = new ArrayList<FsTestDeliveryUsersInfoDTO>();
		try {
			// FS連携状況の条件
			List<String> fsDeliveryStatusList = new ArrayList<String>();
			fsDeliveryStatusList.add(FsDeliveryStatus.WAITING.getValue());
			fsDeliveryStatusList.add(FsDeliveryStatus.DELIVERING.getValue());
			List<Map<String, Object>> resList = mstTestDeliveryUsersDAOCustomize
					.findTestDeliveryUsers(fsDeliveryStatusList, deleteFlagTest.getValue(), deleteFlagApp.getValue());
            if (resList == null) {
              return outputDtoList;
            }
			// DTOに設定する
			for (Map<String, Object> res : resList) {
				FsTestDeliveryUsersInfoDTO outputDto = new FsTestDeliveryUsersInfoDTO();
				outputDto.setCommonInsideId(
						ConvertUtility.objectToString(res.get(MstTestDeliveryUsersProps.COMMON_INSIDE_ID)));
				outputDto.setPopInfoIdTest(
						ConvertUtility.objectToString(res.get(MstTestDeliveryUsersProps.POP_INFO_ID)));
				outputDto.setPopInfoIdApp(ConvertUtility.objectToString(res.get(MstAppUsersProps.POP_INFO_ID + "App")));
				outputDto.setOsType(ConvertUtility.objectToString(res.get(MstAppUsersProps.OS_TYPE)));
				outputDtoList.add(outputDto);
			}
			return outputDtoList;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			return outputDtoList;
		}
	}

	/**
	 * テスト配信ユーザマスタのFS連携状況を更新する。
	 * 
	 * @param processKbn          処理区分
	 * @param fsDeliveryStatusUpd FS連携状況(更新)
	 * @param massage             メッセージ(更新)
	 * @param commonInsideId      共通内部ID(検索条件)
	 * @param fsDeliveryStatus    FS連携状況(検索条件)
	 * @return trueの場合処理成功
	 */
	private boolean updateMstTestDeliveryUsersFsDeliveryStatus(ProcessKbn processKbn,
			FsDeliveryStatus fsDeliveryStatusUpd, String massage, String commonInsideId, 
			FsDeliveryStatus fsDeliveryStatus) {
	  
		// テスト配信ユーザ登録の場合は未削除、テスト配信ユーザ削除の場合は削除済みを設定する
		DeleteFlag deleteFlag = ProcessKbn.TEST_DELIVERY_USER_ADD.equals(processKbn) ? DeleteFlag.NOT_DELETED
				: DeleteFlag.DELETED;
		try {
			// トランザクションを開始する
			transactionBegin(BATCH_ID);
			mstTestDeliveryUsersDAOCustomize.updateByCommonInsideIdAndFsDeliveryStatus(fsDeliveryStatusUpd.getValue(),
					BATCH_ID ,massage ,commonInsideId, fsDeliveryStatus.getValue(), deleteFlag.getValue());
			// トランザクションをコミットする
			transactionCommit(BATCH_ID);
			return true;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// トランザクションをロールバックする
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(commonInsideId, e);
			return false;
		}
	}

	/**
	 * テスト配信ユーザマスタのPOPINFO IDを更新する。
	 * 
	 * @param popInfoIdUpd     POPINFO ID(更新)
	 * @param commonInsideId   共通内部ID(検索条件)
	 * @param fsDeliveryStatus FS連携状況(検索条件)
	 * @param deleteFlag       削除フラグ(検索条件)
	 * @return trueの場合処理成功
	 */
	private boolean updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus(String popInfoIdUpdUpd, String commonInsideId,
			FsDeliveryStatus fsDeliveryStatus, DeleteFlag deleteFlag) {
		try {
			// トランザクションを開始する
			transactionBegin(BATCH_ID);
			mstTestDeliveryUsersDAOCustomize.updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus(popInfoIdUpdUpd,
					BATCH_ID, commonInsideId, fsDeliveryStatus.getValue(), deleteFlag.getValue());
			// トランザクションをコミットする
			transactionCommit(BATCH_ID);
			return true;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			// トランザクションをロールバックする
			transactionRollback(BATCH_ID);

			// DBエラー共通処理
			writeDbErrorLog(commonInsideId, e);
			return false;
		}
	}

	/**
	 * 処理対象レコードなしのログを出力する
	 * 
	 * @param processKbn 処理区分
	 */
	private void writeNoRecordLog(ProcessKbn processKbn) {
		String processMsg = null;
		if (ProcessKbn.TEST_DELIVERY_USER_ADD.equals(processKbn)) {
			processMsg = MSG_FS_TEST_DEVICE_ADD;
		} else {
			processMsg = MSG_FS_TEST_DEVICE_REMOVE;
		}

		// 処理対象レコードがありません。（処理：FSテスト端末登録, 詳細なし）
		// 処理対象レコードがありません。（処理：FSテスト端末削除, 詳細なし）
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, processMsg, MSG_NO_DETAILS));
	}

	/**
	 * popinfo IDがNULLのログを出力する
	 * 
	 * @param commonInsideId 共通内部ID
	 */
	private void writePopinfoIdNull(String commonInsideId) {
		// アプリユーザマスタのpopinfo IDがNULLのため処理できませんでした。（共通内部ID＝「xxx」）
		String errorInfo = String.format(MSG_FORM_COMMON_INSIDE_ID, commonInsideId);
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB928, MSG_MST_APP_USERS, MSG_POPINFO_ID, MSG_NULL,
				errorInfo));
	}

	/**
	 * DBエラー共通処理
	 * 
	 * @param commonInsideId 共通内部ID
	 * @param e              Exception
	 */
	private void writeDbErrorLog(String commonInsideId, Exception e) {
		// メッセージを出力（DBエラーが発生しました。%s）
		String msg = String.format(MSG_FORM_COMMON_INSIDE_ID, commonInsideId);
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), msg), e);
	}

	/**
	 * 処理対象のログを出力する
	 * 
	 * @param commonInsideId 共通内部ID
	 */
	private void writeApiStartLog(String commonInsideId) {
		// メッセージを出力（処理対象：共通内部ID = %s）
		String msg = String.format(MSG_FORM_START_TARGET, commonInsideId);
		logger.info(msg);
	}

	/**
	 * 処理中断スキップ対象のログを出力する
	 * 
	 * @param commonInsideId 共通内部ID
	 */
	private void writeApiSkipLog(String commonInsideId) {
		// メッセージを出力（処理中断スキップ対象：共通内部ID = %s）
		String msg = String.format(MSG_FORM_SKIP_TARGET, commonInsideId);
		logger.info(msg);
	}

	/**
	 * エラー対象のログを出力する
	 * 
	 * @param commonInsideId 共通内部ID
	 */
	private void writeApiErrorTargetLog(String commonInsideId) {
		// メッセージを出力（エラー対象：共通内部ID = %s）
		String msg = String.format(MSG_FORM_ERROR_TARGET, commonInsideId);
		logger.info(msg);
	}

	/**
	 *  処理正常終了メッセージ
	 *  
	 * @param commonInsideId 共通内部ID
	 *  
	 */
	private void writeFinishTargetLog(String commonInsideId) {
		// メッセージを出力（処理正常終了(%s)：クーポンID ＝ %s）
		String msg = String.format(FINISH_MSG, commonInsideId);
		logger.info(msg);
	}
	
	/**
	 * API連携失敗時のログを出力する
	 * 
	 * @param statusCode         ステータスコード
	 * @param apiKbn         API区分
	 * @param commonInsideId 共通内部ID
	 * @param e              Exception
	 */
	private void writeApiFailLog(Integer statusCode, ApiKbn apiKbn, String commonInsideId, Exception e) {
		String statusCodeStr = statusCode == null ? MSG_UNACQUIRED : ConvertUtility.integerToString(statusCode);
		String apiName = ApiKbn.GET.equals(apiKbn) ? API_NAME_REGISTER_INFO_LIST_GET : API_NAME_TEST_DEVICE_ADD_REMOVE;
		String errorInfo = String.format(MSG_FORM_COMMON_INSIDE_ID, commonInsideId);

		// 登録者情報一覧取得のAPI連携に失敗しました。（HTTPレスポンスコード ＝「xxx」,エラー内容 = 「xxx」）
		// テスト端末登録・解除のAPI連携に失敗しました。（HTTPレスポンスコード ＝「xxx」,エラー内容 = 「xxx」）
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB924, apiName, statusCodeStr, errorInfo), e);
	}
	
	/**
	 * プッシュ通知を受信しない設定時のログを出力する
	 * 
	 * @param statusCode     ステータスコード
	 * @param commonInsideId 共通内部ID
	 */
	private void writeE60105Log(Integer statusCode, String commonInsideId) {
		String statusCodeStr = statusCode == null ? MSG_UNACQUIRED : ConvertUtility.integerToString(statusCode);
		String apiName = API_NAME_TEST_DEVICE_ADD_REMOVE;
		String errorInfo = String.format(MSG_FORM_COMMON_INSIDE_ID, commonInsideId);

		// テスト端末登録・解除のAPI連携に失敗しました。（HTTPレスポンスコード ＝「xxx」,エラー内容 = 「xxx」）
		logger.warn(batchLogger.createMsg(BusinessMessageCode.B18MB924, apiName, statusCodeStr, errorInfo));
	}

	/**
	 * カウント結果ログ出力
	 * 
	 * @param processKbn 処理区分
	 */
	private void writeCountLog(ProcessKbn processKbn) {
		int readCount = 0;
		int successCount = 0;
		int failCount = 0;
		int skipCount = 0;
		String processName = null;

		if (ProcessKbn.TEST_DELIVERY_USER_ADD.equals(processKbn)) {
			// 登録処理の場合

			readCount = addReadCount;
			successCount = addSuccessCount;
			failCount = addFailCount;
			skipCount = addSkipCount;
			processName = MSG_FS_TEST_DEVICE_ADD;
		} else {
			// 削除の場合

			readCount = removeReadCount;
			successCount = removeSuccessCount;
			failCount = removeFailCount;
			skipCount = removeSkipCount;
			processName = MSG_FS_TEST_DEVICE_REMOVE;
		}
		// FSテスト端末登録が完了しました。(処理対象件数:xxx , 処理成功件数:xxx, 処理失敗件数:xxx , 処理スキップ件数:xxx, 詳細なし)
		// FSテスト端末削除が完了しました。(処理対象件数:xxx , 処理成功件数:xxx, 処理失敗件数:xxx , 処理スキップ件数:xxx, 詳細なし)
		logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, processName, readCount, successCount, failCount,
				skipCount, MSG_NO_DETAILS));
	}

	/**
	 * 認証トークンを取得する
	 * 
	 * @throws FsMaintenanceException FS APIメンテナンス
	 * @throws FsApiFailedException   認証トークン取得失敗
	 */
	private void processAuthToken() throws FsMaintenanceException, FsApiFailedException {

		AuthTokenResult result = getAuthToken(BATCH_ID);

		// 取得結果に応じて例外処理
		if (result == AuthTokenResult.MAINTENANCE) {
			throw new FsMaintenanceException();
		} else if (result == AuthTokenResult.FAILURE) {
			throw new FsApiFailedException();
		}
	}

	/**
	 * APIの結果処理からステータスを取得する
	 * 
	 * @param response FS APIのレスポンス
	 * @param apiKbn   API区分
	 * @return ステータス
	 * @throws Exception
	 */
	private String getStatus(HttpResponse<String> response, ApiKbn apiKbn) throws Exception {
		String status = null;
		if (ApiKbn.GET.equals(apiKbn)) {
			// 登録者情報一覧取得APIのOutputDTOに変換する
			RegisterInfoListGetOutputDTOForFlatIsTrue registerInfoListGetOutputForFlatIsTrueDTO = mapper.readValue(response.body(),
					RegisterInfoListGetOutputDTOForFlatIsTrue.class);
			if (registerInfoListGetOutputForFlatIsTrueDTO != null) {
				status = registerInfoListGetOutputForFlatIsTrueDTO.getStatus();
			}
		} else {
			// テスト端末登録・解除APIのOutputDTOに変換する
			TestDeviceAddOrRemoveOutputDTO testDeviceAddOrRemoveOutputDTO = mapper.readValue(response.body(),
					TestDeviceAddOrRemoveOutputDTO.class);
			if (testDeviceAddOrRemoveOutputDTO != null) {
				status = testDeviceAddOrRemoveOutputDTO.getStatus();
			}
		}
		return status;
	}
}
