package jp.co.aeoncredit.coupon.batch.main;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.util.Strings;

import com.ibm.jp.awag.common.dao.DAOBase.SelectWhereOperator;
import com.ibm.jp.awag.common.dao.DAOParameter;
import com.ibm.jp.awag.common.util.ConvertUtility;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.common.S3Utils;
import jp.co.aeoncredit.coupon.batch.constants.B18B0068Property;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.DeleteFlag;
import jp.co.aeoncredit.coupon.constants.FsDeliveryStatus;
import jp.co.aeoncredit.coupon.constants.UploadResult;
import jp.co.aeoncredit.coupon.constants.properties.FsCouponUsersProps;
import jp.co.aeoncredit.coupon.constants.properties.MstAppUsersProps;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUsersDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstAppUsersDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsCouponUsers;
import jp.co.aeoncredit.coupon.entity.FsSegmentUploadHistory;
import jp.co.aeoncredit.coupon.entity.MstAppUsers;
import jp.co.aeoncredit.coupon.util.BusinessMessage;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * バッチ B18B0068
 * 
 * @author HungHM
 * @version 1.0
 */
@Named("B18B0068")
@Dependent
public class B18B0068 extends BatchDbConnectionBase {

  /** ログ */
  private Logger myLog = getLogger();

  /** MstAppUsersDAOCustomize */
  @Inject
  private MstAppUsersDAOCustomize mstAppUserDao;

  /** FsCouponUsersDAOCustomize */
  @Inject
  private FsCouponUsersDAOCustomize fsCouponUsersDao;

