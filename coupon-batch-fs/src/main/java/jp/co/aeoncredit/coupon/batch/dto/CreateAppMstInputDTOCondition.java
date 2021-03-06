/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg登録API(CreateAppMst).ConditionのInput DTOクラス。
 */
public class CreateAppMstInputDTOCondition extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** トリガー(trigger) */
	@JsonProperty("trigger")
	private CreateAppMstInputDTOTrigger trigger;

	/** ターゲット(Target) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("target")
	private CreateAppMstInputDTOTarget target;

	/** 配信する累計最大回数(maxTotalCount) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("max_total_count")
	private Short maxTotalCount;

	/** １日に配信する最大回数(maxTotalCountPerDay) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("max_total_count_per_day")
	private Short maxTotalCountPerDay;

	/** カテゴリ(category) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("category")
	private String category;

	/**
	 * トリガー(trigger)を取得する。
	 * 
	 * @return トリガー(trigger)を取得する。
	 */
	public CreateAppMstInputDTOTrigger getTrigger() {
		return trigger;
	}

	/**
	 * トリガー(trigger)を設定する。
	 * 
	 * @param トリガー(trigger)
	 */
	public void setTrigger(CreateAppMstInputDTOTrigger trigger) {
		this.trigger = trigger;
	}

	/**
	 * ターゲット(target)を取得する。
	 * 
	 * @return ターゲット(target)
	 */
	public CreateAppMstInputDTOTarget getTarget() {
		return target;
	}

	/**
	 * ターゲット(target)を設定する。
	 * 
	 * @param target ターゲット(target)
	 */
	public void setTarget(CreateAppMstInputDTOTarget target) {
		this.target = target;
	}

	/**
	 * 配信する累計最大回数(maxTotalCount)を取得する。
	 * 
	 * @return 配信する累計最大回数(maxTotalCount)
	 */
	public Short getMaxTotalCount() {
		return maxTotalCount;
	}

	/**
	 * 配信する累計最大回数(maxTotalCount)を設定する。
	 * 
	 * @param maxTotalCount 配信する累計最大回数(maxTotalCount)
	 */
	public void setMaxTotalCount(Short maxTotalCount) {
		this.maxTotalCount = maxTotalCount;
	}

	/**
	 * １日に配信する最大回数(maxTotalCountPerDay)を取得する。
	 * 
	 * @return １日に配信する最大回数(maxTotalCountPerDay)
	 */
	public Short getMaxTotalCountPerDay() {
		return maxTotalCountPerDay;
	}

	/**
	 * １日に配信する最大回数(maxTotalCountPerDay)を設定する。
	 * 
	 * @param maxTotalCountPerDay １日に配信する最大回数(maxTotalCountPerDay)
	 */
	public void setMaxTotalCountPerDay(Short maxTotalCountPerDay) {
		this.maxTotalCountPerDay = maxTotalCountPerDay;
	}

	/**
	 * カテゴリ(category)を取得する。
	 * 
	 * @return カテゴリ(category)
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * カテゴリ(category)を設定する。
	 * 
	 * @param category カテゴリ(category)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

}
