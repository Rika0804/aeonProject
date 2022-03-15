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
import java.util.List;
import java.util.Properties;
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
import jp.co.aeoncredit.coupon.batch.constants.FsImportDataProcessMode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.TreatedFlag;
import jp.co.aeoncredit.coupon.dao.custom.FsIdlinkDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsIdlink;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * バッチ B18B0039
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0039")
@Dependent
public class B18B0039 extends BatchDBAccessBase {

  /** colum customer_id in file csv */
  private static final String COLUMN_CUSTOMER_ID = "customer_id";

  /** colum popinfo_id in file csv */
  private static final String COLUMN_POPINFO_ID = "popinfo_id";

  /** colum diff_id in file csv */
  private static final String COLUMN_DIFF_ID = "diff_id";

  /** diff_id "U" */
  private static final String DIFF_ID_UPDATE = "U";

  /** diff_id "D" */
  private static final String DIFF_ID_DELETE = "D";

  /** ログ */
  private Logger myLog = getLogger();

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0039.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0039.getBatchId());

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0039.getBatchId());

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
  @Inject
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FSひも付きデータ連携テーブル（FS_IDLINK）Entityのカスタマイズ用DAOクラス。 */
  @Inject
  protected FsIdlinkDAOCustomize fsIdlinkDAO;

  /** 処理対象件数 */
  private Integer numberProcessTarget;

  /** 引数 */
  @Inject
  @BatchProperty
  public String modeSpecification;

  /** 引数 */
  @Inject
  @BatchProperty
  public String executeMode;

  /** AWS S3からダウンロードする際のバケット名 */
  private String s3BucketName;

  /** AWS S3からダウンロードする際のディレクトリ（差分） */
  private String s3DirectoryDiff;

  /** AWS S3からダウンロードする際のファイル名（差分） */
  private String s3FileNameDiff;

  /** AWS S3からダウンロードして解凍したファイル名（差分） */
  private String s3UngzFileNameDiff;

  /** AWS S3からダウンロードする際のディレクトリ（全量） */
  private String s3DirectoryFull;

  /** AWS S3からダウンロードする際のファイル名（全量） */
  private String s3FileNameFull;

  /** AWS S3からダウンロードして解凍したファイル名（全量） */
  private String s3UngzFileNameFull;

  /** ダウンロードディレクトリ */
  private String downloadDirectory;

  /** AWS S3 Utils */
  private S3Utils s3Utils;

  /** 全件モードの場合のコミット単位 */
  private Integer commitUnit;

  /** Insert message for each commit */
  private static final String INSERT_COMMIT_MESSAGE = "%s回目：%s件コミット成功";

  /** 全件モードの場合のコミット単位 */
  public static final String COMMIT_UNIT = "fs.log.import.idlink.commit.unit";

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {

    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0039.getBatchName()));

    // Export log execute mode and execute specification
    myLog.info(batchLogger.createMsg(Strings.EMPTY,
        String.format(Constants.FORMAT_EXPORT_MODE_LOG, Constants.EXECUTION_MODE, executeMode)
            + Constants.COMMA_FULL_SIZE + String.format(Constants.FORMAT_EXPORT_MODE_LOG,
                Constants.MODE_SPECIFICATION, modeSpecification)));

    // (1.1)引数チェック
    if (!FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)
        && !FsImportDataProcessMode.FULL.getValue().equals(modeSpecification)
        || (!Constants.GENERAL.equals(executeMode) && !Constants.LAST_RUN.equals(executeMode))) {
      // 引数が "diff" または "full" 以外の場合、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
      myLog.error(
          String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB919.toString())));
      return setExitStatus(ProcessResult.FAILURE.getValue());
    }

    // プロパティファイルの読み込み
    readProperties();

    // AWS S3 Utils
    s3Utils = new S3Utils(s3BucketName, downloadDirectory, myLog, batchLogger, batchFileHandler);

    // バッチの起動メイン処理 機能概要 : FSログ取込（ひも付きデータ）
    String processResult = processMain();

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0039.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * Process batch FSログ取込（ひも付きデータ）
   * 
   * @return 処理結果
   */
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
    // (3a) 実行引数が"diff"（差分）の場合
    if (FsImportDataProcessMode.DIFF.getValue().equals(modeSpecification)) {
      return processDownloadAWSS3ModeDiff(s3Client);
    }
    // (3b) 実行引数が"full"（全件）の場合
    return processDownloadAWSS3ModeFull(s3Client);
  }

  /**
   * AWS S3ダウンロード mode full
   * 
   * @param s3Client class S3Client
   * @return result process
   */
  private String processDownloadAWSS3ModeFull(S3Client s3Client) {
    // 以下のディレクトリより、FS実績登録テーブルに未登録もしくは登録済みで処理済みフラグが0の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。
    // Get directory from S3
    String prefix = s3DirectoryFull + DateUtils.getTodayAsString() + Constants.SYMBOL_SLASH;
    List<String> keyList = s3Utils.getKeyList(prefix, s3Client, s3DirectoryFull);
    if (keyList.isEmpty()) {
      try {
		prefix = s3DirectoryFull + s3Utils.subtractOneDay(DateUtils.getTodayAsString())
		      + Constants.SYMBOL_SLASH;
	  } catch (ParseException e) {
		myLog.error(e.getMessage(), e);
		return ProcessResult.FAILURE.getValue();
	  }
      keyList = s3Utils.getKeyList(prefix, s3Client, s3DirectoryFull);
    }
    
    if (keyList.isEmpty()) {
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue(); 
      }
      return ProcessResult.FAILURE.getValue(); 
    }
    int countKey = 0;
    for (String key : keyList) {
      countKey++;
      // (3.1)FS実績登録テーブルに処理開始を登録する。
      String targetDirectory = key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
      myLog.info("processDownloadAWSS3ModeFull.targetDirectory: " + targetDirectory);
      try {
        // (3.1)FS実績登録テーブルに処理開始を登録する。
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.setBatchId(BatchInfo.B18B0039.getBatchId());
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue());
        transactionCommit(BatchInfo.B18B0039.getBatchId());
        if (!result) {
          return ProcessResult.SUCCESS.getValue();
        }

        // (3.1)不要ファイルを削除する。
        // (3.2)FSログ（ひも付きデータ）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。
        if (!s3Utils.deleteFile(s3FileNameFull) || !s3Utils.deleteFile(s3UngzFileNameFull)) {
          transactionBegin(BatchInfo.B18B0039.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0039.getBatchId());
          return ProcessResult.FAILURE.getValue();
        }

        if (!key.endsWith(s3FileNameFull)) {
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
        if (!s3Utils.downloadFileAWSS3(key, s3Client, s3FileNameFull)) {
          transactionBegin(BatchInfo.B18B0039.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0039.getBatchId());
          // (3.3ba) 実行モードが0:通常の場合、処理(3.5.2)を実行後、戻り値に"0"を設定し処理を終了する。
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          // (3.3bb) 実行モードが1:ラストランの場合、処理(3.5.2)を実行後、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileNameFull);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (3.3)【FSひも付きデータ連携テーブル】登録
        importCSVModeFull();

        // (3.4)FS実績登録テーブルにレコードを追加/更新する。
        // (3.4.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());

        // (3.5b)全件の場合、FSログ（ひも付きデータ）CSVファイルのレコードの処理件数をログに出力する。
        String message =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
                BatchInfo.B18B0039.getBatchName(), numberProcessTarget, numberProcessTarget,
                Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT,
                Constants.DIRECTORY_KEY_MESS + targetDirectory);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));

        transactionCommit(BatchInfo.B18B0039.getBatchId());
      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        try {
          if (userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            transactionRollback(BatchInfo.B18B0039.getBatchId());
          }
        } catch (SystemException e1) {
          myLog.error(e1.getMessage(), e1);
          return ProcessResult.FAILURE.getValue();
        }
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0039.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }
    return ProcessResult.SUCCESS.getValue();
  }

  /**
   * AWS S3ダウンロード mode diff
   * 
   * @param s3Client class S3Client
   * @return result process
   */
  private String processDownloadAWSS3ModeDiff(S3Client s3Client) {
    // 以下のディレクトリより、FS実績登録テーブルに未登録もしくは登録済みで処理済みフラグが0の全ディレクトリを対象に
    // ディレクトリの日付の昇順に以下の処理を実施する。
    // Get list directory from S3
    List<String> keyList = s3Utils.getKeyList(s3DirectoryDiff, s3Client, s3DirectoryDiff);
    
    if (keyList.isEmpty()) {
      if (Constants.GENERAL.equals(executeMode)) {
        return ProcessResult.SUCCESS.getValue(); 
      }
      return ProcessResult.FAILURE.getValue(); 
    }
    for (String key : keyList) {
      String targetDirectory = key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1);
      myLog.info("processDownloadAWSS3ModeDiff.targetDirectory: " + targetDirectory);
      try {
        // (3.1)FS実績登録テーブルに処理開始を登録する。
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.setBatchId(BatchInfo.B18B0039.getBatchId());
        boolean result = fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.PROCESSING.getValue());
        transactionCommit(BatchInfo.B18B0039.getBatchId());
        // check treated flag || type name
        if (!result || !key.endsWith(s3FileNameDiff)) {
          continue;
        }

        // (3.1)不要ファイルを削除する。
        // (3.2)FSログ（ひも付きデータ）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。
        if (!s3Utils.deleteFile(s3FileNameDiff) || !s3Utils.deleteFile(s3UngzFileNameDiff)) {
          transactionBegin(BatchInfo.B18B0039.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0039.getBatchId());
          continue;
        }
        // AWS S3からダウンロードする際のファイル名
        // AWS S3からダウンロードして解凍したファイル名
        // (3.3)FSログ（来店ユーザ）CSVのgzをAWS S3からダウンロードし、ファイルを解凍する。
        // (3.3a) ダウンロードに成功した場合は、処理を継続する。
        if (!s3Utils.downloadFileAWSS3(key, s3Client, s3FileNameDiff)) {
          transactionBegin(BatchInfo.B18B0039.getBatchId());
          fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
              TreatedFlag.UNTREATED.getValue());
          transactionCommit(BatchInfo.B18B0039.getBatchId());
          // (3.3ba) 実行モードが0:通常の場合、処理(3.5.2)を実行後、戻り値に"0"を設定し処理を終了する。
          if (Constants.GENERAL.equals(executeMode)) {
            return ProcessResult.SUCCESS.getValue();
          }
          // (3.3bb) 実行モードが1:ラストランの場合、処理(3.5.2)を実行後、戻り値に"1"を設定し、エラーメッセージを出力後処理を終了する。
          String message =
              String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB932.toString()),
                  s3BucketName, s3FileNameDiff);
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB932.toString(), message));
          return ProcessResult.FAILURE.getValue();
        }

        // (3.3)【FSひも付きデータ連携テーブル】登録
        importCSVModeDiff();

        // (3.4)FS実績登録テーブルにレコードを追加/更新する。
        // (3.4.1) 処理正常の場合
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.updateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.TREATED.getValue());

        // (3.5a)差分の場合、FSログ（ひも付きデータ）CSVファイルのレコードの処理件数をログに出力後、次の処理対象ディレクトリへ
        String message =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
                BatchInfo.B18B0039.getBatchName(), numberProcessTarget, numberProcessTarget,
                Constants.DEFAULT_COUNT, Constants.DEFAULT_COUNT,
                Constants.DIRECTORY_KEY_MESS + targetDirectory);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));

        transactionCommit(BatchInfo.B18B0039.getBatchId());
      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        try {
          if (userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
            transactionRollback(BatchInfo.B18B0039.getBatchId());
          }
        } catch (SystemException e1) {
          myLog.error(e1.getMessage(), e1);
          return ProcessResult.FAILURE.getValue();
        }
        transactionBegin(BatchInfo.B18B0039.getBatchId());
        fsResultsDAO.registOrUpdateFSResultsWithTreatedFlag(targetDirectory,
            TreatedFlag.UNTREATED.getValue());
        transactionCommit(BatchInfo.B18B0039.getBatchId());
        return ProcessResult.FAILURE.getValue();
      }
    }
    String lastKey = keyList.get(keyList.size() - 1);
    try {
      // Check file path or file doesnot exist
      if (!lastKey.endsWith(s3FileNameDiff) || !s3Utils.checkPathLastday(keyList,
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
   * Check Exist PopinfoId
   * 
   * @param popinfoId POPINFO_ID
   * @return true if exist
   */
  private boolean checkExistPopinfoId(String popinfoId) {
    FsIdlink fsIdlink = fsIdlinkDAO.findById(popinfoId).orElse(null);
    return fsIdlink != null;
  }

  /**
   * import file csv mode full
   * 
   * @throws SQLException
   * @throws IOException
   */
  private void importCSVModeFull() throws SQLException, IOException {
    // (3.3.1)全件の場合、【FSひも付きデータ連携テーブル】を全件DELETEする。
	deleteAllFsIdlink();

    String dstPath = Paths.get(downloadDirectory, s3FileNameFull).toString();

    PreparedStatement preparedStatement = null;

    try (var gzipInputStream = new GZIPInputStream(new FileInputStream(dstPath));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));) {
      CSVParser parser = null;

      // ヘッダーあり
      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);
      var iter = parser.iterator();
      int row = 0;
      int commitCount = 0;
      numberProcessTarget = 0;

      transactionBeginConnection(BatchInfo.B18B0039.getBatchId());
      preparedStatement = prepareStatement(BatchInfo.B18B0039.getBatchId(), "insertFsIdlink");

      while (iter.hasNext()) {
        numberProcessTarget++;
        row++;
        CSVRecord csvRecord = iter.next();
        int index = 0;
        preparedStatement.setString(++index, csvRecord.toMap().get(COLUMN_POPINFO_ID));
        preparedStatement.setString(++index, csvRecord.toMap().get(COLUMN_CUSTOMER_ID));
        preparedStatement.addBatch();
        if (row == commitUnit) {
          preparedStatement.executeBatch();
          transactionCommitConnection(BatchInfo.B18B0039.getBatchId());
          String mess = String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row);
          myLog.info(mess);
          transactionBeginConnection(BatchInfo.B18B0039.getBatchId());
          row = 0;
        }
      }
      if (row != 0) {
        preparedStatement.executeBatch();
        String mess = String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row);
        myLog.info(mess);
      }
      transactionCommitConnection(BatchInfo.B18B0039.getBatchId());
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      transactionRollbackConnection(BatchInfo.B18B0039.getBatchId());
      // クエリに失敗した場合はエラーメッセージを出力し、戻り値に"1"を設定し処理を終了する。
      String message = String.format(
          BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
      throw e;
    } finally {
      closeQuietly(null, preparedStatement);
      closeConnection(BatchInfo.B18B0039.getBatchId());
    }
  }

  /**
   * import file csv mode diff
   * 
   * @throws SQLException
   * @throws IOException
   */
  private void importCSVModeDiff() throws SQLException, IOException {
    String dstPath = Paths.get(downloadDirectory, s3FileNameDiff).toString();

    PreparedStatement preInsert = null;
    PreparedStatement preUpdate = null;
    PreparedStatement preDelete = null;

    try (var gzipInputStream = new GZIPInputStream(new FileInputStream(dstPath));
        BufferedReader br =
            new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));) {
      CSVParser parser = null;

      // ヘッダーあり
      parser = CSVFormat.EXCEL.withIgnoreEmptyLines(true).withFirstRecordAsHeader()
          .withIgnoreSurroundingSpaces(true).parse(br);
      var iter = parser.iterator();
      int row = 0;
      int commitCount = 0;
      numberProcessTarget = 0;

      transactionBeginConnection(BatchInfo.B18B0039.getBatchId());
      preInsert = prepareStatement(BatchInfo.B18B0039.getBatchId(), "insertFsIdlink");
      preUpdate = prepareStatement(BatchInfo.B18B0039.getBatchId(), "updateFsIdlink");
      preDelete = prepareStatement(BatchInfo.B18B0039.getBatchId(), "deleteFsIdlink");

      while (iter.hasNext()) {
        numberProcessTarget++;
        row++;
        CSVRecord csvRecord = iter.next();
        int index = 0;
        if (DIFF_ID_UPDATE.equals(csvRecord.toMap().get(COLUMN_DIFF_ID))) {
          if (checkExistPopinfoId(csvRecord.toMap().get(COLUMN_POPINFO_ID))) {
            preUpdate.setString(++index, csvRecord.toMap().get(COLUMN_CUSTOMER_ID));
            preUpdate.setString(++index, csvRecord.toMap().get(COLUMN_POPINFO_ID));
            preUpdate.addBatch();
          } else {
            preInsert.setString(++index, csvRecord.toMap().get(COLUMN_POPINFO_ID));
            preInsert.setString(++index, csvRecord.toMap().get(COLUMN_CUSTOMER_ID));
            preInsert.addBatch();
          }
        } else if (DIFF_ID_DELETE.equals(csvRecord.toMap().get(COLUMN_DIFF_ID))) {
          preDelete.setString(++index, csvRecord.toMap().get(COLUMN_POPINFO_ID));
          preDelete.addBatch();
        }
        if (row == commitUnit) {
          preUpdate.executeBatch();
          preInsert.executeBatch();
          preDelete.executeBatch();
          transactionCommitConnection(BatchInfo.B18B0039.getBatchId());
          String mess = String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row);
          myLog.info(mess);
          transactionBeginConnection(BatchInfo.B18B0039.getBatchId());
          row = 0;
        }
      }
      if (row != 0) {
        preUpdate.executeBatch();
        preInsert.executeBatch();
        preDelete.executeBatch();
        String mess = String.format(INSERT_COMMIT_MESSAGE, ++commitCount, row);
        myLog.info(mess);
      }
      transactionCommitConnection(BatchInfo.B18B0039.getBatchId());
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      transactionRollbackConnection(BatchInfo.B18B0039.getBatchId());
      // クエリに失敗した場合はエラーメッセージを出力し、戻り値に"1"を設定し処理を終了する。
      String message = String.format(
          BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
      myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
      throw e;
    } finally {
      closeQuietly(null, preUpdate);
      closeQuietly(null, preInsert);
      closeQuietly(null, preDelete);
      closeConnection(BatchInfo.B18B0039.getBatchId());
    }
  }

	/**
	 * FSひも付きデータ連携テーブル全件削除
	 * @throws SQLException 
	 */
	private void deleteAllFsIdlink() throws SQLException {
		
	    int rows = 0;
		
		do {
	
	    	PreparedStatement preparedStatement = null;
			int index = 0;
			try {			
				transactionBeginConnection(BatchInfo.B18B0039.getBatchId());
				preparedStatement = prepareStatement(BatchInfo.B18B0039.getBatchId(), "deleteAllFsIdlink");
				preparedStatement.setInt(++index, commitUnit);
	
				rows = preparedStatement.executeUpdate();
				
				transactionCommitConnection(BatchInfo.B18B0039.getBatchId());
				
			} catch (Exception e) {
				transactionRollbackConnection(BatchInfo.B18B0039.getBatchId());
			    String message = String.format(
			    		BusinessMessage.getMessages(BusinessMessageCode.B18MB901.toString()), e.toString());
			    myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB901.toString(), message));
			    throw e;
			} finally {
				closeQuietly(null, preparedStatement);
			    closeConnection(BatchInfo.B18B0039.getBatchId());
			}
	
		} while (commitUnit == rows);
	
	}

  /**
   * Read file properties
   * 
   */
  private void readProperties() {
    Properties properties = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0039.getBatchId());
    /** AWS S3からダウンロードする際のバケット名(環境変数) */
    s3BucketName = System.getenv(Constants.ENV_FS_LOG_IMPORT_S3_BUCKET_NAME);

    /** AWS S3からダウンロードする際のディレクトリ（差分） */
    s3DirectoryDiff = properties.getProperty(Constants.S3_DIRECTORY_IDLINK_DIFF);

    /** AWS S3からダウンロードする際のファイル名（差分） */
    s3FileNameDiff = properties.getProperty(Constants.S3_FILE_NAME_IDLINK_DIFF);

    /** AWS S3からダウンロードして解凍したファイル名（差分） */
    s3UngzFileNameDiff = properties.getProperty(Constants.S3_UNGZ_FILE_NAME_IDLINK_DIFF);

    /** AWS S3からダウンロードする際のディレクトリ（全量） */
    s3DirectoryFull = properties.getProperty(Constants.S3_DIRECTORY_IDLINK_FULL);

    /** AWS S3からダウンロードする際のファイル名（全量） */
    s3FileNameFull = properties.getProperty(Constants.S3_FILE_NAME_IDLINK_FULL);

    /** AWS S3からダウンロードして解凍したファイル名（全量） */
    s3UngzFileNameFull = properties.getProperty(Constants.S3_UNGZ_FILE_NAME_IDLINK_FULL);

    /** ダウンロードディレクトリ */
    downloadDirectory = properties.getProperty(Constants.DOWNLOAD_DIRECTORY_IDLINK);

    /** 全件モードの場合のコミット単位 */
    commitUnit = ConvertUtility.stringToInteger(properties.getProperty(COMMIT_UNIT));
  }
}
