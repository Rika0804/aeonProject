package jp.co.aeoncredit.coupon.batch.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.Logger;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.GetFsSegmentListOutputDTO;
import jp.co.aeoncredit.coupon.batch.exception.FsApiFailedException;
import jp.co.aeoncredit.coupon.batch.exception.FsMaintenanceException;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.MatchFlag;
import jp.co.aeoncredit.coupon.constants.PurposeSegmentFlag;
import jp.co.aeoncredit.coupon.constants.properties.FsSegmentProps;
import jp.co.aeoncredit.coupon.dao.custom.FsSegmentDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsSegment;

/**
 * バッチ B18B0079
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0079")
@Dependent
public class B18B0079 extends BatchFSApiCalloutBase {

  /** ログ */
  private Logger myLog = getLogger();

  /** Get batch ID */
  private static final String BATCH_ID = BatchInfo.B18B0079.getBatchId();
  
  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BATCH_ID);

  /** ID指定クラウド連携 */
  private static final String ID_DESIGNATION_CLOUD_COOPERATION_INTEGRATION = "integration.subscription";
  
  private static final String ID_DESIGNATION_CLOUD_COOPERATION_DEFAULT = "default";
  
  private static final String ID_DESIGNATION_CLOUD_COOPERATION_FILTRATION = "filtration.query";
  
  private static final String FS_SEGMENT_ID_LIST= "fsSegmentIdList";
  
  /** API名 */
  private static final String FANSHIP_API_NAME = "セグメント一覧取得";

  /** セグメント一覧取得APIのURL */
  private String apiUrl;

  /** FS API 失敗時のAPI実行リトライ回数 */
  private Integer retryCount;

  /** FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒) */
  private Integer sleepTime;

  /** FS API発行時のタイムアウト期間(秒) */
  private Integer timeoutDuration;

  /** FSセグメント連携テーブル（FS_SEGMENT）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsSegmentDAOCustomize fsSegmentDAO;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0079.getBatchName()));

    // プロパティファイルの読み込み
    readProperties();

    // バッチの起動メイン処理 機能概要 : FS目的別セグメント一覧取得バッチ
    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0079.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Process FS目的別セグメント一覧取得バッチ
   * 
   * @return 処理結果
   */
  private String processMain() {
    // (2)認証トークン取得
    try {
      // (2.1a) 【B18BC001_認証トークン取得】の戻り値が「成功（enumで定義）」の場合 は、処理を継続する。
      // (2.1b) 認証トークン取得に失敗した場合
      processAuthToken();
    } catch (FsMaintenanceException e) {
      myLog.error(e.getMessage(), e);

      // (2.1b.1)【B18BC001_認証トークン取得】の戻り値が「メンテナンス（enumで定義）」(FANSHIPメンテナンス)の場合
      // 戻り値に"0"を設定し、処理を終了する（エラーログは【B18BC001_認証トークン取得】で出力される）。
      return ProcessResult.SUCCESS.getValue();
    } catch (FsApiFailedException e) {
      myLog.error(e.getMessage(), e);

      // (2.1b.2)【B18BC001_認証トークン取得】の戻り値が「失敗（enumで定義）」の場合
      // 戻り値に"1"を設定し、処理を終了する（エラーログは【B18BC001_認証トークン取得】で出力される）。
      return ProcessResult.FAILURE.getValue();
    }

    // (3) FS API連携
    // (3.1)セグメント一覧取得API実行
    List<GetFsSegmentListOutputDTO> getFsSegmentListOutputDTOList = null;
    try {
      getFsSegmentListOutputDTOList = getFsSegmentListOutputDTOList();
    } catch (FsMaintenanceException e) {
      myLog.error(e.getMessage(), e);
      return ProcessResult.SUCCESS.getValue();
    } catch (Exception e) {
      myLog.error(e.getMessage(), e);
      return ProcessResult.FAILURE.getValue();
    }

    try {
      transactionBegin(BATCH_ID);
      // (4)FSセグメント連携テーブル登録
      // (4.1)【FSセグメント連携テーブル】の既存の目的別セグメントのレコードを物理削除（DELETE）する。
      deleteDataFsSegmentByPurposeFlag();
      List<Long> fsSegmentIdList = new ArrayList<>();
      FsSegment fsSegment = new FsSegment();
      // (4.2)処理(3.1)で取得したレコード分繰り返す。
      for (GetFsSegmentListOutputDTO getFsSegmentListOutputDTO : getFsSegmentListOutputDTOList) {
        // (4.2a.1)「レスポンスBody．エラー情報」にメッセージが格納されていない場合
        if (ID_DESIGNATION_CLOUD_COOPERATION_INTEGRATION.equals(getFsSegmentListOutputDTO.getType())) {
          if (getFsSegmentListOutputDTO.getErrorInfomation() == null) {
            fsSegmentIdList.add(getFsSegmentListOutputDTO.getId());
            // 以下の条件で【FSセグメント連携テーブル】に登録する。
            fsSegment.setFsSegmentId(getFsSegmentListOutputDTO.getId());
            fsSegment.setFsSegmentName(getFsSegmentListOutputDTO.getName());
            fsSegment.setSegmentByPurposeFlag(PurposeSegmentFlag.BY_PURPOSE.getValue());
            // Convert format 2021-07-27T18:29:32+09:00 to Timestamp
            fsSegment.setFsCreateDate(ConvertUtility.stringToTimestamp(
                getFsSegmentListOutputDTO.getCreated().substring(0, 19).replace("T", " ")));
            fsSegment.setNumberOfPeople(
                ConvertUtility.stringToLong(getFsSegmentListOutputDTO.getNumberOfPeople()));
            fsSegment.setMatchFlag(MatchFlag.NO_MATCH_USER_EXISTS.getValue());
            fsSegmentDAO.insert(fsSegment);
            fsSegmentDAO.detach(fsSegment);
          } else {
            myLog.info(batchLogger.createMsg(Strings.EMPTY,
                getFsSegmentListOutputDTO.getErrorInfomation()));
          }
        } else if (ID_DESIGNATION_CLOUD_COOPERATION_DEFAULT.equals(getFsSegmentListOutputDTO.getType()) ||
            ID_DESIGNATION_CLOUD_COOPERATION_FILTRATION.equals(getFsSegmentListOutputDTO.getType())) {
          if (getFsSegmentListOutputDTO.getErrorInfomation() == null) {
            fsSegmentIdList.add(getFsSegmentListOutputDTO.getId());
            // (4.2b.1b)【FSセグメント連携テーブル】.「FSセグメントID」=レスポンスBody.セグメントIDの場合
            Optional<FsSegment> fsSegmentOptional = fsSegmentDAO.findById(getFsSegmentListOutputDTO.getId());
            if (fsSegmentOptional.isPresent()) {
              continue;
            }
            // (4.2b.1a)【FSセグメント連携テーブル】.「FSセグメントID」≠レスポンスBody.セグメントIDの場合
            // 以下の条件で【FSセグメント連携テーブル】に登録する。
            fsSegment.setFsSegmentId(getFsSegmentListOutputDTO.getId());
            fsSegment.setFsSegmentName(getFsSegmentListOutputDTO.getName());
            fsSegment.setSegmentByPurposeFlag(PurposeSegmentFlag.FS.getValue());
            // Convert format 2021-07-27T18:29:32+09:00 to Timestamp
            fsSegment.setFsCreateDate(ConvertUtility.stringToTimestamp(
                getFsSegmentListOutputDTO.getCreated().substring(0, 19).replace("T", " ")));
            fsSegment.setNumberOfPeople(
                ConvertUtility.stringToLong(getFsSegmentListOutputDTO.getNumberOfPeople()));
            fsSegment.setMatchFlag(MatchFlag.NO_MATCH_USER_EXISTS.getValue());
            fsSegmentDAO.insert(fsSegment);
            fsSegmentDAO.detach(fsSegment);
          } else {
            myLog.info(batchLogger.createMsg(Strings.EMPTY,
                getFsSegmentListOutputDTO.getErrorInfomation()));
          }
        }
      }
      if (CollectionUtils.isNotEmpty(fsSegmentIdList)) {
        // (4.3)(4.2a.1)、(4.2b.1)で処理を実施したセグメントIDに一致しない【FSセグメント連携テーブル】を論理削除する。
        updateDataFsSegmentByPurposeFlag(fsSegmentIdList);
        // (4.4)(4.3)で論理削除した【FSセグメント連携テーブル】に紐づく【FSセグメントマッチユーザ連携テーブル】を論理削除する。
        updateDataFsSegmentMatchUserByPurposeFlag(fsSegmentIdList);
      }
      transactionCommit(BATCH_ID);
      return ProcessResult.SUCCESS.getValue();
    } catch (Exception e) {
      transactionRollback(BATCH_ID);
      myLog.error(e.getMessage(), e);
      return ProcessResult.FAILURE.getValue();
    }
  }

  /**
   * セグメント一覧取得API実行
   * 
   * @return list of GetFsSegmentListOutputDTO
   * @throws IOException class IOException
   * @throws FsMaintenanceException FS APIメンテナンス
   * @throws FsApiFailedException error other
   */
  private List<GetFsSegmentListOutputDTO> getFsSegmentListOutputDTOList()
      throws IOException, FsMaintenanceException, FsApiFailedException {
    List<Integer> successHttpStatusList = new ArrayList<>();
    successHttpStatusList.add(HTTPStatus.HTTP_STATUS_SUCCESS.getValue());

    // API発行
    FsApiCallResponse fsApiCallResponse =
        callFanshipApi(BATCH_ID, FANSHIP_API_NAME, apiUrl, null,
            HttpMethodType.GET, ContentType.APPLICATION_JSON, TokenHeaderType.AUTHORIZATION,
            successHttpStatusList, retryCount, sleepTime, timeoutDuration, RetryKbn.TIMEOUT);

    if (FsApiCallResult.SUCCESS.equals(fsApiCallResponse.getFsApiCallResult())) {
      ObjectMapper mapper = new ObjectMapper();
      return Arrays.asList(mapper.readValue(fsApiCallResponse.getResponse().body(),
          GetFsSegmentListOutputDTO[].class));
    } else if (FsApiCallResult.FANSHIP_MAINTENANCE.equals(fsApiCallResponse.getFsApiCallResult())) {
      // (X.1b) HTTPステータスコード：503の場合
      throw new FsMaintenanceException();
    } else {
      // 例外発生時
      throw new FsApiFailedException();
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
   * Delete data in table FSセグメント連携テーブル by 目的別セグメントフラグ
   */
  private void deleteDataFsSegmentByPurposeFlag() {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(FsSegmentProps.SEGMENT_BY_PURPOSE_FLAG, PurposeSegmentFlag.BY_PURPOSE.getValue());
    sqlExecute(BATCH_ID, "deleteFsSegmentBySegmentByPurposeFlag", paramMap);
  }

  /**
   * Update data in table FSセグメント連携テーブル by 目的別セグメントフラグ
   */
  private void updateDataFsSegmentByPurposeFlag(List<Long> fsSegmentIdList) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(FsSegmentProps.SEGMENT_BY_PURPOSE_FLAG, PurposeSegmentFlag.FS.getValue());
    paramMap.put(FS_SEGMENT_ID_LIST, fsSegmentIdList);
    sqlExecute(BATCH_ID, "updateFsSegmentBySegmentByPurposeFlag", paramMap);
  }

  /**
   * Update data in table FSセグメント連携テーブル by 目的別セグメントフラグ
   */
  private void updateDataFsSegmentMatchUserByPurposeFlag(List<Long> fsSegmentIdList) {
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put(FsSegmentProps.SEGMENT_BY_PURPOSE_FLAG, PurposeSegmentFlag.FS.getValue());
    paramMap.put(FS_SEGMENT_ID_LIST, fsSegmentIdList);
    sqlExecute(BATCH_ID, "updateFsSegmentMatchUserBySegmentByPurposeFlag", paramMap);
  }

  /**
   * Read file properties
   * 
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BATCH_ID);

    /** セグメント一覧取得APIのURL */
    apiUrl = this.integrationUrl + properties.getProperty(Constants.FS_SEGMENT_GET_API_URL);

    /** FS API 失敗時のAPI実行リトライ回数 */
    retryCount = ConvertUtility
        .stringToInteger(properties.getProperty(Constants.FS_SEGMENT_GET_BATCH_RETRY_COUNT));

    /** FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒) */
    sleepTime = ConvertUtility
        .stringToInteger(properties.getProperty(Constants.FS_SEGMENT_GET_RETRY_SLEEP_TIME));

    /** FS API発行時のタイムアウト期間(秒) */
    timeoutDuration = ConvertUtility
        .stringToInteger(properties.getProperty(Constants.FS_SEGMENT_GET_TIMEOUT_DURATION));
  }
}
