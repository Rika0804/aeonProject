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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.co.aeoncredit.coupon.batch.common.BatchConfigfileLoader;
import jp.co.aeoncredit.coupon.batch.common.BatchDBAccessBase;
import jp.co.aeoncredit.coupon.batch.common.BatchFSApiCalloutBase;
import jp.co.aeoncredit.coupon.batch.constants.ProcessResult;
import jp.co.aeoncredit.coupon.constants.HTTPStatus;

/**
 * B18B0076_FSPush通知配信停止バッチのテスト クラスのJUnit
 * 
 * @author to-okawa
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class B18B0076TEST extends B18B0076 {

	/** authToken */
	private static final String AUTH_TOKEN = "authTokenTest";

	/** userAgent */
	private static final String USER_AGENT = "AEON WALLET";

	/** テスト対象のクラス */
	@InjectMocks
	B18B0076 b18B0076;

	/** テスト対象のクラス */
	@Mock
	BatchFSApiCalloutBase batchFSApiCalloutBase;

	/** バッチ用DBアクセスクラスのSuperクラス */
	@Mock
	BatchDBAccessBase batchDBAccessBase;

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
		b18B0076 = Mockito.spy(b18B0076);
		Mockito.doNothing().when(b18B0076).transactionBegin(Mockito.anyString());
		Mockito.doNothing().when(b18B0076).transactionCommit(Mockito.anyString());
		Mockito.doNothing().when(b18B0076).transactionRollback(Mockito.anyString());

		// トークン取得処理をMock化
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0076, "getAuthToken", Mockito.anyString());
		PowerMockito.doReturn(AuthTokenResult.SUCCESS).when(b18B0076, "getAuthToken", Mockito.anyString(),
				Mockito.eq(true));

		// プライベートフフィールドを書き換え
		Whitebox.setInternalState(b18B0076, "authToken", AUTH_TOKEN);
		Whitebox.setInternalState(b18B0076, "userAgent", USER_AGENT);

		// 処理結果設定処理をMock化
		Mockito.doReturn(ProcessResult.SUCCESS.getValue()).when(b18B0076)
				.setExitStatus(ProcessResult.SUCCESS.getValue());
		Mockito.doReturn(ProcessResult.FAILURE.getValue()).when(b18B0076)
				.setExitStatus(ProcessResult.FAILURE.getValue());

		// プロパティ値読み込み処理をMock化
		// アプリ内Msg配信停止APIのURL
		pro.setProperty("fs.cancel.batch.push.notification.api.url", "/reverse-push/info/${id}/cancel/");
		// FS API 失敗時のAPI実行リトライ回数
		pro.setProperty("fs.cancel.batch.push.notification.retry.count", "3");
		// FS API失敗時のAPI実行リトライ時スリープ時間(ミリ秒)
		pro.setProperty("fs.cancel.batch.push.notification.retry.sleep.time", "5000");
		// FS API発行時のタイムアウト期間(秒)
		pro.setProperty("fs.cancel.batch.push.notification.timeout.duration", "5");
		when(batchConfigfileLoader.readPropertyFile(Mockito.anyString())).thenReturn(pro);

		// SQLの呼び出しをMock化
		Mockito.doReturn(getTargetData()).when(b18B0076).sqlSelect(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.doReturn(0).when(b18B0076).sqlExecute(Mockito.any(), Mockito.any(), Mockito.any());
	}

	/**
	 * テスト初期化。このまま記載。
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * b18B0076.process()のテスト<br>
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
			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0076, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
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
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0076, "getAuthToken", Mockito.anyString());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
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
			Mockito.doReturn(new ArrayList<Object[]>()).when(b18B0076).sqlSelect(Mockito.any(), Mockito.any(),
					Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * 正常系
	 */
