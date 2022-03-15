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
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.GetFanshipInAppMsgOutputDTO;
import jp.co.aeoncredit.coupon.batch.exception.FsApiFailedException;
import jp.co.aeoncredit.coupon.batch.exception.FsMaintenanceException;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.properties.AppMessagesProps;
import jp.co.aeoncredit.coupon.dao.custom.AppMessagesDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessages;

/**
 * FSアプリ内Msg配信停止バッチ
 * 
 * @author to-okawa
 */
@Named("B18B0075")
@Dependent
public class B18B0075 extends BatchFSApiCalloutBase {

  /** バッチID */
  private static final String BATCH_ID = BatchInfo.B18B0075.getBatchId();

  /** バッチネーム */
  private static final String BATCH_NAME = BatchInfo.B18B0075.getBatchName();

  /** API名（アプリ内Msg配信情報取得API） */
  private static final String API_NAME_APP_MESSAGE_DELIVERY = "アプリ内Msg配信情報取得API";

  /** API名（アプリ内Msg配信停止API） */
  private static final String API_NAME_APP_MESSAGE_CANCEL = "アプリ内Msg配信停止API";

  /** パスパラメータ（{delivery_id}） */
  private static final String PATH_PARAM_DELIVERY_ID = "{delivery_id}";

  /** パスパラメータ（${condition_id}） */
  private static final String PATH_PARAM_CONDITION_ID = "${condition_id}";

  /** メッセージフォーマット（UUID = %s） */
  private static final String MSG_FORM_UUID = "UUID = %s";

  /** メッセージフォーマット（処理対象：UUID = %s） */
  private static final String MSG_FORM_START_TARGET = "処理対象：UUID = %s";

  /** メッセージフォーマット（処理中断スキップ対象：UUID = %s） */
  private static final String MSG_FORM_SKIP_TARGET = "処理中断スキップ対象：UUID = %s";

  /** メッセージフォーマット（エラー対象：UUID = %s） */
  private static final String MSG_FORM_ERROR_TARGET = "エラー対象：UUID = %s";

  /** メッセージ（アプリ内Msg配信停止） */
  private static final String MSG_FS_APP_MESSAGE_CANCEL = "アプリ内Msg配信停止";

  /** メッセージ（アプリ内Msg配信停止処理） */
  private static final String MSG_FS_APP_MESSAGE_CANCEL_PROC = "アプリ内Msg配信停止処理";

  /** メッセージ（取得不可） */
  private static final String MSG_UNACQUIRED = "取得不可";

  /** メッセージ（詳細なし） */
  private static final String MSG_NO_DETAILS = "詳細なし";

  /** メッセージ（レスポンスのデシリアライズ失敗） */
  private static final String MSG_API_DESERIALIZE_ERROR = "レスポンスのデシリアライズ失敗";

  /** 処理対象件数 */
  private int readCount = 0;

  /** 処理成功件数 */
  private int successCount = 0;

  /** 処理失敗件数 */
  private int failCount = 0;

  /** 処理スキップ件数 */
  private int skipCount = 0;

  /** アプリ内Msg配信情報取得APIのURL */
  private String appMessageDeliveryApiUrl;

  /** アプリ内Msg配信停止APIのURL */
  private String appMessageCancelApiUrl;

  /** API実行リトライ回数 */
  private int apiRetryCount;

  /** API実行リトライ時スリープ時間(ミリ秒) */
  private int apiSleepTime;

  /** タイムアウト期間(秒) */
  private int apiTimeoutDuration;

  /** ログ */
  protected Logger logger = getLogger();

  /** メッセージ共通 */
  protected BatchLogger batchLogger = new BatchLogger(BATCH_ID);

  /** プロパティファイル共通 */
  protected BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

  /** ファイル共通 */
  protected BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

  /** アプリ内メッセージテーブル（APP_MESSAGES）Entityのカスタマイズ用DAOクラス */
  @Inject
  private AppMessagesDAOCustomize appMessagesDAO;

  /** ObjectMapper */
  private ObjectMapper mapper = new ObjectMapper();

  /** API区分(アプリ内Msg配信情報取得、アプリ内Msg配信停止) */
  private enum ApiKbn {
    DELIVERY, CANCEL;
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
    String returnCode = executeFsAppMessageCancel();

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