  /** メッセージ共通 */
  private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0068.getBatchId());

  /** 処理対象件数 */
  private int numberCaseProcess;

  /** プロパティファイル共通 */
  private BatchConfigfileLoader batchConfigfileLoader =
      new BatchConfigfileLoader(BatchInfo.B18B0068.getBatchId());

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0068.getBatchId());

  /** header input csv file for B18I0023-1 */
  protected static final String INPUT_HEADER_B18I0023_1 = "\"共通内部ID\"";
  /** header input csv file for B18I0023-2 */
  protected static final String INPUT_HEADER_B18I0023_2 = "\"会員番号\", \"家族コード\"";

  /** header output csv file for B18I0023-1 */
  protected static final String OUTPUT_HEADER_B18I0020_1 = "awtID";

  /** header output csv file for B18I0023-2 */
  protected static final String OUTPUT_HEADER_B18I0020_2 = "CPPassID";

  /** セグメント管理画面アップロードされたCSVファイルのディレクトリ */
  private String segmentInputDirectory;

  /** CSVファイル出力用ディレクトリ */
  private String segmentCsvOutputDirectory;

  /** zipファイル出力用ディレクトリ */
  private String segmentZipOutputDirectory;

  /** OKリストCSVのファイル名 */
  private String segmentOKOutputFileName;

  /** NGリストCSVのファイル名 */
  private String segmentNGOutputFileName;

  /** OK,NGリストzipのファイル名 */
  private String segmentOKNGZipOutputFileName;

  /** AWS S3にID一括変換リストをアップロードする際のバケット名 */
  private String segmentBucketName;
  
  /** ID一括変換リストをアップロードするオブジェクトキー */
  private String segmentObjectKey;

  /** ATM統計zipファイル名 */
  private int recordSuccessCount;

  /** ATM統計zipファイル名 */
  private int recordFailCount;

  /** AWS S3 Utils */
  private S3Utils s3Utils;

  /** current time */
  private Timestamp currentTime = DateUtils.now();

  /** import type */
  private String importType = "0";

  /** csv all record */
  private int csvRecordCount = 0;

  /** csv record ok */
  private int csvOkRecordCount = 0;

  /** csv record ng */
  private int csvNgRecordCount = 0;

  /**
   * @see jp.co.aeoncredit.coupon.batch.common.BatchDbConnectionBase#process()
   */
  @Override
  public String process() throws Exception {

    // (1)処理開始メッセージを出力する。
    myLog.info(batchLogger.createStartMsg(BusinessMessageCode.B18MB001.toString(),
        BatchInfo.B18B0068.getBatchName()));

    // read file properties
    readProperties();

    // AWS S3 Utils
    s3Utils = new S3Utils(segmentBucketName, segmentZipOutputDirectory, myLog, batchLogger,
        batchFileHandler);

    String processResult = processBatch();

    // Export log
    String message =
        String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB005.toString()),
            Constants.UPLOAD_ALL_CSV_MSG, numberCaseProcess, recordSuccessCount,
            recordFailCount, Constants.DEFAULT_COUNT, Constants.NO_DETAIL);
    myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), message));

    // 終了メッセージを出力する。
    myLog.info(batchLogger.createEndMsg(BusinessMessageCode.B18MB002.toString(),
        BatchInfo.B18B0068.getBatchName(), ProcessResult.SUCCESS.getValue().equals(processResult)));

    return setExitStatus(processResult);
  }

  /**
   * バッチの起動メイン処理 機能概要
   * 
   * @return process result
   */
  public String processBatch() throws SQLException {
    List<FsSegmentUploadHistory> fsSegmentUploadHistoryList;
    try {
      // (2)【配信バッチ情報管理テーブル】取得
      fsSegmentUploadHistoryList = getFsSegmentUploadHistory();
      if (fsSegmentUploadHistoryList.isEmpty()) {
        // Export log
        String msg =
            String.format(BusinessMessage.getMessages(BusinessMessageCode.B18MB006.toString()),
                Constants.MSG_FS_SEGMENT_UPLOAD_HISTORY_NO_RECORD, Strings.EMPTY);
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));
        return ProcessResult.SUCCESS.getValue();
      }
    } catch (SQLException e) {
      myLog.error(e.getMessage(), e);
      return ProcessResult.FAILURE.getValue();
    }
    numberCaseProcess = fsSegmentUploadHistoryList.size();

    return processFsSegmentUploadHistoryList(fsSegmentUploadHistoryList);
  }

  /**
   * (2.1)で取得した全てのレコードに対して、以下(3)～(12)の処理を繰り返す
   * 
   * @return process result
   */
  public String processFsSegmentUploadHistoryList(
      List<FsSegmentUploadHistory> fsSegmentUploadHistoryList) throws SQLException {
    String result = ProcessResult.SUCCESS.getValue();
    boolean checkFile0Byte;
    if (!this.deleteCsvFile(segmentCsvOutputDirectory)) {
      // ファイル削除に失敗した場合には、ログを出力し、処理(14)へ（異常終了）
      return ProcessResult.FAILURE.getValue();
    }
    // (2.1)で取得した全てのレコードに対して、以下(3)～(12)の処理を繰り返す
    for (FsSegmentUploadHistory fsSegmentUploadHistory : fsSegmentUploadHistoryList) {
      try {
        csvRecordCount = 0;
        csvOkRecordCount = 0;
        csvNgRecordCount = 0;
        checkFile0Byte = false; 
        long fsSegmentUploadHistoryId = fsSegmentUploadHistory.getFsSegmentUploadHistoryId();
        String extension = FilenameUtils.getExtension(fsSegmentUploadHistory.getFileName());
        String fileName = ConvertUtility.longToString(fsSegmentUploadHistoryId) + "." + extension;
        String inputDir =
            Paths.get(segmentInputDirectory, fileName).toString();

        // (4.1)指定ディレクトリにID一括変換リストCSV作成
        myLog.info("(4.1)指定ディレクトリにID一括変換リストCSV作成");
        String outputDir =
            Paths.get(segmentCsvOutputDirectory, fileName).toString();
        batchFileHandler.outputCSVFile(outputDir, Collections.emptyList());

        // (5.1)指定ディレクトリにOKリストCSV作成
        myLog.info("(5.1)指定ディレクトリにOKリストCSV作成");
        String okOutputDir =
            Paths.get(segmentCsvOutputDirectory, getFileName(segmentOKOutputFileName,
                fsSegmentUploadHistory.getFsSegmentUploadHistoryId())).toString();
        batchFileHandler.outputCSVFile(okOutputDir, Collections.emptyList());

        // (5.2)指定ディレクトリにNGリストCSV作成
        myLog.info("(5.2)指定ディレクトリにNGリストCSV作成");
        String ngOutputDir =
            Paths.get(segmentCsvOutputDirectory, getFileName(segmentNGOutputFileName,
                fsSegmentUploadHistory.getFsSegmentUploadHistoryId())).toString();
        batchFileHandler.outputCSVFile(ngOutputDir, Collections.emptyList());
        // 出力ストリーム

        // (6.1)指定のディレクトリに格納されているCSVファイルを取得
        myLog.info("(6.1)指定のディレクトリに格納されているCSVファイルを取得: " + inputDir);
        
        if (!this.proccessCSVFile(inputDir, outputDir, okOutputDir, ngOutputDir)) {
          Path inputPaths = Paths.get(segmentInputDirectory, fileName);
          // calculate file size
          long fileSize = Files.size(inputPaths);
          // 取得したCSVファイルのファイルサイズが0バイト
          if(fileSize != 0L) {
            String msg =
                this.convertLog(BusinessMessageCode.B18MB006.toString(), fsSegmentUploadHistoryId);
            msg = String.format(msg, Constants.MSG_REPLACE_SEGMENT_ID_PROCCESS,
                Constants.MSG_REPLACE_NAME + fsSegmentUploadHistory.getFileName());
            myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));
            this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, false);
            recordFailCount++;
            continue;
            // 取得したCSVファイルのファイルサイズが0バイト
          } else {
            checkFile0Byte = true;
          }
        }

        // (7.1)zipファイル作成
        
        if (!checkFile0Byte && !this.createZipFile(Arrays.asList(okOutputDir, ngOutputDir),
            fsSegmentUploadHistoryId)) {
          // zip化に失敗した場合は、ログを出力し、処理(X)を行う。
          String msg =
              this.convertLog(BusinessMessageCode.B18MB905.toString(), fsSegmentUploadHistoryId);
          msg = String.format(msg, segmentZipOutputDirectory, segmentOKNGZipOutputFileName.replace(
              Constants.REPLACE_SEGMENT_ID, ConvertUtility.longToString(fsSegmentUploadHistoryId)));
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB905.toString(), msg));
          this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, false);
          recordFailCount++;
          continue;
        }

        // (8)ID一括変換リスト（B18I0020）CSVがゼロ件の場合、ログを出力し、処理(X)を行う。
        if (!checkFile0Byte && this.checkZeroRecordExport(outputDir)) {
          importType = "0";
          String msg =
              this.convertLog(BusinessMessageCode.B18MB006.toString(), fsSegmentUploadHistoryId);
          msg = String.format(msg, Constants.MSG_REPLACE_SEGMENT_ID_PROCCESS,
              Constants.MSG_REPLACE_NAME + fsSegmentUploadHistory.getFileName());
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB006.toString(), msg));
          this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, false);
          recordFailCount++;
          continue;
        }

        // (9)AWS S3アップロード
        String uploadDirectory = outputDir;
        
        if (!this.uploadFileAWSS3(uploadDirectory, fsSegmentUploadHistory.getFileName(),
            fsSegmentUploadHistoryId)) {
          this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, false);
          recordFailCount++;
          continue;
        }

        // (10)セグメント取込CSVの削除
        
        if (Boolean.FALSE.equals(batchFileHandler.deleteFile(inputDir))) {
          String msg =
              this.convertLog(BusinessMessageCode.B18MB906.toString(), fsSegmentUploadHistoryId);
          msg = String.format(msg, segmentInputDirectory, fsSegmentUploadHistory.getFileName());
          myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB906.toString(), msg));
          this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, true);
          recordFailCount++;
          continue;
        }

        // (11)【FSセグメント連携履歴テーブル】更新
        this.updateFsSegmentUploadHistorySucess(fsSegmentUploadHistory);

        // (12)処理件数をログに出力する。
        String msg =
            this.convertLog(BusinessMessageCode.B18MB005.toString(), fsSegmentUploadHistoryId);
        msg = String.format(msg, Constants.UPLOAD_CSV_MSG, csvRecordCount, csvOkRecordCount,
            csvNgRecordCount, Constants.DEFAULT_COUNT, Constants.MSG_REPLACE_NAME + fsSegmentUploadHistory.getFileName());
        myLog.info(batchLogger.createMsg(BusinessMessageCode.B18MB005.toString(), msg));
        
        recordSuccessCount++;
      } catch (Exception e) {
        myLog.error(e.getMessage(), e);
        this.updateFsSegmentUploadHistoryFail(fsSegmentUploadHistory, false);
        transactionRollback(BatchInfo.B18B0068.getBatchId());
        recordFailCount++;
      } finally {
        // コネクションをクローズする
        closeConnection(BatchInfo.B18B0068.getBatchId());
      }
    }
    return result;
  }

  /**
   * Process Authenticate AWS S3
   */
  private S3Client processAuthenticateAWSS3(Long segmentUploadHistoryId) {
    try {
      // (2.1) S3 Clientをビルドし、AWS認証する。
      // リージョン：ap-northeast-1
      Region clientRegion = Region.AP_NORTHEAST_1;
      return s3Utils.s3Client(clientRegion);
    } catch (S3Exception e) {
      String msg = this.convertLog(BusinessMessageCode.B18MB907.toString(), segmentUploadHistoryId);
      msg = String.format(msg, e.toString());
      myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB907.toString(), msg));
      recordFailCount++;
      throw e;
    }
  }

  /**
   * upload file to AWS S3
   * 
   * @param directory path
   * @param keyName
   * @param segmentUploadHistoryId
   * 
   * @throws S3Exception
   */
  private boolean uploadFileAWSS3(String directory, String keyName, Long segmentUploadHistoryId) {
    try {
      myLog.info("uploadFileAWSS3.key: " + segmentObjectKey + keyName);
      
      // Reading the file to be uploaded
      File file = new File(directory);
      String md5 = Md5Utils.md5AsBase64(file);
      
      S3Client s3Client = processAuthenticateAWSS3(segmentUploadHistoryId);
      PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(segmentBucketName)
          .key(segmentObjectKey + keyName)
          .contentMD5(md5).build();

      // Passing the file to upload
      s3Client.putObject(objectRequest, RequestBody.fromFile(file));
      return true;
    } catch (S3Exception | IOException e) {
      myLog.error(e.getMessage(), e);
      String msg = this.convertLog(BusinessMessageCode.B18MB908.toString(), segmentUploadHistoryId);
      msg = String.format(msg, directory, keyName);
      myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB908.toString(), msg));
      recordFailCount++;
      return false;
    }
  }

  /**
   * update FsSegmentUploadHistory if fail
   * 
   * @param fsSegmentUploadHistory record to update
   * @throws SQLException
   */
  private void updateFsSegmentUploadHistoryFail(FsSegmentUploadHistory fsSegmentUploadHistory,
      boolean isNormal) throws SQLException {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    try {
      transactionBegin(BatchInfo.B18B0068.getBatchId());
      preparedStatement = prepareStatement(BatchInfo.B18B0068.getBatchId(),
          "SQL_UPDATE_FS_FILE_SEGMENT_UPLOAD_HISTORY_FAIL");
      if (isNormal) {
        preparedStatement.setString(1, UploadResult.SUCCESS.getValue());
      } else {
        preparedStatement.setString(1, UploadResult.FAILURE.getValue());
      }
      if ("0".equals(importType)) {
        preparedStatement.setString(2, null);
      } else if ("1".equals(importType)) {
        preparedStatement.setString(2, "A");
      } else if ("2".equals(importType)) {
        preparedStatement.setString(2, "C");
      }
      preparedStatement.setTimestamp(3, currentTime);
      preparedStatement.setLong(4, fsSegmentUploadHistory.getFsSegmentUploadHistoryId());
      preparedStatement.executeUpdate();
      transactionCommit(BatchInfo.B18B0068.getBatchId());
    } catch (SQLException e) {
      transactionRollback(BatchInfo.B18B0068.getBatchId());
    } finally {
      closeQuietly(resultSet, preparedStatement);
    }
  }

  /**
   * update FsSegmentUploadHistory if success
   * 
   * @param list data from table FS_SEGMENT_UPLOAD_HISTORY
   * @throws SQLException
   */
  private void updateFsSegmentUploadHistorySucess(FsSegmentUploadHistory fsSegmentUploadHistory)
      throws SQLException {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    try {
      transactionBegin(BatchInfo.B18B0068.getBatchId());
      preparedStatement = prepareStatement(BatchInfo.B18B0068.getBatchId(),
          "SQL_UPDATE_FS_FILE_SEGMENT_UPLOAD_HISTORY_SUCCESS");
      if ("1".equals(importType)) {
        preparedStatement.setString(1, "A");
      } else if ("2".equals(importType)) {
        preparedStatement.setString(1, "C");
      } else {
        preparedStatement.setString(1, null);
      }
      preparedStatement.setTimestamp(2, currentTime);
      preparedStatement.setLong(3, fsSegmentUploadHistory.getFsSegmentUploadHistoryId());
      preparedStatement.executeUpdate();
      transactionCommit(BatchInfo.B18B0068.getBatchId());
    } catch (SQLException e) {
      transactionRollback(BatchInfo.B18B0068.getBatchId());
    } finally {
      closeQuietly(resultSet, preparedStatement);
    }
  }

  /**
   * Get data from the table FS_SEGMENT_UPLOAD_HISTORY
   * 
   * @return list data from table FS_SEGMENT_UPLOAD_HISTORY
   * @throws SQLException
   */
  private List<FsSegmentUploadHistory> getFsSegmentUploadHistory() throws SQLException {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    try {
      getDbConnection(BatchInfo.B18B0068.getBatchId());
      List<FsSegmentUploadHistory> fsSegmentUploadHistorylist = new ArrayList<>();
      preparedStatement = prepareStatement(BatchInfo.B18B0068.getBatchId(),
          "SQL_SELECT_FS_FILE_SEGMENT_UPLOAD_HISTORY");
      resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        FsSegmentUploadHistory data = new FsSegmentUploadHistory();
        data.setFsSegmentUploadHistoryId(resultSet.getLong(1));
        data.setFileName(resultSet.getString(2));
        fsSegmentUploadHistorylist.add(data);
      }
      return fsSegmentUploadHistorylist;
    } finally {
      closeQuietly(resultSet, preparedStatement);
      closeConnection(BatchInfo.B18B0068.getBatchId());
    }
  }

  /**
   * #1(1) プロパティファイルを読み込む。
   * 
   */
  private void readProperties() {
    Properties prop = batchConfigfileLoader.readPropertyFile(BatchInfo.B18B0068.getBatchId());
    /** セグメント管理画面アップロードされたCSVファイルのディレクトリ */
    segmentInputDirectory = prop.getProperty(B18B0068Property.B18B0068.getCsvInputDirectory());
    /** CSVファイル出力用ディレクトリ */
    segmentCsvOutputDirectory = prop.getProperty(B18B0068Property.B18B0068.getCsvOutputDirectory());
    /** zipファイル出力用ディレクトリ */
    segmentZipOutputDirectory = prop.getProperty(B18B0068Property.B18B0068.getZipOutputDirectory());
    /** OKリストCSVのファイル名 */
    segmentOKOutputFileName = prop.getProperty(B18B0068Property.B18B0068.getOutputOkFileName());
    /** NGリストCSVのファイル名 */
    segmentNGOutputFileName = prop.getProperty(B18B0068Property.B18B0068.getOutputNgFileName());
    /** OK,NGリストzipのファイル名 */
    segmentOKNGZipOutputFileName = prop.getProperty(B18B0068Property.B18B0068.getZipOkNgFileName());
    /** AWS S3にID一括変換リストをアップロードする際のバケット名(環境変数) */
    segmentBucketName = System.getenv(Constants.ENV_SEGMENT_UPLOAD_S3_BUCKET_NAME);
    /** ID一括変換リストをアップロードするオブジェクトキー */
    segmentObjectKey = prop.getProperty(B18B0068Property.B18B0068.getObjectKey());
  }

  /**
   * get file name format
   * 
   * @param fileName file name to format
   * @param segmentUploadHistoryId
   * @return fileName formatted
   * 
   */
  public String getFileName(String fileName, Long segmentUploadHistoryId) {
    SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATA_FORMAT_YYYYMMDD_HHMMSS);
    String newFileName = fileName.replace(Constants.REPLACE_DATE, dateFormat.format(currentTime));
    newFileName = newFileName.replace(Constants.REPLACE_SEGMENT_ID,
        ConvertUtility.longToString(segmentUploadHistoryId));
    return newFileName;
  }

  /**
   * create Zip file
   * 
   * @param path path of files to zip
   * @param segmentUploadHistoryId
   * @return result return true if create success, return false if create fail
   * 
   */
  public boolean createZipFile(List<String> source, Long segmentUploadHistoryId) {
    File directory = new File(segmentZipOutputDirectory);
    String zipFileName = segmentOKNGZipOutputFileName.replace(Constants.REPLACE_SEGMENT_ID,
        ConvertUtility.longToString(segmentUploadHistoryId));
    String fullPath = segmentZipOutputDirectory + zipFileName;
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return batchFileHandler.createZipFile(source, fullPath, null);
  }

  /**
   * convertLog
   * 
   * @param code
   * @param directory
   * @param fileName
   * 
   */
  private String convertLog(String code, Long segmentUploadHistoryId) {
    return BusinessMessage.getMessages(code).replaceFirst(Constants.BRACKET,
        Constants.MSG_FS_SEGMENT_UPLOAD_HISTORY_ID
            + ConvertUtility.longToString(segmentUploadHistoryId) + Constants.COMMA);
  }

  /**
   * Close Quietly
   * 
   * @param resultSet ResultSet
   * @param preparedStatement PreparedStatement
   * @throws SQLException
   */
  private void closeQuietly(ResultSet resultSet, PreparedStatement preparedStatement)
      throws SQLException {
    if (resultSet != null) {
      resultSet.close();
    }
    if (preparedStatement != null) {
      preparedStatement.close();
    }
  }

  /**
   * Delete all file in directory
   * 
   * @param String directoryPath
   * @param preparedStatement PreparedStatement
   * @return true if delete success, false if delete fail
   */
  private boolean deleteCsvFile(String directoryPath) {
    boolean result = true;
    File directory = new File(directoryPath);
    if (!directory.exists()) {
      return false;
    }
    for (File file : directory.listFiles()) {
      if (!file.isDirectory() && file.toString().toLowerCase().endsWith("csv")
          && Boolean.FALSE.equals(batchFileHandler.deleteFile(file.getPath()))) {
        String msg = BusinessMessage.getMessages(BusinessMessageCode.B18MB906.toString());
        msg = String.format(msg, directoryPath, file.getName());
        myLog.error(BusinessMessageCode.B18MB906.toString(), msg);
        result = false;
      }
    }
    return result;
  }

  /**
   * CSVファイルを読み込むメソッド。
   * 
   * @param String inputPathFile
   * @param String outputPathFile
   * @param String okOutPathFile
   * @param String ngOutPathFile
   * @return false if process file success, fail if process file fail
   * 
   */
  private boolean proccessCSVFile(String inputPathFile, String outputPathFile, String okOutPathFile,
      String ngOutPathFile) {
    importType = "0";
    // 入力ストリーム
    try (FileInputStream fileInputStream = new FileInputStream(inputPathFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader br = new BufferedReader(inputStreamReader);
        FileOutputStream fileOutputStream = new FileOutputStream(outputPathFile);
        OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(fileOutputStream, Constants.CHARSET_UTF_8);
        BufferedWriter outBw = new BufferedWriter(outputStreamWriter);
        FileOutputStream fileOkOutputStream = new FileOutputStream(okOutPathFile);
        OutputStreamWriter okOutputStreamWriter =
            new OutputStreamWriter(fileOkOutputStream, Constants.CHARSET_SHIFT_JIS);
        BufferedWriter okOutBw = new BufferedWriter(okOutputStreamWriter);
        FileOutputStream fileNgOutputStream = new FileOutputStream(ngOutPathFile);
        OutputStreamWriter ngOutputStreamWriter =
            new OutputStreamWriter(fileNgOutputStream, Constants.CHARSET_SHIFT_JIS);
        BufferedWriter ngOutBw = new BufferedWriter(ngOutputStreamWriter);) {
      // data of line
      String line = br.readLine();
      if (line == null || (line.isEmpty() && br.readLine() == null)) {
        return false;
      }

      String[] headers = line.split(Constants.COMMA);
      if (headers.length == 1) {
        importType = "1";
        // (6.2a)項目数が1つの場合（セグメント取込リストがB18I0023-1の場合）
        // header
        initFileHeader(outBw, okOutBw, ngOutBw, true);
        line = br.readLine();

        DAOParameter daoParam = new DAOParameter();
        while (line != null) {
          
          csvRecordCount++;
          String writeLine = line;
          line = line.replaceAll(Constants.DOUBLE_QOUTES, Strings.EMPTY);
          String[] columnArray = line.split(Constants.COMMA);

          // (6.2a.2)取得した行のチェックを行う。
          if (columnArray.length != 1 || line.length() != 20) {
            this.writeLine(ngOutBw, writeLine);
            csvNgRecordCount++;
            // 次の行
            line = br.readLine();
            continue;
          }
          daoParam.getParameterMap().clear();
          daoParam.set(MstAppUsersProps.COMMON_INSIDE_ID, line);
          daoParam.set(MstAppUsersProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());
          Optional<MstAppUsers> entity = mstAppUserDao.findOne(daoParam, null);

          if (!entity.isEmpty()) {
            this.writeLine(outBw, entity.get().getAwTrackingId());

            this.writeLine(okOutBw, writeLine);
            csvOkRecordCount++;
          } else {
            this.writeLine(ngOutBw, writeLine);
            csvNgRecordCount++;
          }

          // 次の行
          line = br.readLine();
        }
      } else if (headers.length == 2) {
        // (6.2b)項目数が2つの場合（セグメント取込リストがB18I0023-2の場合）
        importType = "2";
        // header
        initFileHeader(outBw, okOutBw, ngOutBw, false);
        line = line.replaceAll(Constants.DOUBLE_QOUTES, Strings.EMPTY);
        line = br.readLine();

        DAOParameter daoParam = new DAOParameter();
        while (line != null) {
          csvRecordCount++;
          String writeLine = line;
          line = line.replaceAll(Constants.DOUBLE_QOUTES, Strings.EMPTY);
          String[] columnArray = line.split(Constants.COMMA);

          if (columnArray.length != 2 || columnArray[0].length() != 12
              || (columnArray.length == 2 && columnArray[1].length() != 1)) {
            this.writeLine(ngOutBw, writeLine);
            csvNgRecordCount++;
            // 次の行
            line = br.readLine();
            continue;
          }
          daoParam.getParameterMap().clear();
          daoParam.set(FsCouponUsersProps.ACS_USER_CARD_ID, columnArray[0]);
          daoParam.set(FsCouponUsersProps.ACS_USER_CARD_FAMILY_CD, columnArray[1]);
          daoParam.set(FsCouponUsersProps.FS_DELIVERY_STATUS, SelectWhereOperator.NOT_EQUALS, FsDeliveryStatus.FAILURE.getValue());
          daoParam.set(FsCouponUsersProps.DELETE_FLAG, DeleteFlag.NOT_DELETED.getValue());

          List<FsCouponUsers> entityList = fsCouponUsersDao.find(daoParam, null, false, null);
          List<String> cpPassportIdList =
              entityList.stream().map(FsCouponUsers::getAcsUserCardCpPassportId).distinct()
                  .collect(Collectors.toList());

          if (!entityList.isEmpty()) {
            for (String cpPassportId : cpPassportIdList) {
              this.writeLine(outBw,cpPassportId);

              this.writeLine(okOutBw, writeLine);
            }
            csvOkRecordCount++;
          } else {
            this.writeLine(ngOutBw, writeLine);
            csvNgRecordCount++;
          }

          // 次の行
          line = br.readLine();
        }
      } else {
        while (line != null) {
          this.writeLine(ngOutBw, line);
          csvNgRecordCount++;
          line = br.readLine();
        }
      }

      return true;
    } catch (IOException e) {
      myLog.error(e.getMessage(), e);
      return false;
    }
  }

  /**
   * initialize header for output file
   * 
   * @param BufferedWriter outBw
   * @param BufferedWriter okOutBw
   * @param BufferedWriter ngOutBw
   * @param boolean isB18I0023no1
   * 
   */
  private void initFileHeader(BufferedWriter outBw, BufferedWriter okOutBw, BufferedWriter ngOutBw,
      boolean isB18I0023no1) throws IOException {
    if (isB18I0023no1) {
      this.writeLine(outBw, OUTPUT_HEADER_B18I0020_1);
      this.writeLine(okOutBw, INPUT_HEADER_B18I0023_1);
      this.writeLine(ngOutBw, INPUT_HEADER_B18I0023_1);
    } else {
      this.writeLine(outBw, OUTPUT_HEADER_B18I0020_2);
      this.writeLine(okOutBw, INPUT_HEADER_B18I0023_2);
      this.writeLine(ngOutBw, INPUT_HEADER_B18I0023_2);
    }
  }

  /**
   * check if file have zero record
   * 
   * @param String outputPathFile
   * @return true if have zero record, false if have record
   * 
   */
  private boolean checkZeroRecordExport(String outputPathFile) {
    try (FileInputStream fileInputStream = new FileInputStream(outputPathFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader br = new BufferedReader(inputStreamReader);) {
      boolean result = true;
      // header
      String line = br.readLine();
      // first record
      line = br.readLine();
      if (line != null) {
        result = false;
      }
      return result;
    } catch (IOException e) {
      myLog.error(e.getMessage(), e);
      return true;
    }
  }

  /**
   * write new line to file
   * 
   * @param redWriter bw
   * @param redWriter line
   * @throws IOException
   * 
   */
  private void writeLine(BufferedWriter bw, String line) throws IOException {
    bw.write(line);
    bw.write("\r\n");
  }
}
