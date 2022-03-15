/**
 * 
 */
package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFileHandler;
import jp.co.aeoncredit.coupon.batch.constants.BatchInfo;
import jp.co.aeoncredit.coupon.batch.constants.Constants;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.dao.custom.CouponSequenceCountDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.CouponsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponAcquisitionResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponDeliveryResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUseResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsCouponUsersDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsEventsForCouponDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsResultsDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.FsUserEventDAOCustomize;
import jp.co.aeoncredit.coupon.dao.custom.MstAppUsersDAOCustomize;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsCouponDeliveryResults;
import jp.co.aeoncredit.coupon.entity.FsCouponUsers;
import jp.co.aeoncredit.coupon.entity.FsEventsForCoupon;
import jp.co.aeoncredit.coupon.entity.FsUserEvent;
import jp.co.aeoncredit.coupon.entity.MstAppUsers;

/**
 * B18B0030_FSログ取込（アプリ利用イベント）のテスト クラスのJUnit
 */
@Named("B18B0030")
@Dependent
public class B18B0030TEST {
	@InjectMocks
	B18B0030 b18B0030;

	@Mock
	JobContext jobContext;

	@Mock
	BatchConfigfileLoader batchConfigfileLoader;

	@Mock
	BatchDBAccessBase batchDBAccessBase;

	@Mock
	protected UserTransaction userTransaction;

	@Spy
	BatchFileHandler batchFileHandler = new BatchFileHandler(BATCH_ID);

	private Logger log = LoggerFactory.getInstance().getLogger(this);

	private Properties properties = new Properties();

	private static final String BATCH_ID = BatchInfo.B18B0030.getBatchId();

	/** FS実績登録テーブル(FS_RESULTS)EntityのDAOクラス */
	@Mock
	FsResultsDAOCustomize fsResultsDAO;

	/** FSユーザイベントテーブル(FS_USER_EVENT)EntityのDAOクラス */
	@Mock
	FsUserEventDAOCustomize fsUserEventDAO;

	/** クーポンテーブル(COUPONS)EntityのDAOクラス */
	@Mock
	CouponsDAOCustomize couponsDAO;

	/** FSクーポン実績連携テーブル(FS_EVENTS_FOR_COUPON)EntityのDAOクラス */
	@Mock
	FsEventsForCouponDAOCustomize fsEventsForCouponDAO;

	/** FSクーポンユーザテーブル(FS_COUPON_USER)EntityのDAOクラス */
	@Mock
	FsCouponUsersDAOCustomize fsCouponUsersDAO;

	/** アプリユーザマスタテーブル(MST_APP_USER)EntityのDAOクラス */
	@Mock
	MstAppUsersDAOCustomize mstAppUsersDAO;

	/** FSクーポン配信実績テーブル(FS_COUPONDELIVERY_RESULTS)EntityのDAOクラス */
	@Mock
	FsCouponDeliveryResultsDAOCustomize fsCouponDeliveryResultsDAO;

	/** FSクーポン取得実績テーブル(FS_COUPON_ACQUISITION_RESULTS)EntityのDAOクラス */
	@Mock
	FsCouponAcquisitionResultsDAOCustomize fsCouponAcquisitionResultsDAO;

	/** FSクーポン利用実績テーブル(FS_COUPON_USE_RESULTS)EntityのDAOクラス */
	@Mock
	FsCouponUseResultsDAOCustomize fsCouponUseResultsDAO;

	/** クーポンシーケンス取得テーブル(COUPON_SEQUENCE_COUNT)EntityのDAOクラス */
	@Mock
	CouponSequenceCountDAOCustomize couponSequenceCountDAO;

	// 設定値
	// private static final String downloadDirectory = "/CK/data/fslog/event/";
	// private static final String resultFileDirectory = "/CK/data/WT/snd/";
	private static final String downloadDirectory = "C:/B18B0030/";
	private static final String resultFileDirectory = "C:/B18B0030/MA/";

	// サンプルデータ
	private static final String SAMPLE_TARGET_DIRECTORY = "/processed/events/2021/08/23/";
	private static final Long SAMPLE_COUPON_ID = Long.valueOf(1);
	private static final String SAMPLE_ACS_USER_CARD_ID = "SAMPLE_ACS_USER_CARD_ID";
	private static final String SAMPLE_ACS_USER_CARD_FAMILY_CD = "SAMPLE_ACS_USER_CARD_FAMILY_CD";
	private static final String SAMPLE_COMMON_INSIDE_ID = "SAMPLE_COMMON_INSIDE_ID";
	private static final Short SAMPLE_COUNT_FLAG = 0;

