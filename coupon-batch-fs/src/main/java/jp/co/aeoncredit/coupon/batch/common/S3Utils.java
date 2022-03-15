package jp.co.aeoncredit.coupon.batch.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import com.ibm.jp.awag.common.util.DateUtils;
import com.ibm.jp.awag.common.util.Logger;

import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * AWS S3 Utils
 * 
 * @author ngotrungkien
 * @version 1.0
 */
public class S3Utils {

  /** メッセージ共通 */
  private BatchLogger batchLogger;

  /** ファイル共通 */
  private BatchFileHandler batchFileHandler;

  /** ログ */
  private Logger myLog;

  /** ダウンロードディレクトリ */
  private String downloadDirectory;

  /** AWS S3からダウンロードする際のバケット名 */
  private String s3BucketName;

  /** ',' symbol */
  protected static final String COMMA = ",";

  public S3Utils(String s3BucketName, String downloadDirectory, Logger myLog,
      BatchLogger batchLogger, BatchFileHandler batchFileHandler) {
    this.s3BucketName = s3BucketName;
    this.downloadDirectory = downloadDirectory;
    this.myLog = myLog;
    this.batchLogger = batchLogger;
    this.batchFileHandler = batchFileHandler;
  }

  /**
   * Service client for accessing Amazon S3
   * 
   * @param clientRegion Class Region
   * @return S3Client
   */
  public S3Client s3Client(Region clientRegion) {

    String endpoint = System.getenv("S3_END_POINT");
    myLog.info("endpoint：" + endpoint);

    return S3Client.builder().region(clientRegion).endpointOverride(URI.create(endpoint))
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
  }

  /**
   * Get list key form S3 to download
   * 
   * @param prefix Limits the response to keys that begin with the specified prefix
   * @param s3Client S3Client
   * @param s3Directory
   * @return List key
   */
  public List<String> getKeyList(String prefix, S3Client s3Client, String s3Directory) {
    ListObjectsRequest objectList =
        ListObjectsRequest.builder().bucket(s3BucketName).prefix(prefix).build();
    ListObjectsResponse responseList = s3Client.listObjects(objectList);
    List<S3Object> s3ObjectList = responseList.contents();
    Map<String, String> keysMap = new HashMap<>();
    for (S3Object s3Object : s3ObjectList) {
      String key = s3Object.key();
      String targetKey = s3Directory + Constants.FORMAT_FOLDER_S3;
      // Check format key
      if (key.lastIndexOf(Constants.SYMBOL_SLASH) == targetKey.length() - 1) {
        keysMap.put(key.substring(0, key.lastIndexOf(Constants.SYMBOL_SLASH) + 1), key);
      }
    }
    // Sort by date asc
    List<String> keyList = new ArrayList<>(keysMap.values());
    Collections.sort(keyList);
    List<String> keyListAfterFilter = new ArrayList<>();
    for (int i = 0; i < keyList.size(); i++) {
      StringBuilder nextKey = new StringBuilder();
      String key = keyList.get(i);
      if (i < (keyList.size() - 1)) {
        String[] keyListItem = keyList.get(i+1).split("/");
        for (int j = 0; j < keyListItem.length-1; j++) {
          nextKey.append(keyListItem[j]);
        }
      }
      if (key.equals(nextKey.toString())) {
        continue;
      }
      keyListAfterFilter.add(key);
    }
    return keyListAfterFilter;
  }

  /**
   * Check empty folder and file in S3
   * 
   * @param keyList: List path get in S3
   * @param date: executeDate
   * @return true when have file and folder with executeDate in S3 ortherwise return false
   */
  public boolean checkPathLastday(List<String> keyList, String date) {
    String[] keyListItem = keyList.get(keyList.size()-1).split("/");
    int lastDay = 0;
    int lastMonth = 0;
    int lastYear = 0;
    try {
      lastDay = Integer.parseInt(keyListItem[keyListItem.length-2]);
      lastMonth = Integer.parseInt(keyListItem[keyListItem.length-3]);
      lastYear = Integer.parseInt(keyListItem[keyListItem.length-4]);
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
  
  /**
   * Down load file form S3 to local
   * 
   * @param key Key of the object to get
   * @param s3Client S3Client
   * @param s3FileName file name down load in local
   * @return true if download success
   */
  public boolean downloadFileAWSS3(String key, S3Client s3Client, String s3FileName) {
    myLog.debug("downloadFileAWSS3 start");
    s3Client= s3Client(Region.AP_NORTHEAST_1);
    GetObjectRequest request = GetObjectRequest.builder().bucket(s3BucketName).key(key).build();
    try {
      // Run download
      Path dstPath = Paths.get(downloadDirectory, s3FileName);
      s3Client.getObject(request, dstPath);
      s3Client.close();
      myLog.debug("downloadFileAWSS3 end");
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (SdkException e) {
      myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB918, s3BucketName, s3FileName));
      throw e;
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
    String filePath = Paths.get(downloadDirectory, fileName).toString();
    boolean result = batchFileHandler.existFile(filePath);
    // Exist file
    if (result && Boolean.FALSE.equals(batchFileHandler.deleteFile(filePath))) {
      // Export log when delete file failure
      myLog.error(batchLogger.createMsg(BusinessMessageCode.B18MB906, downloadDirectory, fileName));
      return false;
    }
    return true;
  }

  /**
   * Unzip the file downloaded from AWS S3
   * 
   * @param srcFilePath Compressed file path
   * @param dstFilePath Unzipped file path
   * @throws IOException I / O exception
   */
  public boolean decompressGzip(String srcFilePath, String dstFilePath) throws IOException {
    myLog.debug("decompressGzip start");
    File srcFile = Paths.get(srcFilePath).toFile();
    File dstFile = Paths.get(dstFilePath).toFile();
    try (FileInputStream srcStream = new FileInputStream(srcFile);
        GZIPInputStream gzipStream = new GZIPInputStream(srcStream);) {
      Files.copy(gzipStream, dstFile.toPath());     
      myLog.debug("decompressGzip end");
      return true;
    } catch (IOException e) {
      myLog.debug(e.getMessage(), e);
      return false;
    }
  }

  /**
   * subtract 1 day
   * 
   * @param date Date
   * @return date
   * @throws ParseException 
   */
  public String subtractOneDay(String date) throws ParseException {
   SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

   Calendar cal = Calendar.getInstance();
   cal.setTime(dateFormat.parse(date));
   cal.add(Calendar.DAY_OF_MONTH, -1);

   String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
   if (month.length() < 2) {
	   month = "0" + month;
   }
   
   String subDate = String.valueOf(cal.get(Calendar.DATE));
   if (subDate.length() < 2) {
	   subDate = "0" + subDate;
   }

   return cal.get(Calendar.YEAR) + Constants.SYMBOL_SLASH + month + Constants.SYMBOL_SLASH + subDate;

  }

}
