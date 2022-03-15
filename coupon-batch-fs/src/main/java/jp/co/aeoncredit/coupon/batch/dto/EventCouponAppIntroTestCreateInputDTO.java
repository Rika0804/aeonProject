/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * イベントクーポン（アプリ導入）作成テストAPI(EventCouponAppIntroTestCreate)のInput DTOクラス。
 */
public class EventCouponAppIntroTestCreateInputDTO {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** クーポン利用可能店舗ID(providers) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("providers")
	private String[] providers;

	/** 限定クーポンフラグ(isDistributable) */
	@JsonProperty("is_distributable")
	private boolean isDistributable;

	/** クーポン名(name) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("name")
	private String name;

	/** クーポン説明(shortDescription) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("short_description")
	private String shortDescription;

	/** クーポン利用条件(description) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("description")
	private String description;

	/** 表示順ポイント(priority) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("priority")
	private Integer priority;

	/** 一人当たり利用可能枚数(userUsableCount) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("user_usable_count")
	private Short userUsableCount;

	/** 全体利用上限枚数(totalUsableCount) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("total_usable_count")
	private Short totalUsableCount;

	/** 限定CP一人当たり配布枚数(userDistributableCount) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("user_distributable_count")
	private Short userDistributableCount;

	/** 限定CP全体上限配布枚数(totalDistributableCount) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("total_distributable_count")
	private Integer totalDistributableCount;

	/** 表示開始日時(visibleStartAt) */
	@JsonProperty("visible_start_at")
	private String visibleStartAt;

	/** 表示終了日時(visibleEndAt) */
	@JsonProperty("visible_end_at")
	private String visibleEndAt;

	/** 有効開始日時(usableStartAt) */
	@JsonProperty("usable_start_at")
	private String usableStartAt;

	/** 有効終了日時(usableEndAt) */
	@JsonProperty("usable_end_at")
	private String usableEndAt;

	/** 限定配布後有効日数(usableDays) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("usable_days")
	private String usableDays;

	/** 公開ステータス(isOpen) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("is_open")
	private Boolean isOpen;

	/** 任意追加項目(AdditionalItems) */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("additional_items")
	private EventCouponAppIntroTestCreateInputDTOAdditionalItems additionalItems;

	/** アプリ内メッセージ設定(Delivery) */
	@JsonProperty("delivery")
	private EventCouponAppIntroTestCreateInputDTODelivery delivery;

	/**
	 * クーポン利用可能店舗ID(providers)を取得する。
	 * 
	 * @return クーポン利用可能店舗ID(providers)
	 */
	public String[] getProviders() {
		return providers;
	}

	/**
	 * クーポン利用可能店舗ID(providers)を設定する。
	 * 
	 * @param providers クーポン利用可能店舗ID(providers)
	 */
	public void setProviders(String[] providers) {
		this.providers = providers;
	}

	/**
	 * 限定クーポンフラグ(isDistributable)を取得する。
	 * 
	 * @return 限定クーポンフラグ(isDistributable)
	 */
	public boolean getIsDistributable() {
		return isDistributable;
	}

	/**
	 * 限定クーポンフラグ(isDistributable)を設定する。
	 * 
	 * @param isDistributable 限定クーポンフラグ(isDistributable)
	 */
	public void setIsDistributable(boolean isDistributable) {
		this.isDistributable = isDistributable;
	}

	/**
	 * クーポン名(name)を取得する。
	 * 
	 * @return クーポン名(name)
	 */
	public String getName() {
		return name;
	}

	/**
	 * クーポン名(name)を設定する。
	 * 
	 * @param name クーポン名(name)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * クーポン説明(shortDescription)を取得する。
	 * 
	 * @return クーポン説明(shortDescription)
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * クーポン説明(shortDescription)を設定する。
	 * 
	 * @param shortDescription クーポン説明(shortDescription)
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * クーポン利用条件(description)を取得する。
	 * 
	 * @return クーポン利用条件(description)
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * クーポン利用条件(description)を設定する。
	 * 
	 * @param description クーポン利用条件(description)
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 表示順ポイント(priority)を取得する。
	 * 
	 * @return 表示順ポイント(priority)
	 */
	public Integer getPriority() {
		return priority;
	}

	/**
	 * 表示順ポイント(priority)を設定する。
	 * 
	 * @param priority 表示順ポイント(priority)
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * 一人当たり利用可能枚数(userUsableCount)を取得する。
	 * 
	 * @return 一人当たり利用可能枚数(userUsableCount)
	 */
	public Short getUserUsableCount() {
		return userUsableCount;
	}

	/**
	 * 一人当たり利用可能枚数(userUsableCount)を設定する。
	 * 
	 * @param userUsableCount 一人当たり利用可能枚数(userUsableCount)
	 */
	public void setUserUsableCount(Short userUsableCount) {
		this.userUsableCount = userUsableCount;
	}

	/**
	 * 全体利用上限枚数(totalUsableCount)を取得する。
	 * 
	 * @return 全体利用上限枚数(totalUsableCount)
	 */
	public Short getTotalUsableCount() {
		return totalUsableCount;
	}

	/**
	 * 全体利用上限枚数(totalUsableCount)を設定する。
	 * 
	 * @param totalUsableCount 全体利用上限枚数(totalUsableCount)
	 */
	public void setTotalUsableCount(Short totalUsableCount) {
		this.totalUsableCount = totalUsableCount;
	}

