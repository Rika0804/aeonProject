package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import jp.co.aeoncredit.coupon.entity.AppMessageSendPeriods;
import jp.co.aeoncredit.coupon.entity.AppMessages;
import jp.co.aeoncredit.coupon.entity.CouponImages;
import jp.co.aeoncredit.coupon.entity.CouponIncents;
import jp.co.aeoncredit.coupon.entity.Coupons;
import jp.co.aeoncredit.coupon.entity.FsApiJson;
import jp.co.aeoncredit.coupon.entity.MstEvent;
import jp.co.aeoncredit.coupon.entity.MstSensor;
import jp.co.aeoncredit.coupon.entity.PushNotificationSendPeriods;
import jp.co.aeoncredit.coupon.entity.PushNotifications;

/**
 * FSクーポン登録・更新・削除用のクーポン情報DTO
 */
public class FsCouponRegisterOutputDTO {

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

	/** アプリ内メッセージテーブル */
	private AppMessages appMessages;

	/** アプリ内メッセージ配信期間リスト */
	private List<AppMessageSendPeriods> appMessageSendPeriodsList;

	/** イベントマスタ */
	private MstEvent mstEvent;

	/** Push通知テーブル */
	private PushNotifications pushNotifications;

	/** Push通知配信期間リスト */
	private List<PushNotificationSendPeriods> pushNotificationSendPeriodsList;

	/** センサーマスタリスト */
	private List<MstSensor> mstSensorList;

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
	 * アプリ内メッセージテーブルを取得する。
	 * 
	 * @return アプリ内メッセージテーブル
	 */
	public AppMessages getAppMessages() {
		return appMessages;
	}

	/**
	 * アプリ内メッセージテーブルを設定する。
	 * 
	 * @param appMessages アプリ内メッセージテーブル
	 */
	public void setAppMessages(AppMessages appMessages) {
		this.appMessages = appMessages;
	}

	/**
	 * アプリ内メッセージ配信期間リストを取得する。
	 * 
	 * @return アプリ内メッセージ配信期間リスト
	 */
	public List<AppMessageSendPeriods> getAppMessageSendPeriodsList() {
		return appMessageSendPeriodsList;
	}

	/**
	 * アプリ内メッセージ配信期間リストを設定する。
	 * 
	 * @param appMessageSendPeriodsList アプリ内メッセージ配信期間リスト
	 */
	public void setAppMessageSendPeriodsList(List<AppMessageSendPeriods> appMessageSendPeriodsList) {
		this.appMessageSendPeriodsList = appMessageSendPeriodsList;
	}

	/**
	 * イベントマスタを取得する。
	 * 
	 * @return イベントマスタ
	 */
	public MstEvent getMstEvent() {
		return mstEvent;
	}

	/**
	 * イベントマスタを設定する。
	 * 
	 * @param mstEvent イベントマスタ
	 */
	public void setMstEvent(MstEvent mstEvent) {
		this.mstEvent = mstEvent;
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
	 * Push通知配信期間リストを取得する。
	 * 
	 * @return Push通知配信期間リスト
	 */
	public List<PushNotificationSendPeriods> getPushNotificationSendPeriodsList() {
		return pushNotificationSendPeriodsList;
	}

	/**
	 * Push通知配信期間リストを設定する。
	 * 
	 * @param pushNotificationSendPeriodsList Push通知配信期間リスト
	 */
	public void setPushNotificationSendPeriodsList(List<PushNotificationSendPeriods> pushNotificationSendPeriodsList) {
		this.pushNotificationSendPeriodsList = pushNotificationSendPeriodsList;
	}

	/**
	 * センサーマスタリストを取得する。
	 * 
	 * @return センサーマスタリスト
	 */
	public List<MstSensor> getMstSensorList() {
		return mstSensorList;
	}

	/**
	 * センサーマスタリストを設定する。
	 * 
	 * @param mstSensorList センサーマスタリスト
	 */
	public void setMstSensorList(List<MstSensor> mstSensorList) {
		this.mstSensorList = mstSensorList;
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