    // アプリ内Msg配信情報取得APIのURL
    this.appMessageDeliveryApiUrl = pro.getProperty("fs.cancel.batch.app.message.delivery.api.url");
    // アプリ内Msg配信停止APIのURL
    this.appMessageCancelApiUrl = pro.getProperty("fs.cancel.batch.app.message.cancel.api.url");
    // FS API 失敗時のAPI実行リトライ回数
    this.apiRetryCount = Integer.parseInt(pro.getProperty("fs.cancel.batch.retry.count"));
    // FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
    this.apiSleepTime = Integer.parseInt(pro.getProperty("fs.cancel.batch.retry.sleep.time"));
    // FS API発行時のタイムアウト期間(秒)
    this.apiTimeoutDuration = Integer.parseInt(pro.getProperty("fs.cancel.batch.timeout.duration"));
  }

  /**
   * FSアプリ内Msg配信停止処理を実行する
   */
  private String executeFsAppMessageCancel() {

    // 【B18BC001_認証トークン取得】を実行する。
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
    return fsAppMessageCancel();
  }

  /**
   * FSアプリ内Msg配信停止処理を実行する
   * 
   * @return 処理結果
   */
  private String fsAppMessageCancel() {
    String returnCode = null; // 処理結果
    boolean updateSuccess = false; // true:FS連携状況を更新成功
    String uuidStr = null; // FSアプリ内メッセージUUID
    List<AppMessages> appMessagesList = new ArrayList<AppMessages>();
    // 処理対象のアプリ内Msgを取得する
    appMessagesList = findAppMessages();

    // 処理対象件数をカウントする
    readCount = readCount + appMessagesList.size();

    if (CollectionUtils.isEmpty(appMessagesList)) {
      // 処理対象のアプリ内Msgが存在しない場合、ログを出力し処理終了。

      // 処理対象レコードがありません。（処理：アプリ内Msg配信停止）
      logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB006, MSG_FS_APP_MESSAGE_CANCEL,
          MSG_NO_DETAILS));
      return ProcessResult.SUCCESS.getValue();
    }
    // 処理対象のアプリ内Msgが存在する場合、処理を継続する。

    // アプリ内MsgテーブルのFS公開停止状況を「2:FS連携中」に更新する
    updateSuccess = updateAppMessagesList(appMessagesList, FsDeliveryStatus.DELIVERING);
    if (!updateSuccess) {
      skipCount = appMessagesList.size() - 1;
      failCount = 1;
      return ProcessResult.FAILURE.getValue();
    }

    for (AppMessages appMessages : appMessagesList) {
      try {
        uuidStr = ConvertUtility.longToString(appMessages.getFsAppMessageUuid()); // FSアプリ内メッセージUUID
        logger.info(String.format("処理対象：FSアプリ内メッセージUUID = %s", uuidStr));
        // FS API連携(アプリ内Msg配信情報取得)を行う
        HttpResponse<String> response = callFsApiAppMessageDelivery(appMessages);
        if (response == null) {
          // スキップ件数と失敗件数をカウントする
          failCount = failCount + 1;
        }

        // DTOに変換する
        GetFanshipInAppMsgOutputDTO outputDTODelivery = getResponseBody(response);
        if (outputDTODelivery.getCondition() == null
            || !outputDTODelivery.getCondition().isIsActive()) {
          // is_activeがfalseの場合、アプリ内Msg配信停止APIは実行しない
          // 【アプリ内Msgテーブル】を更新する（3:FS連携済み）
          updateSuccess = updateAppMessages(appMessages, FsDeliveryStatus.DELIVERED);
          if (!updateSuccess) {
            failCount = 1;
          }
          // 処理成功件数をカウントする
          successCount = successCount + 1;
          logger.info(String.format("処理正常終了：FSアプリ内メッセージUUID = %s", uuidStr));
          continue;
        }
        // FS API連携(アプリ内Msg配信停止API)を行う
        response = callFsApiAppMessageCancel(appMessages,
            outputDTODelivery.getCondition().getConditionId());
        if (response == null) {
          // スキップ件数と失敗件数をカウントする
          failCount = failCount + 1;
        }

        // 【アプリ内Msgテーブル】を更新する（3:FS連携済み）
        updateSuccess = updateAppMessages(appMessages, FsDeliveryStatus.DELIVERED);
        if (!updateSuccess) {
          failCount = 1;
        }
        // 成功件数をカウントする
        successCount = successCount + 1;
        logger.info(String.format("処理正常終了：FSアプリ内メッセージUUID = %s", uuidStr));
      } catch (FsMaintenanceException e) {
        logger.error(e.getMessage(), e);
        // FSメンテナンス中の場合
        logger.info(String.format("処理正常終了：FSアプリ内メッセージUUID = %s", uuidStr));
      } catch (FsApiFailedException e) {
        logger.error(e.getMessage(), e);
        // FS API連携失敗の場合
        failCount = failCount + 1;
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        // FS API連携失敗、またはその他の例外発生の場合
        failCount = failCount + 1;
        // ログを出力する
        writeErrorTargetLog(uuidStr, e);
      }
    }
    skipCount = appMessagesList.size() - successCount - failCount;

    if (failCount > 0) {
      return ProcessResult.FAILURE.getValue();
    }
    returnCode = ProcessResult.SUCCESS.getValue();


    // 処理件数のログを出力する
    writeCountLog();
    return returnCode;
  }

  /**
   * 処理対象のアプリ内Msgリストを取得する。
   * 
   * @return 処理対象のアプリ内Msgリスト
   */
  private List<AppMessages> findAppMessages() {
    List<AppMessages> outputDtoList = new ArrayList<AppMessages>();
    try {
      // 【アプリ内Msgテーブル】からレコードを取得する
      List<String> fsDeliveryStatusList = new ArrayList<String>();
      fsDeliveryStatusList.add(FsDeliveryStatus.WAITING.getValue());
      fsDeliveryStatusList.add(FsDeliveryStatus.DELIVERING.getValue());
      List<Map<String, Object>> resList =
          appMessagesDAO.findFsAppMessageCancel(fsDeliveryStatusList);
      // DTOに設定する
      resList.stream().filter(res -> res.get(AppMessagesProps.FS_APP_MESSAGE_UUID) != null)
          .forEach(res -> {
            AppMessages outputDto = new AppMessages();
            outputDto.setAppMessageId(
                ConvertUtility.objectToLong(res.get(AppMessagesProps.APP_MESSAGE_ID)));
            outputDto.setFsAppMessageUuid(
                ConvertUtility.objectToLong(res.get(AppMessagesProps.FS_APP_MESSAGE_UUID)));
            outputDtoList.add(outputDto);
          });
      return outputDtoList;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return outputDtoList;
    }
  }

  /**
   * アプリ内MsgテーブルのFS公開停止状況を「2:FS連携中」に更新する。
   * 
   * @param appMessagesList 処理対象のアプリ内Msgリスト
   * @param fsDeliveryStatus FS連携状況
   * @return trueの場合処理成功
   */
  private boolean updateAppMessagesList(List<AppMessages> appMessagesList,
      FsDeliveryStatus fsDeliveryStatus) {
    boolean isSuccess = false;
    for (AppMessages appMessages : appMessagesList) {

      // FS連携状況更新
      isSuccess = updateAppMessages(appMessages, fsDeliveryStatus);
    }
    return isSuccess;
  }

  /**
   * アプリ内Msgテーブルを更新する。
   * 
   * @param appMessages アプリ内Msgテーブル情報
   * @param fsDeliveryStatus FS連携状況
   * @return trueの場合処理成功
   */
  private boolean updateAppMessages(AppMessages appMessages, FsDeliveryStatus fsDeliveryStatus) {
    try {
      transactionBegin(BATCH_ID);
      // 更新対象のアプリ内Msgテーブルを取得する
      AppMessages appMessagesForUpdate =
          appMessagesDAO.findById(appMessages.getAppMessageId()).orElse(null);

      // アプリ内Msgテーブルを更新する
      appMessagesForUpdate.setFsStopStatus(fsDeliveryStatus.getValue());
      appMessagesForUpdate.setUpdateUserId(BATCH_ID);
      appMessagesForUpdate.setUpdateDate(DateUtils.now());
      appMessagesDAO.update(appMessagesForUpdate);

      transactionCommit(BATCH_ID);
      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      transactionRollback(BATCH_ID);

      // DBエラー共通処理
      writeDbErrorLog(appMessages.getFsAppMessageUuid(), e);
      return false;
    }
  }

  /**
   * アプリ内Msg配信情報取得APIを呼び出す
   * 
   * @param appMessages アプリ内Msgテーブル情報
   * @return HttpResponse
   * @throws FsMaintenanceException
   * @throws FsApiFailedException
   */
  private HttpResponse<String> callFsApiAppMessageDelivery(AppMessages appMessages)
      throws FsMaintenanceException, FsApiFailedException {
    // FSアプリ内メッセージUUID
    String uuidStr = ConvertUtility.longToString(appMessages.getFsAppMessageUuid());
    try {
      // HttpRequestを設定する
      String path = appMessageDeliveryApiUrl.replace(PATH_PARAM_DELIVERY_ID, uuidStr);

      // FS APIを呼び出す
      HttpResponse<String> response = callApi(HttpMethodType.GET, path, ApiKbn.DELIVERY, uuidStr);
      return response;
    } catch (FsMaintenanceException | FsApiFailedException e) {
      logger.error(e.getMessage(), e);
      // FSメンテナンス中、またはFS API連携失敗のの場合
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      // エラー対象のログを出力する
      writeApiErrorTargetLog(uuidStr);
      return null;
    }
  }

  /**
   * アプリ内Msg配信停止APIを呼び出す
   * 
   * @param appMessages アプリ内Msgテーブル情報
   * @param conditionId 配信情報取得APIで取得した状態ID
   * @return HttpResponse
   * @throws FsMaintenanceException
   * @throws FsApiFailedException
   */
  private HttpResponse<String> callFsApiAppMessageCancel(AppMessages appMessages,
      Integer conditionId) throws FsMaintenanceException, FsApiFailedException {
    // FSアプリ内メッセージUUID
    String uuidStr = ConvertUtility.longToString(appMessages.getFsAppMessageUuid());
    try {
      // HttpRequestを設定する
      String path = appMessageCancelApiUrl.replace(PATH_PARAM_CONDITION_ID,
          ConvertUtility.integerToString(conditionId));

      // FS APIを呼び出す
      HttpResponse<String> response = callApi(HttpMethodType.PUT, path, ApiKbn.CANCEL, uuidStr);
      return response;
    } catch (FsMaintenanceException | FsApiFailedException e) {
      logger.error(e.getMessage(), e);
      // FSメンテナンス中、またはFS API連携失敗のの場合
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      // エラー対象のログを出力する
      writeApiErrorTargetLog(uuidStr);
      return null;
    }
  }

  /**
   * FS API連携を行う
   * 
   * @param httpMethodType HTTPメソッド
   * @param path APIパス
   * @param apiKbn API区分
   * @param uuidStr FSアプリ内メッセージUUID
   * @return HttpResponse
   * @throws FsMaintenanceException
   * @throws FsApiFailedException
   */
  private HttpResponse<String> callApi(HttpMethodType httpMethodType, String path, ApiKbn apiKbn,
      String uuidStr) throws Exception {
    FsApiCallResponse fsApiCallResponse = null;
    String apiName = ApiKbn.DELIVERY.equals(apiKbn) ? API_NAME_APP_MESSAGE_DELIVERY
        : API_NAME_APP_MESSAGE_CANCEL;
    try {
      // 正常HTTPステータスコードリストを設定する
      List<Integer> successHttpStatusList = new ArrayList<Integer>();
      successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

      // 処理対象のログを出力する
      writeApiStartLog(uuidStr);

      // FANSHIP APIを呼び出す
      String url = this.reverseProxyUrl + path;
      fsApiCallResponse = callFanshipApi(BATCH_ID, apiName, url, null, httpMethodType,
          ContentType.APPLICATION_JSON, TokenHeaderType.X_POPINFO_MAPI_TOKEN, successHttpStatusList,
          apiRetryCount, apiSleepTime, apiTimeoutDuration, RetryKbn.SERVER_ERROR_TIMEOUT);

      if (fsApiCallResponse == null || fsApiCallResponse.getFsApiCallResult() == null
          || fsApiCallResponse.getResponse() == null) {

        // API連携失敗時のログを出力する
        writeApiFailLog(null, apiKbn, uuidStr, null);
        throw new FsApiFailedException();
      } else if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
        // API成功条件を満たす場合
        return fsApiCallResponse.getResponse();
      } else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())
          || FsApiCallResult.TOO_MANY_REQUEST.equals(fsApiCallResponse.getFsApiCallResult())) {
        // HTTPステータスコード：429、503の場合

        // 処理中断スキップ対象のログを出力する
        writeApiSkipLog(uuidStr);
        throw new FsMaintenanceException(fsApiCallResponse.getResponse().statusCode());
      } else {
        // 上記以外の場合

        // エラー対象のログを出力する
        writeApiErrorTargetLog(uuidStr);
        throw new FsApiFailedException(fsApiCallResponse.getResponse().statusCode());
      }
    } catch (FsMaintenanceException | FsApiFailedException e) {
      logger.error(e.getMessage(), e);
      // FSメンテナンス中、またはFS API連携失敗のの場合
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      // その他例外発生時

      // API連携失敗時のログを出力する
      writeApiFailLog(null, apiKbn, uuidStr, e);
      return null;
    }
  }

  /**
   * 認証トークンを取得する
   * 
   * @throws FsMaintenanceException FS APIメンテナンス
   * @throws FsApiFailedException 認証トークン取得失敗
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
   * レスポンスボディ取得
   * 
   * @param response HTTPレスポンス
   * @return GetFanshipInAppMsgOutputDTO
   * @throws Exception
   */
  private GetFanshipInAppMsgOutputDTO getResponseBody(HttpResponse<String> response)
      throws Exception {
    try {
      GetFanshipInAppMsgOutputDTO output = null;
      if (response != null && response.body() != null) {
        // レスポンスBodyを取得
        output = mapper.readValue(response.body(), GetFanshipInAppMsgOutputDTO.class);
      }
      return output;

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      log.info(String.format(MSG_API_DESERIALIZE_ERROR), e);
      throw e;
    }
  }

  /**
   * 処理件数のログを出力する
   */
  private void writeCountLog() {

    // アプリ内Msg配信停止処理が完了しました。(処理対象件数:[xx] , 処理成功件数:[xx], 処理失敗件数:[xx] , 処理スキップ件数:[0])
    logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, MSG_FS_APP_MESSAGE_CANCEL_PROC,
        readCount, successCount, failCount, skipCount, MSG_NO_DETAILS));
  }

  /**
   * DBエラー共通処理
   * 
   * @param uuidStr FSアプリ内メッセージUUID
   * @param e Exception
   */
  private void writeDbErrorLog(Long uuidStr, Exception e) {
    // メッセージを出力（DBエラーが発生しました。%s）
    String msg = String.format(MSG_FORM_UUID, ConvertUtility.longToString(uuidStr));
    logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), msg), e);
  }

  /**
   * 処理対象のログを出力する
   * 
   * @param uuidStr FSアプリ内メッセージUUID
   */
  private void writeApiStartLog(String uuidStr) {
    // メッセージを出力（処理対象：UUID = %s）
    String msg = String.format(MSG_FORM_START_TARGET, uuidStr);
    logger.info(batchLogger.createMsg("", msg));
  }

  /**
   * 処理中断スキップ対象のログを出力する
   * 
   * @param uuidStr FSアプリ内メッセージUUID
   */
  private void writeApiSkipLog(String uuidStr) {
    // メッセージを出力（処理中断スキップ対象：UUID = %s）
    String msg = String.format(MSG_FORM_SKIP_TARGET, uuidStr);
    logger.info(batchLogger.createMsg("", msg));
  }

  /**
   * エラー対象のログを出力する
   * 
   * @param uuidStr FSアプリ内メッセージUUID
   */
  private void writeApiErrorTargetLog(String uuidStr) {
    // メッセージを出力（エラー対象：UUID = %s）
    String msg = String.format(MSG_FORM_ERROR_TARGET, uuidStr);
    logger.info(batchLogger.createMsg("", msg));
  }

  /**
   * API連携失敗時のログを出力する
   * 
   * @param statusCode ステータスコード
   * @param apiKbn API区分
   * @param uuidStr FSアプリ内メッセージUUID
   * @param e Exception
   */
  private void writeApiFailLog(Integer statusCode, ApiKbn apiKbn, String uuidStr, Exception e) {
    String statusCodeStr =
        statusCode == null ? MSG_UNACQUIRED : ConvertUtility.integerToString(statusCode);
    String apiName = ApiKbn.DELIVERY.equals(apiKbn) ? API_NAME_APP_MESSAGE_DELIVERY
        : API_NAME_APP_MESSAGE_CANCEL;
    String errorInfo = String.format(MSG_FORM_UUID, uuidStr);

    // アプリ内Msg配信情報取得のAPI連携に失敗しました。（HTTPステータスコード ＝「xxx」,エラー内容 = 「xxx」）
    // アプリ内Msg配信停止のAPI連携に失敗しました。（HTTPステータスコード ＝「xxx」,エラー内容 = 「xxx」）
    logger.error(
        batchLogger.createMsg(BusinessMessageCode.B18MB924, apiName, statusCodeStr, errorInfo), e);
  }

  /**
   * エラー対象のログを出力する
   * 
   * @param uuidStr FSアプリ内メッセージUUID
   * @param e Exception
   */
  private void writeErrorTargetLog(String uuidStr, Exception e) {
    // メッセージを出力（エラー対象：UUID = %s）
    String msg = String.format(MSG_FORM_ERROR_TARGET, uuidStr);
    logger.error(batchLogger.createMsg("", msg), e);
  }
}
