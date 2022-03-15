package jp.co.aeoncredit.coupon.batch.dto;

import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン⼀覧参照(ReferListCoupon)のOutput DTOクラス。
 *
 */

@XmlType(propOrder = { "list" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferListCouponResourseOutputDTO extends CMapiOutputDTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	
	/**クーポンUUID */
	@JsonProperty("uuid")
	public String uuid;
	
	/**限定クーポンかどうか  true:限定クーポン*/
	@JsonProperty("is_distributable")
	public boolean isDistributable;


	/**クーポン利用上限枚数*/
	@JsonProperty("total_usable_count")
	public int totalUsableCount;

	/**クーポン配布上限枚数*/
	@JsonProperty("total_distributable_count")
	public int totalDistributableCount;

	/**表示開始日時*/
	@JsonProperty("visible_start_at")
	public String visibleStartAt;

	/**表示終了日時*/
	@JsonProperty("visible_end_at")
	public String visibleEndAt;

	/**有効開始日時*/
	@JsonProperty("usable_start_at")
	public String usableStartAt;

	/**有効終了日時*/
	@JsonProperty("usable_end_at")
	public String usableEndAt;

	/**ステータス true=公開*/
	@JsonProperty("is_open")
	public boolean isOpen;

	/**配布数*/
	@JsonProperty("counter_distributed")
	public int counterDistributed;

	/**お気に入り登録数*/
	@JsonProperty("counter_favorite")
	public int counterFavorite;

	/**利用数*/
	@JsonProperty("counter_used")
	public int counterUsed;

	/**作成日時 */
	@JsonProperty("created_at")
	private String createdAt;

	/**	更新日時*/
	@JsonProperty("updated_at")
	private String updatedAt;


	/**
	 * isDistributableを取得
	 * @return isDistributable
	 */
	public boolean isDistributable() {
		return isDistributable;
	}

	/**
	 * isDistributableを設定する
	 * @param isDistributable
	 */
	public void setDistributable(boolean isDistributable) {
		this.isDistributable = isDistributable;
	}

	/**
	 * isOpenを取得する
	 * @return isOpen
	 */
	public boolean isOpen() {
		return isOpen;
	}

	/**
	 * isOpenを設定する
	 * @param isOpen
	 */
	public void setOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	/**
	 * usableEndAtを取得する
	 * @return usableEndAt
	 */
	public String getUsableEndAt() {
		return usableEndAt;
	}

	
	/**
	 * uuidを取得する
	 * @return uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * uuidを設定する
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * is_distributableを取得する
	 * @return is_distributable
	 */
	public boolean isIsDistributable() {
		return isDistributable;
	}

	/**
	 * is_distributableを設定する
	 * @param is_distributable
	 */
	public void setIsDistributable(boolean isDistributable) {
		this.isDistributable = isDistributable;
	}

	/**
	 * total_usable_countを取得する
	 * @return total_usable_count
	 */
	public int getTotalUsableCount() {
		return totalUsableCount;
	}

	/**
	 * total_usable_countを設定する
	 * @param total_usable_count
	 */
	public void setTotalUsableCount(int totalUsableCount) {
		this.totalUsableCount = totalUsableCount;
	}

	/**
	 * visible_start_atを取得する
	 * @return visible_start_at
	 */
	public String getVisibleStartAt() {
		return visibleStartAt;
	}

	/**
	 * visible_start_atを設定する
	 * @param visible_start_at
	 */
	public void setVisibleStartAt(String visibleStartAt) {
		this.visibleStartAt = visibleStartAt;
	}

	/**
	 * visible_end_atを取得する
	 * @return visible_end_at
	 */
	public String getVisibleEndAt() {
		return visibleEndAt;
	}

	/**
	 * visible_end_atを設定する
	 * @param visible_end_at
	 */
	public void setVisibleEndAt(String visibleEndAt) {
		this.visibleEndAt = visibleEndAt;
	}

	/**
	 * usable_start_atを取得する
	 * @return usable_start_at
	 */
	public String getUsableStartAt() {
		return usableStartAt;
	}

	/**
	 * usable_start_atを設定する
	 * @param usable_start_at
	 */
	public void setUsableStartAt(String usableStartAt) {
		this.usableStartAt = usableStartAt;
	}

	/**
	 * usable_end_atを取得する
	 * @return usable_end_at
	 */
	public String getUsablEndAt() {
		return usableEndAt;
	}

	/**
	 * usable_end_atを設定する
	 * @param usable_end_at
	 */
	public void setUsableEndAt(String usableEndAt) {
		this.usableEndAt = usableEndAt;
	}

	/**
	 * total_distributable_countを取得する
	 * @return total_distributable_count
	 */
	public int getTotalDistributableCount() {
		return totalDistributableCount;
	}

	/**
	 * total_distributable_countを設定する
	 * @param total_distributable_count
	 */
	public void setTotalDistributableCount(int totalDistributableCount) {
		this.totalDistributableCount = totalDistributableCount;
	}

	/**
	 * is_openを取得する
	 * @return is_open
	 */
	public boolean isIsOpen() {
		return isOpen;
	}

	/**
	 * is_openを設定する
	 * @param is_open
	 */
	public void setIsOpen(boolean isOpen) {
		this.isOpen = isOpen;
	}

	/**
	 * counter_distributedを取得する
	 * @return counter_distributed
	 */
	public int getCounterDistributed() {
		return counterDistributed;
	}

	/**
	 * counter_distributedを設定する
	 * @param counter_distributed
	 */
	public void setCounterDistributed(int counterDistributed) {
		this.counterDistributed = counterDistributed;
	}

	/**
	 * counter_favoriteを取得する
	 * @return counter_favorite
	 */
	public int getCounterFavorite() {
		return counterFavorite;
	}

	/**
	 * counter_favoriteを設定する
	 * @param counter_favorite
	 */
	public void setCounterFavorite(int counterFavorite) {
		this.counterFavorite = counterFavorite;
	}

	/**
	 * counter_usedを取得する
	 * @return counter_used
	 */
	public int getCounterUsed() {
		return counterUsed;
	}

	/**
	 * counter_usedを設定する
	 * @param counter_used
	 */
	public void setCounterUsed(int counterUsed) {
		this.counterUsed = counterUsed;
	}

	/**
	 * created_atを取得する
	 * @return created_at
	 */
	public String getCreatedAt() {
		return createdAt;
	}

	/**
	 * created_atを設定する
	 * @param created_at
	 */
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * updated_atを取得する
	 * @return updated_at
	 */
	public String getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * updated_atを設定する
	 * @param updated_at
	 */
	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

}