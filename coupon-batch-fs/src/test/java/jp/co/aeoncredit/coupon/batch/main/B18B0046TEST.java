package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.logic.ServiceDBException;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.MapiOutputDTOBaseError;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetInputDTOFilter;
import jp.co.aeoncredit.coupon.batch.dto.RegisterInfoListGetOutputDTOForFlatIsTrue;
import jp.co.aeoncredit.coupon.batch.dto.TestDeviceAddOrRemoveInputDTO;
import jp.co.aeoncredit.coupon.batch.dto.TestDeviceAddOrRemoveOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.TestDeviceAddOrRemoveOutputDTOResult;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.OsType;
import jp.co.aeoncredit.coupon.constants.properties.MstAppUsersProps;
import jp.co.aeoncredit.coupon.constants.properties.MstTestDeliveryUsersProps;
import jp.co.aeoncredit.coupon.dao.custom.MstTestDeliveryUsersDAOCustomize;

/**
 * B18B0046_FSテスト端末登録・削除バッチのテスト クラスのJUnit
 */
public class B18B0046TEST extends B18B0046 {

	/** authToken */
	private static final String AUTH_TOKEN = "authTokenTest";

	/** userAgent */
	private static final String USER_AGENT = "AEON WALLET";

	/** テスト対象のクラス */
	@InjectMocks
	B18B0046 b18B0046;

	/** テスト対象のクラス */
	@Mock
	BatchFSApiCalloutBase batchFSApiCalloutBase;

	/** テスト対象のクラスにInjectされるDAO */
	@Mock
	MstTestDeliveryUsersDAOCustomize mstTestDeliveryUsersDAOCustomize;

	/** Propertiesクラス */
	Properties pro = new Properties();

	/** バッチの設定ファイル読込ユーティリティクラス */
	@Mock
	private BatchConfigfileLoader batchConfigfileLoader;

	/** HttpClientクラス */
	@Mock
	HttpClient httpClient;

