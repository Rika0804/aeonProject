package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 限定クーポン配布API OUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponDeliveryApiOutputDTO {

	/** 配布に失敗したユーザID */
	@JsonProperty("sent")
	private CouponDeliveryApiOutputDTOUuid sent;

	/** 配布に失敗したユーザID */
	@JsonProperty("not_sent")
	private CouponDeliveryApiOutputDTOUuid notSent;

	/** エラー */
	@JsonProperty("error")
	private String error;

	/**
	 * @return sent
	 */
	public CouponDeliveryApiOutputDTOUuid getSent() {
		return sent;
	}

	/**
	 * @param sent セットする sent
	 */
	public void setSent(CouponDeliveryApiOutputDTOUuid sent) {
		this.sent = sent;
	}

	/**
	 * @return notSent
	 */
	public CouponDeliveryApiOutputDTOUuid getNotSent() {
		return notSent;
	}

	/**
	 * @param notSent セットする notSent
	 */
	public void setNotSent(CouponDeliveryApiOutputDTOUuid notSent) {
		this.notSent = notSent;
	}

	/**
	 * @return error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error セットする error
	 */
	public void setError(String error) {
		this.error = error;
	}

}