	// パラメータ名
	private static final String PARAM_COUPON_ID = "couponId";

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
		b18B0030 = Mockito.spy(b18B0030);
		Mockito.doNothing().when(b18B0030).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0030).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0030).transactionRollback(Mockito.anyString());

		// SQL実行結果をモック化
		Mockito.doReturn(0).when(b18B0030).sqlExecute(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(0).when(b18B0030).sqlExecute(Mockito.anyString(), Mockito.anyString(),
				Mockito.any());
		Mockito.doReturn(new ArrayList<>()).when(b18B0030).sqlSelect(Mockito.anyString(),
				Mockito.anyString());
		Mockito.doReturn(new ArrayList<>()).when(b18B0030).sqlSelect(Mockito.anyString(),
				Mockito.anyString(), Mockito.any());

		// AWS S3からダウンロードする際のディレクトリ
		properties.setProperty("fs.log.import.events.s3.directory", "processed/events/");
		// AWS S3からダウンロードする際のファイル名
		properties.setProperty("fs.log.import.events.s3.file.name", "events_coupon.csv.gz");
		// ダウンロードディレクトリ
		properties.setProperty("fs.log.import.events.download.directory", downloadDirectory);
		// MA用配信結果ファイル格納先ディレクトリ
		properties.setProperty("fs.log.import.events.result.file.directory", resultFileDirectory);
		// AWS S3からダウンロードして解凍したファイル名
		properties.setProperty("fs.log.import.events.ungz.file.name", "events_coupon.csv");

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
			log.info(" [B18B0030TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0030TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	@Test
	public void B18B0030_TEST001() {
		b18B0030.executeMode = Constants.GENERAL;
		// FS実績登録を取得をモック化
		Mockito.doReturn(Optional.empty()).when(fsResultsDAO).findOne(Mockito.any(), Mockito.anyList());

		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		// FSユーザイベント登録をモック化
		Mockito.doNothing().when(fsUserEventDAO).insert(Mockito.any(FsUserEvent.class));

		// クーポン取得をモック化
		Mockito.doReturn(Optional.empty()).when(couponsDAO).findOne(Mockito.any(), Mockito.anyList());

		// FSクーポン実績連携登録をモック化
		Mockito.doNothing().when(fsEventsForCouponDAO).insert(Mockito.any(FsEventsForCoupon.class));

		// FSクーポン配信実績登録をモック化
		Mockito.doNothing().when(fsCouponDeliveryResultsDAO)
				.insert(Mockito.any(FsCouponDeliveryResults.class));

		try {
			// テスト開始
			printMsg("TEST001", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	public void B18B0030_TEST002() {
		b18B0030.executeMode = null;
		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case registOrUpdateFSResultsWithTreatedFlag return false
	 */
	@Test
	public void B18B0030_TEST003() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(false).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case delete file fail
	 */
	@Test
	public void B18B0030_TEST004() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(false).when(batchFileHandler).deleteFile(Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case CSV file is empty
	 */
	@Test
	public void B18B0030_TEST005() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Map<Integer, Map<String, String>> dataMap = new HashMap<Integer, Map<String, String>>();

		Mockito.doReturn(dataMap).when(batchFileHandler).loadFromCSVFile(Mockito.anyString(),
				Mockito.anyBoolean(), Mockito.any());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case coupon is not empty and couponType is 3 (PASSPORT)
	 * // (3.3.4a)【クーポンテーブル】.「クーポン種別」が「3:パスポートクーポン」の場合
	 */
	@Test
	public void B18B0030_TEST006() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Coupons coupon = new Coupons();
		coupon.setCouponType(CouponType.PASSPORT.getValue());
		// FS実績登録を取得をモック化
		Mockito.doReturn(Optional.of(coupon)).when(couponsDAO).findOne(Mockito.any(), Mockito.any());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case coupon is not empty and couponType is not 3 (PASSPORT)
	 * / (3.3.4b)上記以外の場合 (1:マスクーポン、2:ターゲットクーポン、4:アプリイベントクーポン、5:センサーイベントクーポン)
	 */
	@Test
	public void B18B0030_TEST007() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Coupons coupon = new Coupons();
		coupon.setCouponType(CouponType.MASS.getValue());
		// FS実績登録を取得をモック化
		Mockito.doReturn(Optional.of(coupon)).when(couponsDAO).findOne(Mockito.any(), Mockito.any());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case coupon is not empty and couponType is 3 (PASSPORT) and fsCouponUsersList is not empty
	 */
	@Test
	public void B18B0030_TEST008() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Coupons coupon = new Coupons();
		coupon.setCouponType(CouponType.PASSPORT.getValue());
		Mockito.doReturn(Optional.of(coupon)).when(couponsDAO).findOne(Mockito.any(), Mockito.any());

		List<FsCouponUsers> fsCouponUsersList = new ArrayList<FsCouponUsers>();
		fsCouponUsersList.add(new FsCouponUsers());
		Mockito.doReturn(fsCouponUsersList).when(fsCouponUsersDAO).find(Mockito.any(), Mockito.any(),
				Mockito.anyBoolean(), Mockito.any());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case coupon is not empty and couponType is not 3 (PASSPORT) and MstAppUsers is not empty
	 */
	@Test
	public void B18B0030_TEST009() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		Coupons coupon = new Coupons();
		coupon.setCouponType(CouponType.MASS.getValue());
		Mockito.doReturn(Optional.of(coupon)).when(couponsDAO).findOne(Mockito.any(), Mockito.any());

		List<MstAppUsers> mstAppUsersList = new ArrayList<MstAppUsers>();
		mstAppUsersList.add(new MstAppUsers());
		Mockito.doReturn(mstAppUsersList).when(mstAppUsersDAO).find(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
				Mockito.any());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test case coupon is not empty
	 */
	@Test
	public void B18B0030_TEST010() {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test process exception
	 * @throws SystemException 
	 */
	@Test
	public void B18B0030_TEST011() throws SystemException {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doReturn(0).when(userTransaction).getStatus();

		Mockito.doThrow(MockitoException.class).when(fsResultsDAO)
				.updateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
	 * Test process exception
	 * @throws SystemException 
	 */
	@Test
	public void B18B0030_TEST012() throws SystemException {
		b18B0030.executeMode = Constants.GENERAL;
		Mockito.doReturn(true).when(fsResultsDAO)
				.registOrUpdateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());
		Mockito.doThrow(SystemException.class).when(userTransaction).getStatus();

		Mockito.doThrow(MockitoException.class).when(fsResultsDAO)
				.updateFSResultsWithTreatedFlag(Mockito.anyString(), Mockito.anyString());

		try {
			// テスト開始
			printMsg("TEST002", "Start");

			// テスト対象のメソッド
			String returnCD = b18B0030.process();

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
}
