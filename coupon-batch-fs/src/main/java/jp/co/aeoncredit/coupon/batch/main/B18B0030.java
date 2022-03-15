package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.SystemException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.util.Strings;
import com.ibm.jp.awag.common.dao.DAOParameter;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.common.S3Utils;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.EventTrackingType;
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.constants.properties.CouponsProps;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUsersDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstAppUsersDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsUserEvent;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * B18B0030_FSログ取込（アプリ利用イベント）
 */
@Named("B18B0030")
@Dependent
public class B18B0030 extends BatchDBAccessBase {

  @Inject
  @BatchProperty
  String executeMode;

  /** バッチID */
  private static final String BATCH_ID = BatchInfo.B18B0030.getBatchId();

  /** バッチネーム */
  private static final String BATCH_NAME = BatchInfo.B18B0030.getBatchName();

  /** ログ */
  private Logger logger = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BATCH_ID);

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader = new BatchConfigfileLoader(BATCH_ID);

  /** AWS S3からダウンロードする際のバケット名 */
  private String s3BucketName;

  /** AWS S3からダウンロードする際のディレクトリ */
  private String s3Directory;

  /** AWS S3からダウンロードする際のファイル名 */
  private String s3FileName;

  /** ダウンロードディレクトリ */
  private String downloadDirectory;

  /** AWS S3からダウンロードして解凍したファイル名 */
  private String ungzFileName;

  /** コミット単位 */
  private Integer commitUnit;
  
  /** NFSからgzファイルダウンロードし、POD内で解凍するためのディレクトリ */
  private String decompressDirectory;

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス */
  @Inject
  protected FsResultsDAOCustomize fsResultsDAO;

  /** クーポンテーブル(COUPONS)EntityのDAOクラス */
  @Inject
  protected CouponsDAOCustomize couponsDAO;

  /** FSクーポンユーザテーブル(FS_COUPON_USER)EntityのDAOクラス */
  @Inject
  protected FsCouponUsersDAOCustomize fsCouponUsersDAO;

  /** アプリユーザマスタテーブル(MST_APP_USER)EntityのDAOクラス */
  @Inject
  protected MstAppUsersDAOCustomize mstAppUsersDAO;

  /** CSVカラム名 ユーザイベント実績ID */
  private static final String CSV_COLUMN_USER_EVENT_ID = "user_event_id";

  /** CSVカラム名 ユーザID */
  private static final String CSV_COLUMN_USER_ID = "user_id";

  /** CSVカラム名 ID種別 */
  private static final String CSV_COLUMN_ID_TYPE = "id_type";

  /** CSVカラム名 popinfo ID */
  private static final String CSV_COLUMN_POPINFO_ID = "popinfo_id";

  /** CSVカラム名 イベントトラッキング種別 */
  private static final String CSV_COLUMN_EVENT_TRACKING_TYPE = "event_tracking_type";

  /** CSVカラム名 イベント対象ID */
  private static final String CSV_COLUMN_EVENT_TARGET_ID = "event_target_id";

  /** CSVカラム名 イベントプロパティ */
  private static final String CSV_COLUMN_EVENT_PROPERTY = "event_property";

  /** CSVカラム名 イベント日時 */
  private static final String CSV_COLUMN_EVENT_DATE = "event_date";

  /** コミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.events.commit.unit";
  
  /** コミット単位 */
  public static final String DECOMPRESS_DIRECTORY = "fs.log.import.events.decompress.directory";

  /** 処理対象件数 */
  private int totalCount;

  /** AWS S3 Utils */
  private S3Utils s3Utils;
  
  private static final int FETCH_SIZE = 1000;

  /**
   * バッチの起動メイン処理
   * 
   * @throws Exception スローされた例外
   * @return 0：正常；1：更新処理異常；9：コード変換処理異常;
   */
  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase#process()
   */
  @Override
  public String process() throws Exception {

    // (1)処理開始メッセージを出力する。
    logger.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(), BATCH_NAME));

    // Export log execute mode
    logger.info(batchLogger.createMsg(Strings.EMPTY,
        String.format(Constants.FORMAT_EXPORT_MODE_LOG, Constants.EXECUTION_MODE, executeMode)));

    // (1.1) 引数チェック
    if (!validate()) {
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB919.toString(),
          BusinessMessage.getMessages(BusinessMessageCode.B18MB919.toString())));
      return setExitStatus(ProcessResult.FAILURE.getValue());
    }

    // プロパティファイルの読み込み
    readProperties();

    // AWS S3 Utils
    s3Utils = new S3Utils(s3BucketName, downloadDirectory, logger, batchLogger, batchFileHandler);

    // バッチの起動メイン処理
    String processResult = processMain();

    // 終了メッセージを出力する。
    logger.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(), BATCH_NAME,
        ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * プロパティファイルの読み込み
   */
  private void readProperties() {
    Properties pro = batchConfigfileLoader.readPropertyFile(BATCH_ID);

    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ */
    s3Directory = pro.getProperty(Constants.S3_DIRECTORY_DETECT_EVENTS);

    /** AWS S3からダウンロードする際のファイル名 */
    s3FileName = pro.getProperty(Constants.S3_FILE_NAME_DETECT_EVENTS);

    /** ダウンロードディレクトリ */
    downloadDirectory = pro.getProperty(Constants.DOWNLOAD_DIRECTORY_DETECT_EVENTS);

    /** AWS S3からダウンロードして解凍したファイル名 */
    ungzFileName = pro.getProperty(Constants.UNGZ_FILE_NAME_DETECT_EVENTS);

    /** コミット単位 */
    commitUnit = ConvertUtility.stringToInteger(pro.getProperty(COMMIT_UNIT));
    
    /** NFSからgzファイルダウンロードし、POD内で解凍するためのディレクトリ */
    decompressDirectory = pro.getProperty(DECOMPRESS_DIRECTORY);
  }

  /**
   * バッチの起動メイン処理
   * 
   * @return 処理結果
   */
  private String processMain() {
    // (2)AWS認証
    // (2.1) S3 Clientをビルドし、AWS認証する。
    S3Client s3Client = s3Utils.s3Client(Region.AP_NORTHEAST_1);

    // (2.1a) AWSの認証に成功した場合は、処理を継続する。
    // (2.1b) AWSの認証に失敗した場合は、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
    if (s3Client == null) {
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB907, "AWS", Constants.NO_DETAIL));
      return ProcessResult.FAILURE.getValue();
    }

    // (3)AWS S3ダウンロード
    // 以下のディレクトリより、FS実績登録テーブルに未登録もしくは登録済みで処理済みフラグが0の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。
    // Get list directory from S3
    List<String> keyList = s3Utils.getKeyList(s3Directory, s3Client, s3Directory);
    
    if (keyList.isEmpty()) {
      s3Client.close();
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue();
      }
      return ProcessResult.FAILURE.getValue();
    }
    int countKey = 0;
    for (String s3Key : keyList) {
      countKey++;
      try {
        // 処理件数を初期化
        totalCount = 0;

        // 処理対象ディレクトリを取得（S3 Keyからファイル名部分を削除）
        String targetDirectory = s3Key.substring(0, s3Key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
        logger.info(targetDirectory);

        // (3.1)FS実績登録テーブルに処理開始を登録する。
        transactionBegin(BATCH_ID);
        fsResultsDAO.setBatchId(BATCH_ID);
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue());
        transactionCommit(BATCH_ID);
        if (!result) {
          continue;
        }

        // (3.1)不要ファイルを削除する。
        if (!s3Utils.deleteFile(s3FileName) || !deleteFile(ungzFileName)) {
          transactionBegin(BATCH_ID);
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BATCH_ID);
          return ProcessResult.FAILURE.getValue();
        }
        
        if (!s3Key.endsWith(s3FileName)) {
          if (countKey == keyList.size()) {
            if (Constants.GENERAL.equals(executeMode)) {
              return ProcessResult.SUCCESS.getValue(); 
            }
            return ProcessResult.FAILURE.getValue(); 
          }
          continue;
        }
        
        if (countKey == keyList.size() && !s3Utils.checkPathLastday(keyList,
            s3Utils.subtractOneDay(DateUtils.getTodayAsString()))) {
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          return ProcessResult.FAILURE.getValue();
        }
        // (3.2)FSログ（アプリ利用イベント）CSVをAWS S3からダウンロードし、ファイルを解凍する。
        // ダウンロードしたファイルを解凍
        String gzFilePath = Paths.get(downloadDirectory, s3FileName).toString();
        String ungzFilePath = Paths.get(decompressDirectory, ungzFileName).toString();
        
        //Create decompress directory
        File directory = new File(decompressDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
          }
        
        if (!s3Utils.downloadFileAWSS3(s3Key, s3Client, s3FileName) || !s3Utils.decompressGzip(gzFilePath, ungzFilePath)) {
          transactionBegin(BATCH_ID);
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BATCH_ID);
          if (Constants.GENERAL.equals(executeMode)) {
            continue;
          }
          // (3.2bb) 実行モードが1:ラストランの場合、処理(3.5.2)を実行後、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileName);
          logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (3.3)(3.2)で解凍した「events_coupon.csv」のレコード分繰り返す。
        importCSV(ungzFilePath);

        transactionBegin(BATCH_ID);
        // (3.5)FS実績登録テーブルにレコードを追加/更新する。
        // (3.5.1) 処理正常の場合
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());

        // (3.6)「events_coupon.csv」のレコードの処理件数をログに出力後、次の処理対象ディレクトリへ
        logger.info(batchLogger.createMsg(BusinessMessageCode.B18MB005, BATCH_NAME, totalCount,
            totalCount, Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT, Constants.NO_DETAIL));

        transactionCommit(BATCH_ID);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
        s3Client.close();
        try {
          if (userTransaction.getStatus() == 0) {
            transactionRollback(BATCH_ID);
          }
        } catch (SystemException e1) {
          logger.error(e1.getMessage(), e1);
          return ProcessResult.FAILURE.getValue();
        }
        return ProcessResult.FAILURE.getValue();
      }
    }
    s3Client.close();
    // 正常終了
    // (5)戻り値に"0"を設定し、処理終了メッセージを出力する。
    return ProcessResult.SUCCESS.getValue();

  }

  /**
   * 【FSユーザイベント】登録
   * 
   * @param row CSVレコード
   * @param entity FsUserEvent
   * @param preFsUserEvent PreparedStatement
   * @throws SQLException
   */
  private void insertFsUserEvent(Map<String, String> row, FsUserEvent entity,
      PreparedStatement preFsUserEvent) throws SQLException {
    String userId = row.get(CSV_COLUMN_USER_ID);
    String eventTrackingType = row.get(CSV_COLUMN_EVENT_TRACKING_TYPE);
    String eventTargetId = row.get(CSV_COLUMN_EVENT_TARGET_ID);
    // Convert format 2021-07-27T18:29:32+09:00 to Timestamp
    Timestamp eventDatetime = ConvertUtility
        .stringToTimestamp(row.get(CSV_COLUMN_EVENT_DATE).substring(0, 19).replace("T", " "));

    entity.setUserId(userId);
    entity.setEventTrackingType(eventTrackingType);
    entity.setEventTargetId(eventTargetId);
    entity.setEventDatetime(eventDatetime);

    int index = 0;
    preFsUserEvent.setString(++index, row.get(CSV_COLUMN_USER_EVENT_ID));
    preFsUserEvent.setString(++index, userId);
    preFsUserEvent.setString(++index, row.get(CSV_COLUMN_ID_TYPE));
    preFsUserEvent.setString(++index, row.get(CSV_COLUMN_POPINFO_ID));
    preFsUserEvent.setString(++index, eventTrackingType);
    preFsUserEvent.setString(++index, eventTargetId);
    preFsUserEvent.setString(++index, row.get(CSV_COLUMN_EVENT_PROPERTY));
    preFsUserEvent.setTimestamp(++index, eventDatetime);
    preFsUserEvent.setString(++index, DeleteFlag.NOT_DELETED.getValue());
    preFsUserEvent.addBatch();
  }

  /**
   * クーポン取得
   * 
   * @param fsUserEvent FSユーザイベントエンティティ
   * @return クーポン
   */
  private Optional<Coupons> getCoupons(FsUserEvent fsUserEvent) {

    DAOParameter daoParam = new DAOParameter();
    daoParam.set(CouponsProps.FS_COUPON_UUID, fsUserEvent.getEventTargetId());
    return couponsDAO.findOne(daoParam, null);
  }

  /**
   * FSクーポン実績連携登録
   * 
   * @param fsUserEvent FSユーザイベント
   * @param coupons クーポン
   * @param preFsEventsForCoupon PreparedStatement
   * @throws SQLException
   */
  private void insertFsEventsForCoupon(FsUserEvent fsUserEvent, Optional<Coupons> coupons,
      PreparedStatement preFsEventsForCoupon) throws SQLException {

    int index = 0;
    preFsEventsForCoupon.setString(++index, fsUserEvent.getUserId());
    preFsEventsForCoupon.setString(++index, fsUserEvent.getEventTargetId());
    if (coupons.isPresent()) {
      // 処理(3.3.2)でクーポンが取得できた場合
      preFsEventsForCoupon.setLong(++index, coupons.get().getCouponId());
    } else {
      // 処理(3.3.2)でクーポンが取得できなかった場合
      preFsEventsForCoupon.setNull(++index, java.sql.Types.NULL);
    }
    preFsEventsForCoupon.setTimestamp(++index, fsUserEvent.getEventDatetime());
    preFsEventsForCoupon.setString(++index, fsUserEvent.getEventTrackingType());
    preFsEventsForCoupon.setString(++index, DeleteFlag.NOT_DELETED.getValue());
    preFsEventsForCoupon.addBatch();
  }

  /**
   * (3.3.4)共通内部ID取得
   * 
   * @param fsUserEvent FSユーザイベント
   * @param coupons クーポン
   * @param preFsCouponUsers PreparedStatement
   * @param preMstAppUsers PreparedStatement
   * @return 共通内部ID
 * @throws SQLException 
   */
  private Optional<String> getCommonInsideId(FsUserEvent fsUserEvent, Optional<Coupons> coupons, 
		  PreparedStatement preFsCouponUsers, PreparedStatement preMstAppUsers) throws SQLException {

    // ユーザIDを取得
    String userId = fsUserEvent.getUserId();

    // 処理(3.4)で共通内部IDが取得できなかった場合
    if (coupons.isEmpty()) {
      return Optional.empty();
    }
    // (3.3.4a)【クーポンテーブル】.「クーポン種別」が「3:パスポートクーポン」の場合
    else if (Objects.equals(coupons.get().getCouponType(), CouponType.PASSPORT.getValue())) {
      String commonInsideId = getCommonInsideIdForPassportCoupons(userId, preFsCouponUsers);
      return Optional.ofNullable(commonInsideId);
    }
    // (3.3.4b)上記以外の場合 (1:マスクーポン、2:ターゲットクーポン、4:アプリイベントクーポン、5:センサーイベントクーポン)
    else {
      String commonInsideId = getCommonInsideIdForOtherCoupons(userId, preMstAppUsers);
      return Optional.ofNullable(commonInsideId);
    }
  }

  /**
   * 共通内部IDを取得（パスポートクーポン）
   * 
   * @param userId ユーザID
   * @param preFsCouponUsers PreparedStatement
   * @return 共通内部ID
 * @throws SQLException 
   */
  private String getCommonInsideIdForPassportCoupons(String userId, PreparedStatement preFsCouponUsers) throws SQLException {
	  ResultSet resultSet = null;
	  int index = 0;
	  try {
		  preFsCouponUsers.setString(++index, userId);   
		  preFsCouponUsers.setFetchSize(FETCH_SIZE);
		  resultSet = preFsCouponUsers.executeQuery();
		  // 会員番号＋家族CDとCPパスポートIDは1:1の関係なので最初の1件目を戻す。
		  if(resultSet.next()) {
			  index = 0;
			  return "PAS" + resultSet.getString(++index) + resultSet.getString(++index) + "0000";
		  }
		  return null;
	  }finally {
		closeQuietly(resultSet, null);
	}
  }

  /**
   * 共通内部IDを取得（マスクーポン、ターゲットクーポン、アプリイベントクーポン、センサーイベントクーポン）
   * 
   * @param userId ユーザID
   * @param preMstAppUsers PreparedStatement
   * @return 共通内部ID
 * @throws SQLException 
   */
  private String getCommonInsideIdForOtherCoupons(String userId, PreparedStatement preMstAppUsers) throws SQLException {
	  ResultSet resultSet = null;
	  int index = 0;
	  try {
		  preMstAppUsers.setString(++index, userId);   
		  preMstAppUsers.setFetchSize(FETCH_SIZE);
          resultSet = preMstAppUsers.executeQuery();
          // イオンウォレットトラッキングIDと共通内部IDは1:1の関係なので最初の1件目を戻す。
          if (resultSet.next()) {
        	  index = 0;
        	  return resultSet.getString(++index);
		  }
          return null;
	  } finally { 
		 closeQuietly(resultSet, null);
	  }
  }

  /**
   * (3.3.5a.1)【FSクーポン配信実績テーブル】に登録する。
   * 
   * @param fsUserEvent FSユーザイベント
   * @param coupons クーポン
   * @param commonInsideId 共通内部ID
   * @param preFsCouponDeliveryResults PreparedStatement
   * @throws SQLException
   */
  private void insertFsCouponDeliveryResults(FsUserEvent fsUserEvent, Optional<Coupons> coupons,
      Optional<String> commonInsideId, PreparedStatement preFsCouponDeliveryResults)
      throws SQLException {
    int index = 0;
    if (coupons.isPresent()) {
      // 処理(3.3.2)でクーポンが取得できた場合
      preFsCouponDeliveryResults.setLong(++index, coupons.get().getCouponId());
    } else {
      // 処理(3.3.2)でクーポンが取得できなかった場合
      preFsCouponDeliveryResults.setNull(++index, java.sql.Types.NULL);
    }
    if (commonInsideId.isPresent()) {
      // 処理(3.4)で共通内部IDが取得できた場合
      preFsCouponDeliveryResults.setString(++index, commonInsideId.get());
    } else {
      // 処理(3.4)で共通内部IDが取得できなかった場合
      preFsCouponDeliveryResults.setNull(++index, java.sql.Types.NULL);
    }
    preFsCouponDeliveryResults.setTimestamp(++index, fsUserEvent.getEventDatetime());
    preFsCouponDeliveryResults.setString(++index, DeleteFlag.NOT_DELETED.getValue());
    preFsCouponDeliveryResults.addBatch();
  }

  /**
   * (3.3.5b.1)【FSクーポン取得実績テーブル】に登録する。
   * 
   * @param fsUserEvent FSユーザイベント
   * @param coupons クーポン
   * @param commonInsideId 共通内部ID
   * @param preFsCouponAcquisitionResults PreparedStatement
   * @throws SQLException
   */
  private void insertFsCouponAcquisitionResults(FsUserEvent fsUserEvent, Optional<Coupons> coupons,
      Optional<String> commonInsideId, PreparedStatement preFsCouponAcquisitionResults)
      throws SQLException {
    int index = 0;
    if (coupons.isPresent()) {
      // 処理(3.3.2)でクーポンが取得できた場合
      preFsCouponAcquisitionResults.setLong(++index, coupons.get().getCouponId());
    } else {
      // 処理(3.3.2)でクーポンが取得できなかった場合
      preFsCouponAcquisitionResults.setNull(++index, java.sql.Types.NULL);
    }
    if (commonInsideId.isPresent()) {
      // 処理(3.4)で共通内部IDが取得できた場合
      preFsCouponAcquisitionResults.setString(++index, commonInsideId.get());
    } else {
      // 処理(3.4)で共通内部IDが取得できなかった場合
      preFsCouponAcquisitionResults.setNull(++index, java.sql.Types.NULL);
    }
    preFsCouponAcquisitionResults.setTimestamp(++index, fsUserEvent.getEventDatetime());
    preFsCouponAcquisitionResults.setString(++index, DeleteFlag.NOT_DELETED.getValue());
    preFsCouponAcquisitionResults.addBatch();
  }

  /**
   * (3.3.5c.1)【FSクーポン利用実績テーブル】に登録する。
   * 
   * @param fsUserEvent FSユーザイベント
   * @param coupons クーポン
   * @param commonInsideId 共通内部ID
   * @param preFsCouponUseResults PreparedStatement
   * @throws SQLException
   */
  private void insertFsCouponUseResults(FsUserEvent fsUserEvent, Optional<Coupons> coupons,
      Optional<String> commonInsideId, PreparedStatement preFsCouponUseResults)
      throws SQLException {
    int index = 0;
    if (coupons.isPresent()) {
      // 処理(3.3.2)でクーポンが取得できた場合
      preFsCouponUseResults.setLong(++index, coupons.get().getCouponId());
    } else {
      // 処理(3.3.2)でクーポンが取得できなかった場合
      preFsCouponUseResults.setNull(++index, java.sql.Types.NULL);
    }
    if (commonInsideId.isPresent()) {
      // 処理(3.4)で共通内部IDが取得できた場合
      preFsCouponUseResults.setString(++index, commonInsideId.get());
    } else {
      // 処理(3.4)で共通内部IDが取得できなかった場合
      preFsCouponUseResults.setNull(++index, java.sql.Types.NULL);
    }
    preFsCouponUseResults.setTimestamp(++index, fsUserEvent.getEventDatetime());
    preFsCouponUseResults.setString(++index, DeleteFlag.NOT_DELETED.getValue());
    preFsCouponUseResults.addBatch();
  }

  /**
   * Validate execute mode
   * 
   * @return true if 通常 or ラストラン
   */
  private boolean validate() {
    boolean check = false;
    if (Constants.GENERAL.equals(executeMode) || Constants.LAST_RUN.equals(executeMode)) {
      check = true;
    }
    return check;
  }

  /**
   * import file csv
   * @param ungzFilePath path file csv
   * @throws SQLException
   * @throws IOException 
   * 
   */
  public void importCSV(String ungzFilePath) throws SQLException, IOException {
    PreparedStatement preFsUserEvent = null;
    PreparedStatement preFsEventsForCoupon = null;
    PreparedStatement preFsCouponDeliveryResults = null;
    PreparedStatement preFsCouponAcquisitionResults = null;
    PreparedStatement preFsCouponUseResults = null;
    PreparedStatement preFsCouponUsers = null;
    PreparedStatement preMstAppUsers = null;

    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ungzFilePath), StandardCharsets.UTF_8))) {
      // ヘッダーあり
      CSVParser parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);
      var iter = parser.iterator();
      int row = 0;
      int commitCount = 0;
      // Init entity
      FsUserEvent fsUserEvent = new FsUserEvent();

      // (3.3)(3.2)で解凍した「events_coupon.csv」のレコード分繰り返す。
      transactionBeginConnection(BATCH_ID);
      preFsUserEvent = prepareStatement(BATCH_ID, "insertFsUserEvent");
      preFsEventsForCoupon = prepareStatement(BATCH_ID, "insertFsEventsForCoupon");
      preFsCouponDeliveryResults = prepareStatement(BATCH_ID, "insertFsCouponDeliveryResults");
      preFsCouponAcquisitionResults =
          prepareStatement(BATCH_ID, "insertFsCouponAcquisitionResults");
      preFsCouponUseResults = prepareStatement(BATCH_ID, "insertFsCouponUseResults");
      preFsCouponUsers = prepareStatement(BATCH_ID, "selectFsCouponUser");
      preMstAppUsers = prepareStatement(BATCH_ID, "selectMstAppUser");
      
      while (iter.hasNext()) {
        totalCount++;
        row++;
        CSVRecord csvRecord = iter.next();
        importCSVRow(csvRecord.toMap(), fsUserEvent, preFsUserEvent, preFsEventsForCoupon,
            preFsCouponDeliveryResults, preFsCouponAcquisitionResults, preFsCouponUseResults, preFsCouponUsers, preMstAppUsers);
        if (row == commitUnit) {
          preFsUserEvent.executeBatch();
          preFsEventsForCoupon.executeBatch();
          preFsCouponDeliveryResults.executeBatch();
          preFsCouponAcquisitionResults.executeBatch();
          preFsCouponUseResults.executeBatch();
          transactionCommitConnection(BATCH_ID);
          em.clear();
          String mess = String.format("%s回目：%s件コミット成功", ++commitCount, row);
          log.info(mess);
          transactionBeginConnection(BATCH_ID);
          row = 0;
        }
      }
      if (row != 0) {
        preFsUserEvent.executeBatch();
        preFsEventsForCoupon.executeBatch();
        preFsCouponDeliveryResults.executeBatch();
        preFsCouponAcquisitionResults.executeBatch();
        preFsCouponUseResults.executeBatch();
        String mess = String.format("%s回目：%s件コミット成功", ++commitCount, row);
        log.info(mess);
      }
      transactionCommitConnection(BATCH_ID);
      em.clear();

    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      transactionRollbackConnection(BATCH_ID);
      throw e;
    } finally {
      closeQuietly(null, preFsUserEvent);
      closeQuietly(null, preFsEventsForCoupon);
      closeQuietly(null, preFsCouponDeliveryResults);
      closeQuietly(null, preFsCouponAcquisitionResults);
      closeQuietly(null, preFsCouponUseResults);
      closeConnection(BATCH_ID);
    }
  }

  /**
   * CSVファイルを取り込む。
   * 
   * @param row content csv
   * @param fsUserEvent FsUserEvent
   * @param preFsUserEvent PreparedStatement
   * @param preFsEventsForCoupon PreparedStatement
   * @param preFsCouponDeliveryResults PreparedStatement
   * @param preFsCouponAcquisitionResults PreparedStatement
   * @param preFsCouponUseResults PreparedStatement
   * @param preFsCouponUsers PreparedStatement
   * @param preMstAppUsers PreparedStatement
   * @throws SQLException
   */
  private void importCSVRow(Map<String, String> row, FsUserEvent fsUserEvent,
      PreparedStatement preFsUserEvent, PreparedStatement preFsEventsForCoupon,
      PreparedStatement preFsCouponDeliveryResults, PreparedStatement preFsCouponAcquisitionResults,
      PreparedStatement preFsCouponUseResults, PreparedStatement preFsCouponUsers, PreparedStatement preMstAppUsers) throws SQLException {
    // (3.3.1)【FSユーザイベント】登録
    insertFsUserEvent(row, fsUserEvent, preFsUserEvent);

    // イベントトラッキング種別を取得
    EventTrackingType eventTrackingType =
        EventTrackingType.valueToEventTrackingType(fsUserEvent.getEventTrackingType());

    // (3.3.1.2)CSVの「イベントトラッキング種別」が「01」～「05」以外の場合、後続処理を行わず処理(3)へ（次のレコード）
    if (eventTrackingType == null) {
      return;
    }

    switch (eventTrackingType) {
      case COUPON_DELIVERED:
      case COUPON_VIEWED:
      case COUPON_USED:
      case COUPON_FAVORITE_REGISTERED:
      case COUPON_FAVORITE_CANCELED:
        break;
      default:
        // 次のレコード
        return;
    }

    // (3.3.2)クーポン取得
    Optional<Coupons> coupons = getCoupons(fsUserEvent);

    // (3.3.3)FSクーポン実績連携登録
    insertFsEventsForCoupon(fsUserEvent, coupons, preFsEventsForCoupon);

    // (3.3.3.3)CSVの「イベントトラッキング種別」が「01」～「03」以外の場合、後続処理を行わず処理(3.3)へ（次のレコード）
    switch (eventTrackingType) {
      case COUPON_DELIVERED:
      case COUPON_VIEWED:
      case COUPON_USED:
        break;
      default:
        // 次のレコード
        return;
    }

    // クーポンが存在する場合
    if (coupons.isPresent()) {

      // (3.3.4)共通内部ID取得
      logger.debug("(3.3.4)共通内部ID取得 start");
      Optional<String> commonInsideId = getCommonInsideId(fsUserEvent, coupons, preFsCouponUsers, preMstAppUsers);
      logger.debug("(3.3.4)共通内部ID取得 end");

      // (3.3.5)FSクーポン実績登録
      switch (eventTrackingType) {
        // (3.3.5a)CSVの「イベントトラッキング種別」が「01:クーポンを配信した」の場合
        case COUPON_DELIVERED:
          insertFsCouponDeliveryResults(fsUserEvent, coupons, commonInsideId,
              preFsCouponDeliveryResults);
          break;
        // (3.3.5b)CSVの「イベントトラッキング種別」が「02:クーポン詳細を閲覧した」の場合
        case COUPON_VIEWED:
          insertFsCouponAcquisitionResults(fsUserEvent, coupons, commonInsideId,
              preFsCouponAcquisitionResults);
          break;
        // (3.3.5c)CSVの「イベントトラッキング種別」が「03:クーポンを使用した」の場合
        case COUPON_USED:
          insertFsCouponUseResults(fsUserEvent, coupons, commonInsideId, preFsCouponUseResults);
          break;
        // その他
        default:
          break;
      }
    }
  }
  
  /**
   * Delete file
   * 
   * @param fileName file name to delete
   * @param downloadDirectory directory
   * @param batchFileHandler class BatchFileHandler
   * @return true if delete file success
   */
  public boolean deleteFile(String fileName) {
    String filePath = Paths.get(decompressDirectory, fileName).toString();
    boolean result = batchFileHandler.existFile(filePath);
    // Exist file
    if (result && Boolean.FALSE.equals(batchFileHandler.deleteFile(filePath))) {
      // Export log when delete file failure
      logger.error(batchLogger.createMsg(BusinessMessageCode.B18MB906, decompressDirectory, fileName));
      return false;
    }
    return true;
  }
}