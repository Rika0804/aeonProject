package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 限定クーポン配布API INPUTDTO
 */
public class CouponDeliveryApiInputDTO {

	/** 配布対象のユーザID(android) */
	@JsonProperty("android_uuid")
	private String[] androidUuid;

	/** 配布対象のユーザID(iOS) */
	@JsonProperty("ios_uuid")
	private String[] iosUuid;

	/**
	 * @return androidUuid
	 */
	public String[] getAndroidUuid() {
		return androidUuid;
	}

	/**
	 * @param androidUuid セットする androidUuid
	 */
	public void setAndroidUuid(String[] androidUuid) {
		this.androidUuid = androidUuid;
	}

	/**
	 * @return iosUuid
	 */
	public String[] getIosUuid() {
		return iosUuid;
	}

	/**
	 * @param iosUuid セットする iosUuid
	 */
	public void setIosUuid(String[] iosUuid) {
		this.iosUuid = iosUuid;
	}

}
