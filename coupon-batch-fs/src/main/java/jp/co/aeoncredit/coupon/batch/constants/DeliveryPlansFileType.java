package jp.co.aeoncredit.coupon.batch.constants;

import jp.co.aeoncredit.coupon.constants.CouponType;
import jp.co.aeoncredit.coupon.constants.DeliveryType;

/**
 * B18B0009 配信予定・依頼リスト出力バッチ
 * 出力するファイルタイプ
 * @author m-omori
 *
 */
public enum DeliveryPlansFileType {

	/** B18P0006_配信依頼リスト（ターゲット） */
	REQUEST_LIST_TARGET(CouponType.TARGET, null),
	/** B18P0006_配信依頼リスト（パスポート） */
	REQUEST_LIST_PASSPORT(CouponType.PASSPORT, DeliveryType.AEON_WALLET_APP),
	/** B18P0012_配信予定リスト（ATM） */
	PLAN_LIST_ATM(CouponType.PASSPORT, DeliveryType.ATM),
	/** その他（エラー） */
	OTHER(null, null);
	
	private CouponType couponType;
	private DeliveryType deliveryType;
	
	DeliveryPlansFileType(CouponType couponType, DeliveryType deliveryType) {
		this.couponType = couponType;
		this.deliveryType = deliveryType;
	}

	public CouponType getCouponType() {
		return couponType;
	}

	public DeliveryType getDeliveryType() {
		return deliveryType;
	}

}
