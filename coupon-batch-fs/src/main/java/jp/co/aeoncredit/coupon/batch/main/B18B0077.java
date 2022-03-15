package jp.co.aeoncredit.coupon.batch.main;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsIdlinkProcessMode;
import jp.co.aeoncredit.coupon.batch.dto.FsIdlinkDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsIdlinkDeleteApiInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsIdlinkInsertApiInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.FsIdlinkSimulatedLoginApiOutputDTO;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.entity.CouponPassport;

/**
 * FSID-Link登録・更新・削除バッチ
 */
@Named("B18B0077")
@Dependent
public class B18B0077 extends BatchFSApiCalloutBase {

  /** バッチID */
  protected static final String BATCH_ID = BatchInfo.B18B0077.getBatchId();

  /** バッチネーム */
  protected static final String BATCH_NAME = BatchInfo.B18B0077.getBatchName();

  /** 正常終了_戻り値 */
  protected static final String SUCCESS_RETURN_VALUE = "0";

  /** 異常終了_戻り値 */
  protected static final String FAIL_RETURN_VALUE = "1";

  /** ログ */
  protected Logger logger = getLogger();

  /** メッセージ共通 */
  protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

  /** プロパティファイル共通 */
  protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

  /** ファイル共通 */
  protected BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

  /** レスポンスBody.status API成功時 */
  private static final String API_RESPONCE_STATUS_SUCCEESS = "OK";

  /** リトライ回数 */
  private int retryCount;

  /** リトライ時スリープ時間(ミリ秒) */
  private int sleepTime;

  /** タイムアウト時間 */
  private int timeoutDuration;

  /** イオンアプリのIDLink利⽤形態APIのURL（紐づけ） */
  private String insertApiUrl;

  /** イオンアプリのIDLink利⽤形態APIのURL（紐づけ解除） */
  private String deleteApiUrl;

  /** CUSTOMER_ID */
  private String fsCustomerId;
  
  /** CPパスポートIDリストの保持期限（月単位） */
  private String retentionPeriod;

  /** 擬似ログイン（ID Link）のURL */
  private String cpPassIdApiUrl;

  /** 擬似ログイン（ID Link）のSID */
  private String cpPassIdApiSid;

  /** プロパティファイル変数 アプリケーションID */
  private static final String TEMP_EXP_APP_ID = "{customer_id}";
  /** プロパティファイル変数 sid */
  private static final String TEMP_EXP_SID = "{sid}";
  /** プロパティファイル変数 dtype */
  private static final String TEMP_EXP_DTYPE = "{dtype}";
  /** プロパティファイル変数 uid */
  private static final String TEMP_EXP_UID = "{uid}";

  /** DTYPE */
  private static final String ACT_DTYPE = "a";

  /** SQL ID IDLINK */
  private static final String SQL_SELECT_IDLINK_MODE = "selectCpPassportListModeUpsert";
  /** SQL ID 紐づけ解除 */
  private static final String SQL_SELECT_DELETE_MODE = "selectCpPassportListModeDelete";
  /** SQL ID CPパスポートIDリストテーブル FS連携状況更新 */
  private static final String SQL_UPDATE_CP_PASSPORT_LIST = "updateCpPassportList";

  /** テーブル名 CPパスポートIDテーブル */
  private static final String TABLE_CP_PASSPORT_ID = "CPパスポートIDテーブル";
  /** テーブル名 CPパスポートIDリストテーブル */
  private static final String TABLE_CP_PASSPORT_ID_LIST = "CPパスポートIDリストテーブル";

  private static final int FETCH_SIZE = 1000;

  /** 起動モード */
  private FsIdlinkProcessMode mode;
  /** 処理名 */
  private String processName;
  /** API名 */
  private String apiName;
  /** API成功時のステータスコード */
  private List<Integer> apiStatusSuccess;

  /** 引数 */
  @Inject
  @BatchProperty
  private String param = null;

  /** JSON作成用のオブジェクトマッパー */
  private ObjectMapper mapper = new ObjectMapper();


