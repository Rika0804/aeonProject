package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API コンディションINPUTDTO(アプリイベントクーポン用)
 */
public class CouponRegisterApiInputDTOCondition {

	/** 機動トリガーイベント名 */
	@JsonProperty("event_name")
	private String[] eventName;

	/** 配布期間 */
	@JsonProperty("period")
	private List<CouponRegisterApiInputDTOPeriod> period;

	/** ターゲット */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("target")
	private CouponRegisterApiInputDTOTarget target;

	/** 配信する累計最大回数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("max_total_count")
	private Short maxTotalCount;

	/** １日に配信する最大回数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("max_total_count_per_day")
	private Short maxTotalCountPerDay;

	/** カテゴリ */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("category")
	private String category;

	/**
	 * @return eventName
	 */
	public String[] getEventName() {
		return eventName;
	}

	/**
	 * @param eventName セットする eventName
	 */
	public void setEventName(String[] eventName) {
		this.eventName = eventName;
	}

	/**
	 * @return period
	 */
	public List<CouponRegisterApiInputDTOPeriod> getPeriod() {
		return period;
	}

	/**
	 * @param period セットする period
	 */
	public void setPeriod(List<CouponRegisterApiInputDTOPeriod> period) {
		this.period = period;
	}

	/**
	 * @return target
	 */
	public CouponRegisterApiInputDTOTarget getTarget() {
		return target;
	}

	/**
	 * @param target セットする target
	 */
	public void setTarget(CouponRegisterApiInputDTOTarget target) {
		this.target = target;
	}

	/**
	 * @return maxTotalCount
	 */
	public Short getMaxTotalCount() {
		return maxTotalCount;
	}

	/**
	 * @param maxTotalCount セットする maxTotalCount
	 */
	public void setMaxTotalCount(Short maxTotalCount) {
		this.maxTotalCount = maxTotalCount;
	}

	/**
	 * @return maxTotalCountPerDay
	 */
	public Short getMaxTotalCountPerDay() {
		return maxTotalCountPerDay;
	}

	/**
	 * @param maxTotalCountPerDay セットする maxTotalCountPerDay
	 */
	public void setMaxTotalCountPerDay(Short maxTotalCountPerDay) {
		this.maxTotalCountPerDay = maxTotalCountPerDay;
	}

	/**
	 * @return category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category セットする category
	 */
	public void setCategory(String category) {
		this.category = category;
	}

}
