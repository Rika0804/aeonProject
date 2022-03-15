package jp.co.aeoncredit.coupon.batch.main;

import java.net.http.HttpClient;
import java.util.Properties;
import javax.batch.runtime.context.JobContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;

/**
 * FSクーポン公開停止バッチのテスト クラスのJUnit
 * 
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class B18B0053TEST extends B18B0053 {

  /**
   * テスト対象のクラス
   */
  /** ファイル共通 */
  @Spy
  BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0053");

  @InjectMocks
  B18B0053 b18B0053;

  @Mock
  BatchConfigfileLoader batchConfigfileLoader;

  /** DB共通 */
  @Mock
  BatchDBAccessBase batchDBAccessBase;


  /** JOB対象 */
  @Mock
  JobContext jobContext;

  @Spy
  HttpClient httpClient;

  Properties properties = new Properties();

  /**
   * テスト初期化処理
   * 
   * @throws Exception スローされた例外
   */
  @Before
  public void setUp() throws Exception {

    // トランザクションのモック化
  }
  
  @Test
  public void test01() throws Exception {
	  
  }

  /**
   * テスト終了処理
   * 
   * @throws Exception スローされた例外
   */
  @After
  public void tearDown() throws Exception {

  }


}