	/**
	 * テスト初期化処理
	 * 
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		// モックを初期化
		MockitoAnnotations.initMocks(this);

		// トランザクションのモック化
		b18B0046 = Mockito.spy(b18B0046);
		Mockito.doNothing().when(b18B0046).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0046).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0046).transactionRollback(Mockito.anyString());

		// トークン取得処理をモック化
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0046, "getAuthToken", Mockito.anyString());
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0046, "getAuthToken", Mockito.anyString(),
				Mockito.eq(true));

		// プライベートフフィールドを書き換え
		Whitebox.setInternalState(b18B0046, "authToken", AUTH_TOKEN);
		Whitebox.setInternalState(b18B0046, "userAgent", USER_AGENT);

		// 処理結果設定処理をモック化
		Mockito.doReturn(ProcessResult.SUCCESS.getValue()).when(b18B0046)
				.setExitStatus(ProcessResult.SUCCESS.getValue());
		Mockito.doReturn(ProcessResult.FAILURE.getValue()).when(b18B0046)
				.setExitStatus(ProcessResult.FAILURE.getValue());

		// プロパティ値読み込み処理をMock化
		// テスト端末登録・解除APIのURL(登録)
		pro.setProperty("fs.test.device.add.remove.batch.add.api.url", "/reverse-push/account/testdevice/add/");
		// テスト端末登録・解除APIのURL(削除)
		pro.setProperty("fs.test.device.add.remove.batch.remove.api.url", "/reverse-push/account/testdevice/remove/");
		// 登録者情報一覧取得APIのURL
		pro.setProperty("fs.test.device.add.remove.batch.get.api.url", "/reverse-push/user/");
		// FS API 失敗時のAPI実行リトライ回数
		pro.setProperty("fs.test.device.add.remove.batch.retry.count", "3");
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		pro.setProperty("fs.test.device.add.remove.batch.retry.sleep.time", "5000");
		// FS API発行時のタイムアウト期間(秒)
		pro.setProperty("fs.test.device.add.remove.batch.timeout.duration", "5");
		when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(pro);

		// DaoをMock化
		Mockito.doReturn(0).when(mstTestDeliveryUsersDAOCustomize).updateByCommonInsideIdAndFsDeliveryStatus(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.doReturn(0).when(mstTestDeliveryUsersDAOCustomize).updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
	}

	/**
	 * テスト初期化。このまま記載。
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * getAuthTokenの戻り値がMAINTENANCE
	 */
	@Test
	public void testProcessTokenMaintenance() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0046, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.SUCCESS.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * getAuthTokenの戻り値がFAILURE
	 */
	@Test
	public void testProcessTokenFailure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0046, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * B18B0046.process()のTest<br>
	 * 処理対象データが0件
	 */
	@Test
	public void testProcessNoTestDate() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			when(mstTestDeliveryUsersDAOCustomize.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new ArrayList<Map<String, Object>>());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.SUCCESS.getValue(), res);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * B18B0046.process()のTest<br>
	 * アプリユーザマスタのpopinfo IDがNULL
	 */
	@Test
	public void testProcessPopinfoIdIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<Map<String, Object>>();
			resList.add(
					getTargetData("B18B0046_00000000001", "7777777-test-4566-a08d-23325d26532e1", null, OsType.IPHONE));
			when(mstTestDeliveryUsersDAOCustomize.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(resList);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.SUCCESS.getValue(), res);

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * テスト端末登録：登録済み<br>
//	 * 正常系<br>
//	 */
//	@Test
//	public void testProcessRegisteredOk() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * テスト端末登録：未登録<br>
//	 * 正常系<br>
//	 */
//	@Test
//	public void testProcessUnRegisteredOk() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時に503
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGet503() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時に401<br>
//	 * 再認証成功
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGet401_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時に401<br>
//	 * 再認証で503
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGet401_Maintenance() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// トークン取得処理をMock化
//			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0046, "getAuthToken", Mockito.anyString(),
//					Mockito.eq(true));
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGet401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0046, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
					getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時に429
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGet429() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGet500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(500, 500, false, false, false,
					getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時に500<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGet500_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGetTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(null, null, true, false, false,
					getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録者情報一覧取得時にタイムアウト<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessFsApiRegisterInfoListGetTimeoutEx_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時405
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGet405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(405, null, false, false, false,
					getReqParamForFsApiRegisterInfoListGet(popinfoId));
			RegisterInfoListGetOutputDTOForFlatIsTrue output = getOutputForFsApiRegisterInfoListGet("OK", 0);
			MapiOutputDTOBaseError errOutput = new MapiOutputDTOBaseError();
			errOutput.setCode("E5000");
			errOutput.setMessage("errMessage");
			output.setError(errOutput);
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, output);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時200だがステータスコードがNG
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGet200Ng() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(200, null, false, false, false,
					getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, null);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録者情報一覧取得時に予期せぬエラー
	 */
	@Test
	public void testProcessFsApiRegisterInfoListGetException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			Mockito.doThrow(NullPointerException.class).when(b18B0046).callFanshipApi(Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時に503
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove503() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時に401<br>
//	 * 再認証成功
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove401_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時に401<br>
//	 * 再認証で503
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove401_Maintenance() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// トークン取得処理をMock化
//			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0046, "getAuthToken", Mockito.anyString(),
//					Mockito.eq(true));
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末解除時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessAddFsApiTestDeviceRemove401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0046, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時に429
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove429() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末解除時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessAddFsApiTestDeviceRemove500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時に500<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove500_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末解除時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessAddFsApiTestDeviceRemoveTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, null, true, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時にタイムアウト<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemoveTimeoutEx_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末解除時405
	 */
	@Test
	public void testProcessAddFsApiTestDeviceRemove405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(405, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時422
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove422() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.FAILURE.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}
//
//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時422<br>
//	 * JSONがnull
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove422_jsonIsNull() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.FAILURE.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}
//
//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時422<br>
//	 * 出力項目がnull
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove422_outputIsNull() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.FAILURE.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}
//
//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末解除時422<br>
//	 * エラー詳細配列(detail)がnull
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceRemove422_DetailIsIsNull() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.FAILURE.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末解除時に予期せぬエラー
	 */
	@Test
	public void testProcessAddFsApiTestDeviceRemoveException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			Mockito.doThrow(NullPointerException.class).when(b18B0046).callFanshipApi(Mockito.any(), Mockito.any(),
					Mockito.eq("/reverse-push/account/testdevice/remove/"), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時に503
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAdd503() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時に401<br>
//	 * 再認証成功
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAdd401_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).doReturn(null).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時に401<br>
//	 * 再認証で503
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAdd401_Maintenance() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// トークン取得処理をMock化
//			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0046, "getAuthToken", Mockito.anyString(),
//					Mockito.eq(true));
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末登録時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessAddFsApiTestDeviceAdd401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.SUCCESS, AuthTokenResult.FAILURE).when(b18B0046, "getAuthToken",
					Mockito.anyString());

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時に429
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAdd429() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
//					Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末登録時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessAddFsApiTestDeviceAdd500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時に500<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAdd500_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).doReturn(null).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末登録時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessAddFsApiTestDeviceAddTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, null, true, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 登録処理<br>
//	 * テスト端末登録時にタイムアウト<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessAddFsApiTestDeviceAddTimeoutEx_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(resList).doReturn(null).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 登録処理<br>
	 * テスト端末登録時405
	 */
	@Test
	public void testProcessAddFsApiTestDeviceAdd405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(405, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時にステータスコードがNGでcodeが「E60104」
//	 * 
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemoveNgE60104() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("NG"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時に503
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemove503() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時に401<br>
//	 * 再認証成功
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemove401_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
//					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時に401<br>
//	 * 再認証で503
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemove401_Maintenance() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// トークン取得処理をMock化
//			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0046, "getAuthToken", Mockito.anyString(),
//					Mockito.eq(true));
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.SUCCESS, AuthTokenResult.FAILURE).when(b18B0046, "getAuthToken",
					Mockito.anyString());

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), null,
					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時に429
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemove429() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null,
//					false, false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時に500<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemove500_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemoveTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, null, true, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

