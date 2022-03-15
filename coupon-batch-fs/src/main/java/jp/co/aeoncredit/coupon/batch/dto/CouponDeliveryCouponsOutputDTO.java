package jp.co.aeoncredit.coupon.batch.dto;

import java.sql.Timestamp;

import javax.persistence.Column;

/**
 * FSクーポン配信バッチ用 クーポン情報OUTPUTDTO
 */
public class CouponDeliveryCouponsOutputDTO {

	/** クーポンID(COUPON_ID) */
	private Long couponId;

	/** 枝番(BRANCH_ID) */
	private String branchId;

	/** クーポン種別(COUPON_TYPE) */
	private String couponType;

	/** FSクーポンUUID(FS_COUPON_UUID) */
	private String fsCouponUuid;

	/** クーポン有効期間_開始(LIMITDATE_FROM) */
	private Timestamp limitdateFrom;

	/** クーポン有効期間_終了(LIMITDATE_TO) */
	private Timestamp limitdateTo;

	/** クーポン表示期間_開始(DISPLAYDATE_FROM) */
	private Timestamp displaydateFrom;

	/** クーポン表示期間_終了(DISPLAYDATE_TO) */
	private Timestamp displaydateTo;

	/** 配信先登録方法(DELIVERY_SAVE_METHOD) */
	private String deliverySaveMethod;

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
	 * @param couponId クーポンID(COUPON_ID)
	 * 
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
		return branchId;
	}

	/**
	 * 枝番(BRANCH_ID)を設定する。
	 * @param branchId 枝番(BRANCH_ID)
	 * 
	 */
	public void setBranchId(String branchId) {
		this.branchId = branchId;
	}

	/**
	 * クーポン種別(COUPON_TYPE)を取得する。
	 * 
	 * @return クーポン種別(COUPON_TYPE)
	 */
	public String getCouponType() {
		return this.couponType;
	}

	/**
	 * クーポン種別(COUPON_TYPE)を設定する。
	 * 
	 * @param couponType クーポン種別(COUPON_TYPE)
	 */
	public void setCouponType(String couponType) {
		this.couponType = couponType;
	}

	/**
	 * FSクーポンUUID(FS_COUPON_UUID)を取得する。
	 * 
	 * @return FSクーポンUUID(FS_COUPON_UUID)
	 */
	public String getFsCouponUuid() {
		return this.fsCouponUuid;
	}

	/**
	 * FSクーポンUUID(FS_COUPON_UUID)を設定する。
	 * 
	 * @param fsCouponUuid FSクーポンUUID(FS_COUPON_UUID)
	 */
	public void setFsCouponUuid(String fsCouponUuid) {
		this.fsCouponUuid = fsCouponUuid;
	}

	/**
	 * クーポン有効期間_開始(LIMITDATE_FROM)を取得する。
	 * 
	 * @return クーポン有効期間_開始(LIMITDATE_FROM)
	 */
	@Column(name = "LIMITDATE_FROM")
	public Timestamp getLimitdateFrom() {
		return this.limitdateFrom;
	}

	/**
	 * クーポン有効期間_開始(LIMITDATE_FROM)を設定する。
	 * 
	 * @param limitdateFrom クーポン有効期間_開始(LIMITDATE_FROM)
	 */
	public void setLimitdateFrom(Timestamp limitdateFrom) {
		this.limitdateFrom = limitdateFrom;
	}

	/**
	 * クーポン有効期間_終了(LIMITDATE_TO)を取得する。
	 * 
	 * @return クーポン有効期間_終了(LIMITDATE_TO)
	 */
	@Column(name = "LIMITDATE_TO")
	public Timestamp getLimitdateTo() {
		return this.limitdateTo;
	}

	/**
	 * クーポン有効期間_終了(LIMITDATE_TO)を設定する。
	 * 
	 * @param limitdateTo クーポン有効期間_終了(LIMITDATE_TO)
	 */
	public void setLimitdateTo(Timestamp limitdateTo) {
		this.limitdateTo = limitdateTo;
	}

	/**
	 * クーポン表示期間_開始(DISPLAYDATE_FROM)を取得する。
	 * 
	 * @return クーポン表示期間_開始(DISPLAYDATE_FROM)
	 */
	@Column(name = "DISPLAYDATE_FROM")
	public Timestamp getDisplaydateFrom() {
		return this.displaydateFrom;
	}

	/**
	 * クーポン表示期間_開始(DISPLAYDATE_FROM)を設定する。
	 * 
	 * @param displaydateFrom クーポン表示期間_開始(DISPLAYDATE_FROM)
	 */
	public void setDisplaydateFrom(Timestamp displaydateFrom) {
		this.displaydateFrom = displaydateFrom;
	}

	/**
	 * クーポン表示期間_終了(DISPLAYDATE_TO)を取得する。
	 * 
	 * @return クーポン表示期間_終了(DISPLAYDATE_TO)
	 */
	@Column(name = "DISPLAYDATE_TO")
	public Timestamp getDisplaydateTo() {
		return this.displaydateTo;
	}

	/**
	 * クーポン表示期間_終了(DISPLAYDATE_TO)を設定する。
	 * 
	 * @param displaydateTo クーポン表示期間_終了(DISPLAYDATE_TO)
	 */
	public void setDisplaydateTo(Timestamp displaydateTo) {
		this.displaydateTo = displaydateTo;
	}

	/**
	 * 配信先登録方法(DELIVERY_SAVE_METHOD)を取得する。
	 * 
	 * @return 配信先登録方法(DELIVERY_SAVE_METHOD)
	 */
	@Column(name = "DELIVERY_SAVE_METHOD")
	public String getDeliverySaveMethod() {
		return this.deliverySaveMethod;
	}

	/**
	 * 配信先登録方法(DELIVERY_SAVE_METHOD)を設定する。
	 * 
	 * @param deliverySaveMethod 配信先登録方法(DELIVERY_SAVE_METHOD)
	 */
	public void setDeliverySaveMethod(String deliverySaveMethod) {
		this.deliverySaveMethod = deliverySaveMethod;
	}
}
