package jp.co.aeoncredit.coupon.batch.dto;

import jp.co.aeoncredit.coupon.constants.CouponType;

/**
 * B18B0009 配信予定・依頼リスト出力バッチ
 * 配信依頼リストDTO
 * @author m-omori
 *
 */
public class DeliveryRequestListDTO {
	

	/** 会員番号 */
	private String customerId;
	
	/** 家族コード */
	private String familyCode;
	
	/** 共通内部ID */
	private String commonInsideId;

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFamilyCode() {
		return familyCode;
	}

	public void setFamilyCode(String familyCode) {
		this.familyCode = familyCode;
	}

	public String getCommonInsideId() {
		return commonInsideId;
	}

	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	/**
	 * DBから取得したデータをDTOに変換する
	 * @param data ... DBから取得したデータ
	 * @param couponType ... クーポン種別
	 * @return 変換結果
	 */
	public boolean setValue(Object[] data, CouponType couponType) {
		
		if (couponType == CouponType.PASSPORT) {
			// パスポートクーポン
			this.customerId = (String)data[0] ;
			this.familyCode = (String)data[1];
			this.commonInsideId = (String)data[2];
		} else if (couponType == CouponType.TARGET) {
			// ターゲットクーポン
			this.commonInsideId = (String)data[0];
		}
		
		return true;
	}
	
	
}
