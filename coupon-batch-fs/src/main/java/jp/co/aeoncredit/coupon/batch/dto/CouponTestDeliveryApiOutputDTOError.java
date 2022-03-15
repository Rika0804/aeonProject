package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * その他クーポンテスト配信API エラーOUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CouponTestDeliveryApiOutputDTOError {

	/** エラーコード */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("code")
	private String code;

	/** 開発者向けメッセージ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("developerMessage")
	private String developerMessage;

	/** ユーザー向けメッセージ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("userMessage")
	private String userMessage;

	/**
	 * @return code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code セットする code
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return developerMessage
	 */
	public String getDeveloperMessage() {
		return developerMessage;
	}

	/**
	 * @param developerMessage セットする developerMessage
	 */
	public void setDeveloperMessage(String developerMessage) {
		this.developerMessage = developerMessage;
	}

	/**
	 * @return userMessage
	 */
	public String getUserMessage() {
		return userMessage;
	}

	/**
	 * @param userMessage セットする userMessage
	 */
	public void setUserMessage(String userMessage) {
		this.userMessage = userMessage;
	}

}
