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

import com.ibm.jp.awag.common.logic.ServiceAppException;
import com.ibm.jp.awag.common.util.Logger;
import com.ibm.jp.awag.common.util.LoggerFactory;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.FsImportDataProcessMode;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.dao.custom.FsIdlinkDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.entity.FsIdlink;

/**
 * FSログ取込（ひも付きデータ） クラスのJUnit
 * 
 * @author ngotrungkien
 * @version 1.0
 */
@Named("B18B0039")
@Dependent
public class B18B0039TEST {

	/** FSログ取込（ひも付きデータ） */
	@InjectMocks
	private B18B0039 b18B0039;

	/** A JobContext provides information about the current job execution */
	@Mock
	private JobContext jobContext;

	/** バッチの設定ファイル読込ユーティリティクラス */
	@Mock
	private BatchConfigfileLoader batchConfigfileLoader;

	/** バッチのファイルユーティリティクラス */
	@Spy
	private BatchFileHandler batchFileHandler = new BatchFileHandler(BatchInfo.B18B0039.getBatchId());

	/** Loggerインターフェース。 */
	private Logger log = LoggerFactory.getInstance().getLogger(this);

	/** Properties */
	private Properties properties = new Properties();

	/** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス */
	@Mock
	protected FsResultsDAOCustomize fsResultsDAO;

	/** FSひも付きデータ連携テーブル（FS_IDLINK）Entityのカスタマイズ用DAOクラス。 */
	@Mock
	protected FsIdlinkDAOCustomize fsIdlinkDAO;

	/** ダウンロードディレクトリ */
	private static final String downloadDirectory = "D:/dsdb_data/cpn/nfs/ckbtwork/fslog/idlink/";

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
		b18B0039 = Mockito.spy(b18B0039);
		Mockito.doNothing().when(b18B0039).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0039).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0039).transactionRollback(Mockito.anyString());

		// SQL実行結果をモック化
		Mockito.doReturn(0).when(b18B0039).sqlExecute(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(0).when(b18B0039).sqlExecute(Mockito.anyString(), Mockito.anyString(),
				Mockito.any());

		// AWS S3からダウンロードする際のディレクトリ（差分）
		properties.setProperty("fs.log.import.idlink.diff.s3.directory", "processed/idlink_diff/");
		// AWS S3からダウンロードする際のファイル名（差分）
		properties.setProperty("fs.log.import.idlink.diff.s3.file.name", "idlink_diff.csv.gz");
		// AWS S3からダウンロードして解凍したファイル名（差分）
		properties.setProperty("fs.log.import.idlink.diff.ungz.file.name", "idlink_diff.csv");
		// AWS S3からダウンロードする際のディレクトリ（全量）
		properties.setProperty("fs.log.import.idlink.full.s3.directory", "processed/idlink_full/");
		// AWS S3からダウンロードする際のファイル名（全量）
		properties.setProperty("fs.log.import.idlink.full.s3.file.name", "idlink_full.csv.gz");
		// AWS S3からダウンロードして解凍したファイル名（全量）
		properties.setProperty("fs.log.import.idlink.full.ungz.file.name", "idlink_full.csv");
		// ダウンロードディレクトリ
		properties.setProperty("fs.log.import.idlink.download.directory", downloadDirectory);

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
			log.info(" [B18B0039TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0039TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	/**
	 * Test case success mode diff
	 */
	@Test
	public void B18B0039_TEST1() {
		b18B0039.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
		b18B0039.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST001", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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
	 * Test case success mode full
	 */
	@Test
	public void B18B0039_TEST2() {
		b18B0039.modeSpecification = FsImportDataProcessMode.FULL.getValue();
		b18B0039.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST001", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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
	public void B18B0039_TEST3() {
		b18B0039.modeSpecification = null;
		b18B0039.executeMode = null;
		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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
	 * Test case fail to delete file mode diff
	 */
	@Test
	public void B18B0039_TEST4() {
		b18B0039.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
		b18B0039.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST003", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

			// 戻り値の確認
			assertEquals(ProcessResult.SUCCESS.getValue(), returnCD);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了
			printMsg("TEST003", "End");
		}
	}

//	/**
//	 * Test case fail to delete file mode full
//	 */
//	@Test
//	public void B18B0039_TEST5() {
//		b18B0039.modeSpecification = FsImportDataProcessMode.FULL.getValue();
//		b18B0039.executeMode = Constants.GENERAL;
//		Mockito.doReturn(true).when(fsResultsDAO)
//				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
//		Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());
//
//		try {
//			// テスト開始
//			printMsg("TEST003", "Start");
//
//			// テスト対象のメソッド
//			String returnCD = b18B0039.process();
//
//			// 戻り値の確認
//			assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了
//			printMsg("TEST003", "End");
//		}
//	}

	/**
	 * Test case registOrUpdateFSResultsWithTreatedFlag return false mode diff
	 */
	@Test
	public void B18B0039_TEST6() {
		b18B0039.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
		b18B0039.executeMode = Constants.GENERAL;
		Mockito.doReturn(false).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST004", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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
	 * Test case registOrUpdateFSResultsWithTreatedFlag return false mode full
	 */
	@Test
	public void B18B0039_TEST7() {
		b18B0039.modeSpecification = FsImportDataProcessMode.FULL.getValue();
		b18B0039.executeMode = Constants.GENERAL;
		Mockito.doReturn(false).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST004", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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
	 * Test process exception mode diff
	 * 
	 * @throws IOException
	 */
	@Test
	public void B18B0039_TEST8() throws IOException {
		b18B0039.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
		b18B0039.executeMode = Constants.LAST_RUN;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Mockito.doThrow(ServiceAppException.class).when(fsResultsDAO)
				.updateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST006", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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

//	/**
//	 * Test process exception mode full
//	 * 
//	 * @throws IOException
//	 */
//	@Test
//	public void B18B0039_TEST9() throws IOException {
//		b18B0039.modeSpecification = FsImportDataProcessMode.FULL.getValue();
//		b18B0039.executeMode = Constants.LAST_RUN;
//		Mockito.doReturn(true).when(fsResultsDAO)
//				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
//
//		Mockito.doThrow(ServiceAppException.class).when(fsResultsDAO)
//				.updateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
//
//		try {
//			// テスト開始
//			printMsg("TEST006", "Start");
//
//			// テスト対象のメソッド
//			String returnCD = b18B0039.process();
//
//			// 戻り値の確認
//			assertEquals(ProcessResult.FAILURE.getValue(), returnCD);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了
//			printMsg("TEST006", "End");
//		}
//	}

	/**
	 * Test exception processInsertFSData mode diff
	 * 
	 * @throws IOException
	 */
	@Test
	public void B18B0039_TEST10() throws IOException {
		b18B0039.modeSpecification = FsImportDataProcessMode.DIFF.getValue();
		b18B0039.executeMode = Constants.LAST_RUN;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Mockito.doThrow(ServiceAppException.class).when(fsIdlinkDAO).insert(Mockito.<FsIdlink> anyList());

		try {
			// テスト開始
			printMsg("TEST006", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0039.process();

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

}
