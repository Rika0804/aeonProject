package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API bluetooth端末情報INPUTDTO(センサーイベントクーポン用)
 */
public class CouponRegisterApiInputDTOBluetooth {

	/** 種類 */
	@JsonProperty("type")
	private String type;

	/** uuid */
	@JsonProperty("uuid")
	private String uuid;

	/** メジャー番号 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("major")
	private Integer major;

	/** マイナー番号 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("minor")
	private Integer minor;

	/** 電波強度 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("rssi")
	private Integer rssi;

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type セットする type
	 */
	public void setType(String type) {
		this.type = type;
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
	 * @return major
	 */
	public Integer getMajor() {
		return major;
	}

	/**
	 * @param major セットする major
	 */
	public void setMajor(Integer major) {
		this.major = major;
	}

	/**
	 * @return minor
	 */
	public Integer getMinor() {
		return minor;
	}

	/**
	 * @param minor セットする minor
	 */
	public void setMinor(Integer minor) {
		this.minor = minor;
	}

	/**
	 * @return rssi
	 */
	public Integer getRssi() {
		return rssi;
	}

	/**
	 * @param rssi セットする rssi
	 */
	public void setRssi(Integer rssi) {
		this.rssi = rssi;
	}

}
