package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import java.net.http.HttpClient;
import java.util.Properties;
import javax.batch.runtime.context.JobContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;

/**
 * FS店舗登録・更新・削除バッチのテスト クラスのJUnit
 * 
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class B18B0013TEST extends B18B0013 {

//  /**
//   * テスト対象のクラス
//   */
//  /** ファイル共通 */
//  @Spy
//  BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0013");
//
//  @InjectMocks
//  B18B0013 b18B0013;
//
//  @Mock
//  BatchConfigfileLoader batchConfigfileLoader;
//
//  /** DB共通 */
//  @Mock
//  BatchDBAccessBase batchDBAccessBase;
//
//
//  /** JOB対象 */
//  @Mock
//  JobContext jobContext;
//
//  @Spy
//  HttpClient httpClient;
//
//  Properties properties = new Properties();

  /**
   * テスト初期化処理
   * 
   * @throws Exception スローされた例外
   */
  @Before
  public void setUp() throws Exception {

//    // トランザクションのモック化
//    b18B0013 = Mockito.spy(b18B0013);
//    doNothing().when(b18B0013).transactionBegin(Mockito.anyString());
//    doNothing().when(b18B0013).transactionCommit(Mockito.anyString());
//    doNothing().when(b18B0013).transactionRollback(Mockito.anyString());
//    MockitoAnnotations.initMocks(this);
//
//
//    // MA用配信結果ファイル格納先ディレクトリ
//    properties.setProperty("fs.regist.update.delete.batch.store.api.url.regist",
//        "/reverse-coupon/providers/");
//    // MA用配信結果ファイル名
//    when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(properties);

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
   * 【試験対象】：B18B0013#process()<br>
   * 
   * Test Exception
   * 
   * @throws Exception
   */
  @Test
  public void b18B0013TestException() throws Exception {

//    String returnCD = null;
    // テスト対象のメソッド
//    returnCD = b18B0013.process();
//    // 戻り値の確認
//    assertEquals("1", returnCD);
  }

}
