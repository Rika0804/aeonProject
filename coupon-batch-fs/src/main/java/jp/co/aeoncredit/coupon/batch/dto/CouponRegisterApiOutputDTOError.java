package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録・更新 エラーOUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CouponRegisterApiOutputDTOError {

	/** エラーコード */
	@JsonProperty("code")
	private String code;

	/** 開発者向けメッセージ */
	@JsonProperty("developerMessage")
	private String developerMessage;

	/** ユーザー向けメッセージ */
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
