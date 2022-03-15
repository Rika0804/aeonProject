package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * その他クーポンテスト配信API INPUTDTO
 */
public class CouponTestDeliveryApiInputDTO {

	/** クーポン利用可能店舗ID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("providers")
	private String[] providers;

	/** 限定クーポンフラグ */
	@JsonProperty("is_distributable")
	private Boolean isDistributable;

	/** クーポン名 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("name")
	private String name;

	/** クーポン説明 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("short_description")
	private String shortDescription;

	/** クーポン利用条件 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("description")
	private String description;

	/** 表示順ポイント */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("priority")
	private Integer priority;

	/** 一人当たり利用可能枚数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("user_usable_count")
	private Short userUsableCount;

	/** 全体利用上限枚数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("total_usable_count")
	private Integer totalUsableCount;

	/** 限定CP一人当たり配布枚数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("user_distributable_count")
	private Short userDistributableCount;

	/** 限定CP全体上限配布枚数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("total_distributable_count")
	private Integer totalDistributableCount;

	/** 表示開始日時 */
	@JsonProperty("visible_start_at")
	private String visibleStartAt;

	/** 表示終了日時 */
	@JsonProperty("visible_end_at")
	private String visibleEndAt;

	/** 有効開始日時 */
	@JsonProperty("usable_start_at")
	private String usableStartAt;

	/** 有効終了日時 */
	@JsonProperty("usable_end_at")
	private String usableEndAt;

	/** 限定配布後有効日数 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("usable_days")
	private Integer usableDays;

	/** 公開ステータス */
	@JsonProperty("is_open")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean isOpen;

	/** 任意追加項目 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("additional_items")
	private CouponTestDeliveryApiInputDTOAdditional additionalItems;

	/** Push通知設定 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("pushTarget")
	private CouponTestDeliveryApiInputDTOPushTarget pushTarget;

	/**
	 * @return providers
	 */
	public String[] getProviders() {
		return providers;
	}

	/**
	 * @param providers セットする providers
	 */
	public void setProviders(String[] providers) {
		this.providers = providers;
	}

	/**
	 * @return isDistributable
	 */
	public Boolean getIsDistributable() {
		return isDistributable;
	}

	/**
	 * @param isDistributable セットする isDistributable
	 */
	public void setDistributable(Boolean isDistributable) {
		this.isDistributable = isDistributable;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name セットする name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return shortDescription
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * @param shortDescription セットする shortDescription
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description セットする description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return priority
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * @param priority セットする priority
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * @return userUsableCount
	 */
	public Short getUserUsableCount() {
		return userUsableCount;
	}

	/**
	 * @param userUsableCount セットする userUsableCount
	 */
	public void setUserUsableCount(Short userUsableCount) {
		this.userUsableCount = userUsableCount;
	}

	/**
	 * @return totalUsableCount
	 */
	public Integer getTotalUsableCount() {
		return totalUsableCount;
	}

	/**
	 * @param totalUsableCount セットする totalUsableCount
	 */
	public void setTotalUsableCount(Integer totalUsableCount) {
		this.totalUsableCount = totalUsableCount;
	}

	/**
	 * @return userDistributableCount
	 */
	public Short getUserDistributableCount() {
		return userDistributableCount;
	}

	/**
	 * @param userDistributableCount セットする userDistributableCount
	 */
	public void setUserDistributableCount(Short userDistributableCount) {
		this.userDistributableCount = userDistributableCount;
	}

	/**
	 * @return totalDistributableCount
	 */
	public Integer getTotalDistributableCount() {
		return totalDistributableCount;
	}

	/**
	 * @param totalDistributableCount セットする totalDistributableCount
	 */
	public void setTotalDistributableCount(Integer totalDistributableCount) {
		this.totalDistributableCount = totalDistributableCount;
	}

	/**
	 * @return visibleStartAt
	 */
	public String getVisibleStartAt() {
		return visibleStartAt;
	}

	/**
	 * @param visibleStartAt セットする visibleStartAt
	 */
	public void setVisibleStartAt(String visibleStartAt) {
		this.visibleStartAt = visibleStartAt;
	}

	/**
	 * @return visibleEndAt
	 */
	public String getVisibleEndAt() {
		return visibleEndAt;
	}

	/**
	 * @param visibleEndAt セットする visibleEndAt
	 */
	public void setVisibleEndAt(String visibleEndAt) {
		this.visibleEndAt = visibleEndAt;
	}

	/**
	 * @return usableStartAt
	 */
	public String getUsableStartAt() {
		return usableStartAt;
	}

	/**
	 * @param usableStartAt セットする usableStartAt
	 */
	public void setUsableStartAt(String usableStartAt) {
		this.usableStartAt = usableStartAt;
	}

	/**
	 * @return usableEndAt
	 */
	public String getUsableEndAt() {
		return usableEndAt;
	}

	/**
	 * @param usableEndAt セットする usableEndAt
	 */
	public void setUsableEndAt(String usableEndAt) {
		this.usableEndAt = usableEndAt;
	}

	/**
	 * @return usableDays
	 */
	public Integer getUsableDays() {
		return usableDays;
	}

	/**
	 * @param usableDays セットする usableDays
	 */
	public void setUsableDays(Integer usableDays) {
		this.usableDays = usableDays;
	}

	/**
	 * @return isOpen
	 */
	public Boolean getIsOpen() {
		return isOpen;
	}

	/**
	 * @param isOpen セットする isOpen
	 */
	public void setOpen(Boolean isOpen) {
		this.isOpen = isOpen;
	}

	/**
	 * @return additionalItems
	 */
	public CouponTestDeliveryApiInputDTOAdditional getAdditionalItems() {
		return additionalItems;
	}

	/**
	 * @param additionalItems セットする additionalItems
	 */
	public void setAdditionalItems(CouponTestDeliveryApiInputDTOAdditional additionalItems) {
		this.additionalItems = additionalItems;
	}

	/**
	 * @return pushTarget
	 */
	public CouponTestDeliveryApiInputDTOPushTarget getPushTarget() {
		return pushTarget;
	}

	/**
	 * @param pushTarget セットする pushTarget
	 */
	public void setPushTarget(CouponTestDeliveryApiInputDTOPushTarget pushTarget) {
		this.pushTarget = pushTarget;
	}

}
