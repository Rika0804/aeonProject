package jp.co.aeoncredit.coupon.batch.dto;

public class DeliveryTargetDTO {

	/** FSクーポンユーザID(FS_COUPON_USER_ID) */
	private Long fsCouponUserId;

	/** クーポンID(COUPON_ID) */
	private Long couponId;

	/** 枝番(BRANCH_ID) */
	private String branchId;

	/** 共通内部ID(COMMON_INSIDE_ID) */
	private String commonInsideId;

	/** CPパスポートID(ACS_USER_CARD_CP_PASSPORT_ID) */
	private String acsUserCardCpPassportId;

	/** FS枝番(FS_BRANCH_ID) */
	private String fsBranchId;

	/** FS連携状況(FS_DELIVERY_STATUS) デフォルト値：0 */
	private String fsDeliveryStatus = "0";

	/** イオンウォレットトラッキングID(AW_TRACKING_ID) */
	private String awTrackingId;

	/**
	 * FSクーポンユーザID(FS_COUPON_USER_ID)を取得する。
	 * 
	 * @return FSクーポンユーザID(FS_COUPON_USER_ID)
	 */
	public Long getFsCouponUserId() {
		return this.fsCouponUserId;
	}

	/**
	 * FSクーポンユーザID(FS_COUPON_USER_ID)を設定する。
	 * @param fsCouponUserId FSクーポンユーザID(FS_COUPON_USER_ID)
	 * 
	 */
	public void setFsCouponUserId(Long fsCouponUserId) {
		this.fsCouponUserId = fsCouponUserId;
	}

	/**
	 * クーポンID(COUPON_ID)を取得する。
	 * 
	 * @return クーポンID(COUPON_ID)
	 */
	public Long getCouponId() {
		return this.couponId;
	}

	/**
	 * クーポンID(COUPON_ID)を設定する。
	 * 
	 * @param couponId クーポンID(COUPON_ID)
	 */
	public void setCouponId(Long couponId) {
		this.couponId = couponId;
	}

	/**
	 * 枝番(BRANCH_ID)を取得する。
	 * 
	 * @return 枝番(BRANCH_ID)
	 */
	public String getBranchId() {
		return this.branchId;
	}

	/**
	 * 枝番(BRANCH_ID)を設定する。
	 * 
	 * @param branchId 枝番(BRANCH_ID)
	 */
	public void setBranchId(String branchId) {
		this.branchId = branchId;
	}

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
	 */
	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	/**
	 * CPパスポートID(ACS_USER_CARD_CP_PASSPORT_ID)を取得する。
	 * 
	 * @return CPパスポートID(ACS_USER_CARD_CP_PASSPORT_ID)
	 */
	public String getAcsUserCardCpPassportId() {
		return this.acsUserCardCpPassportId;
	}

	/**
	 * CPパスポートID(ACS_USER_CARD_CP_PASSPORT_ID)を設定する。
	 * 
	 * @param acsUserCardCpPassportId CPパスポートID(ACS_USER_CARD_CP_PASSPORT_ID)
	 */
	public void setAcsUserCardCpPassportId(String acsUserCardCpPassportId) {
		this.acsUserCardCpPassportId = acsUserCardCpPassportId;
	}

	/**
	 * FS枝番(FS_BRANCH_ID)を取得する。
	 * 
	 * @return FS枝番(FS_BRANCH_ID)
	 */
	public String getFsBranchId() {
		return this.fsBranchId;
	}

	/**
	 * FS枝番(FS_BRANCH_ID)を設定する。
	 * 
	 * @param fsBranchId FS枝番(FS_BRANCH_ID)
	 */
	public void setFsBranchId(String fsBranchId) {
		this.fsBranchId = fsBranchId;
	}

	/**
	 * FS連携状況(FS_DELIVERY_STATUS)を取得する。
	 * 
	 * @return FS連携状況(FS_DELIVERY_STATUS)
	 */
	public String getFsDeliveryStatus() {
		return this.fsDeliveryStatus;
	}

	/**
	 * FS連携状況(FS_DELIVERY_STATUS)を設定する。
	 * 
	 * @param fsDeliveryStatus FS連携状況(FS_DELIVERY_STATUS)
	 */
	public void setFsDeliveryStatus(String fsDeliveryStatus) {
		this.fsDeliveryStatus = fsDeliveryStatus;
	}

	/**
	 * イオンウォレットトラッキングID(AW_TRACKING_ID)を取得する。
	 * 
	 * @return イオンウォレットトラッキングID(AW_TRACKING_ID)
	 */
	public String getAwTrackingId() {
		return this.awTrackingId;
	}

	/**
	 * イオンウォレットトラッキングID(AW_TRACKING_ID)を設定する。
	 * 
	 * @param awTrackingId イオンウォレットトラッキングID(AW_TRACKING_ID)
	 */
	public void setAwTrackingId(String awTrackingId) {
		this.awTrackingId = awTrackingId;
	}
}
