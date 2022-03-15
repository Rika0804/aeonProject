package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.batch.api.BatchProperty;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Status;
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
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.common.S3Utils;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsImportDataProcessMode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.MatchFlag;
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsSegmentDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsSegmentMatchUserDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsSegment;
import jp.co.aeoncredit.coupon.entity.FsSegmentMatchUser;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * バッチ B18B0038
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0038")
@Dependent
public class B18B0038 extends BatchFSApiCalloutBase {
  /** ログ */
  private Logger myLog = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0038.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0038.getBatchId());

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0038.getBatchId());

  /** AWS S3からダウンロードする際のバケット名 */
  private String s3BucketName;

  /** AWS S3からダウンロードする際のディレクトリ */
  private String s3Directory;

  /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー差分) */
  private String s3FileName;

  /** AWS S3からダウンロードする際のファイル名(セグメントマスタ差分) */
  private String s3MasterFileName;

  /** ダウンロードディレクトリ */
  private String downloadDirectory;

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー) */
  private String ungzFileName;

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ) */
  private String masterUngzFileName;

  /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー全件) */
  private String s3FileAllName;

  /** AWS S3からダウンロードする際のファイル名(セグメントマスタ全件) */
  private String s3MasterFileAllName;

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー全件) */
  private String ungzFileAllName;

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ全件) */
  private String masterUngzFileAllName;

  /** 全件モードの場合のコミット単位 */
  private Integer commitUnit;
  
  private int numberRecordInFileCsv = 0;

  private int numberRecordInsertSuccess = 0;

  private int numberRecordInDB = 0;

  private int numberRecordInsert = 0;

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
  @Inject
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FSセグメントマッチユーザー連携テーブル（FS_SEGMENT_MATCH_USER）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsSegmentMatchUserDAOCustomize fsSegmentMatchUserDAO;

  /** FSセグメント連携テーブル（FS_SEGMENT）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsSegmentDAOCustomize fsSegmentDAO;

  /** AWS S3からダウンロードする際のディレクトリ(セグメントマッチユーザー） */
  public static final String S3_DIRECTORY_DETECT_MATCH_USER =
      "fs.log.import.segment.match.user.s3.directory";

  /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー差分) */
  public static final String S3_FILE_NAME_DETECT_MATCH_USER_DIFF =
      "fs.log.import.segment.match.user.s3.file.name";

  /** AWS S3からダウンロードする際のファイル名(セグメントマスタ差分) */
  public static final String S3_MASTER_FILE_NAME_DETECT_MATCH_USER_DIFF =
      "fs.log.import.segment.match.user.s3.master.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_DETECT_MATCH_USER =
      "fs.log.import.segment.match.user.download.directory";

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー） */
  public static final String UNGZ_FILE_NAME_DETECT_MATCH_USER =
      "fs.log.import.segment.ungz.file.name";

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ) */
  public static final String MASTER_UNGZ_FILE_NAME_DETECT_MATCH_USER =
      "fs.log.import.segment.ungz.master.file.name";

  /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー全件) */
  public static final String S3_FILE_NAME_DETECT_MATCH_USER_ALL =
      "fs.log.import.segment.match.user.s3.file.all.name";

  /** AWS S3からダウンロードする際のファイル名(セグメントマスタ全件) */
  public static final String S3_MASTER_FILE_NAME_DETECT_MATCH_USER_ALL =
      "fs.log.import.segment.match.user.s3.master.file.all.name";

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー全件) */
  public static final String UNGZ_FILE_NAME_DETECT_MATCH_USER_ALL =
      "fs.log.import.segment.ungz.file.all.name";

  /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ全件) */
  public static final String MASTER_UNGZ_FILE_NAME_DETECT_MATCH_USER_ALL =
      "fs.log.import.segment.ungz.master.file.all.name";

  /** 全件モードの場合のコミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.segment.commit.unit";

  /** column customer_id in file csv match user */
  private static final String COLUMN_CUSTOMER_ID = "customer_id";

  /** column segment_id in file csv match user */
  private static final String COLUMN_SEGMENT_ID = "segment_id";

  /** column status in file csv match user */
  private static final String COLUMN_STATUS = "status";

  /** column segment_id in file csv master */
  private static final String COLUMN_MASTER_SEGMENT_ID = "segment_id";

  /** column name in file csv master */
  private static final String COLUMN_MASTER_NAME = "name";

  /** column status in file csv master */
  private static final String COLUMN_MASTER_STATUS = "status";

  /** Number of users that match the corresponding egment_id */
  private List<Object[]> userMatchSegmentIdList;

  /** FSセグメントID(FS_SEGMENT_ID) */
  public static final String FS_SEGMENT_ID = "fsSegmentId";

  /** イオンウォレットトラッキングID(AW_TRACKING_ID) */
  public static final String AW_TRACKING_ID = "awTrackingId";

  /** 削除フラグ(DELETE_FLAG) デフォルト値：0 */
  public static final String DELETE_FLAG = "deleteFlag";

  /** status del in file csv */
  private static final String TEXT_DELETE = "del";

  /** FSログ取込（セグメントマスタ） */
  private static final String SEGMENT_FS_LINKED_INSERT = "FSログ取込（セグメントマスタ）";

  /** 処理対象ディレクトリ */
  private static final String DIRECTORY_KEY_MESS = "処理対象ディレクトリ:";

  /** Insert message for each commit */
  private static final String INSERT_COMMIT_MESSAGE = "%s回目：%s件コミット成功";
  
  /** AWS S3 Utils */
  private S3Utils s3Utils;

  @Inject
  @BatchProperty
  /** 実行モード */
  String executeMode;

  @Inject
  @BatchProperty
  /** 差分/全件モード指定 */
  String modeSpecification;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0038.getBatchName()));

    // Export log execute mode and execute specification
    myLog.info(batchLogger.createMsg(Strings.EMPTY,
        String.format(Constants.FORMAT_EXPORT_MODE_LOG, Constants.EXECUTION_MODE, executeMode)
            + Constants.COMMA_FULL_SIZE + String.format(Constants.FORMAT_EXPORT_MODE_LOG,
                Constants.MODE_SPECIFICATION, modeSpecification)));

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

    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0038.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Read file properties
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0038.getBatchId());

    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ */
    s3Directory = properties.getProperty(S3_DIRECTORY_DETECT_MATCH_USER);

    /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー差分) */
    s3FileName = properties.getProperty(S3_FILE_NAME_DETECT_MATCH_USER_DIFF);

    /** AWS S3からダウンロードする際のファイル名(セグメントマスタ) */
    s3MasterFileName = properties.getProperty(S3_MASTER_FILE_NAME_DETECT_MATCH_USER_DIFF);

    /** ダウンロードディレクトリ */
    downloadDirectory = properties.getProperty(DOWNLOAD_DIRECTORY_DETECT_MATCH_USER);

    /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー）) */
    ungzFileName = properties.getProperty(UNGZ_FILE_NAME_DETECT_MATCH_USER);

    /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ) */
    masterUngzFileName = properties.getProperty(MASTER_UNGZ_FILE_NAME_DETECT_MATCH_USER);

    /** AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー全件) */
    s3FileAllName = properties.getProperty(S3_FILE_NAME_DETECT_MATCH_USER_ALL);

    /** AWS S3からダウンロードする際のファイル名(セグメントマスタ全件) */
    s3MasterFileAllName = properties.getProperty(S3_MASTER_FILE_NAME_DETECT_MATCH_USER_ALL);

    /** AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー全件) */
    ungzFileAllName = properties.getProperty(UNGZ_FILE_NAME_DETECT_MATCH_USER_ALL);

    /** AWS S3からダウンロードして解凍したファイル名(セグメントマスタ全件) */
    masterUngzFileAllName = properties.getProperty(MASTER_UNGZ_FILE_NAME_DETECT_MATCH_USER_ALL);

    /** 全件モードの場合のコミット単位 */
    commitUnit = ConvertUtility.stringToInteger(properties.getProperty(COMMIT_UNIT));
  }

  /**
   * Process batch
   * 
   * @return Process Result
   */
  private String processMain() {
    // (3)AWS認証
    // (3.1a) AWSの認証に成功した場合は、処理を継続する。
    S3Client s3Client = s3Utils.s3Client(Region.AP_NORTHEAST_1);
    if (s3Client == null) {
      // (3.1b) AWSの認証に失敗した場合は、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
      // Export log
      String message =
          String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB907.toString()),
              Constants.TEXT_AWS, Constants.NO_DETAIL);
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB907.toString(), message));
      return ProcessResult.FAILURE.getValue();
    }
    // (4)AWS S3ダウンロード
    return processDownloadAWSS3(s3Client);
  }

  /**
   * AWS S3ダウンロード
   * 
   * @param s3Client class S3Client
   * @return result process
   */
  private String processDownloadAWSS3(S3Client s3Client) {
    // (4a) 実行引数が"diff"（差分）の場合
    // 以下のディレクトリより、FS実績登録テーブルに未登録、または、処理済みフラグが0:未処理の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。
    // (4b) 実行引数が"full"（全件）の場合
    // 以下のディレクトリより、FS実績登録テーブルに未登録、または、処理済みフラグが0:未処理の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。

    String prefix = FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? s3Directory
        : s3Directory + DateUtils.getTodayAsString() + Constants.SYMBOL_SLASH;
    // AWS S3からダウンロードする際のファイル名
    String fileName = FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? s3FileName
        : s3FileAllName;
    // AWS S3からダウンロードして解凍したファイル名
    String ungzFile =
        FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? ungzFileName
            : ungzFileAllName;
    // AWS S3からダウンロードする際のファイル名
    String masterFileName =
        FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? s3MasterFileName
            : s3MasterFileAllName;
    // AWS S3からダウンロードする際のファイル名
    String ungzMasterFile =
        FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? masterUngzFileName
            : masterUngzFileAllName;

    // Get list directory from S3
    ListObjectsRequest s3RequestList =
        ListObjectsRequest.builder().bucket(s3BucketName).prefix(prefix).build();
    ListObjectsResponse s3ResponseList = s3Client.listObjects(s3RequestList);
    List<S3Object> s3objectList = s3ResponseList.contents();
    if (modeSpecification.equals(FsImportDataProcessMode.FULL.getValue())
        && s3objectList.isEmpty()) {
      try {
		prefix = s3Directory + s3Utils.subtractOneDay(DateUtils.getTodayAsString())
		      + Constants.SYMBOL_SLASH;
	  } catch (ParseException e) {
		myLog.error(e.getMessage(), e);
		return ProcessResult.FAILURE.getValue();
	  }
      s3RequestList = ListObjectsRequest.builder().bucket(s3BucketName).prefix(prefix).build();
      s3ResponseList = s3Client.listObjects(s3RequestList);
      s3objectList = s3ResponseList.contents();
    }
    Map<String, List<S3Object>> mapS3ObjectList =
        s3objectList.stream().collect(Collectors.groupingBy(s3Object -> s3Object.key().substring(0,
            s3Object.key().lastIndexOf(Constants.SYMBOL_SLASH) + 1)));
    // Sort by date asc
    Map<String, List<S3Object>> treeMap = new TreeMap<>(mapS3ObjectList);
    if (treeMap.size() == 0) {
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue();
      }
      return ProcessResult.FAILURE.getValue();
    }
    String targetDirectory = "";
    Map.Entry<String, List<S3Object>> lastEntry = null;
    for (Map.Entry<String, List<S3Object>> entry : treeMap.entrySet()) {
      lastEntry = entry;
      targetDirectory = entry.getKey();
      myLog.info(targetDirectory);
      String targetKey = s3Directory + Constants.FORMAT_FOLDER_S3;
      // Check format key
      if (targetDirectory.lastIndexOf(Constants.SYMBOL_SLASH) != targetKey.length() - 1) {
        continue;
      }
      try {
        // (4.1)FS実績登録テーブルに処理開始を登録する。
        fsResultsDAO.setBatchId(BatchInfo.B18B0038.getBatchId());
        transactionBegin(BatchInfo.B18B0038.getBatchId());
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue());
        transactionCommit(BatchInfo.B18B0038.getBatchId());
        if (!result) {
          continue;
        }

        // (4.2)不要ファイルを削除する。
        if (!s3Utils.deleteFile(fileName) || !s3Utils.deleteFile(ungzFile)
            || !s3Utils.deleteFile(masterFileName) || !s3Utils.deleteFile(ungzMasterFile)) {
          transactionBegin(BatchInfo.B18B0038.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0038.getBatchId());
          return ProcessResult.FAILURE.getValue();
        }
        
        // (4.3)FSログ（セグメントマッチユーザー）CSVをAWS S3からダウンロードし、ファイルを解凍する。
        if (!downloadFile(entry, s3Client, fileName)) {
          transactionBegin(BatchInfo.B18B0038.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0038.getBatchId());
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, fileName);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (4.4)FSログ（セグメントマスタ）CSVをAWS S3からダウンロードし、ファイルを解凍する。
        if (!downloadFile(entry, s3Client, masterFileName)) {
          transactionBegin(BatchInfo.B18B0038.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0038.getBatchId());
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, masterFileName);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (4.5)【FSセグメントマッチユーザー連携テーブル】登録
        processRegistFSSegmentMatchUser(targetDirectory);

        // (4.6)【FSセグメント連携テーブル】登録
        processRegistFSSegment(targetDirectory);

        // (4.7)FS実績登録テーブルにレコードを追加/更新する。
        // (4.7.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0038.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());
        transactionCommit(BatchInfo.B18B0038.getBatchId());

      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        try {
          if (userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            transactionRollback(BatchInfo.B18B0038.getBatchId());
          }
        } catch (SystemException e1) {
          myLog.error(e1.getMessage(), e1);
          return ProcessResult.FAILURE.getValue();
        }
        transactionBegin(BatchInfo.B18B0038.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0038.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }

    try {
      // Check folder last day, case directory does not exist
      if (!isPathLastday(targetDirectory, s3Utils.subtractOneDay(DateUtils.getTodayAsString()))) {
        if (Constants.GENERAL.equals(executeMode)) {
          return ProcessResult.SUCCESS.getValue();
        }
        return ProcessResult.FAILURE.getValue();
      }
    } catch (ParseException e) {
      myLog.error(e.getMessage(), e);
      return ProcessResult.FAILURE.getValue();
    }
    // Check file in folder lastday
    if (lastEntry != null && !enoughFileLastDay(lastEntry, fileName, masterFileName)) {
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue();
      }
      return ProcessResult.FAILURE.getValue();
    }
    return ProcessResult.SUCCESS.getValue();
  }

  /**
   * Check file in folder lastday
   * 
   * @param entry entry of map list s3Object
   * @param String masterFileName
   * @param String fileName
   * @return true when enough file in folder last day ortherwise false 
   */
  private boolean enoughFileLastDay(Map.Entry<String, List<S3Object>> entry, String masterFileName,
      String fileName) {
    int countFileInFolderLastDay = 0;
    for (S3Object s3Object : entry.getValue()) {
      String key = s3Object.key();
      // (4.1)不要ファイルを削除する。
      // セグメントマッチユーザー
      if (key.contains(fileName) || key.contains(masterFileName)) {
        countFileInFolderLastDay++;
      }
    }
    return countFileInFolderLastDay == 2;
  }

  
  /**
   * download file from s3
   * 
   * @param entry entry of map list s3Object
   * @param s3Client S3Client
   * @param fileName file name
   */
  private boolean downloadFile(Map.Entry<String, List<S3Object>> entry, S3Client s3Client,
      String fileName) {
    boolean checkDownloadFile = false;
    for (S3Object s3Object : entry.getValue()) {
      String key = s3Object.key();
      // (4.1)不要ファイルを削除する。
      // セグメントマッチユーザー
      if (key.contains(fileName)) {
        checkDownloadFile = s3Utils.downloadFileAWSS3(key, s3Client, fileName);
      }
    }
    return checkDownloadFile;
  }
  /**
   * count data in table FsSegmentMatchUser
   * 
   * @return list message for export log
   */
  private void countFsSegmentMatchUser() {
    userMatchSegmentIdList = sqlSelect(BatchInfo.B18B0038.getBatchId(), "countFsSegmentMatchUser");
    for (Object[] objects : userMatchSegmentIdList) {
      myLog.info(batchLogger.createMsg(Strings.EMPTY, String.format("[%s]: %s",
          ConvertUtility.objectToString(objects[0]), ConvertUtility.objectToString(objects[1]))));
    }
  }

  /**
   * Map content file CSV to Entity (FsSegmentMatchUser)
   * 
   * @param fsSegmentMatchUserInsertList list for insert
   */
  private boolean mapContentCSVToFsSegmentMatchUser(Map<String, String> row,
      FsSegmentMatchUser fsSegmentMatchUser) {
    // insertの場合、以下の条件で【FSセグメントマッチユーザー連携テーブル】を検索し、存在しない場合のみinsertを行う。
    if (FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        && checkExistFsSegmentMatchUser(row.get(COLUMN_SEGMENT_ID), row.get(COLUMN_CUSTOMER_ID))) {
      numberRecordInDB++;
      return false;
    }
    // Set content file CSV to Entity
    fsSegmentMatchUser.setAwTrackingId(row.get(COLUMN_CUSTOMER_ID));
    fsSegmentMatchUser.setFsSegmentId(Long.valueOf(row.get(COLUMN_SEGMENT_ID)));
    if (FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        && TEXT_DELETE.equals(row.get(COLUMN_STATUS))) {
      fsSegmentMatchUser.setDeleteFlag(DeleteFlag.DELETED.getValue());
    } else {
      fsSegmentMatchUser.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());
    }
    numberRecordInsert++;
    return true;
  }

  /**
   * Check exist data for table FsSegmentMatchUser
   * 
   * @param segmentId FSセグメントID
   * @param awTrackingId イオンウォレットトラッキングID(AW_TRACKING_ID)
   * @return true if exist data
   */
  private boolean checkExistFsSegmentMatchUser(String segmentId, String awTrackingId) {

    DAOParameter daoParam = new DAOParameter();
    daoParam.set(FS_SEGMENT_ID, segmentId);
    daoParam.set(AW_TRACKING_ID, awTrackingId);
    return fsSegmentMatchUserDAO.findOne(daoParam, null).isPresent();
  }

  /**
   * Map content file CSV to Entity (FsSegment)
   * 
   * @param fsSegmentInsertList list for insert
   */
  private boolean mapContentCSVToFsSegment(Map<String, String> row, FsSegment fsSegment) {
    // Set content file CSV to Entity
    fsSegment.setFsSegmentId(Long.valueOf(row.get(COLUMN_MASTER_SEGMENT_ID)));
    fsSegment.setFsSegmentName(row.get(COLUMN_MASTER_NAME));
    fsSegment.setFsCreateDate(DateUtils.now());
    fsSegment.setMatchFlag(MatchFlag.THERE_IS_A_MATCH_USER.getValue());
    boolean isExistNumberOfPeople = false;
    for (Object[] objects : userMatchSegmentIdList) {
      if (ConvertUtility.objectToString(objects[0]).equals(row.get(COLUMN_MASTER_SEGMENT_ID))) {
        fsSegment.setNumberOfPeople(ConvertUtility.objectToLong(objects[1]));
        isExistNumberOfPeople = true;
      }
    }
    if (!isExistNumberOfPeople) {
      fsSegment.setNumberOfPeople(null);
    }
    if (FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        && TEXT_DELETE.equals(row.get(COLUMN_MASTER_STATUS))) {
      fsSegment.setDeleteFlag(DeleteFlag.DELETED.getValue());
    } else {
      fsSegment.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());
    }
    if (FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        && checkExistFsSegment(row.get(COLUMN_MASTER_SEGMENT_ID))) {
      numberRecordInDB++;
      return false;
    }
    numberRecordInsert++;
    return true;
  }

  /**
   * Check exist data for table FsSegment
   * 
   * @param segmentId FSセグメントID
   * @return true if exist data
   */
  private boolean checkExistFsSegment(String segmentId) {

    DAOParameter daoParam = new DAOParameter();
    daoParam.set(FS_SEGMENT_ID, segmentId);
    return fsSegmentDAO.findOne(daoParam, null).isPresent();
  }

  /**
   * Insert data for table FSセグメントマッチユーザー連携テーブル
   * 
   * @param targetDirectory directory
   * @throws SQLException
   * @throws IOException
   */
  private void processRegistFSSegmentMatchUser(String targetDirectory)
      throws SQLException, IOException {
    // (4.4.1)全件の場合、【FSセグメントマッチユーザー連携テーブル】を全件DELETEする。
    if (FsImportDataProcessMode.FULL.getValue().equals(modeSpecification)) {
    	deleteTable("deleteAllFsSegmentMatchUser");
    }

    // (4.4.2)【FSセグメントマッチユーザー連携テーブル】にデータを登録する。
    String ungzFile = FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? s3FileName
        : s3FileAllName;
    int row = 0;
    int commitCount = 0;
    PreparedStatement preparedStatement = null;

    try (
        var gzipInputStream = new GZIPInputStream(
            new FileInputStream(Paths.get(downloadDirectory, ungzFile).toString()));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));) {
      CSVParser parser = null;

      // ヘッダーあり
      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);

      var iter = parser.iterator();
      // Init entity
      FsSegmentMatchUser fsSegmentMatchUser = new FsSegmentMatchUser();
      transactionBeginConnection(BatchInfo.B18B0038.getBatchId());
      preparedStatement =
          prepareStatement(BatchInfo.B18B0038.getBatchId(), "insertFsSegmentMatchUser");

      while (iter.hasNext()) {
        numberRecordInFileCsv++;
        CSVRecord csvRecord = iter.next();
        if (mapContentCSVToFsSegmentMatchUser(csvRecord.toMap(), fsSegmentMatchUser)) {
          row++;
          int index = 0;
          preparedStatement.setLong(++index, fsSegmentMatchUser.getFsSegmentId());
          preparedStatement.setString(++index, fsSegmentMatchUser.getAwTrackingId());
          preparedStatement.setString(++index, DeleteFlag.NOT_DELETED.getValue());
          preparedStatement.addBatch();
        }
        if (row == commitUnit) {
          preparedStatement.executeBatch();
          numberRecordInsertSuccess = numberRecordInsertSuccess + row;
          transactionCommitConnection(BatchInfo.B18B0038.getBatchId());
          log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row));
          transactionBeginConnection(BatchInfo.B18B0038.getBatchId());
          row = 0;
        }
      }
      if (row != 0) {
        preparedStatement.executeBatch();
        numberRecordInsertSuccess = numberRecordInsertSuccess + row;
        log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row));
      }
      transactionCommitConnection(BatchInfo.B18B0038.getBatchId());
      countFsSegmentMatchUser();

      // (4.4.2a.3)INSERTした件数、UPDATEした件数をログに出力する。
      String messageInsert =
          String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
              BatchInfo.B18B0038.getBatchName(), numberRecordInFileCsv, numberRecordInsertSuccess,
              numberRecordInsert - numberRecordInsertSuccess, numberRecordInDB,
              DIRECTORY_KEY_MESS + targetDirectory);
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), messageInsert));
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      transactionRollbackConnection(BatchInfo.B18B0038.getBatchId());
      String message = String.format(
          BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
      throw e;
    } finally {
      closeQuietly(null, preparedStatement);
      closeConnection(BatchInfo.B18B0038.getBatchId());
    }
  }

  /**
   * Insert data for table FSセグメント連携テーブル
   * 
   * @param targetDirectory directory
   * @throws SQLException
   * @throws IOException
   */
  private void processRegistFSSegment(String targetDirectory) throws SQLException, IOException {
    // (4.5)【FSセグメント連携テーブル】登録
    // (4.4.1)全件の場合、【FSセグメント連携テーブル】を以下の条件でDELETEする。
    if (FsImportDataProcessMode.FULL.getValue().equals(modeSpecification)) {
    	deleteTable("deleteFsSegmentBySegmentByPurposeFlag");
    }
    // (4.5.1)【FSセグメント連携テーブル】にFSログ（セグメントマスタ）CSVを INSERTする。
    // AWS S3からダウンロードする際のファイル名
    String ungzMasterFile =
        FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification) ? s3MasterFileName
            : s3MasterFileAllName;
    // (4.5.2)【FSセグメントマッチユーザー連携テーブル】にデータを登録する。
    int row = 0;
    int commitCount = 0;
    // case insert
    PreparedStatement preparedStatementInsert = null;
    // case update
    PreparedStatement preparedStatementUpdate = null;
    // boolean check
    boolean checkMapContent = true;
    try (
        var gzipInputStream = new GZIPInputStream(
            new FileInputStream(Paths.get(downloadDirectory, ungzMasterFile).toString()));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));) {
      CSVParser parser = null;
      // ヘッダーあり
      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);

      var iter = parser.iterator();
      // Init entity
      FsSegment fsSegment = new FsSegment();
      transactionBeginConnection(BatchInfo.B18B0038.getBatchId());
      preparedStatementInsert = prepareStatement(BatchInfo.B18B0038.getBatchId(), "insertFsSegment");
      preparedStatementUpdate = prepareStatement(BatchInfo.B18B0038.getBatchId(), "updateFsSegment");

      numberRecordInFileCsv = 0;
      numberRecordInsertSuccess = 0;
      numberRecordInDB = 0;
      numberRecordInsert = 0;
      int rowInsert = 0;
      while (iter.hasNext()) {
        numberRecordInFileCsv++;
        CSVRecord csvRecord = iter.next();
        checkMapContent = mapContentCSVToFsSegment(csvRecord.toMap(), fsSegment);
        row++;
        int index = 0;
        if (checkMapContent) {
          preparedStatementInsert.setLong(++index, fsSegment.getFsSegmentId());
          preparedStatementInsert.setString(++index, fsSegment.getFsSegmentName());
          preparedStatementInsert.setString(++index, fsSegment.getSegmentByPurposeFlag());
          preparedStatementInsert.setTimestamp(++index, fsSegment.getFsCreateDate());
          if (fsSegment.getNumberOfPeople() == null) {
            preparedStatementInsert.setNull(++index, java.sql.Types.NULL);
          } else {
            preparedStatementInsert.setLong(++index, fsSegment.getNumberOfPeople());
          } 
          preparedStatementInsert.setString(++index, fsSegment.getMatchFlag());
          preparedStatementInsert.setString(++index, DeleteFlag.NOT_DELETED.getValue());
          preparedStatementInsert.addBatch();
          rowInsert++;
        } else {
          preparedStatementUpdate.setString(++index, fsSegment.getFsSegmentName());
          preparedStatementUpdate.setString(++index, fsSegment.getSegmentByPurposeFlag());
          preparedStatementUpdate.setTimestamp(++index, fsSegment.getFsCreateDate());
          if (fsSegment.getNumberOfPeople() == null) {
            preparedStatementUpdate.setNull(++index, java.sql.Types.NULL);
          } else {
            preparedStatementUpdate.setLong(++index, fsSegment.getNumberOfPeople());
          }  
          preparedStatementUpdate.setString(++index, fsSegment.getMatchFlag());
          preparedStatementUpdate.setString(++index, fsSegment.getDeleteFlag());
          preparedStatementUpdate.setLong(++index, fsSegment.getFsSegmentId());
          preparedStatementUpdate.addBatch();
        }
        if (row == commitUnit) {
            preparedStatementInsert.executeBatch();
            preparedStatementUpdate.executeBatch();
          
          numberRecordInsertSuccess = numberRecordInsertSuccess + rowInsert;
          transactionCommitConnection(BatchInfo.B18B0038.getBatchId());
          log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row));
          transactionBeginConnection(BatchInfo.B18B0038.getBatchId());
          row = 0;
        }
      }
      if (row != 0) {
          preparedStatementInsert.executeBatch();
          preparedStatementUpdate.executeBatch();
        
        numberRecordInsertSuccess = numberRecordInsertSuccess + rowInsert;
        log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row));
      }
      transactionCommitConnection(BatchInfo.B18B0038.getBatchId());

      // (4.5.2a.1)BULK INSERTに成功した場合は以下(4.4.2a.1)以降の処理を行う。
      String messageInsert =
          String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
              SEGMENT_FS_LINKED_INSERT, numberRecordInFileCsv, numberRecordInsertSuccess,
              numberRecordInsert - numberRecordInsertSuccess, numberRecordInDB,
              DIRECTORY_KEY_MESS + targetDirectory);
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), messageInsert));
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      transactionRollbackConnection(BatchInfo.B18B0038.getBatchId());
      String message = String.format(
          BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
      throw e;
    } finally {
        closeQuietly(null, preparedStatementInsert);
        closeQuietly(null, preparedStatementUpdate);
      
      closeConnection(BatchInfo.B18B0038.getBatchId());
    }
  }

  /**
   * Check parameter
   * 
   * @return true if invalid
   */
  private boolean validate() {
    // Check 実行モード
    if (!Constants.GENERAL.equals(executeMode) && !Constants.LAST_RUN.equals(executeMode)) {
      return false;
    }
    // Check 差分/全件モード指定
    return FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        || FsImportDataProcessMode.FULL.getValue().equals(modeSpecification);
  }
  
	/**
	 * FSセグメント連携テーブルまたはFSセグメントマッチユーザー連携テーブルを削除
	 * @param sqlId 
	 * @throws SQLException 
	 */
	private void deleteTable(String sqlId) throws SQLException {
		
	    int rows = 0;
		
		do {
	
	    	PreparedStatement preparedStatement = null;
			int index = 0;
			try {			
				transactionBeginConnection(BatchInfo.B18B0038.getBatchId());
				preparedStatement = prepareStatement(BatchInfo.B18B0038.getBatchId(), sqlId);
				preparedStatement.setInt(++index, commitUnit);
	
				rows = preparedStatement.executeUpdate();
				
				transactionCommitConnection(BatchInfo.B18B0038.getBatchId());
				
			} catch (Exception e) {
				transactionRollbackConnection(BatchInfo.B18B0038.getBatchId());
			    String message = String.format(
			    		BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
			    myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
			    throw e;
			} finally {
				closeQuietly(null, preparedStatement);
			    closeConnection(BatchInfo.B18B0038.getBatchId());
			}
	
		} while (commitUnit == rows);
	
	}
	
	  /**
	   * Check empty folder and file in S3
	   * 
	   * @param keyList: List path get in S3
	   * @param date: executeDate
	   * @return true when have file and folder with executeDate in S3 ortherwise return false
	   */
	  private boolean isPathLastday(String key, String date) {
	    String[] keyListItem = key.split("/");
	    int lastDay = 0;
	    int lastMonth = 0;
	    int lastYear = 0;
	    try {
	      lastDay = Integer.parseInt(keyListItem[keyListItem.length-1]);
	      lastMonth = Integer.parseInt(keyListItem[keyListItem.length-2]);
	      lastYear = Integer.parseInt(keyListItem[keyListItem.length-3]);
	    } catch (NumberFormatException e) {
	      return false;
	    }
	    // Get last day
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	    Calendar cal = Calendar.getInstance();
	    try {
	      cal.setTime(dateFormat.parse(date));
	    } catch (ParseException e2) {
	      myLog.error(e2.getMessage(), e2);
	      return false;
	    }

	    return (lastDay == cal.get(Calendar.DAY_OF_MONTH) && lastMonth== (cal.get(Calendar.MONTH)+1) && lastYear == cal.get(Calendar.YEAR));
	  }
}
