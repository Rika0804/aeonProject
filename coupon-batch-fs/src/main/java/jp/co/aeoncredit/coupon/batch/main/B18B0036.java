package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsFacilityType;
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.dao.custom.FsFacilitiesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsFacilities;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * バッチ B18B0036
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0036")
@Dependent
public class B18B0036 extends BatchDBAccessBase {

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
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0036.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0036.getBatchId());

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0036.getBatchId());

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

  /** FS登録店舗データ連携テーブル（FS_FACILITIES）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsFacilitiesDAOCustomize fsFacilitiesDAO;

  /** column id in file csv */
  private static final String COLUMN_ID = "id";

  /** column name in file csv */
  private static final String COLUMN_NAME = "name";

  /** column wkt in file csv */
  private static final String COLUMN_WKT = "wkt";

  /** column address in file csv */
  private static final String COLUMN_ADDRESS = "address";

  /** column radius in file csv */
  private static final String COLUMN_RADIUS = "radius";

  /** column type in file csv */
  private static final String COLUMN_TYPE = "type";

  /** column tags in file csv */
  private static final String COLUMN_TAGS = "tags";

  /** column area_name in file csv */
  private static final String COLUMN_AREA_NAME = "area_name";

  /** 処理対象件数 */
  private int numberProcessTarget;

  /** 全件モードの場合のコミット単位 */
  private Integer commitUnit;

  /** Insert message for each commit */
  private static final String INSERT_COMMIT_MESSAGE = "%s回目：%s件コミット成功";

  /** content type own in file csv */
  private static final String TEXT_OWN = "own";

  /** content type rival in file csv */
  private static final String TEXT_RIVAL = "rival";

  /** AWS S3からダウンロードする際のディレクトリ */
  public static final String S3_DIRECTORY_FACILITIES = "fs.log.import.facilities.s3.directory";

  /** AWS S3からダウンロードする際のファイル名 */
  public static final String S3_FILE_NAME_FACILITIES = "fs.log.import.facilities.s3.file.name";

  /** ダウンロードディレクトリ */
  public static final String DOWNLOAD_DIRECTORY_FACILITIES =
      "fs.log.import.facilities.download.directory";

  /** AWS S3からダウンロードして解凍したファイル名 */
  public static final String UNGZ_FILE_NAME_FACILITIES = "fs.log.import.facilities.ungz.file.name";

  /** 全件モードの場合のコミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.facilities.commit.unit";

  /** AWS S3 Utils */
  private S3Utils s3Utils;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {
    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0036.getBatchName()));

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

    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0036.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Read file properties
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0036.getBatchId());

    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ */
    s3Directory = properties.getProperty(S3_DIRECTORY_FACILITIES);

    /** AWS S3からダウンロードする際のファイル名 */
    s3FileName = properties.getProperty(S3_FILE_NAME_FACILITIES);

    /** ダウンロードディレクトリ */
    downloadDirectory = properties.getProperty(DOWNLOAD_DIRECTORY_FACILITIES);

    /** AWS S3からダウンロードして解凍したファイル名 */
    ungzFileName = properties.getProperty(UNGZ_FILE_NAME_FACILITIES);

    /** 全件モードの場合のコミット単位 */
    commitUnit = ConvertUtility.stringToInteger(properties.getProperty(COMMIT_UNIT));
  }

  /**
   * Process FS登録店舗データ連携テーブル
   * 
   * @return 処理結果
   */
  @SuppressWarnings("resource")
  private String processMain() {
    // (2)AWS認証
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
    String prefix = s3Directory + executeDate + Constants.SYMBOL_SLASH;
    List<String> keyList = s3Utils.getKeyList(prefix, s3Client, s3Directory);
    if (executeDate.equals(DateUtils.getTodayAsString()) && keyList.isEmpty()) {
      try {
        prefix = s3Directory + s3Utils.subtractOneDay(DateUtils.getTodayAsString())
            + Constants.SYMBOL_SLASH;
	  } catch (ParseException e) {
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
      // FS実績登録テーブルに処理開始を登録する。
      // 処理中に更新してから進む。
      transactionBegin(BatchInfo.B18B0036.getBatchId());
      String targetDirectory = key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
      myLog.info("processMain.targetDirectory: " + targetDirectory);
      try {
        fsResultsDAO.setBatchId(BatchInfo.B18B0036.getBatchId());
        // (3.1)FS実績登録テーブルに処理開始を登録する。
        if (!fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue())) {
          transactionCommit(BatchInfo.B18B0036.getBatchId());
          return ProcessResult.SUCCESS.getValue();
        }

        // (3.1)不要ファイルを削除する。
        // (3.2)FSログ（登録店舗データ）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。
        if (!s3Utils.deleteFile(s3FileName) || !s3Utils.deleteFile(ungzFileName)) {
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0036.getBatchId());
          return ProcessResult.FAILURE.getValue();
        }

        // (3.3)【FS登録店舗データ連携テーブル】登録
        transactionCommit(BatchInfo.B18B0036.getBatchId());

        if (!key.endsWith(s3FileName)) {
          if (countKey == keyList.size()) {
            if (Constants.GENERAL.equals(executeMode)) {
              return ProcessResult.SUCCESS.getValue(); 
            }
            return ProcessResult.FAILURE.getValue(); 
          }
          continue;
        }
        boolean resultImport =
            downloadAndImportCSV(key, s3Client, s3FileName, StandardCharsets.UTF_8);
        if (!resultImport) {
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0036.getBatchId());
          // (3.2ba) 実行モードが0:通常の場合、処理(3.5.2)を実行後、戻り値に"0"を設定し処理を終了する。
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          // (3.2bb) 実行モードが1:ラストランの場合、処理(3.5.2)を実行後、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileName);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (3.4)FS実績登録テーブルにレコードを追加/更新する。
        // (3.4.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0036.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());

        // (3.5)FSログ（来店ユーザ）CSVファイルのレコードの処理件数をログに出力する。
        String message =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
                BatchInfo.B18B0036.getBatchName(), numberProcessTarget, numberProcessTarget,
                Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT,
                Constants.DIRECTORY_KEY_MESS + targetDirectory);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));

        transactionCommit(BatchInfo.B18B0036.getBatchId());

      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        transactionRollback(BatchInfo.B18B0036.getBatchId());
        transactionBegin(BatchInfo.B18B0036.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0036.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }
    return ProcessResult.SUCCESS.getValue();
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
      throws Exception {
    // (3.3.1)【FS登録店舗データ連携テーブル】を全件DELETEする。
    // Delete all record 【FS登録店舗データ連携テーブル】
    transactionBegin(BatchInfo.B18B0036.getBatchId());
    sqlExecute(BatchInfo.B18B0036.getBatchId(), "deleteFsFacilities");
    transactionCommit(BatchInfo.B18B0036.getBatchId());
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
      FsFacilities fsFacilities = new FsFacilities();

      // (3.3)(3.2)で解凍した「events_coupon.csv」のレコード分繰り返す。
      log.debug("insert start");
      transactionBegin(BatchInfo.B18B0036.getBatchId());
      while (iter.hasNext()) {
        index++;
        // 処理対象件数
        numberProcessTarget++;
        CSVRecord record = iter.next();

        // (3.3.2)【FS登録店舗データ連携テーブル】にデータを登録する。
        insertFsFacilities(record.toMap(), fsFacilities);
        if (index == commitUnit) {
          log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, index));
          transactionCommit(BatchInfo.B18B0036.getBatchId());
          transactionBegin(BatchInfo.B18B0036.getBatchId());
          index = 0;
        }
      }
      if (index != 0) {
        log.info(String.format(INSERT_COMMIT_MESSAGE, ++commitCount, index));
      }
      transactionCommit(BatchInfo.B18B0036.getBatchId());
      log.debug("insert end");
      return true;
    } catch (IOException e) {
      log.debug(e.getMessage(), e);
      String message = String.format(
          BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
      return false;
    }
  }

  /**
   * (3.3.2)【FS登録店舗データ連携テーブル】登録
   * 
   * @param row
   * @param Entity FsFacilities fsFacilities
   */
  private void insertFsFacilities(Map<String, String> row, FsFacilities fsFacilities) {

    fsFacilities.setFsFacilityId(row.get(COLUMN_ID));
    fsFacilities.setFsFacilityName(row.get(COLUMN_NAME));
    fsFacilities.setFsFacilityLongitudeAndLatitude(row.get(COLUMN_WKT));
    fsFacilities.setFsFacilityAddress(row.get(COLUMN_ADDRESS));
    String fsFacilityRadius = row.get(COLUMN_RADIUS).indexOf(".") < 0 ? row.get(COLUMN_RADIUS)
        : row.get(COLUMN_RADIUS).substring(0, row.get(COLUMN_RADIUS).indexOf("."));
    fsFacilities.setFsFacilityRadius(Short.valueOf(fsFacilityRadius));
    if ((TEXT_OWN).equals(row.get(COLUMN_TYPE))) {
      fsFacilities.setFsFacilityType(FsFacilityType.OWN.getValue());
    } else if ((TEXT_RIVAL).equals(row.get(COLUMN_TYPE))) {
      fsFacilities.setFsFacilityType(FsFacilityType.RIVAL.getValue());
    } else {
      fsFacilities.setFsFacilityType(null);
    }
    fsFacilities.setFsFacilityTags(row.get(COLUMN_TAGS));
    fsFacilities.setFsFacilityAreaName(row.get(COLUMN_AREA_NAME));
    fsFacilities.setDeleteFlag(DeleteFlag.NOT_DELETED.getValue());
    fsFacilitiesDAO.insert(fsFacilities);
    fsFacilitiesDAO.detach(fsFacilities);
  }
}
