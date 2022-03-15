package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.http.HttpClient;
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
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.common.BatchLogger;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetOutputDTO;
import jp.co.aeoncredit.coupon.dao.custom.ExternalApiUserDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.SiteStatisticsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.SiteStatistics;

/**
 * B18B0049 Junit
 * @author matsui
 *
 */
public class B18B0049TEST {
	private final String AUTH_JSON_STRING = "{\n"
			+ "\"status\": \"OK\",\n"
			+ "\"result\": {\n"
			+ "\"auth_token\": \"161d66dd6194452d9e47eb850d3bbb58\"\n"
			+ "}\n"
			+ "}";
	
	
	private static final String BATCH_ID = BatchInfo.B18B0049.getBatchId();

    @Spy
    BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0049");

    @InjectMocks
    B18B0049 b18b0049;

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
    
    @Mock
	ExternalApiUserDAOCustomize externalApiUserDAO;
    
	@Mock
	private SiteStatisticsDAOCustomize sitestatisticsDAO;
	
	
	/**追加したもの*/
	@Mock
	private RegisterInfoListGetOutputDTO dto;
	
	@Mock
	private String errorMessage;
	
	@Mock
	private BatchLogger batchLogger = new BatchLogger(BatchInfo.B18B0049.getBatchId());

    
    Properties properties = new Properties();

    private Logger log = LoggerFactory.getInstance().getLogger(this);
    
    /**
     * テスト初期化処理
     * 
     * @throws Exception スローされた例外
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
         b18b0049 = Mockito.spy(b18b0049);
        
         // transaction
        doNothing().when(b18b0049).transactionBegin(Mockito.anyString());
        doNothing().when(b18b0049).transactionRollback(Mockito.anyString());
        
        // properties(デフォルト)
        properties.setProperty("fs.site.statistics.get.batch.api.url", "/mapi/3.1/user/");
        properties.setProperty("fs.site.statistics.get.batch.retry.count", "3");
        properties.setProperty("fs.site.statistics.get.retry.sleep.time", "5000");
        properties.setProperty("fs.site.statistics.get.timeout.duration", "5");
        when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(properties);
        //readPropertyを呼んでいる↑
        
        // 認証処理(親クラス部分）
        // httpClientはメソッド内のローカル変数なのでモック化できない
//        Mockito.doReturn(new HttpResponseStringMock(200, AUTH_JSON_STRING)).when(httpClient).send(Mockito.any(), Mockito.any());
        //API呼び出し↑
        
        // daoはインスタンス変数なんのでモックにできる
//        Mockito.doReturn(Optional.ofNullable(new ExternalApiUser())).when(externalApiUserDAO).findById(Mockito.any());
        
        // publicメソッド、public enumにすれば差し替えられるが、authToken変数が変更できない
//        Mockito.doReturn(BatchFSApiCalloutBase.AuthTokenResult.SUCCESS).when(b18b0049).getAuthToken(Mockito.anyString());
        
        // dao
        doNothing().when(sitestatisticsDAO).insert(Mockito.any(SiteStatistics.class));
    }	//DBに登録しない　SiteStatisticsには何も登録しない

    /**
     * テスト終了処理
     * 
     * @throws Exception スローされた例外
     */
    @After
    public void tearDown() throws Exception {
    }
//    
//    @Test
//    public void test_ERROR1(){
//    	//エラーメッセージがでるかのテスト
//    	
//    	
//    	
//    	//テスト開始
//    	printMsg("test_ERROR1", "Start");
//    	try {
//    		
//  //  		Mockito.when(b18b0049.createErrorMessage(1, dto, "device",null)).thenReturn(batchLogger.createMsg(BusinessMessageCode.B18MB924.toString(), errorMessage));
//    		
//   // 		String error = b18b0049.createErrorMessage(404,dto,"iphone",null);
//    		
//  //  		assertEquals("B18MB924:登録者情報一覧取得のAPI連携に失敗しました。（HTTPレスポンスコード ＝「404」,エラー内容 = 「デバイス =iphone)",error);
//    		
//    	}catch(Exception e) {
//    		e.printStackTrace();
//    		throw e;
//    	}finally {
//    		printMsg("test_ERROR1","End");
//    	}
//    	
//    }
    
//    @Test
//    public void test_ERROR2() {
//
//    	//テスト開始
//    	printMsg("test_ERROR1", "Start");
//    	
//    	try {
//    		
//    	}catch(Exception e) {
//    		e.printStackTrace();
//    		throw e;
//    	}finally {
//    		printMsg("test_ERROR2","End");
//    	}
//    	
//    }
//
//    @Test
//    public void test_SAMPLE() throws Exception{
//    	// テストケース固有のセットアップ
//        // httpClientが差し替えれないが、本来ならFS APIの戻り値や結果をケースに合わせて変更する。
//
//        // テスト開始
//        printMsg("test_SAMPLE", "Start");
//        try {
//            // テスト対象のメソッド
//            String returnCD = b18b0049.process();
//            // 戻り値の確認
//            assertEquals("0", returnCD);
//;        } catch (Exception e) {
//        	e.printStackTrace();
//        	throw e;
//        } finally {
//            printMsg("test_SAMPLE", "End");
//        }
//    }

    /**
     * テスト開始終了メッセージ出力
     * @param testId テストID
     * @param processType 処理区分(開始/終了)
     */
    public void printMsg(String testId, String processType) {
        if (processType.equals("Start")) {
            log.info(" [B18B0049TEST] 【******************" + testId + " 実施開始******************】");
        }else {
            log.info(" [B18B0049TEST] 【******************" + testId + " 実施終了******************】");
        }

    }

}
