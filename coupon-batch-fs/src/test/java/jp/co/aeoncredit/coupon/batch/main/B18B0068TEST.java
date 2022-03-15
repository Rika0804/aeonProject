package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.batch.runtime.context.JobContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUsersDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstAppUsersDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsCouponUsers;
import jp.co.aeoncredit.coupon.entity.MstAppUsers;

public class B18B0068TEST {

  /** ファイル共通 */
  @Spy
  BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0068");

  @InjectMocks
  B18B0068 b18B0068;

  @Mock
  BatchConfigfileLoader batchConfigfileLoader;

  String inputDirectory = "D://pv_ckapp01/ckmgwork/couponList/segment/input/File.csv";

  /** JOB対象 */
  @Mock
  JobContext jobContext;

  @Mock
  private FsCouponUsersDAOCustomize fsCouponUsersDao;

  @Mock
  private MstAppUsersDAOCustomize mstAppUsersDAO;

  @Mock
  Connection con;

  @Mock
  PreparedStatement preparedStatement;

  Properties properties = new Properties();

  private Logger log = LoggerFactory.getInstance().getLogger(this);

  private static final String BATCH_ID = BatchInfo.B18B0068.getBatchId();

  /**
   * テスト初期化処理
   * 
   * @throws Exception スローされた例外
   */
  @Before
  public void setUp() throws Exception {
    // モックを初期化
    MockitoAnnotations.initMocks(this);
    // トランザクションのモック化
    b18B0068 = Mockito.spy(b18B0068);
    Mockito.doReturn(con).when(b18B0068).transactionBegin(Mockito.anyString());
    Mockito.doNothing().when(b18B0068).transactionCommit(Mockito.anyString());
    Mockito.doNothing().when(b18B0068).transactionRollback(Mockito.anyString());
    Mockito.doNothing().when(b18B0068).closeConnection(Mockito.anyString());

    properties.setProperty("purpose.segment.upload.batch.input.directory",
        "D://pv_ckapp01/ckmgwork/couponList/segment/input");
    properties.setProperty("purpose.segment.upload.batch.csv.output.directory",
        "D://pv_ckapp01/ckmgwork/couponList/segment");
    properties.setProperty("purpose.segment.upload.batch.zip.output.directory",
        "D://pv_ckapp01/ckmgwork/couponList/");
    properties.setProperty("purpose.segment.upload.batch.output.ok.filename",
        "segment_[【FSセグメント連携履歴テーブル】.「FSセグメント連携履歴ID」]_OK_[yyyymmdd_hh24miss].csv");
    properties.setProperty("purpose.segment.upload.batch.output.ng.filename",
        "segment_[【FSセグメント連携履歴テーブル】.「FSセグメント連携履歴ID」]_NG_[yyyymmdd_hh24miss].csv");
    properties.setProperty("purpose.segment.upload.batch.output.ok.ng.zip.filename",
        "segment_[【FSセグメント連携履歴テーブル】.「FSセグメント連携履歴ID」]_OKNG.zip");
//    properties.setProperty("purpose.segment.upload.batch.s3.bucketName",
//        "cpn-prd-onprem-send-sss-vendor-dev-env");
    properties.setProperty("SQL_SELECT_FS_FILE_SEGMENT_UPLOAD_HISTORY",
        "SELECT FROM FS_SEGMENT_UPLOAD_HISTORY");
    properties.setProperty("SQL_UPDATE_FS_FILE_SEGMENT_UPLOAD_HISTORY_FAIL",
        "UPDATE FS_SEGMENT_UPLOAD_HISTORY");
    properties.setProperty("SQL_UPDATE_FS_FILE_SEGMENT_UPLOAD_HISTORY_SUCCESS",
        "UPDATE FS_SEGMENT_UPLOAD_HISTORY");

    when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(properties);
    Mockito.when(con.prepareStatement(Mockito.anyString())).thenReturn(preparedStatement);
    Mockito.when(preparedStatement.executeUpdate()).thenReturn(1);

    //システム環境変数で必要なものだけを差し替える
    Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
    Field theCaseInsensitiveEnvironment = clazz.getDeclaredField("theCaseInsensitiveEnvironment");
    theCaseInsensitiveEnvironment.setAccessible(true);
    Map<String,String> sytemEnviroment = (Map<String, String>) theCaseInsensitiveEnvironment.get(null);
    sytemEnviroment.put("SEGMENT_UPLOAD_S3_BUCKET_NAME","cpn-prd-onprem-send-sss-vendor-dev-env");
    
  }

