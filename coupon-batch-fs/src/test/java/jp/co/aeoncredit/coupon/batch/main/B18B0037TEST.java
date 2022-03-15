package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
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

import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsVisitorsDAOCustomize;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * FSログ取込（来店ユーザ） クラスのJUnit
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0037")
@Dependent
public class B18B0037TEST {

	/** FSログ取込（来店ユーザ） */
	@InjectMocks
	private B18B0037 b18B0037;

	/** A JobContext provides information about the current job execution */
	@Mock
	private JobContext jobContext;

	/** バッチの設定ファイル読込ユーティリティクラス */
	@Mock
	private BatchConfigfileLoader batchConfigfileLoader;

	/** バッチのファイルユーティリティクラス */
	@Spy
	private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0037.getBatchId());

	/** Loggerインターフェース。 */
	private Logger log = LoggerFactory.getInstance().getLogger(this);

	/** Properties */
	private Properties properties = new Properties();

	/** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス */
	@Mock
	protected FsResultsDAOCustomize fsResultsDAO;

	/** FS来店ユーザ連携テーブル（FS_VISITORS）Entityのカスタマイズ用DAOクラス。 */
	@Mock
	protected FsVisitorsDAOCustomize fsVisitorsDAO;

	@Mock
	protected S3Client s3Client;

	/** ダウンロードディレクトリ */
	private static final String downloadDirectory = "D:/CK/data/fslog/visitors/";

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
		b18B0037 = Mockito.spy(b18B0037);
		Mockito.doNothing().when(b18B0037).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0037).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0037).transactionRollback(Mockito.anyString());

		// SQL実行結果をモック化
		Mockito.doReturn(0).when(b18B0037).sqlExecute(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(0).when(b18B0037).sqlExecute(Mockito.anyString(), Mockito.anyString(),
				Mockito.any());

		// AWS S3からダウンロードする際のディレクトリ
		properties.setProperty("fs.log.import.visitors.s3.directory", "processed/visitors/");
		// AWS S3からダウンロードする際のファイル名
		properties.setProperty("fs.log.import.visitors.s3.file.name", "visitors.csv.gz");
		// ダウンロードディレクトリ
		properties.setProperty("fs.log.import.visitors.download.directory", downloadDirectory);
		// AWS S3からダウンロードして解凍したファイル名
		properties.setProperty("fs.log.import.visitors.ungz.file.name", "visitors.csv");

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
			log.info(" [B18B0037TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0037TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	/**
	 * Test case download and execute success
	 */
	@Test
	public void B18B0037_TEST1() {
		b18B0037.executeMode = Constants.GENERAL;
		b18B0037.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST001", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	public void B18B0037_TEST2() {
		b18B0037.executeMode = null;
		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	public void B18B0037_TEST3() {
		b18B0037.executeMode = Constants.GENERAL;
		b18B0037.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST003", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	public void B18B0037_TEST4() {
		b18B0037.executeMode = Constants.GENERAL;
		b18B0037.executeDate = "20210929";
		Mockito.doReturn(false).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST004", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	public void B18B0037_TEST5() throws IOException {
		b18B0037.executeMode = Constants.LAST_RUN;
		b18B0037.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Mockito.when(b18B0037.sqlExecute(Mockito.anyString(), Mockito.anyString()))
				.thenThrow(MockitoException.class);

		try {
			// テスト開始
			printMsg("TEST005", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	 * Test case executeMode != 0, 1
	 */
	@Test
	public void B18B0037_TEST6() {
		b18B0037.executeMode = "3";
		b18B0037.executeDate = "20210929";
		try {
			// テスト開始
			printMsg("TEST006", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
	 * Test case executeDate != fomrat yyyyMMdd
	 */
	@Test
	public void B18B0037_TEST7() {
		b18B0037.executeMode = "1";
		b18B0037.executeDate = "202109291";
		try {
			// テスト開始
			printMsg("TEST007", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0037.process();

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
}
