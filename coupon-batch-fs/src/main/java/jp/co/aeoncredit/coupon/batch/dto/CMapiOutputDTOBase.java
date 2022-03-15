package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

public class CMapiOutputDTOBase extends DTOBase {
	/** エラー */
	@JsonProperty("error")
	private String error;

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
