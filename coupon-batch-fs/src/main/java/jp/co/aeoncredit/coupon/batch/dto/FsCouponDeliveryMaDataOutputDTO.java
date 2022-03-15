package jp.co.aeoncredit.coupon.batch.dto;

/**
 * FSクーポン配信バッチ MAクーポン配信結果データ取得DTO
 */
public class FsCouponDeliveryMaDataOutputDTO {

	/** 会員番号 */
	private String acsUserCardId;

	/** 家族CD */
	private String acsUserCardFamilyCd;

	/** 共通内部ID */
	private String commonInsideId;

	/** 枚数フラグ */
	private Short countFlag;

	/** 配布結果 */
	private String deliveryResult;

	/**
	 * @return acsUserCardId
	 */
	public String getAcsUserCardId() {
		return acsUserCardId;
	}

	/**
	 * @param acsUserCardId セットする acsUserCardId
	 */
	public void setAcsUserCardId(String acsUserCardId) {
		this.acsUserCardId = acsUserCardId;
	}

	/**
	 * @return acsUserCardFamilyCd
	 */
	public String getAcsUserCardFamilyCd() {
		return acsUserCardFamilyCd;
	}

	/**
	 * @param acsUserCardFamilyCd セットする acsUserCardFamilyCd
	 */
	public void setAcsUserCardFamilyCd(String acsUserCardFamilyCd) {
		this.acsUserCardFamilyCd = acsUserCardFamilyCd;
	}

	/**
	 * @return commonInsideId
	 */
	public String getCommonInsideId() {
		return commonInsideId;
	}

	/**
	 * @param commonInsideId セットする commonInsideId
	 */
	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	/**
	 * @return countFlag
	 */
	public Short getCountFlag() {
		return countFlag;
	}

	/**
	 * @param countFlag セットする countFlag
	 */
	public void setCountFlag(Short countFlag) {
		this.countFlag = countFlag;
	}

	/**
	 * @return deliveryResult
	 */
	public String getDeliveryResult() {
		return deliveryResult;
	}

	/**
	 * @param deliveryResult セットする deliveryResult
	 */
	public void setDeliveryResult(String deliveryResult) {
		this.deliveryResult = deliveryResult;
	}

}
