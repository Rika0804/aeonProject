package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 限定クーポン配布API UUID OUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CouponDeliveryApiOutputDTOUuid {

	/** 配布に成功(失敗)したユーザID (android) */
	@JsonProperty("android_uuid")
	private String[] androidUuid;

	/** 配布に成功(失敗)したユーザID (iOS) */
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
