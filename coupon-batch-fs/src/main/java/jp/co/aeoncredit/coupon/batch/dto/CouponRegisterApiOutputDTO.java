package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録・更新 OUTPUTDTO
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CouponRegisterApiOutputDTO {

	/** 実行結果 */
	@JsonProperty("status")
	private String status;

	/** 現在タイムスタンプ */
	@JsonProperty("timestamp")
	private String timestamp;

	/** クーポンUUID */
	@JsonProperty("uuid")
	private String uuid;

	/** 配信ID(アプリイベント用) */
	@JsonProperty("deliveryId")
	private Long deliveryId;

	/** Push配信ID(センサーイベント用) */
	@JsonProperty("id")
	private Long id;

	/** エラー結果 */
	@JsonProperty("error")
	private CouponRegisterApiOutputDTOError error;

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
	 * @return deliveryId
	 */
	public Long getDeliveryId() {
		return deliveryId;
	}

	/**
	 * @param deliveryId セットする deliveryId
	 */
	public void setDeliveryId(Long deliveryId) {
		this.deliveryId = deliveryId;
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
	public CouponRegisterApiOutputDTOError getError() {
		return error;
	}

	/**
	 * @param error セットする error
	 */
	public void setError(CouponRegisterApiOutputDTOError error) {
		this.error = error;
	}

}
