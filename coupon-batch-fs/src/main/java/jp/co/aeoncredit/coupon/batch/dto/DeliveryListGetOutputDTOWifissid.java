/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * 配信一覧取得 API(DeliveryListGet).WifissidのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryListGetOutputDTOWifissid extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** essid(Essid) */
	@JsonProperty("essid")
	private String essid;

	/** bssid(Bssid) */
	@JsonProperty("bssid")
	private String bssid;

	/** rssi(Rssi) */
	@JsonProperty("rssi")
	private String rssi;

	/**
	 * essid(essid)を取得する。
	 * 
	 * @return essid(essid)
	 */
	public String getEssid() {
		return essid;
	}

	/**
	 * essid(essid)を設定する。
	 * 
	 * @param essid essid(essid)
	 */
	public void setEssid(String essid) {
		this.essid = essid;
	}

	/**
	 * bssid(bssid)を取得する。
	 * 
	 * @return bssid(bssid)
	 */
	public String getBssid() {
		return bssid;
	}

	/**
	 * bssid(bssid)を設定する。
	 * 
	 * @param bssid bssid(bssid)
	 */
	public void setBssid(String bssid) {
		this.bssid = bssid;
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