//	@Test
//	public void testProcessOk() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * 正常系 FSPUSH通知UUID(FS_PUSH_NOTIFICATION_UUID)がNULL
	 */
	@Test
	public void testProcessOkUuidIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// SQLの呼び出しをMock化
			List<Object[]> resultList = new ArrayList<Object[]>();
			Object[] result = { 111111111111111111L, null };
			resultList.add(result);
			Mockito.doReturn(resultList).when(b18B0076).sqlSelect(Mockito.any(), Mockito.any(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に200だが、statusがNG
	 */
	@Test
	public void testProcessNg() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			Map<String, Object> resBody = new HashMap<String, Object>();
			resBody.put("status", "NG");
			resBody.put("id", 123456789012345678L);
			mockCallFsApi(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false, resBody);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に503
	 */
//	@Test
//	public void testProcessFsApi503() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// SQLの呼び出しをMock化
//			List<Object[]> targetDataList = getTargetData();
//			targetDataList.addAll(getTargetData());
//			Mockito.doReturn(targetDataList).when(b18B0076).sqlSelect(Mockito.any(), Mockito.any(), Mockito.any());
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue(), null, false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に401<br>
	 * 再認証成功
	 */
//	@Test
//	public void testProcessFsApi401_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), HTTPStatus.HTTP_STATUS_SUCCESS.getValue(),
//					false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に401<br>
	 * 再認証で503
	 */
//	@Test
//	public void testProcessFsApi401_Maintenance() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// トークン取得処理をMock化
//			PowerMockito.doReturn(AuthTokenResult.MAINTENANCE).when(b18B0076, "getAuthToken", Mockito.anyString(),
//					Mockito.eq(true));
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), HTTPStatus.HTTP_STATUS_SUCCESS.getValue(),
//					false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に401<br>
	 * 再認証失敗
	 */
	@Test
	public void testProcessFsApi401_Failure() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// SQLの呼び出しをMock化
			List<Object[]> targetDataList = getTargetData();
			targetDataList.addAll(getTargetData());
			Mockito.doReturn(targetDataList).when(b18B0076).sqlSelect(Mockito.any(), Mockito.any(), Mockito.any());

			// トークン取得処理をMock化
			PowerMockito.doReturn(AuthTokenResult.FAILURE).when(b18B0076, "getAuthToken", Mockito.anyString(),
					Mockito.eq(true));

			// FsApiのHTTP通信をMock化
			Map<String, Object> resBody = new HashMap<String, Object>();
			resBody.put("status", "NG");
			mockCallFsApi(HTTPStatus.HTTP_STATUS_UNAUTHORIZED.getValue(), HTTPStatus.HTTP_STATUS_SUCCESS.getValue(),
					false, false, false, resBody);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に429
	 */
//	@Test
//	public void testProcessFsApi429() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(HTTPStatus.HTTP_STATUS_TOO_MANY_REQUEST.getValue(), null, false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に500<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApi500_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			Map<String, Object> resBody = new HashMap<String, Object>();
			resBody.put("status", "NG");
			mockCallFsApi(500, 500, false, false, false, resBody);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に500<br>
	 * リトライ後に正常レスポンス
	 */
//	@Test
//	public void testProcessFsApi500_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(500, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), false, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時にタイムアウト<br>
	 * リトライ超過
	 */
	@Test
	public void testProcessFsApiTimeoutEx_OverRetry() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			mockCallFsApi(null, null, true, false, false, null);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時にタイムアウト<br>
	 * リトライ後に正常レスポンス
	 */
//	@Test
//	public void testProcessFsApiTimeoutEx_OK() {
//		// メソッド名を取得
//		String testCase = new Object() {
//		}.getClass().getEnclosingMethod().getName();
//		try {
//			// テスト開始ログ
//			printMsg(testCase, "Start");
//
//			// FsApiのHTTP通信をMock化
//			mockCallFsApi(null, HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), true, false, false, null);
//
//			// テスト対象のメソッド実行
//			String res = b18B0076.process();
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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時405
	 */
	@Test
	public void testProcessFsApi405() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			Map<String, Object> resBody = new HashMap<String, Object>();
			resBody.put("status", "NG");
			mockCallFsApi(405, null, false, false, false, resBody);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時に予期せぬエラー
	 */
	@Test
	public void testProcessFsApiException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			Mockito.doThrow(NullPointerException.class).when(b18B0076).callFanshipApi(Mockito.any(), Mockito.any(),
					Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時にresponseがNULL
	 */
	@Test
	public void testProcessFsApiResponseIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時にresponseBodyがNULL
	 */
	@Test
	public void testProcessFsApiResponseBodyIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			mockCallFsApi(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, true, null);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * Push通知配信停止時にstatusがNULL
	 */
	@Test
	public void testProcessFsApiStatusIsNull() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// FsApiのHTTP通信をMock化
			Map<String, Object> resBody = new HashMap<String, Object>();
			resBody.put("id", 123456789012345678L);
			mockCallFsApi(HTTPStatus.HTTP_STATUS_SUCCESS.getValue(), null, false, false, false, resBody);

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 * b18B0076.process()のテスト<br>
	 * FS公開停止状況を更新時に予期せぬエラー
	 */
	@Test
	public void testProcessUpdateException() {
		// メソッド名を取得
		String testCase = new Object() {
		}.getClass().getEnclosingMethod().getName();
		try {
			// テスト開始ログ
			printMsg(testCase, "Start");

			// SQLの呼び出しをMock化
			Mockito.doThrow(NullPointerException.class).when(b18B0076).sqlExecute(Mockito.any(), Mockito.any(),
					Mockito.any());

			// テスト対象のメソッド実行
			String res = b18B0076.process();

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
	 */
	private List<Object[]> getTargetData() {
		List<Object[]> resultList = new ArrayList<Object[]>();
		Object[] result = { 111111111111111111L, 123456789012345678L };
		resultList.add(result);
		return resultList;
	}

	/**
	 * FS API通信をMock化する
	 * 
	 * @param httpStatus1 呼び出し1回目のHTTPステータスコード
	 * @param httpStatus2 呼び出し2回目のHTTPステータスコード
	 * @param IsTimeout   trueの場合HttpTimeoutExceptionを発生させる
	 * @param IsException trueの場合予期せぬエラーを発生させる
	 * @param isJsonNull  trueの場合JSONをNULLにする
	 * @param resBody     FS API通信後のレスポンス
	 * @throws Exception
	 */
	private void mockCallFsApi(Integer httpStatus1, Integer httpStatus2, boolean IsTimeout, boolean IsException,
			boolean isJsonNull, Map<String, Object> resBody) throws Exception {
		// HttpRequestを設定
		HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
		httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(
						"http://dev-reverse.acs-coupon.iridgeapp.com/reverse-push/info/123456789012345678/cancel/"))
				.POST(BodyPublishers.ofString("")).timeout(Duration.ofSeconds(5))
				.headers("Content-Type", "application/json", "Authorization", "PopinfoLogin auth=" + AUTH_TOKEN, "User-Agent", USER_AGENT)
				.build();

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
		if (resBody == null) {
			resBody = new HashMap<String, Object>();
			resBody.put("status", "OK");
			resBody.put("id", 123456789012345678L);
		}
		String jsonString = null;
		if (!isJsonNull) {
			ObjectMapper mapper = new ObjectMapper();
			jsonString = mapper.writeValueAsString(resBody);
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
	 * テスト開始終了メッセージ出力
	 * 
	 * @param testId      テストID
	 * @param processType 処理区分（Start/End）
	 */
	private void printMsg(String testId, String processType) {
		if (processType.equals("Start")) {
			log.info(" [B18B0076TEST] 【******************" + testId + " 実施開始******************】");
		} else {
			log.info(" [B18B0076TEST] 【******************" + testId + " 実施終了******************】");
		}
	}
}