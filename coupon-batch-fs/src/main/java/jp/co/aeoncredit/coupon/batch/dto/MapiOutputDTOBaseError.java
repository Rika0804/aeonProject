package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

public class MapiOutputDTOBaseError extends DTOBase {
	/** エラーコード*/
	@JsonProperty("code")
	private String code;

	/** エラーメッセージ */
	@JsonProperty("message")
	private String message;

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
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message セットする message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
