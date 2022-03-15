package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * その他クーポンテスト配信API Push通知設定INPUTDTO(センサーイベントクーポン用)
 */
public class CouponTestDeliveryApiInputDTOPushTarget {

	/** コンテンツタイプ */
	@JsonProperty("content_type")
	private String contentType;

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

}
