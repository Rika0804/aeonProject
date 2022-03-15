package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).PeriodのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOPeriod extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** start(start) */
	@JsonProperty("start")
	private String start;

	/** end(end) */
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
