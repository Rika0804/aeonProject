package jp.co.aeoncredit.coupon.batch.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.jp.awag.common.logic.ServiceDBException;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.batch.dto.FanshipInAppMsgUnsubscribeOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.FanshipInAppMsgUnsubscribeOutputDTODetail;
import jp.co.aeoncredit.coupon.batch.dto.GetFanshipInAppMsgOutputDTO;
import jp.co.aeoncredit.coupon.batch.dto.GetFanshipInAppMsgOutputDTOCondition;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;
import jp.co.aeoncredit.coupon.constants.properties.AppMessagesProps;
import jp.co.aeoncredit.coupon.dao.custom.AppMessagesDAOCustomize;
import jp.co.aeoncredit.coupon.entity.AppMessages;

/**
 * B18B0075_FSアプリ内Msg配信停止バッチのテスト クラスのJUnit
 * 
 * @author to-okawa
 */
@RunWith(MockitoJUnitRunner.class)
public class B18B0075TEST extends B18B0075 {

	/** authToken */
	private static final String AUTH_TOKEN = "authTokenTest";

	/** userAgent */
	private static final String USER_AGENT = "AEON WALLET";

	/** テスト対象のクラス */
	@InjectMocks
	B18B0075 b18B0075;

	/** テスト対象のクラス */
	@Mock
	BatchFSApiCalloutBase batchFSApiCalloutBase;

	/** テスト対象のクラスにInjectされるDAO */
	@Mock
	AppMessagesDAOCustomize appMessagesDAOCustomize;

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