  /**
   * バッチの起動メイン処理
   * 
   */
  @Override
  public String process() throws Exception {

    // 起動メッセージを出力する。
    logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

    // 処理結果
    String result = SUCCESS_RETURN_VALUE;

    // プロパティファイルの読み込み
    readProperties();

    // 引数チェック、モード設定
    boolean setModeResult = setMode();

    // 主処理
    if (!setModeResult) {
      // 引数が不正な場合
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB919));
      result = FAIL_RETURN_VALUE;

    } else if (mode == FsIdlinkProcessMode.REGIST_USER) {
      // FSID-Link登録・更新・削除バッチ主処理（2:register-userモード）
      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB003, mode.getModeName()));
      result = processRegistUserMode();

    } else {
      // FSID-Link登録・更新・削除バッチ主処理（1:ID-Linkモード、3:deleteモード）
      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB003, mode.getModeName()));
      result = processIdlinkMode();
    }

    // 処理終了メッセージを出力する。
    logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
        SUCCESS_RETURN_VALUE.equals(result)));

    return setExitStatus(result);
  }

  /**
   * プロパティ読み込み
   */
  private void readProperties() {
    Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);
    this.insertApiUrl = pro.getProperty("fs.id.link.registration.batch.insert.api.url");
    this.deleteApiUrl = pro.getProperty("fs.id.link.registration.batch.delete.api.url");
    this.retentionPeriod = pro.getProperty("fs.id.link.registration.batch.retention.period");
    this.cpPassIdApiUrl = pro.getProperty("fs.id.link.registration.batch.cp.pass.id.api.url");
    this.retryCount = ConvertUtility
        .stringToInteger(pro.getProperty("fs.id.link.registration.batch.retry.count"));
    this.sleepTime =
        ConvertUtility.stringToInteger(pro.getProperty("fs.id.link.registration.batch.sleep.time"));
    this.timeoutDuration = ConvertUtility
        .stringToInteger(pro.getProperty("fs.id.link.registration.batch.timeout.duration"));

    // 環境変数
    this.cpPassIdApiSid = System.getenv(Constants.ENV_FS_SID);
    this.fsCustomerId = System.getenv(Constants.ENV_FS_CUSTOMER_ID);
  }

  /**
   * FSID-Link登録・更新・削除バッチ主処理
   */
  private boolean setMode() {

    // (2)引数チェック
    if (Objects.equals(param, FsIdlinkProcessMode.IDLINK.getValue())) {
      // (2.1)引数の起動区分が「1:ID-Linkモード」の場合、処理(4)へ
      setInsertMode();
      return true;

    } else if (Objects.equals(param, FsIdlinkProcessMode.REGIST_USER.getValue())) {
      // (2.2)引数の起動区分が「2:register-userモード」の場合、処理(3)へ
      setRegistUserMode();
      return true;

    } else if (Objects.equals(param, FsIdlinkProcessMode.DELETE.getValue())) {
      // (2.3)引数の起動区分が「3:deleteモード」の場合、処理(5)へ
      setDeleteMode();
      return true;

    } else {
      // (2.4)引数の起動区分が上記以外の場合、メッセージをログに出力し処理終了する。
      return false;
    }
  }

  /**
   * 起動モード「1」の場合の変数をセットする
   */
  private void setInsertMode() {
    this.mode = FsIdlinkProcessMode.IDLINK;
    this.apiStatusSuccess = List.of(HTTPStatus.HTTP_STATUS_CREATED.getValue(),
        HTTPStatus.HTTP_STATUS_CONFLICT.getValue());
    this.processName = "IDLink紐付け";
    this.apiName = "IDLink API（紐付けを行う_リンク操作）";
  }

  /**
   * 起動モード「3」の場合の変数をセットする
   */
  private void setDeleteMode() {
    this.mode = FsIdlinkProcessMode.DELETE;
    this.apiStatusSuccess = List.of(HTTPStatus.HTTP_STATUS_DELETED.getValue());
    this.processName = "IDLink紐付け解除";
    this.apiName = "IDLink API（削除_リンク操作）";
  }

  /**
   * 起動モード「2」の場合の変数をセットする
   */
  private void setRegistUserMode() {
    this.mode = FsIdlinkProcessMode.REGIST_USER;
    this.apiStatusSuccess = List.of(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());
    this.processName = "擬似ログイン";
    this.apiName = "擬似ログイン";
  }

  /**
   * 引数の起動区分が 「1:ID-Linkモード」 「3:deleteモード」 の処理を行う
   * 
   * @return 実行結果
   * @throws SQLException
   */
  private String processIdlinkMode() throws SQLException {
    // 処理結果
    String result = SUCCESS_RETURN_VALUE;

    // ログ出力用の件数カウント
    int errorCount = 0;
    int successCount = 0;
    int skipCount = 0;
    // Check open connection
    boolean hasConnection = false;

	// (4.0)配信バッチ情報管理テーブル更新(2:処理中)
	boolean updateResult = updateStatus(TreatedFlag.PROCESSING);
	if (!updateResult) {
		return FAIL_RETURN_VALUE;
	}
    
    // CPパスポートIDリスト取得
    log.debug("getCpPassportList start");
    List<FsIdlinkDTO> cpPassportList = getCpPassportList();
    log.debug("getCpPassportList end");
    // 取得結果が0件の場合
    if (cpPassportList.isEmpty()) {
      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, processName,
          TABLE_CP_PASSPORT_ID_LIST));

      // (4.4)配信バッチ情報管理テーブル更新(3:処理済み)
      updateResult = updateStatus(TreatedFlag.TREATED);
      if (!updateResult) {
    	  return FAIL_RETURN_VALUE;
      }

      return SUCCESS_RETURN_VALUE;
    }

    try {

      // Create DTO
      // DTOに値をセット
      FsIdlinkInsertApiInputDTO fsIdlinkInsertApiInputDTO = new FsIdlinkInsertApiInputDTO();
      FsIdlinkDeleteApiInputDTO fsIdlinkDeleteApiInputDTO = new FsIdlinkDeleteApiInputDTO();
      for (FsIdlinkDTO cpPassport : cpPassportList) {
        logger.info("processIdlinkMode.awTrackingId: " + cpPassport.getAwTrackingId());
        log.debug("callFsIdlinkApi start");
        // (4.3.1)FS API連携
        FsApiCallResult apiResult = callFsIdlinkApi(cpPassport, fsIdlinkInsertApiInputDTO, fsIdlinkDeleteApiInputDTO);
        log.debug("callFsIdlinkApi end");
        // 結果に応じたログ出力用件数をカウント
        if (apiResult == FsApiCallResult.SUCCESS) {
          log.debug("updateCpPassportList start");
          // (4.3.2)CPパスポートIDリスト更新
          transactionBeginConnection(BATCH_ID);
          hasConnection = true;
          updateCpPassportList(cpPassport);
          transactionCommitConnection(BATCH_ID);
          hasConnection = false;
          successCount++;
          log.debug("updateCpPassportList end");
        } else if (apiResult == FsApiCallResult.FANSHIP_MAINTENANCE
            || apiResult == FsApiCallResult.TOO_MANY_REQUEST) {
          // 正常終了で処理終了
          skipCount++;
          break;
        } else {
          // 異常終了で処理終了
          errorCount++;
          result = FAIL_RETURN_VALUE;
          break;
        }

      }
    } catch (Exception e) {
      // DBエラー発生時
      errorCount++;
      result = FAIL_RETURN_VALUE;
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
      if (hasConnection) {
        transactionRollbackConnection(BATCH_ID);
      }
    } finally {
      closeConnection(BATCH_ID);
    }

    // (4.3.3)FS IDLink 紐づけ連携の処理対象件数、処理成功件数、処理失敗件数をログに出力する。
    if (!cpPassportList.isEmpty()) {

      int skip = cpPassportList.size() - (successCount + errorCount + skipCount);
      if (skip != 0) {
        skipCount = skipCount + skip;
      }

      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, processName,
          cpPassportList.size(), successCount, errorCount, skipCount, ""));
    }
    
    // 処理成功のみの場合(処理スキップ件数と処理失敗件数が0件)
    if (skipCount == 0 && errorCount == 0) {
        // (4.4)配信バッチ情報管理テーブル更新(3:処理済み)
        updateResult = updateStatus(TreatedFlag.TREATED);
        if (!updateResult) {
            return FAIL_RETURN_VALUE;
    	}
    }

    return result;
  }

  /**
   * 引数の起動区分が「2:register-userモード」の場合の主処理 擬似ログイン（ID Link）
   * 
   * @throws SQLException
   */
  private String processRegistUserMode() throws SQLException {

    // 処理結果
    String result = SUCCESS_RETURN_VALUE;

    // ログ出力用件数カウント
    int successCount = 0;
    int errorCount = 0;
    int skipCount = 0;
    // Check open connection
    boolean hasConnection = false;

	// (3.0)配信バッチ情報管理テーブル更新(2:処理中)
	boolean updateResult = updateStatus(TreatedFlag.PROCESSING);
	if (!updateResult) {
		return FAIL_RETURN_VALUE;
	}

    logger.debug("getCpPassport start");
    // (3.1)CPパスポートID取得
    List<CouponPassport> couponPassports = getCpPassport();
    logger.debug("getCpPassport end");
    // 取得結果が0件の場合
    if (couponPassports.isEmpty()) {
      logger.info(
          batchLogger.createMsg(BusinessMessageCode.B18MB006, processName, TABLE_CP_PASSPORT_ID));

      // (3.3)配信バッチ情報管理テーブル更新(3:処理済み)
      updateResult = updateStatus(TreatedFlag.TREATED);
      if (!updateResult) {
    	  return FAIL_RETURN_VALUE;
      }

      return SUCCESS_RETURN_VALUE;
    }

    try {
      for (CouponPassport couponPassport : couponPassports) {
        logger.info("processRegistUserMode.acsUserCardCpPassportId: "
            + couponPassport.getAcsUserCardCpPassportId());
        // (3.2)擬似ログイン（ID Link）
        logger.debug("callSimulatedLoginApi start");
        FsApiCallResult apiResult = callSimulatedLoginApi(couponPassport);
        logger.debug("callSimulatedLoginApi end");

        // (3.2.1b)APIの戻り値のstatusが「OK」以外の場合（タイムアウト含む）
        if (apiResult == FsApiCallResult.SUCCESS) {
          // FS連携状況更新
          logger.debug("updateCpPassport start");
          transactionBeginConnection(BATCH_ID);
          hasConnection = true;
          updateCpPassport(couponPassport);
          transactionCommitConnection(BATCH_ID);
          hasConnection = false;
          successCount++;
          logger.debug("updateCpPassport end");
        } else if (apiResult == FsApiCallResult.FANSHIP_MAINTENANCE
            || apiResult == FsApiCallResult.TOO_MANY_REQUEST) {
          // 正常終了
          skipCount++;
          break;
        } else {
          // 異常終了
          errorCount++;
          result = FAIL_RETURN_VALUE;
          break;
        }
      }

    } catch (Exception e) {
      // DBエラー発生時
      errorCount++;
      result = FAIL_RETURN_VALUE;
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
      if (hasConnection) {
        transactionRollbackConnection(BATCH_ID);
      }
    } finally {
      closeConnection(BATCH_ID);
    }

    // (3.2.2)オンプレAPI連携の処理対象件数、処理成功件数、処理失敗件数をログに出力する。
    if (!couponPassports.isEmpty()) {

      int skip = couponPassports.size() - (successCount + errorCount + skipCount);
      if (skip != 0) {
        skipCount = skipCount + skip;
      }

      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, processName,
          couponPassports.size(), successCount, errorCount, skipCount, ""));
    }

    // 処理成功のみの場合(処理スキップ件数と処理失敗件数が0件)
    if (skipCount == 0 && errorCount == 0) {
        // (3.3)配信バッチ情報管理テーブル更新(3:処理済み)
        updateResult = updateStatus(TreatedFlag.TREATED);
        if (!updateResult) {
            return FAIL_RETURN_VALUE;
    	}
    }

    return result;
  }

  /**
   * FS API連携結果に応じてCPパスポートIDリストテーブルを更新する
   * 
   * @param cpPassport
   * @throws SQLException
   */
  private void updateCpPassportList(FsIdlinkDTO cpPassport) throws SQLException {
    if (mode == FsIdlinkProcessMode.IDLINK) {
      updateCpPassportListUpsert(cpPassport);
    } else if (mode == FsIdlinkProcessMode.DELETE) {
      updateCpPassportListDelete(cpPassport);
    }

  }

  /**
   * FS API連携結果に応じてCPパスポートIDリストテーブルを更新する 起動モード：IDLINKの場合
   * 
   * @throws SQLException
   */
  private void updateCpPassportListUpsert(FsIdlinkDTO cpPassport) throws SQLException {
    PreparedStatement preparedStatement = null;
    int index = 0;
    try {
      preparedStatement = prepareStatement(BATCH_ID, SQL_UPDATE_CP_PASSPORT_LIST);
      preparedStatement.setString(++index, FsDeliveryStatus.DELIVERED.getValue());
      preparedStatement.setString(++index, cpPassport.getAwTrackingId());
      preparedStatement.setString(++index, cpPassport.getCpPassportId());
      // 更新
      preparedStatement.executeUpdate();
    } finally {
      closeQuietly(null, preparedStatement);
    }
  }

  /**
   * FS API連携結果に応じてCPパスポートIDリストテーブルを更新する 起動モード：deleteの場合
   * 
   * @throws SQLException
   */
  private void updateCpPassportListDelete(FsIdlinkDTO cpPassport) throws SQLException {
    PreparedStatement preparedStatement = null;
    int index = 0;
    try {
      preparedStatement = prepareStatement(BATCH_ID, SQL_UPDATE_CP_PASSPORT_LIST);
      preparedStatement.setString(++index, FsDeliveryStatus.UNDELIVERED.getValue());
      preparedStatement.setString(++index, cpPassport.getAwTrackingId());
      preparedStatement.setString(++index, cpPassport.getCpPassportId());
      // 更新
      preparedStatement.executeUpdate();
    } finally {
      closeQuietly(null, preparedStatement);
    }
  }

  /**
   * FS 統合API を呼び出す（FS-IDLINK紐づけ・紐づけ解除）
   * 
   * @param cpPassport
   * @return 実行結果
   * @throws Exception
   */
  private FsApiCallResult callFsIdlinkApi(FsIdlinkDTO cpPassport,  FsIdlinkInsertApiInputDTO fsIdlinkInsertApiInputDTO, FsIdlinkDeleteApiInputDTO fsIdlinkDeleteApiInputDTO) {

    // URLを取得
    String apiUrl = getIdlinkApiUrl();
    // パラメータ取得
    String requestBody = getRequestBody(cpPassport, fsIdlinkInsertApiInputDTO, fsIdlinkDeleteApiInputDTO);
    // ログ出力詳細
    String msgDetail = "イオンウォレットトラッキングID＝" + cpPassport.getAwTrackingId() + ", CPパスポートID="
        + cpPassport.getCpPassportId();

    // FS API 実行
    FsApiCallResponse response = callFanshipApi(BATCH_ID, apiName, apiUrl, requestBody,
        HttpMethodType.POST_NOT_AUTH, ContentType.APPLICATION_JSON, TokenHeaderType.NONE,
        apiStatusSuccess, retryCount, sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);

    // ログ出力
    if (response.getFsApiCallResult() != FsApiCallResult.SUCCESS) {
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931, processName, msgDetail));
    }

    return response.getFsApiCallResult();

  }

  /**
   * 疑似ログインAPI を呼び出す
   * 
   * @param cpPassport
   * @return 実行結果
   */
  private FsApiCallResult callSimulatedLoginApi(CouponPassport cpPassport) {

    // URLを取得
    String apiUrl = getSimulatedLoginApiUrl(cpPassport);
    // パラメータなし
    String requestBody = "";
    // ログ出力詳細
    String msgDetail = "CPパスポートID=" + cpPassport.getAcsUserCardCpPassportId();

    // FS API 実行
    FsApiCallResponse response = callFanshipApi(BATCH_ID, apiName, apiUrl, requestBody,
        HttpMethodType.POST_PSEUDO_LOGIN, ContentType.APPLICATION_JSON, TokenHeaderType.NONE,
        apiStatusSuccess, retryCount, sleepTime, timeoutDuration, RetryKbn.SERVER_ERROR);

    // 結果判定
    FsApiCallResult apiResult = judgeSimulatedLoginApiResult(response);

    // ログ出力
    if (apiResult != FsApiCallResult.SUCCESS) {
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931, processName, msgDetail));
    }

    return apiResult;
  }

  /**
   * 共通処理で実行した処理結果からさらに結果を判定する <br>
   * ステータスコードが「200」（<code>FsApiCallResult</code>が<code>SUCCESS</code>）かつ、レスポンスBody.statusが「OK」の場合のみ成功とする
   * <br>
   * 
   * @param response 共通処理で実行したAPIの実行結果
   * @return FsApiCallResult レスポンスBodyを含めて判定したAPI実行結果
   * @throws IllegalArgumentException 引数のレスポンスがnullの場合
   */
  private FsApiCallResult judgeSimulatedLoginApiResult(FsApiCallResponse response) {

    if (response == null) {
      throw new IllegalArgumentException();
    } else {

      FsApiCallResult responseResult = response.getFsApiCallResult();

      if (responseResult == FsApiCallResult.SUCCESS) {
        // レスポンスBodyをDTOにセット
        FsIdlinkSimulatedLoginApiOutputDTO dto =
            setSimulatedLoginResponseToDto(response.getResponse());
        String status = dto.getStatus();

        if (Objects.equals(status, API_RESPONCE_STATUS_SUCCEESS)) {
          return FsApiCallResult.SUCCESS;
        } else {
          return FsApiCallResult.OTHERS_ERROR;
        }

      } else {
        return responseResult;
      }

    }
  }


  /**
   * 疑似ログインAPIのレスポンスをDTOにセットする
   * 
   * @param response 疑似ログインAPI実行結果
   * @return APIレスポンスBodyを格納したDTO
   */
  private FsIdlinkSimulatedLoginApiOutputDTO setSimulatedLoginResponseToDto(
      HttpResponse<String> response) {
    try {
      return mapper.readValue(response.body(), FsIdlinkSimulatedLoginApiOutputDTO.class);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new AssertionError();
    }
  }

  /**
   * 統合API（ID-Link紐づけ・解除）のURLを取得する
   * 
   * @param cpPassport CPパスポートリストテーブルのレコード
   * @return
   */
  private String getIdlinkApiUrl() {
    if (mode == FsIdlinkProcessMode.IDLINK) {
      return reverseProxyUrl + insertApiUrl.replace(TEMP_EXP_APP_ID, fsCustomerId);
    } else if (mode == FsIdlinkProcessMode.DELETE) {
      return reverseProxyUrl + deleteApiUrl.replace(TEMP_EXP_APP_ID, fsCustomerId);
    } else {
      throw new AssertionError();
    }
  }

  /**
   * リバプロAPI（疑似ログイン）のURLを取得する
   * 
   * @param couponPassport
   * @return
   */
  private String getSimulatedLoginApiUrl(CouponPassport couponPassport) {
    return reverseProxyUrl
        + cpPassIdApiUrl.replace(TEMP_EXP_SID, cpPassIdApiSid).replace(TEMP_EXP_DTYPE, ACT_DTYPE)
            .replace(TEMP_EXP_UID, couponPassport.getAcsUserCardCpPassportId());
  }

  /**
   * リクエストボディ（JSON形式）を取得する
   * @param cpPassport
   * @param fsIdlinkInsertApiInputDTO
   * @param fsIdlinkDeleteApiInputDTO
   * @return
   * @throws JsonProcessingException
   */
  private String getRequestBody(FsIdlinkDTO cpPassport, FsIdlinkInsertApiInputDTO fsIdlinkInsertApiInputDTO, FsIdlinkDeleteApiInputDTO fsIdlinkDeleteApiInputDTO) {
    try {
      if (mode == FsIdlinkProcessMode.IDLINK) {
        return getRequestBodyIdlink(cpPassport, fsIdlinkInsertApiInputDTO);
      } else if (mode == FsIdlinkProcessMode.DELETE) {
        return getRequestBodyDelete(cpPassport, fsIdlinkDeleteApiInputDTO);
      } else {
        throw new AssertionError();
      }
    } catch (JsonProcessingException e) {
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB931, "APIリクエスト作成", ""));
      logger.error(e.getMessage(), e);
      throw new AssertionError();
    }
  }

  /**
   * 起動モード：deleteの場合のリクエストBodyを作成する
   * 
   * @param cpPassport
   * @param fsIdlinkDeleteApiInputDTO
   * @return
   * @throws JsonProcessingException
   */
  private String getRequestBodyDelete(FsIdlinkDTO cpPassport, FsIdlinkDeleteApiInputDTO fsIdlinkDeleteApiInputDTO) throws JsonProcessingException {

    fsIdlinkDeleteApiInputDTO.setAwTrackingId(cpPassport.getAwTrackingId());
    fsIdlinkDeleteApiInputDTO.setCPPassId(cpPassport.getCpPassportId());

    return mapper.writeValueAsString(fsIdlinkDeleteApiInputDTO);

  }

  /**
   * 起動モード：IDLinkの場合のリクエストBodyを作成する
   * @param cpPassport
   * @param fsIdlinkInsertApiInputDTO
   * @return
   * @throws JsonProcessingException
   */
  private String getRequestBodyIdlink(FsIdlinkDTO cpPassport, FsIdlinkInsertApiInputDTO fsIdlinkInsertApiInputDTO) throws JsonProcessingException {

    fsIdlinkInsertApiInputDTO.setAwTrackingId(cpPassport.getAwTrackingId());
    fsIdlinkInsertApiInputDTO.setCPPassId(cpPassport.getCpPassportId());

    // json形式に変換
    return mapper.writeValueAsString(fsIdlinkInsertApiInputDTO);
  }

  /**
   * 起動モードに対応したCPパスポートIDリストを取得する
   * 
   * @return
   * @throws SQLException
   */
  @SuppressWarnings("resource")
  private List<FsIdlinkDTO> getCpPassportList() throws SQLException {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    int index = 0;
    try {
      getDbConnection(BATCH_ID);
      if (mode == FsIdlinkProcessMode.IDLINK) {
        preparedStatement = prepareStatement(BATCH_ID, SQL_SELECT_IDLINK_MODE);
        preparedStatement.setString(++index, FsDeliveryStatus.WAITING.getValue());
      } else if (mode == FsIdlinkProcessMode.DELETE) {
        preparedStatement = prepareStatement(BATCH_ID, SQL_SELECT_DELETE_MODE);
        // 現在日時 - 下記のプロパティの値(月単位)
        Timestamp updateDate =
            DateUtils.addMonth(DateUtils.now(), -ConvertUtility.stringToInteger(retentionPeriod));
        logger.debug(updateDate + "以前のデータを取得");
        preparedStatement.setString(++index, FsDeliveryStatus.DELIVERED.getValue());
        preparedStatement.setTimestamp(++index, updateDate);
      }

      // DTOにセット
      List<FsIdlinkDTO> fsIdlinkDTOList = new ArrayList<>();
      if (preparedStatement != null) {
        preparedStatement.setFetchSize(FETCH_SIZE);
        resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
          index = 0;
          FsIdlinkDTO dto = new FsIdlinkDTO();
          dto.setAwTrackingId(resultSet.getString(++index));
          dto.setCpPassportId(resultSet.getString(++index));
          fsIdlinkDTOList.add(dto);
        }
      }
      return fsIdlinkDTOList;
    } finally {
      closeQuietly(resultSet, preparedStatement);
      closeConnection(BATCH_ID);
    }
  }

  /**
   * 疑似ログイン処理完了後、処理を行ったCPパスポートIDのFS連携状況を更新する
   * 
   * @param couponsPassport
   * @param apiResult
   * @throws SQLException
   */
  private void updateCpPassport(CouponPassport couponsPassport) throws SQLException {
    PreparedStatement preparedStatement = null;
    int index = 0;
    try {
      preparedStatement = prepareStatement(BATCH_ID, "updateFsDeliveryStatusCpPassport");
      preparedStatement.setString(++index, FsDeliveryStatus.DELIVERED.getValue());
      preparedStatement.setString(++index, couponsPassport.getAcsUserCardCpPassportId());
      // 更新
      preparedStatement.executeUpdate();
    } finally {
      closeQuietly(null, preparedStatement);
    }
  }

  /**
   * 2:register-userモードで クーポンユーザパスポートを取得する
   * 
   * @return
   * @throws SQLException
   */
  @SuppressWarnings("resource")
  private List<CouponPassport> getCpPassport() throws SQLException {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    int index = 0;
    try {
      getDbConnection(BATCH_ID);
      preparedStatement = prepareStatement(BATCH_ID, "selectCpPassport");
      // 【CPパスポートIDテーブル】.「FS連携状況」 = 1：FS連携待ち
      preparedStatement.setString(++index, FsDeliveryStatus.WAITING.getValue());
      preparedStatement.setFetchSize(FETCH_SIZE);
      resultSet = preparedStatement.executeQuery();
      List<CouponPassport> couponPassportList = new ArrayList<>();
      while (resultSet.next()) {
        index = 0;
        CouponPassport couponPassport = new CouponPassport();
        couponPassport.setAcsUserCardCpPassportId(resultSet.getString(++index));
        couponPassport.setFsDeliveryStatus(resultSet.getString(++index));
        couponPassport.setCreateDate(resultSet.getTimestamp(++index));
        couponPassportList.add(couponPassport);
      }
      return couponPassportList;
    } finally {
      closeQuietly(resultSet, preparedStatement);
      closeConnection(BATCH_ID);
    }
  }

  /**
   * 配信バッチ情報管理テーブルのID-Linkステータスまたはregister-userステータスを更新する
   * 
   * @param treatedFlag 処理済みフラグ
   */
  private boolean updateStatus(TreatedFlag treatedFlag)  {
    // deleteモードの場合
	if (mode == FsIdlinkProcessMode.DELETE) {
		return true;  
	}
	  
	try {
		transactionBeginConnection(BATCH_ID);
		updateDeliveryBatchInfoCtl(treatedFlag);
		transactionCommitConnection(BATCH_ID);

		return true;
		
	} catch (Exception e) {
		// DBエラー発生時
		logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901, e));
		transactionRollbackConnection(BATCH_ID);
		return false;
	} finally {
		closeConnection(BATCH_ID);
	}
  }
  
  /**
   * 配信バッチ情報管理テーブルを更新する
   * 
   * @param treatedFlag 処理済みフラグ
   * @throws SQLException 
   */
  private void updateDeliveryBatchInfoCtl(TreatedFlag treatedFlag) throws SQLException {
    PreparedStatement preparedStatement = null;
    int index = 0;
    try {
    	transactionBeginConnection(BATCH_ID);
        if (mode == FsIdlinkProcessMode.IDLINK) {
            // ID-Linkモードの場合
        	if (TreatedFlag.PROCESSING.equals(treatedFlag)) {
        		// 処理開始時の場合
        	    preparedStatement = prepareStatement(BATCH_ID, "updateIdLinkStatusProcessing");
        		
        	} else {
        		// 処理終了時の場合
        	    preparedStatement = prepareStatement(BATCH_ID, "updateIdLinkStatusTreated");
        		
        	}

    	} else {
    		// register-userモードの場合
        	if (TreatedFlag.PROCESSING.equals(treatedFlag)) {
        		// 処理開始時の場合
        	    preparedStatement = prepareStatement(BATCH_ID, "updateRegisterUserStatusProcessing");
        		
        	} else {
        		// 処理終了時の場合
        	    preparedStatement = prepareStatement(BATCH_ID, "updateRegisterUserStatusTreated");
        		
        	}

    	}
        // ID-Linkステータスまたはregister-userステータス
	    preparedStatement.setString(++index, treatedFlag.getValue());
	    // 更新者ID
	    preparedStatement.setString(++index, BATCH_ID);
	    // 更新日
		preparedStatement.setTimestamp(++index, DateUtils.now());

		// 更新
		preparedStatement.executeUpdate();

    } finally {
    	closeQuietly(null, preparedStatement);
    }
  }
  
}
