package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).TriggerのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOTrigger extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** eventName(eventName) */
	@JsonProperty("event_name")
	private List<String> eventName;

	/** period(period) */
	@JsonProperty("period")
	private List<GetFanshipInAppMsgOutputDTOPeriod> period;

	/**
	 * eventName(eventName)を取得する。
	 * 
	 * @return eventName
	 */
	public List<String> getEventName() {
		return eventName;
	}

	/**
	 * eventName(eventName)を設定する。
	 * 
	 * @param eventName eventName(eventName)
	 */
	public void setEventName(List<String> eventName) {
		this.eventName = eventName;
	}

	/**
	 * period(period)を取得する。
	 * 
	 * @return period
	 */
	public List<GetFanshipInAppMsgOutputDTOPeriod> getPeriod() {
		return period;
	}

	/**
	 * period(period)を設定する。
	 * 
	 * @param period period(period)
	 */
	public void setPeriod(List<GetFanshipInAppMsgOutputDTOPeriod> period) {
		this.period = period;
	}
}