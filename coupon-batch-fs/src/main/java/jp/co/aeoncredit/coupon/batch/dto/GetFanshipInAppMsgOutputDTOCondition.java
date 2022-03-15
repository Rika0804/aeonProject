package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg配信情報取得API(GetFanshipInAppMsg).ConditionのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class GetFanshipInAppMsgOutputDTOCondition extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** trigger(trigger) */
	@JsonProperty("trigger")
	private GetFanshipInAppMsgOutputDTOTrigger trigger;

	/** target(target) */
	@JsonProperty("target")
	private GetFanshipInAppMsgOutputDTOTarget target;

	/** maxTotalCount(maxTotalCount) */
	@JsonProperty("max_total_count")
	private Integer maxTotalCount;

	/** maxTotalCountPerDay(maxTotalCountPerDay) */
	@JsonProperty("max_total_count_per_day")
	private Integer maxTotalCountPerDay;

	/** category(category) */
	@JsonProperty("category")
	private String category;

	/** conditionId(conditionId) */
	@JsonProperty("condition_id")
	private Integer conditionId;

	/** isActive(isActive) */
	@JsonProperty("is_active")
	private boolean isActive;

	/**
	 * trigger(trigger)を取得する。
	 * 
	 * @return trigger
	 */
	public GetFanshipInAppMsgOutputDTOTrigger getTrigger() {
		return trigger;
	}

	/**
	 * trigger(trigger)を設定する。
	 * 
	 * @param trigger trigger(trigger)
	 */
	public void setTrigger(GetFanshipInAppMsgOutputDTOTrigger trigger) {
		this.trigger = trigger;
	}

	/**
	 * target(target)を取得する。
	 * 
	 * @return target
	 */
	public GetFanshipInAppMsgOutputDTOTarget getTarget() {
		return target;
	}

	/**
	 * target(target)を設定する。
	 * 
	 * @param target target(target)
	 */
	public void setTarget(GetFanshipInAppMsgOutputDTOTarget target) {
		this.target = target;
	}

	/**
	 * maxTotalCount(maxTotalCount)を取得する。
	 * 
	 * @return maxTotalCount
	 */
	public Integer getMaxTotalCount() {
		return maxTotalCount;
	}

	/**
	 * maxTotalCount(maxTotalCount)を設定する。
	 * 
	 * @param maxTotalCount maxTotalCount(maxTotalCount)
	 */
	public void setMaxTotalCount(Integer maxTotalCount) {
		this.maxTotalCount = maxTotalCount;
	}

	/**
	 * maxTotalCountPerDay(maxTotalCountPerDay)を取得する。
	 * 
	 * @return maxTotalCountPerDay
	 */
	public Integer getMaxTotalCountPerDay() {
		return maxTotalCountPerDay;
	}

	/**
	 * maxTotalCountPerDay(maxTotalCountPerDay)を設定する。
	 * 
	 * @param maxTotalCountPerDay maxTotalCountPerDay(maxTotalCountPerDay)
	 */
	public void setMaxTotalCountPerDay(Integer maxTotalCountPerDay) {
		this.maxTotalCountPerDay = maxTotalCountPerDay;
	}

	/**
	 * category(category)を取得する。
	 * 
	 * @return category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * category(category)を設定する。
	 * 
	 * @param category category(category)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * conditionId(conditionId)を取得する。
	 * 
	 * @return conditionId
	 */
	public Integer getConditionId() {
		return conditionId;
	}

	/**
	 * conditionId(conditionId)を設定する。
	 * 
	 * @param conditionId conditionId(conditionId)
	 */
	public void setConditionId(Integer conditionId) {
		this.conditionId = conditionId;
	}

	/**
	 * isActive(isActive)を取得する。
	 * 
	 * @return isActive
	 */
	public boolean isIsActive() {
		return isActive;
	}

	/**
	 * isActive(isActive)を設定する。
	 * 
	 * @param isActive isActive(isActive)
	 */
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
}