	/**
	 * 限定CP一人当たり配布枚数(userDistributableCount)を取得する。
	 * 
	 * @return 限定CP一人当たり配布枚数(userDistributableCount)
	 */
	public Short getUserDistributableCount() {
		return userDistributableCount;
	}

	/**
	 * 限定CP一人当たり配布枚数(userDistributableCount)を設定する。
	 * 
	 * @param userDistributableCount 限定CP一人当たり配布枚数(userDistributableCount)
	 */
	public void setUserDistributableCount(Short userDistributableCount) {
		this.userDistributableCount = userDistributableCount;
	}

	/**
	 * 限定CP全体上限配布枚数(totalDistributableCount)を取得する。
	 * 
	 * @return 限定CP全体上限配布枚数(totalDistributableCount)
	 */
	public Integer getTotalDistributableCount() {
		return totalDistributableCount;
	}

	/**
	 * 限定CP全体上限配布枚数(totalDistributableCount)を設定する。
	 * 
	 * @param totalDistributableCount 限定CP全体上限配布枚数(totalDistributableCount)
	 */
	public void setTotalDistributableCount(Integer totalDistributableCount) {
		this.totalDistributableCount = totalDistributableCount;
	}

	/**
	 * 表示開始日時(visibleStartAt)を取得する。
	 * 
	 * @return 表示開始日時(visibleStartAt)
	 */
	public String getVisibleStartAt() {
		return visibleStartAt;
	}

	/**
	 * 表示開始日時(visibleStartAt)を設定する。
	 * 
	 * @param visibleStartAt 表示開始日時(visibleStartAt)
	 */
	public void setVisibleStartAt(String visibleStartAt) {
		this.visibleStartAt = visibleStartAt;
	}

	/**
	 * 表示終了日時(visibleEndAt)を取得する。
	 * 
	 * @return 表示終了日時(visibleEndAt)
	 */
	public String getVisibleEndAt() {
		return visibleEndAt;
	}

	/**
	 * 表示終了日時(visibleEndAt)を設定する。
	 * 
	 * @param visibleEndAt 表示終了日時(visibleEndAt)
	 */
	public void setVisibleEndAt(String visibleEndAt) {
		this.visibleEndAt = visibleEndAt;
	}

	/**
	 * 有効開始日時(usableStartAt)を取得する。
	 * 
	 * @return 有効開始日時(usableStartAt)
	 */
	public String getUsableStartAt() {
		return usableStartAt;
	}

	/**
	 * 有効開始日時(usableStartAt)を設定する。
	 * 
	 * @param usableStartAt 有効開始日時(usableStartAt)
	 */
	public void setUsableStartAt(String usableStartAt) {
		this.usableStartAt = usableStartAt;
	}

	/**
	 * 有効終了日時(usableEndAt)を取得する。
	 * 
	 * @return 有効終了日時(usableEndAt)
	 */
	public String getUsableEndAt() {
		return usableEndAt;
	}

	/**
	 * 有効終了日時(usableEndAt)を設定する。
	 * 
	 * @param usableEndAt 有効終了日時(usableEndAt)
	 */
	public void setUsableEndAt(String usableEndAt) {
		this.usableEndAt = usableEndAt;
	}

	/**
	 * 限定配布後有効日数(usableDays)を取得する。
	 * 
	 * @return 限定配布後有効日数(usableDays)
	 */
	public String getUsableDays() {
		return usableDays;
	}

	/**
	 * 限定配布後有効日数(usableDays)を設定する。
	 * 
	 * @param usableDays 限定配布後有効日数(usableDays)
	 */
	public void setUsableDays(String usableDays) {
		this.usableDays = usableDays;
	}

	/**
	 * 公開ステータス(isOpen)を取得する。
	 * 
	 * @return 公開ステータス(isOpen)
	 */
	public Boolean getIsOpen() {
		return isOpen;
	}

	/**
	 * 公開ステータス(isOpen)を設定する。
	 * 
	 * @param isOpen 公開ステータス(isOpen)
	 */
	public void setIsOpen(Boolean isOpen) {
		this.isOpen = isOpen;
	}

	/**
	 * 任意追加項目(additionalItems)を取得する。
	 * 
	 * @return 任意追加項目(additionalItems)
	 */
	public EventCouponAppIntroTestCreateInputDTOAdditionalItems getAdditionalItems() {
		return additionalItems;
	}

	/**
	 * 任意追加項目(additionalItems)を設定する。
	 * 
	 * @param additionalItems 任意追加項目(additionalItems)
	 */
	public void setAdditionalItems(EventCouponAppIntroTestCreateInputDTOAdditionalItems additionalItems) {
		this.additionalItems = additionalItems;
	}

	/**
	 * アプリ内メッセージ設定(delivery)を取得する。
	 * 
	 * @return アプリ内メッセージ設定(delivery)
	 */
	public EventCouponAppIntroTestCreateInputDTODelivery getDelivery() {
		return delivery;
	}

	/**
	 * アプリ内メッセージ設定(delivery)を設定する。
	 * 
	 * @param delivery アプリ内メッセージ設定(delivery)
	 */
	public void setDelivery(EventCouponAppIntroTestCreateInputDTODelivery delivery) {
		this.delivery = delivery;
	}
}
