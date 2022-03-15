/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * 配信一覧取得 API(DeliveryListGet).BluetoothのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryListGetOutputDTOBluetooth extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** type(Type) */
	@JsonProperty("type")
	private String type;

	/** uuid(Uuid) */
	@JsonProperty("uuid")
	private String uuid;

	/** major(Major) */
	@JsonProperty("major")
	private String major;

	/** minor(Minor) */
	@JsonProperty("minor")
	private String minor;

	/** rssi(Rssi) */
	@JsonProperty("rssi")
	private String rssi;

	/**
	 * type(type)を取得する。
	 * 
	 * @return type(type)
	 */
	public String getType() {
		return type;
	}

	/**
	 * type(type)を設定する。
	 * 
	 * @param type type(type)
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * uuid(uuid)を取得する。
	 * 
	 * @return uuid(uuid)
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * uuid(uuid)を設定する。
	 * 
	 * @param uuid uuid(uuid)
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * major(major)を取得する。
	 * 
	 * @return major(major)
	 */
	public String getMajor() {
		return major;
	}

	/**
	 * major(major)を設定する。
	 * 
	 * @param major major(major)
	 */
	public void setMajor(String major) {
		this.major = major;
	}

	/**
	 * minor(minor)を取得する。
	 * 
	 * @return minor(minor)
	 */
	public String getMinor() {
		return minor;
	}

	/**
	 * minor(minor)を設定する。
	 * 
	 * @param minor minor(minor)
	 */
	public void setMinor(String minor) {
		this.minor = minor;
	}

	/**
	 * rssi(rssi)を取得する。
	 * 
	 * @return rssi(rssi)
	 */
	public String getRssi() {
		return rssi;
	}

	/**
	 * rssi(rssi)を設定する。
	 * 
	 * @param rssi rssi(rssi)
	 */
	public void setRssi(String rssi) {
		this.rssi = rssi;
	}
}