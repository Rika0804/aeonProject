package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * その他クーポンテスト配信API OUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CouponTestDeliveryApiOutputDTO {

	/** 実行結果 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("status")
	private String status;

	/** 現在タイムスタンプ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("timestamp")
	private String timestamp;

	/** クーポンUUID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("uuid")
	private String uuid;

	/** Push配信ID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("id")
	private Long id;

	/** エラー結果 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("error")
	private CouponTestDeliveryApiOutputDTOError error;

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
	 * @return timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp セットする timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid セットする uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id セットする id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return error
	 */
	public CouponTestDeliveryApiOutputDTOError getError() {
		return error;
	}

	/**
	 * @param error セットする error
	 */
	public void setError(CouponTestDeliveryApiOutputDTOError error) {
		this.error = error;
	}

}
