package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ内Msg一覧取得API(GetAppMsgOutput).TriggerのOutput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAppMsgOutputDTOConditionTrigger {

	/** 起動トリガーイベント名 */
	@JsonProperty("event_name")
	private List<String> eventName;
	
	/** 配布期間 */
	@JsonProperty("period")
	private GetAppMsgOutputDTOConditionTriggerPeriod[] period;

	public List<String> getEventName() {
		return eventName;
	}

	public void setEventName(List<String> eventName) {
		this.eventName = eventName;
	}

	public GetAppMsgOutputDTOConditionTriggerPeriod[] getPeriod() {
		return period;
	}

	public void setPeriod(GetAppMsgOutputDTOConditionTriggerPeriod[] period) {
		this.period = period;
	}
	
}
	
