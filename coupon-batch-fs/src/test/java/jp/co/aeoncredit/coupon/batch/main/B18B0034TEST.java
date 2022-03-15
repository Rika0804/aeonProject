package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.exceptions.base.MockitoException;

import com.ibm.jp.awag.common.logic.ServiceAppException;
import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.S3Utils;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsUsersDAOCustomize;

/**
 * Payment更新処理のテスト クラスのJUnit
 * 
 * 
 */
@Named("B18B0034")
@Dependent
public class B18B0034TEST {

  /**
   * テスト対象のクラス
   */
  /** ファイル共通 */
  @Spy
  BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0034");

  @InjectMocks
  B18B0034 b18B0034;

  @Mock
  BatchConfigfileLoader batchConfigfileLoader;

  /** JOB対象 */
  @Mock
  JobContext jobContext;

  @Mock
  S3Utils s3Utils;

  Properties properties = new Properties();

  private Logger log = LoggerFactory.getInstance().getLogger(this);

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
  @Mock
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FSユーザ情報連携テーブル（FS_USERS）Entityのカスタマイズ用DAOクラス。 */
  @Mock
  protected FsUsersDAOCustomize fsUsersDAO;

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
    b18B0034 = Mockito.spy(b18B0034);
    Mockito.doNothing().when(b18B0034).transactionBegin(Mockito.anyString());
    Mockito.doNothing().when(b18B0034).transactionCommit(Mockito.anyString());
    Mockito.doNothing().when(b18B0034).transactionRollback(Mockito.anyString());

    // SQL実行結果をモック化
    Mockito.doReturn(0).when(b18B0034).sqlExecute(Mockito.anyString(), Mockito.anyString());
    Mockito.doReturn(0).when(b18B0034).sqlExecute(Mockito.anyString(), Mockito.anyString(),
        Mockito.any());
    Mockito.doReturn(new ArrayList<>()).when(b18B0034).sqlSelect(Mockito.anyString(),
        Mockito.anyString());
    Mockito.doReturn(new ArrayList<>()).when(b18B0034).sqlSelect(Mockito.anyString(),
        Mockito.anyString(), Mockito.any());

    // AWS S3からダウンロードする際のディレクトリ
    properties.setProperty("fs.log.import.users.s3.directory", "processed/users/");
    // AWS S3からダウンロードする際のファイル名
    properties.setProperty("fs.log.import.users.s3.file.name", "users.csv.gz");
    // ダウンロードディレクトリ
    properties.setProperty("fs.log.import.users.download.directory", "D:/CK/data/fslog/users/");
    // AWS S3からダウンロードして解凍したファイル名
    properties.setProperty("fs.log.import.users.ungz.file.name", "users.csv");

    when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(properties);
    
	//システム環境変数で必要なものだけを差し替える
	Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
	Field theCaseInsensitiveEnvironment = clazz.getDeclaredField("theCaseInsensitiveEnvironment");
	theCaseInsensitiveEnvironment.setAccessible(true);
	Map<String, String> sytemEnviroment = (Map<String, String>) theCaseInsensitiveEnvironment.get(null);
	sytemEnviroment.put("FS_LOG_IMPORT_S3_BUCKET_NAME", "cpn-prd-onprem-recv-sss-vendor-dev-env");
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
      log.info(" [B18B0034TEST] 【******************" + testId + " 実施開始******************】");
    } else {
      log.info(" [B18B0034TEST] 【******************" + testId + " 実施終了******************】");
    }
  }

  @Test
  /**
   * Test case download and execute success
   */
  public void B18B0034_TEST1() {
    b18B0034.executeMode = Constants.GENERAL;
    b18B0034.executeDate = "20210929";
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST001", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

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
   * Test case input = null
   */
  @Test
  public void B18B0034_TEST2() {
    b18B0034.executeMode = null;
    try {
      // テスト開始
      printMsg("TEST002", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST002", "End");
    }
  }

  /**
   * Test case fail to delete file
   */
  @Test
  public void B18B0034_TEST3() {
    b18B0034.executeMode = Constants.GENERAL;
    b18B0034.executeDate = "20210929";
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
    Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST003", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST003", "End");
    }
  }

  /**
   * Test case registOrUpdateFSResultsWithTreatedFlag return false
   */
  @Test
  public void B18B0034_TEST4() {
    b18B0034.executeMode = Constants.GENERAL;
    b18B0034.executeDate = "20210929";
    Mockito.doReturn(false).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST004", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST004", "End");
    }
  }

  /**
   * Test process exception
   * 
   * @throws IOException
   */
  @Test
  public void B18B0034_TEST5() throws IOException {
    b18B0034.executeMode = Constants.LAST_RUN;
    b18B0034.executeDate = "20210929";
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    Mockito.when(b18B0034.sqlExecute(Mockito.anyString(), Mockito.anyString()))
        .thenThrow(MockitoException.class);

    try {
      // テスト開始
      printMsg("TEST005", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST005", "End");
    }
  }

  /**
   * Test process exception InsertFSData() (3.3.2b)BULK
   * INSERTに失敗した場合はエラーメッセージを出力し、戻り値に"1"を設定し処理を終了する。
   * 
   * @throws IOException
   */
  @Test
  public void B18B0034_TEST6() throws IOException {
    b18B0034.executeMode = Constants.LAST_RUN;
    b18B0034.executeDate = "20210929";
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    Mockito.doThrow(ServiceAppException.class).when(fsUsersDAO).insert(Mockito.anyList());

    try {
      // テスト開始
      printMsg("TEST006", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST006", "End");
    }
  }

  /**
   * Test case executeMode != 0, 1
   */
  @Test
  public void B18B0034_TEST7() {
    b18B0034.executeMode = "3";
    b18B0034.executeDate = "20210929";
    try {
      // テスト開始
      printMsg("TEST007", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST007", "End");
    }
  }

  /**
   * Test case executeDate != fomrat yyyyMMdd
   */
  @Test
  public void B18B0034_TEST8() {
    b18B0034.executeMode = "1";
    b18B0034.executeDate = "202109291";
    try {
      // テスト開始
      printMsg("TEST008", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0034.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST008", "End");
    }
  }
}
