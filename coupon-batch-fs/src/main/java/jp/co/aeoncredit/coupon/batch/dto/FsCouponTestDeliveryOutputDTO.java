package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.PushNotifications;

/**
 * FSクーポンテスト配信バッチ用のクーポン情報DTO
 */
public class FsCouponTestDeliveryOutputDTO {

	/** クーポンID(COUPON_ID) */
	private Long couponId;

	/** FS店舗UUID(FS_STORE_UUID) */
	private String fsStoreUuid;

	/** 加盟店/カテゴリID(MERCHANT_CATEGORY_ID) */
	private Long merchantCategoryId;

	/** クーポンテーブル */
	private Coupons coupons;

	/** クーポン画像テーブルリスト */
	private List<CouponImages> couponImagesList;

	/** クーポン特典テーブルリスト */
	private List<CouponIncents> couponIncentsList;

	/** Push通知テーブル */
	private PushNotifications pushNotifications;

	/** FSAPI用JSONテーブルリスト */
	private List<FsApiJson> fsApiJsonList;

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
	 * FS店舗UUID(FS_STORE_UUID)を取得する。
	 * 
	 * @return FS店舗UUID(FS_STORE_UUID)
	 */
	public String getFsStoreUuid() {
		return this.fsStoreUuid;
	}

	/**
	 * FS店舗UUID(FS_STORE_UUID)を設定する。
	 * 
	 * @param fsStoreUuid FS店舗UUID(FS_STORE_UUID)
	 */
	public void setFsStoreUuid(String fsStoreUuid) {
		this.fsStoreUuid = fsStoreUuid;
	}

	/**
	 * 加盟店/カテゴリID(MERCHANT_CATEGORY_ID)を取得する。
	 * 
	 * @return 加盟店/カテゴリID(MERCHANT_CATEGORY_ID)
	 */
	public Long getMerchantCategoryId() {
		return this.merchantCategoryId;
	}

	/**
	 * 加盟店/カテゴリID(MERCHANT_CATEGORY_ID)を設定する。
	 * @param merchantCategoryId 加盟店/カテゴリID(MERCHANT_CATEGORY_ID)
	 * 
	 */
	public void setMerchantCategoryId(Long merchantCategoryId) {
		this.merchantCategoryId = merchantCategoryId;
	}

	/**
	 * クーポンテーブル(ステータス更新用)を取得する。
	 * 
	 * @return クーポンテーブル(ステータス更新用)
	 */
	public Coupons getCoupons() {
		return coupons;
	}

	/**
	 * クーポンテーブル(ステータス更新用)を設定する。
	 * 
	 * @param coupons クーポンテーブル(ステータス更新用)
	 */
	public void setCoupons(Coupons coupons) {
		this.coupons = coupons;
	}

	/**
	 * クーポン画像テーブルリストを取得する。
	 * 
	 * @return クーポン画像テーブルリスト
	 */
	public List<CouponImages> getCouponImagesList() {
		return couponImagesList;
	}

	/**
	 * クーポン画像テーブルリストを設定する。
	 * 
	 * @param couponImagesList クーポン画像テーブルリスト
	 */
	public void setCouponImagesList(List<CouponImages> couponImagesList) {
		this.couponImagesList = couponImagesList;
	}

	/**
	 * クーポン特典テーブルリストを取得する。
	 * 
	 * @return クーポン特典テーブルリスト
	 */
	public List<CouponIncents> getCouponIncentsList() {
		return couponIncentsList;
	}

	/**
	 * クーポン特典テーブルリストを設定する。
	 * 
	 * @param couponIncentsList クーポン特典テーブルリスト
	 */
	public void setCouponIncentsList(List<CouponIncents> couponIncentsList) {
		this.couponIncentsList = couponIncentsList;
	}

	/**
	 * Push通知テーブルを取得する。
	 * 
	 * @return Push通知テーブル
	 */
	public PushNotifications getPushNotifications() {
		return pushNotifications;
	}

	/**
	 * Push通知テーブルを設定する。
	 * 
	 * @param pushNotifications Push通知テーブル
	 */
	public void setPushNotifications(PushNotifications pushNotifications) {
		this.pushNotifications = pushNotifications;
	}

	/**
	 * FSAPI用JSONテーブルリストを取得する。
	 * 
	 * @return FSAPI用JSONテーブルリスト
	 */
	public List<FsApiJson> getFsApiJsonList() {
		return fsApiJsonList;
	}

	/**
	 * FSAPI用JSONテーブルリストを設定する。
	 * 
	 * @param fsApiJsonList FSAPI用JSONテーブルリスト
	 */
	public void setFsApiJsonList(List<FsApiJson> fsApiJsonList) {
		this.fsApiJsonList = fsApiJsonList;
	}

}