//	/**
//	 * b18B0046.process()のテスト<br>
//	 * 削除処理<br>
//	 * テスト端末解除時にタイムアウト<br>
//	 * リトライ後に正常レスポンス
//	 */
//	@Test
//	public void testProcessRemoveFsApiTestDeviceRemoveTimeoutEx_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
//			OsType osType = OsType.IPHONE;
//
//			// DaoをMock化
//			List<Map<String, Object>> resList = new ArrayList<>();
//			resList.add(
//					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
//			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
//					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// 登録者情報一覧取得APIをMock化
//			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
//					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
//			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));
//
//			// テスト端末登録・解除APIをMock化
//			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true,
//					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//			fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
//					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
//			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));
//
//			// テスト対象のメソッド実行
//			String res = b18B0046.process();
//
//			// 検証
//			assertEquals(ProcessResult.SUCCESS.getValue(), res);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail();
//		} finally {
//			// テスト終了ログ
//			printMsg(testCase, "End");
//		}
//	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時405
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(405, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, null);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時ステータスコードがNGだが、出力項目がNULL
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemoveNgOutputDtoNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, null);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時200だがステータスコードがNG
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove200Ng() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			TestDeviceAddOrRemoveOutputDTO output = getOutputForFsApiAppMessageAddOrCancel("NG");
			output.setError(null);
			mockFsTestDeviceRemove(fsApiMockModeAdd, output);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時422<br>
	 * ステータスコードがNGだが、出力項目のERRORがNULL
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove422_NgOutputErrNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(422, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			TestDeviceAddOrRemoveOutputDTO output = getOutputForFsApiAppMessageAddOrCancel("NG");
			output.setError(null);
			mockFsTestDeviceRemove(fsApiMockModeAdd, output);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時422<br>
	 * ステータスコードがNGだが、出力項目のERRORがE60104でない
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove422_NgNotE60104() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(422, null, false, false, false,
					getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			TestDeviceAddOrRemoveOutputDTO output = getOutputForFsApiAppMessageAddOrCancel("NG");
			MapiOutputDTOBaseError errOutput = new MapiOutputDTOBaseError();
			errOutput.setCode("E601041");
			output.setError(errOutput);
			mockFsTestDeviceRemove(fsApiMockModeAdd, output);

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * 削除処理<br>
	 * テスト端末解除時422<br>
	 * JSONがnull
	 */
	@Test
	public void testProcessRemoveFsApiTestDeviceRemove422_jsonIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(null).doReturn(resList).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 1));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, true, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceRemove(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * updateMstTestDeliveryUsersFsDeliveryStatus()で予期せぬエラー
	 */
	@Test
	public void testProcess_updateMstTestDeliveryUsersFsDeliveryStatusException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).when(mstTestDeliveryUsersDAOCustomize).findTestDeliveryUsers(Mockito.any(),
					Mockito.any(), Mockito.any());
			Mockito.doThrow(ServiceDBException.class).doReturn(0).when(mstTestDeliveryUsersDAOCustomize)
					.updateByCommonInsideIdAndFsDeliveryStatus(Mockito.any(), Mockito.any(), Mockito.any(),
							Mockito.any(), Mockito.any(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * b18B0046.process()のテスト<br>
	 * updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus()で予期せぬエラー
	 */
	@Test
	public void testProcess_updatePopinfoIdByCommonInsideIdAndFsDeliveryStatusException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");
			String popinfoId = "7777777-test-4566-a08d-23325d26532e1";
			OsType osType = OsType.IPHONE;

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(
					getTargetData("B18B0046_00000000001", popinfoId, "7777777-app-4566-a08d-23325d26532e1", osType));
			Mockito.doReturn(resList).doReturn(null).when(mstTestDeliveryUsersDAOCustomize)
					.findTestDeliveryUsers(Mockito.any(), Mockito.any(), Mockito.any());
			Mockito.doThrow(ServiceDBException.class).doReturn(0).when(mstTestDeliveryUsersDAOCustomize)
					.updatePopinfoIdByCommonInsideIdAndFsDeliveryStatus(Mockito.any(), Mockito.any(), Mockito.any(),
							Mockito.any(), Mockito.any());

			// 登録者情報一覧取得APIをMock化
			FsApiMockMode fsApiMockModeGet = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiRegisterInfoListGet(popinfoId));
			mockFsApiRegisterInfoListGet(fsApiMockModeGet, osType, getOutputForFsApiRegisterInfoListGet("OK", 0));

			// テスト端末登録・解除APIをMock化
			FsApiMockMode fsApiMockModeAdd = new FsApiMockMode(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false,
					false, false, getReqParamForFsApiAppMessageAddOrCancel(popinfoId, osType));
			mockFsTestDeviceAdd(fsApiMockModeAdd, getOutputForFsApiAppMessageAddOrCancel("OK"));

			// テスト対象のメソッド実行
			String res = b18B0046.process();

			// 検証
			assertEquals(ProcessResult.FAILURE.getValue(), res);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		} finally {
			// テスト終了ログ
			printMsg(testCase, "End");
		}
	}

	/**
	 * 処理対象データを取得する
	 * 
	 * @param commonInsideId 共通内部ID
	 * @param popInfoIdTest  テスト配信ユーザマスタのpopinfo ID
	 * @param popInfoIdApp   アプリユーザマスタのpopinfo ID
	 * @param osType         OS区分
	 */
	private Map<String, Object> getTargetData(String commonInsideId, String popInfoIdTest, String popInfoIdApp,
			OsType osType) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(MstTestDeliveryUsersProps.COMMON_INSIDE_ID, commonInsideId);
		map.put(MstTestDeliveryUsersProps.POP_INFO_ID, popInfoIdTest);
		map.put(MstAppUsersProps.POP_INFO_ID + "App", popInfoIdApp);
		map.put(MstAppUsersProps.OS_TYPE, osType.getValue());
		return map;
	}

	/**
	 * 登録者情報一覧取得API用のリクエストパラメータを取得する
	 * 
	 * @param popinfoId popinfo ID
	 * @throws JsonProcessingException
	 */
	private String getReqParamForFsApiRegisterInfoListGet(String popinfoId) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		RegisterInfoListGetInputDTO inputDTO = new RegisterInfoListGetInputDTO();
		String[] name = { "popinfo_id" };
		inputDTO.setFields(name);
		inputDTO.setFlat(true);
		List<RegisterInfoListGetInputDTOFilter> inputFilterList = new ArrayList<RegisterInfoListGetInputDTOFilter>();
		RegisterInfoListGetInputDTOFilter inputFilter = new RegisterInfoListGetInputDTOFilter();
		inputFilter.setPopinfoId(popinfoId);
		inputFilter.setTestSend(true);
		inputFilterList.add(inputFilter);
		inputDTO.setFilter(inputFilterList);
		inputDTO.setPage(1);
		return mapper.writeValueAsString(inputDTO);
	}

	/**
	 * テスト端末登録・解除API用のリクエストパラメータを取得する
	 * 
	 * @param popinfoId popinfo ID
	 * @param osType    OS区分
	 * @throws JsonProcessingException
	 */
	private String getReqParamForFsApiAppMessageAddOrCancel(String popInfoId, OsType osType)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		TestDeviceAddOrRemoveInputDTO inputDTO = new TestDeviceAddOrRemoveInputDTO();
		inputDTO.setDeviceType(osType.getDefinition());
		inputDTO.setPopinfoId(popInfoId);
		return mapper.writeValueAsString(inputDTO);
	}

	/**
	 * 登録者情報一覧取得API用の出力項目を取得する
	 * 
	 * @param status       ステータスコード
	 * @param totalResults ユーザーの合計数
	 */
	private RegisterInfoListGetOutputDTOForFlatIsTrue getOutputForFsApiRegisterInfoListGet(String status, int totalResults) {
		RegisterInfoListGetOutputDTOForFlatIsTrue output = new RegisterInfoListGetOutputDTOForFlatIsTrue();
		output.setStatus(status);
		output.setTotalResults(totalResults);
		output.setPage(1);
		output.setPages(1);
		List<String> result = new ArrayList<String>();
		result.add("1111aaaa");
		output.setResult(result);
		return output;
	}

	/**
	 * テスト端末登録・解除API用の出力項目を取得する
	 * 
	 * @param status ステータスコード
	 */
	private TestDeviceAddOrRemoveOutputDTO getOutputForFsApiAppMessageAddOrCancel(String status) {
		TestDeviceAddOrRemoveOutputDTO output = new TestDeviceAddOrRemoveOutputDTO();
		output.setStatus(status);
		TestDeviceAddOrRemoveOutputDTOResult result = new TestDeviceAddOrRemoveOutputDTOResult();
		output.setResult(result);
		MapiOutputDTOBaseError errOutput = new MapiOutputDTOBaseError();
		errOutput.setCode("E60104");
		output.setError(errOutput);
		return output;
	}

	/**
	 * HTTP通信をMock化する(登録者情報一覧取得API用)
	 * 
	 * @param fsApiMockMode FS API呼び出し処理のMock化方法を指定するクラス
	 * @param osType        OS区分
	 * @param output        登録者情報一覧取得API用の出力項目
	 * @throws Exception
	 */
	private void mockFsApiRegisterInfoListGet(FsApiMockMode fsApiMockMode, OsType osType,
			RegisterInfoListGetOutputDTOForFlatIsTrue output) throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(
						"http://dev-reverse.acs-coupon.iridgeapp.com/reverse-push/user/" + osType.getDefinition() + "/"))
				.POST(BodyPublishers.ofString(fsApiMockMode.getReqParam())).timeout(Duration.ofSeconds(5))
				.headers("Content-Type", "application/json;charset=UTF-8", "Authorization", "PopinfoLogin auth=" + AUTH_TOKEN, "User-Agent",
						USER_AGENT)
				.build();

		// HttpResponseを設定
		String jsonString = null;
		if (!fsApiMockMode.isJsonNull()) {
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(output);
		}
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
		if (fsApiMockMode.getHttpStatus1() != null) {
			when(httpResponse1.statusCode()).thenReturn(fsApiMockMode.getHttpStatus1());
		}
		if (fsApiMockMode.getHttpStatus2() != null) {
			when(httpResponse2.statusCode()).thenReturn(fsApiMockMode.getHttpStatus2());
		}
		when(httpResponse1.body()).thenReturn(jsonString);
		when(httpResponse2.body()).thenReturn(jsonString);

		// HTTP通信をMock化する
		if (fsApiMockMode.isIsException()) {
			Mockito.doThrow(IOException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.isIsTimeout() && fsApiMockMode.getHttpStatus2() != null) {
			Mockito.doThrow(HttpTimeoutException.class).doReturn(httpResponse2).when(httpClient)
					.send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.isIsTimeout()) {
			Mockito.doThrow(HttpTimeoutException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else {
			Mockito.doReturn(httpResponse1).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		}
	}

	/**
	 * HTTP通信をMock化する(テスト端末登録・解除APIの登録処理用)
	 * 
	 * @param fsApiMockMode FS API呼び出し処理のMock化方法を指定するクラス
	 * @param output        テスト端末登録・解除API用の出力項目
	 * @throws Exception
	 */
	private void mockFsTestDeviceAdd(FsApiMockMode fsApiMockMode, TestDeviceAddOrRemoveOutputDTO output)
			throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://dev-reverse.acs-coupon.iridgeapp.com/reverse-push/account/testdevice/add/"))
				.POST(BodyPublishers.ofString(fsApiMockMode.getReqParam())).timeout(Duration.ofSeconds(5))
				.headers("Content-Type", "application/json;charset=UTF-8", "Authorization", "PopinfoLogin auth=" + AUTH_TOKEN, "User-Agent",
						USER_AGENT)
				.build();

		// HttpResponseを設定
		String jsonString = null;
		if (!fsApiMockMode.isJsonNull()) {
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(output);
		}
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
		if (fsApiMockMode.getHttpStatus1() != null) {
			when(httpResponse1.statusCode()).thenReturn(fsApiMockMode.getHttpStatus1());
		}
		if (fsApiMockMode.getHttpStatus2() != null) {
			when(httpResponse2.statusCode()).thenReturn(fsApiMockMode.getHttpStatus2());
		}
		when(httpResponse1.body()).thenReturn(jsonString);
		when(httpResponse2.body()).thenReturn(jsonString);

		// HTTP通信をMock化する
		if (fsApiMockMode.isIsException()) {
			Mockito.doThrow(IOException.class).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		} else if (fsApiMockMode.isIsTimeout() && fsApiMockMode.getHttpStatus2() != null) {
			Mockito.doThrow(HttpTimeoutException.class).doReturn(httpResponse2).when(httpClient)
					.send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.isIsTimeout()) {
			Mockito.doThrow(HttpTimeoutException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.getHttpStatus2() != null) {
			Mockito.doReturn(httpResponse1).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		} else {
			Mockito.doReturn(httpResponse1).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		}
	}

	/**
	 * HTTP通信をMock化する(テスト端末登録・解除APIの解除処理用)
	 * 
	 * @param fsApiMockMode FS API呼び出し処理のMock化方法を指定するクラス
	 * @param output        テスト端末登録・解除API用の出力項目
	 * @throws Exception
	 */
	private void mockFsTestDeviceRemove(FsApiMockMode fsApiMockMode, TestDeviceAddOrRemoveOutputDTO output)
			throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://dev-reverse.acs-coupon.iridgeapp.com/reverse-push/account/testdevice/remove/"))
				.POST(BodyPublishers.ofString(fsApiMockMode.getReqParam())).timeout(Duration.ofSeconds(5))
				.headers("Content-Type", "application/json;charset=UTF-8", "Authorization", "PopinfoLogin auth=" + AUTH_TOKEN, "User-Agent",
						USER_AGENT)
				.build();

		// HttpResponseを設定
		String jsonString = null;
		if (!fsApiMockMode.isJsonNull()) {
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(output);
		}
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
		if (fsApiMockMode.getHttpStatus1() != null) {
			when(httpResponse1.statusCode()).thenReturn(fsApiMockMode.getHttpStatus1());
		}
		if (fsApiMockMode.getHttpStatus2() != null) {
			when(httpResponse2.statusCode()).thenReturn(fsApiMockMode.getHttpStatus2());
		}
		when(httpResponse1.body()).thenReturn(jsonString);
		when(httpResponse2.body()).thenReturn(jsonString);

		// HTTP通信をMock化する
		if (fsApiMockMode.isIsException()) {
			Mockito.doThrow(IOException.class).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		} else if (fsApiMockMode.isIsTimeout() && fsApiMockMode.getHttpStatus2() != null) {
			Mockito.doThrow(HttpTimeoutException.class).doReturn(httpResponse2).when(httpClient)
					.send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.isIsTimeout()) {
			Mockito.doThrow(HttpTimeoutException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else if (fsApiMockMode.getHttpStatus2() != null) {
			Mockito.doReturn(httpResponse1).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		} else {
			Mockito.doReturn(httpResponse1).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		}
	}

	/**
	 * テスト開始終了メッセージ出力
	 * 
	 * @param testId      テストID
	 * @param processType 処理区分（Start/End）
	 */
	private void printMsg(String testId, String processType) {
		if (processType.equals("Start")) {
			log.info(" [B18B0030TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0030TEST] 【******************" + testId + " 実施終了******************】");
		}
	}

	/**
	 * FS API呼び出し処理のMock化方法を指定するクラス
	 */
	private class FsApiMockMode {

		/** 呼び出し1回目のHTTPステータスコード */
		private Integer httpStatus1;

		/** 呼び出し2回目のHTTPステータスコード */
		private Integer httpStatus2;

		/** trueの場合HttpTimeoutExceptionを発生させる */
		private boolean IsTimeout;

		/** trueの場合予期せぬエラーを発生させる */
		private boolean IsException;

		/** trueの場合JSONをNULLにする */
		private boolean isJsonNull;

		/** FS APIのリクエストパラメーター */
		private String reqParam;

		public FsApiMockMode(Integer httpStatus1, Integer httpStatus2, boolean isTimeout, boolean isException,
				boolean isJsonNull, String reqParam) {
			super();
			this.httpStatus1 = httpStatus1;
			this.httpStatus2 = httpStatus2;
			this.IsTimeout = isTimeout;
			this.IsException = isException;
			this.isJsonNull = isJsonNull;
			this.reqParam = reqParam;
		}

		public Integer getHttpStatus1() {
			return httpStatus1;
		}

		public Integer getHttpStatus2() {
			return httpStatus2;
		}

		public boolean isIsTimeout() {
			return IsTimeout;
		}

		public boolean isIsException() {
			return IsException;
		}

		public boolean isJsonNull() {
			return isJsonNull;
		}

		public String getReqParam() {
			return reqParam;
		}
	}
}