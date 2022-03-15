/**
 * 
 */
package jp.co.aeoncredit.coupon.batch.dto;

/**
 * MAクーポン配布結果のDTOクラス
 */
public class MaCouponDeliveryResultDTO {
	/** 会員番号 */
	private String acsUserCardId;

	/** 家族CD */
	private String acsUserCardFamilyCd;

	/** 共通内部ID */
	private String commonInsideId;

	/** 枚数フラグ */
	private Short countFlag;

	/**
	 * 会員番号を取得する。
	 * @return 会員番号
	 */
	public String getAcsUserCardId() {
		return acsUserCardId;
	}

	/**
	 * 会員番号を設定する。
	 * @param acsUserCardId 会員番号
	 */
	public void setAcsUserCardId(String acsUserCardId) {
		this.acsUserCardId = acsUserCardId;
	}

	/**
	 * 家族CDを取得する。
	 * @return 家族CD
	 */
	public String getAcsUserCardFamilyCd() {
		return acsUserCardFamilyCd;
	}

	/**
	 * 家族CDを設定する。
	 * @param acsUserCardFamilyCd 家族CD
	 */
	public void setAcsUserCardFamilyCd(String acsUserCardFamilyCd) {
		this.acsUserCardFamilyCd = acsUserCardFamilyCd;
	}

	/**
	 * 共通内部IDを取得する。
	 * @return 共通内部ID
	 */
	public String getCommonInsideId() {
		return commonInsideId;
	}

	/**
	 * 共通内部IDを設定する。
	 * @param commonInsideId 共通内部ID
	 */
	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	/**
	 * 枚数フラグを取得する。
	 * @return 枚数フラグ
	 */
	public Short getCountFlag() {
		return countFlag;
	}

	/**
	 * 枚数フラグを設定する。
	 * @param countFlag 枚数フラグ
	 */
	public void setCountFlag(Short countFlag) {
		this.countFlag = countFlag;
	}
}
