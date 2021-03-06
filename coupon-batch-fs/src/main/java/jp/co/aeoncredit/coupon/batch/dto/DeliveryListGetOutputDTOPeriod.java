/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * 配信一覧取得 API(DeliveryListGet).PeriodのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryListGetOutputDTOPeriod extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** start(Start) */
	@JsonProperty("start")
	private String start;

	/** end(End) */
	@JsonProperty("end")
	private String end;

	/**
	 * start(start)を取得する。
	 * 
	 * @return start(start)
	 */
	public String getStart() {
		return start;
	}

	/**
	 * start(start)を設定する。
	 * 
	 * @param start start(start)
	 */
	public void setStart(String start) {
		this.start = start;
	}

	/**
	 * end(end)を取得する。
	 * 
	 * @return end(end)
	 */
	public String getEnd() {
		return end;
	}

	/**
	 * end(end)を設定する。
	 * 
	 * @param end end(end)
	 */
	public void setEnd(String end) {
		this.end = end;
	}
}
