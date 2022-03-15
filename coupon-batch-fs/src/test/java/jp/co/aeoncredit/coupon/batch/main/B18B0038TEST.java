package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.ibm.jp.awag.common.logic.ServiceDBException;
import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsImportDataProcessMode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsSegmentDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsSegmentMatchUserDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsSegment;
import jp.co.aeoncredit.coupon.entity.FsSegmentMatchUser;

/**
 * FSログ取込（セグメントマッチユーザー） クラスのJUnit
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0038")
@Dependent
public class B18B0038TEST {

  /** FSログ取込（セグメントマッチユーザー） */
  @InjectMocks
  private B18B0038 b18B0038;

  /** A JobContext provides information about the current job execution */
  @Mock
  private JobContext jobContext;

  /** バッチの設定ファイル読込ユーティリティクラス */
  @Mock
  private BatchConfigfileLoader batchConfigfileLoader;

  /** バッチのファイルユーティリティクラス */
  @Spy
  private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0038.getBatchId());

  /** Loggerインターフェース。 */
  private Logger log = LoggerFactory.getInstance().getLogger(this);

  /** Properties */
  private Properties properties = new Properties();

  /** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス */
  @Mock
  protected FsResultsDAOCustomize fsResultsDAO;

  /** FSセグメントマッチユーザー連携テーブル（FS_SEGMENT_MATCH_USER）Entityのカスタマイズ用DAOクラス。 */
  @Mock
  protected FsSegmentMatchUserDAOCustomize fsSegmentMatchUserDAO;

  /** FSセグメント連携テーブル（FS_SEGMENT）Entityのカスタマイズ用DAOクラス。 */
  @Mock
  protected FsSegmentDAOCustomize fsSegmentDAO;

  /** ダウンロードディレクトリ */
  private static final String downloadDirectory = "D:/dsdb_data/cpn/nfs/ckbtwork/fslog/segment/";

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
    b18B0038 = Mockito.spy(b18B0038);
    Mockito.doNothing().when(b18B0038).transactionBegin(Mockito.anyString());
    Mockito.doNothing().when(b18B0038).transactionCommit(Mockito.anyString());
    Mockito.doNothing().when(b18B0038).transactionRollback(Mockito.anyString());

    // SQL実行結果をモック化
    Mockito.doReturn(0).when(b18B0038).sqlExecute(Mockito.anyString(), Mockito.anyString());
    Mockito.doReturn(0).when(b18B0038).sqlExecute(Mockito.anyString(), Mockito.anyString(),
        Mockito.any());

    // #AWS S3からダウンロードする際のディレクトリ
    properties.setProperty("fs.log.import.segment.match.user.s3.directory", "processed/segment/");
    // AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー差分)
    properties.setProperty("fs.log.import.segment.match.user.s3.file.name",
        "segment_customerid_diff.csv.gz");
    // AWS S3からダウンロードする際のファイル名(セグメントマスタ差分)
    properties.setProperty("fs.log.import.segment.match.user.s3.master.file.name",
        "segment_name_diff.csv.gz");
    // ダウンロードディレクトリ
    properties.setProperty("fs.log.import.segment.match.user.download.directory",
        downloadDirectory);
    // AWS S3からダウンロードして解凍したファイル名
    properties.setProperty("fs.log.import.segment.ungz.file.name", "segment_customer.csv");
    // AWS S3からダウンロードして解凍したファイル名(セグメントマスタ)
    properties.setProperty("fs.log.import.segment.ungz.master.file.name", "segment_name.csv");
    // AWS S3からダウンロードする際のファイル名(セグメントマッチユーザー全件)
    properties.setProperty("fs.log.import.segment.match.user.s3.file.all.name",
        "segment_customerid_all.csv.gz");
    // AWS S3からダウンロードする際のファイル名(セグメントマスタ全件)
    properties.setProperty("fs.log.import.segment.match.user.s3.master.file.all.name",
        "segment_name_all.csv.gz");
    // AWS S3からダウンロードして解凍したファイル名(セグメントマッチユーザー全件)
    properties.setProperty("fs.log.import.segment.ungz.file.all.name", "segment_customer_all.csv");
    // AWS S3からダウンロードして解凍したファイル名(セグメントマスタ全件)
    properties.setProperty("fs.log.import.segment.ungz.master.file.all.name",
        "segment_name_all.csv");
    // 全件モードの場合のコミット単位
    properties.setProperty("fs.log.import.segment.commit.unit", "5000");
    when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(properties);

    // システム環境変数で必要なものだけを差し替える
    Class<?> clazz = Class.forName("java.lang.ProcessEnvironment");
    Field theCaseInsensitiveEnvironment = clazz.getDeclaredField("theCaseInsensitiveEnvironment");
    theCaseInsensitiveEnvironment.setAccessible(true);
    Map<String, String> sytemEnviroment =
        (Map<String, String>) theCaseInsensitiveEnvironment.get(null);
    sytemEnviroment.put("FS_LOG_IMPORT_S3_BUCKET_NAME", "cpn-prd-onprem-recv-sss-vendor-dev-env");
  }

  /**
   * テスト終了処理
   * 
   * @throws Exception スローされた例外
   */
  @After
  public void tearDown() throws Exception {}

  /**
   * テスト開始終了メッセージ出力
   * 
   * @param testId テストID
   * @param processType 処理区分（Start/End）
   */
  public void printMsg(String testId, String processType) {
    if (processType.equals("Start")) {
      log.info(" [B18B0038TEST] 【******************" + testId + " 実施開始******************】");
    } else {
      log.info(" [B18B0038TEST] 【******************" + testId + " 実施終了******************】");
    }
  }

  /**
   * Test AWS S3ダウンロード success mode diff, 通常
   */
  @SuppressWarnings("unchecked")
  @Test
  public void B18B0038_TEST1() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.GENERAL;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    FsSegmentMatchUser fsSegmentMatchUser = new FsSegmentMatchUser();
    Mockito.doReturn(Optional.of(fsSegmentMatchUser)).when(fsSegmentMatchUserDAO)
        .findOne(Mockito.any(), Mockito.nullable(List.class));

    Object[] object = {"1", "2"};
    List<Object[]> userMatchSegmentIdList = new ArrayList<>();
    userMatchSegmentIdList.add(object);
    Mockito.doReturn(userMatchSegmentIdList).when(b18B0038).sqlSelect(Mockito.anyString(),
        Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST001", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

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
   * Test not FS実績登録テーブルに処理開始を登録する。
   */
  @Test
  public void B18B0038_TEST2() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.GENERAL;
    Mockito.doReturn(false).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST002", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST002", "End");
    }
  }

  /**
   * Test 実行モード null
   */
  @Test
  public void B18B0038_TEST3() {
    b18B0038.executeMode = null;
    try {
      // テスト開始
      printMsg("TEST003", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

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
   * Test 差分/全件モード指定 null
   */
  @Test
  public void B18B0038_TEST4() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.modeSpecification = null;
    try {
      // テスト開始
      printMsg("TEST004", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

      // 戻り値の確認
      assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST004", "End");
    }
  }

  /**
   * Test AWS S3ダウンロード exception
   */
  @Test
  public void B18B0038_TEST5() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.GENERAL;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST005", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

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
   * Test case delete file false
   */
  @Test
  public void B18B0038_TEST6() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.GENERAL;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
    Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());
    try {
      // テスト開始
      printMsg("TEST006", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

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
   * Test Insert data for table FSセグメント連携テーブル exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void B18B0038_TEST7() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.GENERAL;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    FsSegmentMatchUser fsSegmentMatchUser = new FsSegmentMatchUser();
    Mockito.doReturn(Optional.of(fsSegmentMatchUser)).when(fsSegmentMatchUserDAO)
        .findOne(Mockito.any(), Mockito.nullable(List.class));

    Object[] object = {"1", "2"};
    List<Object[]> userMatchSegmentIdList = new ArrayList<>();
    userMatchSegmentIdList.add(object);
    Mockito.doReturn(userMatchSegmentIdList).when(b18B0038).sqlSelect(Mockito.anyString(),
        Mockito.anyString());

    Mockito.doThrow(ServiceDBException.class).when(fsSegmentDAO)
        .insert(Mockito.any(FsSegment.class));

    try {
      // テスト開始
      printMsg("TEST007", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

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
   * Test AWS S3ダウンロード mode diff , ラストラン
   */
  @SuppressWarnings("unchecked")
  @Test
  public void B18B0038_TEST8() {
    b18B0038.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
    b18B0038.executeMode = Constants.LAST_RUN;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    FsSegmentMatchUser fsSegmentMatchUser = new FsSegmentMatchUser();
    Mockito.doReturn(Optional.of(fsSegmentMatchUser)).when(fsSegmentMatchUserDAO)
        .findOne(Mockito.any(), Mockito.nullable(List.class));

    Object[] object = {"1", "2"};
    List<Object[]> userMatchSegmentIdList = new ArrayList<>();
    userMatchSegmentIdList.add(object);
    Mockito.doReturn(userMatchSegmentIdList).when(b18B0038).sqlSelect(Mockito.anyString(),
        Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST008", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST008", "End");
    }
  }

  /**
   * Test AWS S3ダウンロード success mode full , ラストラン
   */
  @SuppressWarnings("unchecked")
  @Test
  public void B18B0038_TEST9() {
    b18B0038.modeSpecification = FsImportDataProcessMode.FULL.getValue();
    b18B0038.executeMode = Constants.LAST_RUN;
    Mockito.doReturn(true).when(fsResultsDAO)
        .registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

    FsSegmentMatchUser fsSegmentMatchUser = new FsSegmentMatchUser();
    Mockito.doReturn(Optional.of(fsSegmentMatchUser)).when(fsSegmentMatchUserDAO)
        .findOne(Mockito.any(), Mockito.nullable(List.class));

    Object[] object = {"1", "2"};
    List<Object[]> userMatchSegmentIdList = new ArrayList<>();
    userMatchSegmentIdList.add(object);
    Mockito.doReturn(userMatchSegmentIdList).when(b18B0038).sqlSelect(Mockito.anyString(),
        Mockito.anyString());

    try {
      // テスト開始
      printMsg("TEST008", "Start");

      // テスト対象のメソッド
      String returnCD = b18B0038.process();

      // 戻り値の確認
      assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    } finally {
      // テスト終了
      printMsg("TEST008", "End");
    }
  }

}