  /**
   * テスト終了処理
   * 
   * @throws Exception スローされた例外
   */
  @After
  public void tearDown() throws Exception {

  }

  /**
   * テスト開始終了メッセージ出力
   * 
   * @param testId テストID
   * @param processType 処理区分（Start/End）
   */
  public void printMsg(String testId, String processType) {
    if (processType.equals("Start")) {
      log.info(" [B18B0068TEST] 【******************" + testId + " 実施開始******************】");
    } else {
      log.info(" [B18B0068TEST] 【******************" + testId + " 実施終了******************】");
    }
  }

  /**
   * case run if delete file fail
   */
  @Test
  public void B18B0068_TEST1() {
    try {
      // テスト開始
      printMsg("TEST001", "Start");

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // demoP.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("demoP.csv");

      Mockito.when(batchFileHandler.deleteFile(Mockito.anyString())).thenReturn(false);

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case run if FsSegmentUploadHistory empty
   */
  @Test
  public void B18B0068_TEST2() {
    try {
      // テスト開始
      printMsg("TEST002", "Start");

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(false);

      Mockito.when(batchFileHandler.deleteFile(Mockito.anyString())).thenReturn(false);

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file is B18I0023-2
   */
  @Test
  public void B18B0068_TEST3() {
    try {
      // テスト開始
      printMsg("TEST001", "Start");
      List<List<String>> inputData = new ArrayList<List<String>>();
      List<String> row1 = new ArrayList<String>();
      row1.add("12345678901");
      row1.add("2");
      inputData.add(row1);
      List<String> row2 = new ArrayList<String>();
      row2.add("123456789012");
      row2.add("1");
      inputData.add(row2);
      List<String> row3 = new ArrayList<String>();
      row3.add("123456789020");
      row3.add("2");
      inputData.add(row3);
      List<String> row4 = new ArrayList<String>();
      row4.add("123456789013");
      row4.add("9");
      inputData.add(row4);
      List<String> row5 = new ArrayList<String>();
      row5.add("123456789014");
      row5.add("1");
      inputData.add(row5);
      List<String> row6 = new ArrayList<String>();
      row6.add("1234567890123");
      row6.add("1");
      inputData.add(row6);
      List<String> row7 = new ArrayList<String>();
      row6.add("123456789015");
      row6.add("12");
      inputData.add(row7);
      List<String> row8 = new ArrayList<String>();
      row8.add("123456789016");
      inputData.add(row8);
      List<String> row9 = new ArrayList<String>();
      row9.add("123456789016");
      row9.add("1");
      row9.add("1234567");
      inputData.add(row9);
      String[] fileHeader = {"会員番号", "家族コード"};

      batchFileHandler.outputCSVFileWithQuote(inputDirectory, inputData, fileHeader, '"',
          Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      List<FsCouponUsers> fsCouponUsersList = new ArrayList<FsCouponUsers>();
      FsCouponUsers fsCouponUsers = new FsCouponUsers();
      fsCouponUsers.setAcsUserCardCpPassportId("11111111111111111100");
      List<FsCouponUsers> fsCouponUsersListEmpty = new ArrayList<FsCouponUsers>();
      fsCouponUsersList.add(fsCouponUsers);
      Mockito.doReturn(fsCouponUsersList).doReturn(fsCouponUsersListEmpty).when(fsCouponUsersDao)
          .find(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file is B18I0023-1
   */
  @Test
  public void B18B0068_TEST4() {
    try {
      // テスト開始
      printMsg("TEST001", "Start");
      List<List<String>> inputData = new ArrayList<List<String>>();
      List<String> row1 = new ArrayList<String>();
      row1.add("1234567890123456789");
      inputData.add(row1);
      List<String> row2 = new ArrayList<String>();
      row2.add("12345678901234567891");
      inputData.add(row2);
      List<String> row3 = new ArrayList<String>();
      row3.add("12345678901234567892");
      inputData.add(row3);
      List<String> row4 = new ArrayList<String>();
      row4.add("123456789012345678921");
      inputData.add(row4);
      List<String> row5 = new ArrayList<String>();
      row5.add("12345678901234567890");
      row5.add("12345678901234567890");
      inputData.add(row5);
      String[] fileHeader = {"共通内部ID"};

      batchFileHandler.outputCSVFileWithQuote(inputDirectory, inputData, fileHeader, '"',
          Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      MstAppUsers mstAppUser = new MstAppUsers();
      Mockito.doReturn(Optional.of(mstAppUser)).doReturn(Optional.empty()).when(mstAppUsersDAO)
          .findOne(Mockito.any(), Mockito.any());

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file is empty
   */
  @Test
  public void B18B0068_TEST5() {
    try {

      batchFileHandler.outputCSVFileWithQuote(inputDirectory, Collections.EMPTY_LIST, null, '"',
          Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file has 3 header
   */
  @Test
  public void B18B0068_TEST6() {
    try {

      String[] fileHeader = {"会員番号", "家族コード", "aaa"};
      batchFileHandler.outputCSVFileWithQuote(inputDirectory, Collections.EMPTY_LIST, fileHeader,
          '"', Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getLong(1)).thenReturn(1L);
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case create zip file fail
   */
  @Test
  public void B18B0068_TEST7() {
    try {

      String[] fileHeader = {"共通内部ID", "共通内部ID1", "共通内部ID2"};
      batchFileHandler.outputCSVFileWithQuote(inputDirectory, Collections.EMPTY_LIST, fileHeader,
          '"', Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      Mockito.doReturn(false).when(batchFileHandler).createZipFile(Mockito.any(), Mockito.any(),
          Mockito.any());

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file has 2 header and fail
   */
  @Test
  public void B18B0068_TEST8() {
    try {

      String[] fileHeader = {"会員番号", "家族コード"};
      batchFileHandler.outputCSVFileWithQuote(inputDirectory, Collections.EMPTY_LIST, fileHeader,
          '"', Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case input file has 1header and fail
   */
  @Test
  public void B18B0068_TEST9() {
    try {

      String[] fileHeader = {"共通内部ID"};
      batchFileHandler.outputCSVFileWithQuote(inputDirectory, Collections.EMPTY_LIST, fileHeader,
          '"', Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }

  /**
   * case delete input file fail
   */
  @Test
  public void B18B0068_TEST10() {
    try {
      // テスト開始
      printMsg("TEST001", "Start");
      List<List<String>> inputData = new ArrayList<List<String>>();
      List<String> row1 = new ArrayList<String>();
      row1.add("12345678901234567891");
      inputData.add(row1);
      String[] fileHeader = {"共通内部ID"};

      batchFileHandler.outputCSVFileWithQuote(inputDirectory, inputData, fileHeader, '"',
          Constants.CHARSET_SHIFT_JIS);

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("File.csv");

      MstAppUsers mstAppUser = new MstAppUsers();
      Mockito.doReturn(Optional.of(mstAppUser)).doReturn(Optional.empty()).when(mstAppUsersDAO)
          .findOne(Mockito.any(), Mockito.any());

      Mockito.when(batchFileHandler.deleteFile(Mockito.anyString())).thenReturn(true)
          .thenReturn(true).thenReturn(true).thenReturn(false);

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }
  
  /**
   * case file not exist
   */
  @Test
  public void B18B0068_TEST11() {
    try {
      // テスト開始

      // mock FsSegmentUploadHistory
      ResultSet resultSetFsSegmentUploadHistory = Mockito.mock(ResultSet.class);
      Mockito.when(resultSetFsSegmentUploadHistory.next()).thenReturn(true).thenReturn(false);
      // File.csv not exist
      Mockito.when(resultSetFsSegmentUploadHistory.getString(2)).thenReturn("FileNotExist.csv");

      MstAppUsers mstAppUser = new MstAppUsers();
      Mockito.doReturn(Optional.of(mstAppUser)).doReturn(Optional.empty()).when(mstAppUsersDAO)
          .findOne(Mockito.any(), Mockito.any());

      Mockito.when(batchFileHandler.deleteFile(Mockito.anyString())).thenReturn(true)
          .thenReturn(true).thenReturn(true).thenReturn(false);

      Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSetFsSegmentUploadHistory);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }
  
  /**
   * case run throw sql exception
   */
  @Test
  public void B18B0068_TEST12() {
    try {
      // テスト開始
      printMsg("TEST001", "Start");

      Mockito.when(batchFileHandler.deleteFile(Mockito.anyString())).thenReturn(false);

      Mockito.when(preparedStatement.executeQuery()).thenThrow(SQLException.class);
      // テスト対象のメソッド
      String returnCD = b18B0068.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST001", "End");
    }
  }
}
