package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.util.Strings;
import com.ibm.jp.awag.common.logic.ServiceAppException;
import com.ibm.jp.awag.common.logic.ServiceDBException;
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
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.dao.custom.FsEventsForIbeaconDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsEventsForIbeacon;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * バッチ B18B0032
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0032")
@Dependent
public class B18B0032 extends BatchDBAccessBase {

  @Inject
  @BatchProperty
  /** 実行モード */
  String executeMode;

  /** ログ */
  private Logger myLog = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0032.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0032.getBatchId());

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0032.getBatchId());

  /** AWS S3 Utils */
  private S3Utils s3Utils;

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

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
  @Inject
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FSiBeacon検出連携テーブル(FS_EVENTS_FOR_IBEACON)EntityのDAOクラス。 */
  @Inject
  protected FsEventsForIbeaconDAOCustomize fsEventsForIbeaconDAO;

  /** colum customer_id in file csv */
  private static final String COLUMN_CUSTOMER_ID = "customer_id";

  /** colum uuid in file csv */
  private static final String COLUMN_UUID = "uuid";

  /** colum timestamp in file csv */
  private static final String COLUMN_TIMESTAMP = "timestamp";

  /** colum popinfo_id in file csv */
  private static final String COLUMN_POPINFO_ID = "popinfo_id";

  /** コミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.ibeacon.commit.unit";

  /** 処理対象件数 */
  private int numberProcessTarget;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0032.getBatchName()));

    // Export log execute mode
    myLog.info(new BatchLogger(BatchInfo.B18B0032.getBatchId()).createMsg(Strings.EMPTY,
        String.format(Constants.FORMAT_EXPORT_MODE_LOG, Constants.EXECUTION_MODE, executeMode)));

    // (1.1) 引数チェック
    if (!validate()) {
      myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB919.toString(),
          BusinessMessage.getMessages(BusinessMessageCode.B18MB919.toString())));
      return setExitStatus(ProcessResult.FAILURE.getValue());
    }

    // プロパティファイルの読み込み
    readProperties();

    // AWS S3 Utils
    s3Utils = new S3Utils(s3BucketName, downloadDirectory, myLog, batchLogger, batchFileHandler);

    // バッチの起動メイン処理 機能概要 : FSログ取込（iBeacon検出）
    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0032.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Process FSログ取込（iBeacon検出）
   * 
   * @return 処理結果
   */
  @SuppressWarnings("resource")
  private String processMain() {
    // (2)AWS認証
    // (2.1) S3 Clientをビルドし、AWS認証する。
    // リージョン：ap-northeast-1
    // (2.1a) AWSの認証に成功した場合は、処理を継続する。
    S3Client s3Client = s3Utils.s3Client(Region.AP_NORTHEAST_1);
    if (s3Client == null) {
      // (2.1b) AWSの認証に失敗した場合は、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
      // Export log
      String message =
          String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
              Constants.TEXT_AWS, Constants.NO_DETAIL);
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB907.toString(), message));
      return ProcessResult.FAILURE.getValue();
    }

    // (3)AWS S3ダウンロード
    // 以下のディレクトリより、FS実績登録テーブルに未登録もしくは登録済みで処理済みフラグが0の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。
    // Get list directory from S3
    List<String> keyList = s3Utils.getKeyList(s3Directory, s3Client, s3Directory);
    
    if (keyList.isEmpty()) {
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue(); 
      }
      return ProcessResult.FAILURE.getValue(); 
    }
    for (String key : keyList) {
      myLog.info("processMain.key：" + key);
      // FS実績登録テーブルに処理開始を登録する。
      // 処理中に更新してから進む。
      String targetDirectory = key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
      try {
        // (3.1)FS実績登録テーブルに処理開始を登録する。
        fsResultsDAO.setBatchId(BatchInfo.B18B0032.getBatchId());
        transactionBegin(BatchInfo.B18B0032.getBatchId());
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue());
        transactionCommit(BatchInfo.B18B0032.getBatchId());
        
        // Check file doesnot exist || Not the object to handle
        if (!key.endsWith(s3FileName) || !result) {
          continue;
        }

        // (3.1)不要ファイルを削除する。
        // ダウンロードディレクトリに以前実行時のファイルが存在する場合削除する。
        // AWS S3からダウンロードする際のファイル名
        // AWS S3からダウンロードして解凍したファイル名
        // (3.2)FSログ（iBeacon検出）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。

        if (!s3Utils.deleteFile(s3FileName) || !s3Utils.deleteFile(ungzFileName)) {
          transactionBegin(BatchInfo.B18B0032.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0032.getBatchId());
          continue;
        }

        // ダウンロードしたファイルを解凍
        // (3.3)【FSiBeacon検出連携テーブル】登録
        boolean resultImport =
            downloadAndImportCSV(key, s3Client, s3FileName, StandardCharsets.UTF_8);

        if (!resultImport) {
          transactionBegin(BatchInfo.B18B0032.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0032.getBatchId());
          if (Constants.GENERAL.equals(executeMode)) {
            continue;
          }
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileName);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (3.4)FS実績登録テーブルにレコードを追加/更新する。
        // (3.4.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0032.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());
        transactionCommit(BatchInfo.B18B0032.getBatchId());

        // (3.5)FSログ（iBeacon検出）CSVファイルのレコードの処理件数をログに出力後、次の処理対象ディレクトリへ
        String message =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
                BatchInfo.B18B0032.getBatchName(), numberProcessTarget, numberProcessTarget,
                Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT, Constants.NO_DETAIL);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));
      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        transactionRollback(BatchInfo.B18B0032.getBatchId());
        transactionBegin(BatchInfo.B18B0032.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0032.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }
    String lastKey = keyList.get(keyList.size() - 1);
    try {
      // Check file path or file doesnot exist
      if (!lastKey.endsWith(s3FileName) || !s3Utils.checkPathLastday(keyList,
          s3Utils.subtractOneDay(DateUtils.getTodayAsString()))) {
        if (Constants.GENERAL.equals(executeMode)) {
          return ProcessResult.SUCCESS.getValue();
        }
        return ProcessResult.FAILURE.getValue();
      }
    } catch (ParseException e) {
      e.printStackTrace();
      return ProcessResult.FAILURE.getValue();
    }

    return ProcessResult.SUCCESS.getValue();
  }

  /**
   * Read file properties
   * 
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0032.getBatchId());

    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ */
    s3Directory = properties.getProperty(Constants.S3_DIRECTORY_DETECT_IBEACON);

    /** AWS S3からダウンロードする際のファイル名 */
    s3FileName = properties.getProperty(Constants.S3_FILE_NAME_DETECT_IBEACON);

    /** ダウンロードディレクトリ */
    downloadDirectory = properties.getProperty(Constants.DOWNLOAD_DIRECTORY_DETECT_IBEACON);

    /** AWS S3からダウンロードして解凍したファイル名 */
    ungzFileName = properties.getProperty(Constants.UNGZ_FILE_NAME_DETECT_IBEACON);

    /** コミット単位 */
    commitUnit = ConvertUtility.stringToInteger(properties.getProperty(COMMIT_UNIT));
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
   * download and import file csv
   * 
   * @param key Key of the object to get
   * @param s3Client S3Client
   * @param cs Charset
   * @param hasHeader ヘッダー項目の有無
   * @return true if success
   * @throws IOException
   */
  @SuppressWarnings("resource")
  public boolean downloadAndImportCSV(String key, S3Client s3Client, String s3FileName, Charset cs)
      throws ServiceDBException, ServiceAppException {
    log.debug("download start");
    GetObjectRequest request = GetObjectRequest.builder().bucket(s3BucketName).key(key).build();
    // Run download
    Path dstPath = Paths.get(downloadDirectory, s3FileName);
    s3Client.getObject(request, dstPath);
    log.debug("download end");
    try (var gzipInputStream = new GZIPInputStream(new FileInputStream(dstPath.toString()));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzipInputStream, cs));) {
      CSVParser parser = null;

      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);

      var iter = parser.iterator();
      int index = 0;
      int commitCount = 0;
      numberProcessTarget = 0;
      // Init entity
      // Set content file CSV to Entity
      FsEventsForIbeacon fsEventsForIbeacon = new FsEventsForIbeacon();


      // (3.3)(3.2)で解凍した「events_coupon.csv」のレコード分繰り返す。
      log.debug("insert start");
      transactionBegin(BatchInfo.B18B0032.getBatchId());
      while (iter.hasNext()) {
        index++;
        // 処理対象件数
        numberProcessTarget++;
        CSVRecord record = iter.next();

        // (3.3.2)【FSiBeacon検出連携テーブル】にデータを登録する。
        insertFsEventsForIbeacon(record.toMap(), fsEventsForIbeacon);
        if (index == commitUnit) {
          transactionCommit(BatchInfo.B18B0032.getBatchId());
          String mess = String.format("%s回目：%s件コミット成功", ++commitCount, index);
          log.info(mess);
          transactionBegin(BatchInfo.B18B0032.getBatchId());
          index = 0;
        }
      }
      transactionCommit(BatchInfo.B18B0032.getBatchId());
      if (index != 0) {
        String mess = String.format("%s回目：%s件コミット成功", ++commitCount, index);
        log.info(mess);
      }
      log.debug("insert end");
      return true;
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      return false;
    }
  }

  /**
   * (3.3.2)【FSiBeacon検出連携テーブル】登録
   * 
   * @param row
   * @param Entity FsEventsForIbeacon fsEventsForIbeacon
   */
  private void insertFsEventsForIbeacon(Map<String, String> row,
      FsEventsForIbeacon fsEventsForIbeacon) {

    fsEventsForIbeacon.setFsEventsForIbeaconId(
        fsEventsForIbeaconDAO.getSequenceNextVal(Constants.SEQ_FS_EVENTS_FOR_IBEACON_ID));
    fsEventsForIbeacon.setAwTrackingId(row.get(COLUMN_CUSTOMER_ID));
    fsEventsForIbeacon.setPopinfoId(row.get(COLUMN_POPINFO_ID));
    fsEventsForIbeacon.setFsIbeaconUuid(row.get(COLUMN_UUID));
    // Convert format 2021-07-27T18:29:32+09:00 to Timestamp
    fsEventsForIbeacon.setEventTimestamp(ConvertUtility
        .stringToTimestamp(row.get(COLUMN_TIMESTAMP).substring(0, 19).replace("T", " ")));
    fsEventsForIbeacon.setCreateUserId(BatchInfo.B18B0032.getBatchId());
    fsEventsForIbeacon.setUpdateUserId(BatchInfo.B18B0032.getBatchId());
    fsEventsForIbeaconDAO.insert(fsEventsForIbeacon);
    fsEventsForIbeaconDAO.detach(fsEventsForIbeacon);
  }
}


