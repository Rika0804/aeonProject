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
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsFacilitiesDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;

@Named("B18B0036")
@Dependent
public class B18B0036TEST {

	/** ファイル共通 */
	@Spy
	BatchFileHandler batchFileHandler = new BatchFileHandler("B18B0036");

	@InjectMocks
	B18B0036 b18B0036;

	@Mock
	BatchConfigfileLoader batchConfigfileLoader;

	/** JOB対象 */
	@Mock
	JobContext jobContext;

	Properties properties = new Properties();

	private Logger log = LoggerFactory.getInstance().getLogger(this);

	/** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス。 */
	@Mock
	protected FsResultsDAOCustomize fsResultsDAO;

	/** FS登録店舗データ連携テーブル（FS_FACILITIES）Entityのカスタマイズ用DAOクラス。 */
	@Mock
	protected FsFacilitiesDAOCustomize fsFacilitiesDAO;

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
		b18B0036 = Mockito.spy(b18B0036);
		Mockito.doNothing().when(b18B0036).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0036).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0036).transactionRollback(Mockito.anyString());

		// SQL実行結果をモック化
		Mockito.doReturn(0).when(b18B0036).sqlExecute(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(0).when(b18B0036).sqlExecute(Mockito.anyString(), Mockito.anyString(),
				Mockito.any());
		Mockito.doReturn(new ArrayList<>()).when(b18B0036).sqlSelect(Mockito.anyString(),
				Mockito.anyString());
		Mockito.doReturn(new ArrayList<>()).when(b18B0036).sqlSelect(Mockito.anyString(),
				Mockito.anyString(), Mockito.any());

		// AWS S3からダウンロードする際のディレクトリ
		properties.setProperty("fs.log.import.facilities.s3.directory", "processed/facilities/");
		// AWS S3からダウンロードする際のファイル名
		properties.setProperty("fs.log.import.facilities.s3.file.name", "facilities.csv.gz");
		// ダウンロードディレクトリ
		properties.setProperty("fs.log.import.facilities.download.directory",
				"D:/dsdb_data/cpn/nfs/ckbtwork/fslog/facilities/");
		// AWS S3からダウンロードして解凍したファイル名
		properties.setProperty("fs.log.import.facilities.ungz.file.name", "facilities.csv");

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
			log.info(" [B18B0036TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0036TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	//	@Test
	//	/**
	//	 * Test case download and execute success
	//	 */
	//	public void B18B0036_TEST1() {
	//		b18B0036.executeMode = Constants.GENERAL;
	//		b18B0036.executeDate = "20210929";
	//		Mockito.doReturn(true).when(fsResultsDAO)
	//				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
	//
	//		try {
	//			// テスト開始
	//			printMsg("TEST001", "Start");
	//
	//			// テスト対象のメソッド
	//			String returnCD = b18B0036.process();
	//
	//			// 戻り値の確認
	//			assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//			fail();
	//		} finally {
	//			// テスト終了
	//			printMsg("TEST001", "End");
	//		}
	//	}

	/**
	 * Test case input = null
	 */
	@Test
	public void B18B0036_TEST2() {
		b18B0036.executeMode = null;
		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	public void B18B0036_TEST3() {
		b18B0036.executeMode = Constants.GENERAL;
		b18B0036.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST003", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	public void B18B0036_TEST4() {
		b18B0036.executeMode = Constants.GENERAL;
		b18B0036.executeDate = "20210929";
		Mockito.doReturn(false).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST004", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	public void B18B0036_TEST5() throws IOException {
		b18B0036.executeMode = Constants.LAST_RUN;
		b18B0036.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Mockito.when(b18B0036.sqlExecute(Mockito.anyString(), Mockito.anyString()))
				.thenThrow(MockitoException.class);

		try {
			// テスト開始
			printMsg("TEST005", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	 * Test process exception
	 * 
	 * @throws IOException
	 */
	@Test
	public void B18B0036_TEST6() throws IOException {
		b18B0036.executeMode = Constants.LAST_RUN;
		b18B0036.executeDate = "20210929";
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Mockito.doThrow(ServiceAppException.class).when(fsFacilitiesDAO).insert(Mockito.anyList());

		try {
			// テスト開始
			printMsg("TEST006", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	public void B18B0036_TEST7() {
		b18B0036.executeMode = "3";
		b18B0036.executeDate = "20210929";
		try {
			// テスト開始
			printMsg("TEST007", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
	public void B18B0036_TEST8() {
		b18B0036.executeMode = "1";
		b18B0036.executeDate = "202109291";
		try {
			// テスト開始
			printMsg("TEST008", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0036.process();

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
