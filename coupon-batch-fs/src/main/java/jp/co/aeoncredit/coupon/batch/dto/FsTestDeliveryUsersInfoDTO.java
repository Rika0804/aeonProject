package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * FSテスト端末登録・解除用のテスト配信ユーザ情報DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FsTestDeliveryUsersInfoDTO {

	/** 共通内部ID(COMMON_INSIDE_ID) */
	private String commonInsideId;

	/** テスト配信ユーザマスタ.POPINFO ID(POPINFO_ID) */
	private String popInfoIdTest;

	/** アプリユーザマスタ.POPINFO ID(POPINFO_ID) */
	private String popInfoIdApp;

	/** OS区分(OS_TYPE) */
	private String osType;

	/**
	 * 共通内部ID(COMMON_INSIDE_ID)を取得する。
	 * 
	 * @return 共通内部ID(COMMON_INSIDE_ID)
	 */
	public String getCommonInsideId() {
		return this.commonInsideId;
	}

	/**
	 * 共通内部ID(COMMON_INSIDE_ID)を設定する。
	 * 
	 * @param commonInsideId 共通内部ID(COMMON_INSIDE_ID)
	 * 
	 */
	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	/**
	 * テスト配信ユーザマスタ.POPINFO ID(POPINFO_ID)を取得する。
	 * 
	 * @return テスト配信ユーザマスタ.POPINFO ID(POPINFO_ID)
	 */
	public String getPopInfoIdTest() {
		return this.popInfoIdTest;
	}

	/**
	 * テスト配信ユーザマスタ.POPINFO ID(POPINFO_ID)を設定する。
	 * 
	 * @param popInfoIdTest テスト配信ユーザマスタ.POPINFO ID(POPINFO_ID)
	 */
	public void setPopInfoIdTest(String popInfoIdTest) {
		this.popInfoIdTest = popInfoIdTest;
	}

	/**
	 * アプリユーザマスタ.POPINFO ID(POPINFO_ID)を取得する。
	 * 
	 * @return アプリユーザマスタ.POPINFO ID(POPINFO_ID)
	 */
	public String getPopInfoIdApp() {
		return this.popInfoIdApp;
	}

	/**
	 * アプリユーザマスタ.POPINFO ID(POPINFO_ID)を設定する。
	 * 
	 * @param popInfoIdApp アプリユーザマスタ.POPINFO ID(POPINFO_ID)
	 */
	public void setPopInfoIdApp(String popInfoIdApp) {
		this.popInfoIdApp = popInfoIdApp;
	}

	/**
	 * OS区分(OS_TYPE)を取得する。
	 * 
	 * @return OS区分(OS_TYPE)
	 */
	public String getOsType() {
		return this.osType;
	}

	/**
	 * OS区分(OS_TYPE)を設定する。
	 * 
	 * @param osType OS区分(OS_TYPE)
	 * 
	 */
	public void setOsType(String osType) {
		this.osType = osType;
	}
}