		// トランザクションのMock化
		b18B0075 = Mockito.spy(b18B0075);
		Mockito.doNothing().when(b18B0075).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0075).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0075).transactionRollback(Mockito.anyString());

		// トークン取得処理をMock化
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0075, "getAuthToken", Mockito.anyString());
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0075, "getAuthToken", Mockito.anyString(),
				Mockito.eq(true));

		// プライベートフフィールドを書き換え
		Whitebox.setInternalState(b18B0075, "authToken", AUTH_TOKEN);
		Whitebox.setInternalState(b18B0075, "userAgent", USER_AGENT);

		// 処理結果設定処理をMock化
		Mockito.doReturn(ProcessResult.SUCCESS.getValue()).when(b18B0075)
				.setExitStatus(ProcessResult.SUCCESS.getValue());
		Mockito.doReturn(ProcessResult.FAILURE.getValue()).when(b18B0075)
				.setExitStatus(ProcessResult.FAILURE.getValue());

		// プロパティ値読み込み処理をMock化
		// アプリ内Msg配信情報取得APIのURL
		pro.setProperty("fs.cancel.batch.app.message.delivery.api.url", "/reverse-inappmsg/delivery/{delivery_id}");
		// アプリ内Msg配信停止APIのURL
		pro.setProperty("fs.cancel.batch.app.message.cancel.api.url",
				"/reverse-inappmsg/condition/${condition_id}/_disable");
		// FS API 失敗時のAPI実行リトライ回数
		pro.setProperty("fs.cancel.batch.retry.count", "3");
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		pro.setProperty("fs.cancel.batch.retry.sleep.time", "5000");
		// FS API発行時のタイムアウト期間(秒)
		pro.setProperty("fs.cancel.batch.timeout.duration", "5");
		when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(pro);

		// DaoをMock化
		Mockito.doReturn(getFindById()).when(appMessagesDAOCustomize).findById(Mockito.anyLong());
		Mockito.doReturn(null).when(appMessagesDAOCustomize).update(Mockito.any());
	}

	/**
	 * テスト初期化。このまま記載。
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * b18B0075.process()のテスト<br>
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
			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0075, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
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
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0075, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * 処理対象のアプリ内Msgテーブルが存在しない
	 */
	@Test
	public void testProcessNoDate() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			Mockito.doReturn(new ArrayList<AppMessages>()).when(appMessagesDAOCustomize)
					.findFsAppMessageCancel(Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * 正常系 is_active=true
	 */
	@Test
	public void testProcessOk_isActiveTrue() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			resList.add(getTargetData(true));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * 正常系 is_active=false
	 */
	@Test
	public void testProcessOk_isActiveFalse() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			GetFanshipInAppMsgOutputDTO output = new GetFanshipInAppMsgOutputDTO();
			GetFanshipInAppMsgOutputDTOCondition outputCondition = new GetFanshipInAppMsgOutputDTOCondition();
			outputCondition.setConditionId(3);
			outputCondition.setIsActive(false);
			output.setCondition(outputCondition);
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, output);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * 正常系 conditionがNULL
	 */
	@Test
	public void testProcessOk_ConditionNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			GetFanshipInAppMsgOutputDTO output = new GetFanshipInAppMsgOutputDTO();
			output.setCondition(null);
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, output);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に503
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery503() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に401<br>
	 * 再認証成功
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery401_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に401<br>
	 * 再認証で503
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery401_Maintenance() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0075, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0075, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に429
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery429() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(500, 500, false, false, null);

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に500<br>
	 * リトライ後に正常レスポンス
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery500_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiAppMessageDeliveryTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(null, null, true, false, null);

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時にタイムアウト<br>
	 * リトライ後に正常レスポンス
	 */
	@Test
	public void testProcessFsApiAppMessageDeliveryTimeoutEx_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時405
	 */
	@Test
	public void testProcessFsApiAppMessageDelivery405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(405, null, false, false, null);

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に予期せぬエラー
	 */
	@Test
	public void testProcessFsApiAppMessageDeliveryException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			Mockito.doThrow(NullPointerException.class).when(b18B0075).callFanshipApi(Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に503
	 */
	@Test
	public void testProcessFsApiAppMessageCancel503() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に401<br>
	 * 再認証成功
	 */
	@Test
	public void testProcessFsApiAppMessageCancel401_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に401<br>
	 * 再認証で503
	 */
	@Test
	public void testProcessFsApiAppMessageCancel401_Maintenance() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.SUCCESS, AuthTokenResult.MAINTENANCE).when(b18B0075, "getAuthToken",
					Mockito.anyString());

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessFsApiAppMessageCancel401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0075, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(),
					HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に429
	 */
	@Test
	public void testProcessFsApiAppMessageCancel429() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null, false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiAppMessageCancel500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(500, 500, false, false, false, getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時に500<br>
	 * リトライ後に正常レスポンス
	 */
	@Test
	public void testProcessFsApiAppMessageCancel500_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiAppMessageCancelTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(null, null, true, false, false, getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時にタイムアウト<br>
	 * リトライ後に正常レスポンス
	 */
	@Test
	public void testProcessFsApiAppMessageCancelTimeoutEx_OK() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true, false, false,
					getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時405
	 */
	@Test
	public void testProcessFsApiAppMessageCancel405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(405, null, false, false, false, getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時422
	 */
	@Test
	public void testProcessFsApiAppMessageCancel422() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(422, null, false, false, false, getOutputForFsApiAppMessageCancel(false, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時422<br>
	 * JSONがnull
	 */
	@Test
	public void testProcessFsApiAppMessageCancel422_jsonIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(422, null, false, false, true, getOutputForFsApiAppMessageCancel(true, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時422<br>
	 * 出力項目がnull
	 */
	@Test
	public void testProcessFsApiAppMessageCancel422_outputIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(422, null, false, false, false, getOutputForFsApiAppMessageCancel(true, false));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信停止時422<br>
	 * エラー詳細配列(detail)がnull
	 */
	@Test
	public void testProcessFsApiAppMessageCancel422_DetailIsIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			mockFsApiAppMessageCancel(422, null, false, false, false, getOutputForFsApiAppMessageCancel(false, true));

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * アプリ内Msg配信情報取得時に予期せぬエラー
	 */
	@Test
	public void testProcessFsApiAppMessageCancelException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());

			// FsApiのHTTP通信をMock化
			mockFsApiAppMessageDelivery(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, null);
			Mockito.doThrow(NullPointerException.class).when(b18B0075).callFanshipApi(Mockito.any(),
					Mockito.eq("アプリ内Msg配信停止API"), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * findAppMessages()で予期せぬエラー
	 */
	@Test
	public void testProcess_findAppMessagesException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			Mockito.doThrow(ServiceDBException.class).when(appMessagesDAOCustomize)
					.findFsAppMessageCancel(Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * b18B0075.process()のテスト<br>
	 * updateAppMessages()で予期せぬエラー
	 */
	@Test
	public void testProcess_updateAppMessagesException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// DaoをMock化
			List<Map<String, Object>> resList = new ArrayList<>();
			resList.add(getTargetData(false));
			Mockito.doReturn(resList).when(appMessagesDAOCustomize).findFsAppMessageCancel(Mockito.any());
			Mockito.doThrow(ServiceDBException.class).doReturn(null).when(appMessagesDAOCustomize)
					.update(Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0075.process();

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
	 * @param uuidIsNull trueの場合FSアプリ内メッセージUUIDをNULLにする
	 */
	private Map<String, Object> getTargetData(boolean uuidIsNull) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(AppMessagesProps.APP_MESSAGE_ID, "1");
		if (uuidIsNull) {
			map.put(AppMessagesProps.FS_APP_MESSAGE_UUID, null);
		} else {
			map.put(AppMessagesProps.FS_APP_MESSAGE_UUID, "123456789012345678");
		}
		return map;
	}

	/**
	 * 更新対象のアプリ内Msgテーブルを取得する
	 */
	@SuppressWarnings("static-access")
	private Optional<AppMessages> getFindById() {
		AppMessages entity = new AppMessages();
		entity.setAppMessageId(1L);
		entity.setFsAppMessageUuid(123456789012345678L);
		Optional<AppMessages> entityOptional = Optional.of(entity);
		return entityOptional;
	}

	/**
	 * アプリ内Msg配信停止API用の出力項目を取得する
	 * 
	 * @param outputIsNull trueの場合outputをNULLにする
	 * @param detailIsNull trueの場合outputDetailをNULLにする
	 */
	private FanshipInAppMsgUnsubscribeOutputDTO getOutputForFsApiAppMessageCancel(boolean outputIsNull,
			boolean detailIsNull) {
		if (outputIsNull) {
			return null;
		}
		FanshipInAppMsgUnsubscribeOutputDTO output = new FanshipInAppMsgUnsubscribeOutputDTO();
		if (detailIsNull) {
			output.setDetail(null);
		} else {
			List<FanshipInAppMsgUnsubscribeOutputDTODetail> outputDetailList = new ArrayList<FanshipInAppMsgUnsubscribeOutputDTODetail>();
			FanshipInAppMsgUnsubscribeOutputDTODetail outputDetail = new FanshipInAppMsgUnsubscribeOutputDTODetail();
			outputDetail.setLoc(new ArrayList<String>(Arrays.asList("locTest")));
			outputDetail.setMsg("エラーメッセージ");
			outputDetail.setType("typeTest");
			outputDetailList.add(outputDetail);
			output.setDetail(outputDetailList);
		}
		return output;
	}

	/**
	 * HTTP通信をMock化する(アプリ内Msg配信情報取得API用)
	 * 
	 * @param httpStatus1 呼び出し1回目のHTTPステータスコード
	 * @param httpStatus2 呼び出し2回目のHTTPステータスコード
	 * @param IsTimeout   trueの場合HttpTimeoutExceptionを発生させる
	 * @param IsException trueの場合予期せぬエラーを発生させる
	 * @param outputList  GetFanshipInAppMsgOutputDTO
	 * @throws Exception
	 */
	private void mockFsApiAppMessageDelivery(Integer httpStatus1, Integer httpStatus2, boolean IsTimeout,
			boolean IsException, GetFanshipInAppMsgOutputDTO output) throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(
						"http://dev-reverse.acs-coupon.iridgeapp.com/reverse-inappmsg/delivery/123456789012345678"))
				.GET().timeout(Duration.ofSeconds(5)).headers("Content-Type", "application/json",
						"X-POPINFO-MAPI-TOKEN", AUTH_TOKEN, "User-Agent", USER_AGENT)
				.build();

		// HttpResponseを設定
		if (output == null) {
			output = new GetFanshipInAppMsgOutputDTO();
			GetFanshipInAppMsgOutputDTOCondition outputCondition = new GetFanshipInAppMsgOutputDTOCondition();
			outputCondition.setConditionId(3);
			outputCondition.setIsActive(true);
			output.setCondition(outputCondition);
		}
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(output);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
		if (httpStatus1 != null) {
			when(httpResponse1.statusCode()).thenReturn(httpStatus1);
		}
		if (httpStatus2 != null) {
			when(httpResponse2.statusCode()).thenReturn(httpStatus2);
		}
		when(httpResponse1.body()).thenReturn(jsonString);
		when(httpResponse2.body()).thenReturn(jsonString);

		// HTTP通信をMock化する
		if (IsException) {
			Mockito.doThrow(IOException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else if (IsTimeout && httpStatus2 != null) {
			Mockito.doThrow(HttpTimeoutException.class).doReturn(httpResponse2).when(httpClient)
					.send(Mockito.eq(httpRequest), Mockito.any());
		} else if (IsTimeout) {
			Mockito.doThrow(HttpTimeoutException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else {
			Mockito.doReturn(httpResponse1).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		}
	}

	/**
	 * HTTP通信をMock化する(アプリ内Msg配信停止API用)
	 * 
	 * @param httpStatus1 呼び出し1回目のHTTPステータスコード
	 * @param httpStatus2 呼び出し2回目のHTTPステータスコード
	 * @param IsTimeout   trueの場合HttpTimeoutExceptionを発生させる
	 * @param IsException trueの場合予期せぬエラーを発生させる
	 * @param jsonIsNull  trueの場合JSONをNULLにする
	 * @param output      FanshipInAppMsgUnsubscribeOutputDTO
	 * @throws Exception
	 */
	private void mockFsApiAppMessageCancel(Integer httpStatus1, Integer httpStatus2, boolean IsTimeout,
			boolean IsException, boolean jsonIsNull, FanshipInAppMsgUnsubscribeOutputDTO output) throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create("http://dev-reverse.acs-coupon.iridgeapp.com/reverse-inappmsg/condition/3/_disable"))
				.PUT(BodyPublishers.noBody()).timeout(Duration.ofSeconds(5)).headers("Content-Type", "application/json",
						"X-POPINFO-MAPI-TOKEN", AUTH_TOKEN, "User-Agent", USER_AGENT)
				.build();

		// HttpResponseを設定
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
		@SuppressWarnings("rawtypes")
		HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
		if (httpStatus1 != null) {
			when(httpResponse1.statusCode()).thenReturn(httpStatus1);
		}
		if (httpStatus2 != null) {
			when(httpResponse2.statusCode()).thenReturn(httpStatus2);
		}

		// HTTP通信をMock化する
		if (IsException) {
			Mockito.doThrow(IOException.class).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
		} else if (IsTimeout && httpStatus2 != null) {
			Mockito.doThrow(HttpTimeoutException.class).doReturn(httpResponse2).when(httpClient)
					.send(Mockito.eq(httpRequest), Mockito.any());
		} else if (IsTimeout) {
			Mockito.doThrow(HttpTimeoutException.class).when(httpClient).send(Mockito.eq(httpRequest), Mockito.any());
		} else {
			Mockito.doReturn(httpResponse1).doReturn(httpResponse2).when(httpClient).send(Mockito.eq(httpRequest),
					Mockito.any());
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
			log.info(" [B18B0075TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0075TEST] 【******************" + testId + " 実施終了******************】");
		}
	}
}