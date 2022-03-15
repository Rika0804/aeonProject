package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * クーポン新規登録API Push通知設定INPUTDTO(センサーイベントクーポン用)
 */
public class CouponRegisterApiInputDTOPushTarget {

	/** 通知タイプ */
	@JsonProperty("type")
	private String type;

	/** コンテンツタイプ */
	@JsonProperty("content_type")
	private String contentType;

	/** プラットフォーム */
	@JsonProperty("platform")
	private String[] platform;

	/** Push通知本文 */
	@JsonProperty("popup")
	private String popup;

	/** 件名 */
	@JsonProperty("title")
	private String title;

	/** 本文 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("content")
	private String content;

	/** リンク先URL */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("url")
	private String url;

	/** アイコン */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("icon")
	private String icon;

	/** カテゴリー */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("category")
	private String category;

	/** 配信タイプ */
	@JsonProperty("delivery_type")
	private Integer deliveryType;

	/** 配布対象位置リスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("location")
	private List<CouponRegisterApiInputDTOLocation> location;

	/** bluetooth端末情報リスト */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("bluetooth")
	private List<CouponRegisterApiInputDTOBluetooth> bluetooth;

	/** 配布期間 */
	@JsonProperty("period")
	private List<CouponRegisterApiInputDTOPeriod> period;

	/** セグメントID */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("segmentation_id")
	private Long segmentationId;

	/**
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type セットする type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return contentType
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * @param contentType セットする contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @return platform
	 */
	public String[] getPlatform() {
		return platform;
	}

	/**
	 * @param platform セットする platform
	 */
	public void setPlatform(String[] platform) {
		this.platform = platform;
	}

	/**
	 * @return popup
	 */
	public String getPopup() {
		return popup;
	}

	/**
	 * @param popup セットする popup
	 */
	public void setPopup(String popup) {
		this.popup = popup;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title セットする title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content セットする content
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url セットする url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return icon
	 */
	public String getIcon() {
		return icon;
	}

	/**
	 * @param icon セットする icon
	 */
	public void setIcon(String icon) {
		this.icon = icon;
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

	/**
	 * @return deliveryType
	 */
	public Integer getDeliveryType() {
		return deliveryType;
	}

	/**
	 * @param deliveryType セットする deliveryType
	 */
	public void setDeliveryType(Integer deliveryType) {
		this.deliveryType = deliveryType;
	}

	/**
	 * @return location
	 */
	public List<CouponRegisterApiInputDTOLocation> getLocation() {
		return location;
	}

	/**
	 * @param location セットする location
	 */
	public void setLocation(List<CouponRegisterApiInputDTOLocation> location) {
		this.location = location;
	}

	/**
	 * @return bluetooth
	 */
	public List<CouponRegisterApiInputDTOBluetooth> getBluetooth() {
		return bluetooth;
	}

	/**
	 * @param bluetooth セットする bluetooth
	 */
	public void setBluetooth(List<CouponRegisterApiInputDTOBluetooth> bluetooth) {
		this.bluetooth = bluetooth;
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
	 * @return segmentationId
	 */
	public Long getSegmentationId() {
		return segmentationId;
	}

	/**
	 * @param segmentationId セットする segmentationId
	 */
	public void setSegmentationId(Long segmentationId) {
		this.segmentationId = segmentationId;
	}

}
