package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

public class MapiOutputDTOBase extends DTOBase {
	/** ステータス */
	@JsonProperty("status")
	private String status;

	/** エラー */
	@JsonProperty("error")
	private MapiOutputDTOBaseError error;

	/**
	 * @return status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status セットする status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return error
	 */
	public MapiOutputDTOBaseError getError() {
		return error;
	}

	/**
	 * @param error セットする error
	 */
	public void setError(MapiOutputDTOBaseError error) {
		this.error = error;
	}

}
