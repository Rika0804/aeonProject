package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsVisitorsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsVisitors;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * バッチ B18B0037
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0037")
@Dependent
public class B18B0037 extends BatchDBAccessBase {

  @Inject
  @BatchProperty
  /** 実行モード */
  String executeMode;

  @Inject
  @BatchProperty
  /** 実行日付 */
  String executeDate;

  /** ログ */
  private Logger myLog = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0037.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0037.getBatchId());

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0037.getBatchId());

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

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
  @Inject
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FS来店ユーザ連携テーブル（FS_VISITORS）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsVisitorsDAOCustomize fsVisitorsDAO;

  /** colum customer_id in file csv */
  private static final String COLUMN_CUSTOMER_ID = "customer_id";

  /** colum facility_id in file csv */
  private static final String COLUMN_FACILITY_ID = "facility_id";

  /** colum popinfo_id in file csv */
  private static final String COLUMN_POPINFO_ID = "popinfo_id";

  /** colum visited_at in file csv */
  private static final String COLUMN_VISITED_AT = "visited_at";

  /** 処理対象件数 */
  private Integer numberProcessTarget = 0;

  /** 全件モードの場合のコミット単位 */
  private Integer commitUnit;

  /** Insert message for each commit */
  private static final String INSERT_COMMIT_MESSAGE = "%s回目：%s件コミット成功";

  /** 全件モードの場合のコミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.visitors.commit.unit";

  /** AWS S3 Utils */
  private S3Utils s3Utils;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0037.getBatchName()));

    // Export log execute mode and execute date
    myLog.info(batchLogger.createMsg(Strings.EMPTY,
        String.format(Constants.FORMAT_EXPORT_MODE_LOG, Constants.EXECUTION_MODE, executeMode)
            + Constants.COMMA_FULL_SIZE + String.format(Constants.FORMAT_EXPORT_MODE_LOG,
                Constants.EXECUTION_DATE, executeDate)));

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

    // バッチの起動メイン処理 機能概要 : FSログ取込（来店ユーザ）
    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0037.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Process FSログ取込（来店ユーザ）
   * 
   * @return 処理結果
   */
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
    String prefix = s3Directory + executeDate + Constants.SYMBOL_SLASH;
    List<String> keyList = s3Utils.getKeyList(prefix, s3Client, s3Directory);
    if (executeDate.equals(DateUtils.getTodayAsString()) && keyList.isEmpty()) {
      try {
        prefix = s3Directory + s3Utils.subtractOneDay(DateUtils.getTodayAsString())
              + Constants.SYMBOL_SLASH;
      } catch (ParseException e) {
        s3Client.close();
        myLog.error(e.getMessage(), e);
        return ProcessResult.FAILURE.getValue();
      }
      keyList = s3Utils.getKeyList(prefix, s3Client, s3Directory);
    }
    
    if (keyList.isEmpty()) {
      s3Client.close();
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue();
      }
      return ProcessResult.FAILURE.getValue();
    }
    int countKey = 0;
    for (String key : keyList) {
      countKey++;
      myLog.info("processMain.key：" + key);
      String targetDirectory = key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
      try {
        // (3.1)FS実績登録テーブルに処理開始を登録する。
        fsResultsDAO.setBatchId(BatchInfo.B18B0037.getBatchId());
        transactionBegin(BatchInfo.B18B0037.getBatchId());
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
                TreatedFlag.PROCESSING.getValue());
        transactionCommit(BatchInfo.B18B0037.getBatchId());
        if (!result) {
          return ProcessResult.SUCCESS.getValue();
        }

        // (3.2)不要ファイルを削除する。
        // ダウンロードディレクトリに以前実行時のファイルが存在する場合削除する。
        if (!s3Utils.deleteFile(s3FileName) || !s3Utils.deleteFile(ungzFileName)) {
          transactionBegin(BatchInfo.B18B0037.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0037.getBatchId());
          return ProcessResult.FAILURE.getValue();
        }
        
        if (!key.endsWith(s3FileName)) {
          if (countKey == keyList.size()) {
            if (Constants.GENERAL.equals(executeMode)) {
              return ProcessResult.SUCCESS.getValue(); 
            }
            return ProcessResult.FAILURE.getValue(); 
          }
          continue;
        }
        // AWS S3からダウンロードする際のファイル名
        // AWS S3からダウンロードして解凍したファイル名
        // (3.3)FSログ（来店ユーザ）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。
        // (3.3a) ダウンロードに成功した場合は、処理を継続する。
        if (!s3Utils.downloadFileAWSS3(key, s3Client, s3FileName)) {
          transactionBegin(BatchInfo.B18B0037.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0037.getBatchId());
          // (3.3ba) 実行モードが0:通常の場合、処理(3.5.2)を実行後、戻り値に"0"を設定し処理を終了する。
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          // (3.3bb) 実行モードが1:ラストランの場合、処理(3.5.2)を実行後、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileName);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        processInsertFSData();

        // (3.5)FS実績登録テーブルを更新する。
        // (3.5.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0037.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());

        // (3.6)FSログ（来店ユーザ）CSVファイルのレコードの処理件数をログに出力する。
        String message =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
                BatchInfo.B18B0037.getBatchName(), numberProcessTarget, numberProcessTarget,
                Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT,
                Constants.DIRECTORY_KEY_MESS + targetDirectory);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));

        transactionCommit(BatchInfo.B18B0037.getBatchId());
      } catch (Exception e) {
        s3Client.close();
        myLog.error(e.getMessage(), e);
        transactionRollback(BatchInfo.B18B0037.getBatchId());
        transactionBegin(BatchInfo.B18B0037.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0037.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }
    s3Client.close();
    return ProcessResult.SUCCESS.getValue();
  }


  /**
   * @param row
   * @param fsVisitor
   * @return
   */
  public void insertFsVisitors(Map<String, String> row, FsVisitors fsVisitor) {
    // Set content file CSV to Entity
    fsVisitor.setFsVisitorId(fsVisitorsDAO.getSequenceNextVal(Constants.SEQ_FS_VISITOR_ID));
    fsVisitor.setAwTrackingId(row.get(COLUMN_CUSTOMER_ID));
    fsVisitor.setPopinfoId(row.get(COLUMN_POPINFO_ID));
    fsVisitor.setFsFacilityId(row.get(COLUMN_FACILITY_ID));
    if (!row.get(COLUMN_VISITED_AT).equals(Strings.EMPTY)) {
      fsVisitor.setVisitedAt(ConvertUtility
          .stringToTimestamp(row.get(COLUMN_VISITED_AT).substring(0, 19).replace("T", " ")));
    } else {
      fsVisitor.setVisitedAt(null);
    }
    fsVisitorsDAO.insert(fsVisitor);
    fsVisitorsDAO.detach(fsVisitor);
  }

  /**
   * @param key Key of the object to get
   * @param s3Client S3Client
   * @param s3FileName
   * @param cs Charset
   * @param hasHeader
   * @return
   * @throws SQLException 
   */
  public void processInsertFSData() throws SQLException {

    try (
        var gzipInputStream = new GZIPInputStream(
            new FileInputStream(Paths.get(downloadDirectory, s3FileName).toString()));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));) {
      CSVParser parser = null;

      // ヘッダーあり
      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);

      // (3.4.2)【FS来店ユーザ連携テーブル】にデータを登録する。
      var iter = parser.iterator();
      int index = 0;
      int commitCount = 0;

      FsVisitors fsVisitor = new FsVisitors();
      log.debug("insert start");
      transactionBegin(BatchInfo.B18B0037.getBatchId());
      while (iter.hasNext()) {
        index++;
        CSVRecord record = iter.next();
        insertFsVisitors(record.toMap(), fsVisitor);
        if (index == commitUnit) {
          myLog.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, index));
          transactionCommit(BatchInfo.B18B0037.getBatchId());
          transactionBegin(BatchInfo.B18B0037.getBatchId());
          index = 0;
        }
        numberProcessTarget++;
      }
      if (index != 0) {
        myLog.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, index));
      }
      transactionCommit(BatchInfo.B18B0037.getBatchId());
    } catch (IOException e) {
      myLog.debug(e.getMessage(), e);
    }
  }

  /**
   * Read file properties
   * 
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0037.getBatchId());

    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ */
    s3Directory = properties.getProperty(Constants.S3_DIRECTORY_IMPORT_VISITORS);

    /** AWS S3からダウンロードする際のファイル名 */
    s3FileName = properties.getProperty(Constants.S3_FILE_NAME_IMPORT_VISITORS);

    /** ダウンロードディレクトリ */
    downloadDirectory = properties.getProperty(Constants.DOWNLOAD_DIRECTORY_IMPORT_VISITORS);

    /** AWS S3からダウンロードして解凍したファイル名 */
    ungzFileName = properties.getProperty(Constants.UNGZ_FILE_NAME_IMPORT_VISITORS);

    /** 全件モードの場合のコミット単位 */
    commitUnit = ConvertUtility.stringToInteger(properties.getProperty(COMMIT_UNIT));
  }

  /**
   * Validate execute mode
   * 
   * @return true if 通常 or ラストラン
   */
  private boolean validate() {
    // Check 実行モード
    if (!Constants.GENERAL.equals(executeMode) && !Constants.LAST_RUN.equals(executeMode)) {
      return false;
    }
    // Check 実行日付
    if (executeDate != null && !executeDate.matches(Constants.REGEX_CHECK_FLAT_DATE)) {
      return false;
    }
    try {
      if (executeDate == null) {
        executeDate = DateUtils.getTodayAsString();
      } else {
        DateFormat dateFormat = new SimpleDateFormat(Constants.FLAT_FORMAT_DATE);
        java.util.Date utilDate = dateFormat.parse(executeDate);
        java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
        executeDate = DateUtils.toString(sqlDate);
      }
    } catch (ParseException e) {
      myLog.error(e.getMessage(), e);
      return false;
    }
    return true;
  }
}


