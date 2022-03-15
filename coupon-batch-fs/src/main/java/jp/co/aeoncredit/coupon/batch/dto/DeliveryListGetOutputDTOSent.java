/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * 配信一覧取得 API(DeliveryListGet).SentのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryListGetOutputDTOSent extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** iphone(Iphone) */
	@JsonProperty("iphone")
	private String iphone;

	/** android(Android) */
	@JsonProperty("android")
	private String android;

	/** total(Total) */
	@JsonProperty("total")
	private String total;

	/** updated(Updated) */
	@JsonProperty("updated")
	private String updated;

	/**
	 * iphone(iphone)を取得する。
	 * 
	 * @return iphone(iphone)
	 */
	public String getIphone() {
		return iphone;
	}

	/**
	 * iphone(iphone)を設定する。
	 * 
	 * @param iphone iphone(iphone)
	 */
	public void setIphone(String iphone) {
		this.iphone = iphone;
	}

	/**
	 * android(android)を取得する。
	 * 
	 * @return android(android)
	 */
	public String getAndroid() {
		return android;
	}

	/**
	 * android(android)を設定する。
	 * 
	 * @param android android(android)
	 */
	public void setAndroid(String android) {
		this.android = android;
	}

	/**
	 * total(total)を取得する。
	 * 
	 * @return total(total)
	 */
	public String getTotal() {
		return total;
	}

	/**
	 * total(total)を設定する。
	 * 
	 * @param total total(total)
	 */
	public void setTotal(String total) {
		this.total = total;
	}

	/**
	 * updated(updated)を取得する。
	 * 
	 * @return updated(updated)
	 */
	public String getUpdated() {
		return updated;
	}

	/**
	 * updated(updated)を設定する。
	 * 
	 * @param updated updated(updated)
	 */
	public void setUpdated(String updated) {
		this.updated = updated;
	}
}