package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * アプリ内Msg一覧取得API(GetAppMsgOutput).ConditionのOutput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAppMsgOutputDTOCondition {

	/** 状態ID */
	@JsonProperty("condition_id")
	private int conditionId;
	
	/** トリガー */
	@JsonProperty("trigger")
	private GetAppMsgOutputDTOConditionTrigger trigger;
	
	/** ターゲット */
	@JsonProperty("target")
	private GetAppMsgOutputDTOConditionTarget target;
	
	/** 配信する累計最大回数 */
	@JsonProperty("max_total_count")
	private int maxTotalCount;
	
	/** １日に配信する最大回数 */
	@JsonProperty("max_total_count_per_day")
	private int maxTotalCountPerDay;

	/** カテゴリ */
	@JsonProperty("category")
	private String category;
	
	/** 有効化フラグ */
	@JsonProperty("is_active")
	private boolean isActive;

	public int getConditionId() {
		return conditionId;
	}

	public void setConditionId(int conditionId) {
		this.conditionId = conditionId;
	}

	public GetAppMsgOutputDTOConditionTrigger getTrigger() {
		return trigger;
	}

	public void setTrigger(GetAppMsgOutputDTOConditionTrigger trigger) {
		this.trigger = trigger;
	}

	public GetAppMsgOutputDTOConditionTarget getTarget() {
		return target;
	}

	public void setTarget(GetAppMsgOutputDTOConditionTarget target) {
		this.target = target;
	}

	public int getMaxTotalCount() {
		return maxTotalCount;
	}

	public void setMaxTotalCount(int maxTotalCount) {
		this.maxTotalCount = maxTotalCount;
	}

	public int getMaxTotalCountPerDay() {
		return maxTotalCountPerDay;
	}

	public void setMaxTotalCountPerDay(int maxTotalCountPerDay) {
		this.maxTotalCountPerDay = maxTotalCountPerDay;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
}
